package com.ecommerce.pedidos.messaging;

import com.ecommerce.pedidos.domain.entity.OutboxEvent;
import com.ecommerce.pedidos.domain.entity.OutboxStatus;
import com.ecommerce.pedidos.domain.repository.OutboxRepositoryPort;
import com.ecommerce.pedidos.infrastructure.persistence.entity.OutboxJpaEntity;
import com.ecommerce.pedidos.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de concorrência com PostgreSQL real via Testcontainers:
 * Valida que o mecanismo FOR UPDATE SKIP LOCKED permite que múltiplos nós/threads
 * processem lotes concorrentes sem colisão de registros e sem duplicação.
 */
class OutboxConcorrenciaIT extends AbstractIntegrationTest {

    @Autowired
    private OutboxRepositoryPort outboxRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void buscarPendentesPaginado_comSkipLocked_duasTransacoesConcorrentesNaoColidem() throws Exception {
        // Cria 10 eventos pendentes no banco
        for (long i = 1; i <= 10; i++) {
            OutboxJpaEntity e = new OutboxJpaEntity();
            e.setAggregateType("PEDIDO");
            e.setAggregateId(i);
            e.setEventType("PEDIDO_CRIADO");
            e.setPayload("{\"id\":" + i + "}");
            e.setStatus(OutboxStatus.PENDING);
            e.setCreatedAt(LocalDateTime.now().plusSeconds(i));
            e.setRetryCount(0);
            outboxJpaRepository.save(e);
        }

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        CountDownLatch latchInicio = new CountDownLatch(1);
        CountDownLatch latchTx1Leu = new CountDownLatch(1);
        CountDownLatch latchTx2Terminou = new CountDownLatch(1);

        List<Long> idsProcessadosTx1 = Collections.synchronizedList(new ArrayList<>());
        List<Long> idsProcessadosTx2 = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Thread 1: Abre transação, lê 5 eventos com SKIP LOCKED e segura o lock até Thread 2 ler
        Future<?> f1 = executor.submit(() -> {
            txTemplate.execute(status -> {
                try {
                    latchInicio.await();
                    List<OutboxEvent> lote1 = outboxRepository.buscarPendentesPaginado(5);
                    lote1.forEach(e -> idsProcessadosTx1.add(e.getId()));
                    latchTx1Leu.countDown();
                    // Segura a transação e os locks até que a Thread 2 tenha tentado ler
                    latchTx2Terminou.await(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return null;
            });
        });

        // Thread 2: Executa concorrentemente enquanto Thread 1 ainda segura o lock nos 5 primeiros
        Future<?> f2 = executor.submit(() -> {
            txTemplate.execute(status -> {
                try {
                    latchInicio.await();
                    latchTx1Leu.await(); // Aguarda Thread 1 travar o primeiro lote
                    List<OutboxEvent> lote2 = outboxRepository.buscarPendentesPaginado(5);
                    lote2.forEach(e -> idsProcessadosTx2.add(e.getId()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    latchTx2Terminou.countDown();
                }
                return null;
            });
        });

        latchInicio.countDown();
        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Ambas conseguiram ler 5 itens
        assertThat(idsProcessadosTx1).hasSize(5);
        assertThat(idsProcessadosTx2).hasSize(5);

        // Não há nenhuma interseção entre os IDs: SKIP LOCKED pulou os registros da Thread 1 perfeitamente!
        Set<Long> intersecao = new HashSet<>(idsProcessadosTx1);
        intersecao.retainAll(idsProcessadosTx2);
        assertThat(intersecao).as("Nenhum ID deve ser compartilhado entre lotes concorrentes").isEmpty();
    }
}
