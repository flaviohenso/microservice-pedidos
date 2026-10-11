package com.ecommerce.pedidos.persistence;

import com.ecommerce.pedidos.domain.entity.ItemPedido;
import com.ecommerce.pedidos.domain.entity.OutboxEvent;
import com.ecommerce.pedidos.domain.entity.OutboxStatus;
import com.ecommerce.pedidos.domain.entity.Pedido;
import com.ecommerce.pedidos.domain.entity.StatusPedido;
import com.ecommerce.pedidos.domain.repository.OutboxRepositoryPort;
import com.ecommerce.pedidos.domain.repository.PedidoRepositoryPort;
import com.ecommerce.pedidos.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TESTES DE CARACTERIZAÇÃO da persistência (portas de domínio + JPA + PostgreSQL reais).
 */
class PersistenciaIT extends AbstractIntegrationTest {

    @Autowired
    private PedidoRepositoryPort pedidoRepository;

    @Autowired
    private OutboxRepositoryPort outboxRepository;

    private Pedido novoPedido(long clienteId) {
        return new Pedido(clienteId, List.of(
                new ItemPedido(1L, "Notebook", 2, new BigDecimal("2500.00")),
                new ItemPedido(2L, "Mouse", 1, new BigDecimal("50.50"))));
    }

    // ---------- Pedido ----------

    @Test
    void salvar_geraIdsEPreservaTodosOsCampos() {
        Pedido original = novoPedido(10L);

        Pedido salvo = pedidoRepository.salvar(original);

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getNumeroPedido()).isEqualTo(original.getNumeroPedido());
        assertThat(salvo.getClienteId()).isEqualTo(10L);
        assertThat(salvo.getStatus()).isEqualTo(StatusPedido.PENDENTE);
        assertThat(salvo.getItens()).hasSize(2);
        assertThat(salvo.getItens()).allSatisfy(i -> assertThat(i.getId()).isNotNull());
        assertThat(salvo.calcularTotal()).isEqualByComparingTo("5050.50");
        assertThat(salvo.getDataCriacao()).isNotNull();
    }

    @Test
    void buscarPorId_reconstituiPedidoComItens() {
        Long id = pedidoRepository.salvar(novoPedido(10L)).getId();

        Pedido buscado = pedidoRepository.buscarPorId(id).orElseThrow();

        assertThat(buscado.getItens()).extracting(ItemPedido::getNomeProduto)
                .containsExactlyInAnyOrder("Notebook", "Mouse");
        assertThat(buscado.getItens()).extracting(ItemPedido::getQuantidade)
                .containsExactlyInAnyOrder(2, 1);
        assertThat(buscado.calcularTotal()).isEqualByComparingTo("5050.50");
    }

    @Test
    void buscarPorId_inexistente_retornaVazio() {
        assertThat(pedidoRepository.buscarPorId(987654L)).isEmpty();
    }

    @Test
    void buscarPorNumeroPedido_encontraPedido() {
        Pedido salvo = pedidoRepository.salvar(novoPedido(10L));

        assertThat(pedidoRepository.buscarPorNumeroPedido(salvo.getNumeroPedido()))
                .get().extracting(Pedido::getId).isEqualTo(salvo.getId());
        assertThat(pedidoRepository.buscarPorNumeroPedido("PED-NAOEXISTE")).isEmpty();
    }

    @Test
    void listarPorClienteId_eListarTodos() {
        pedidoRepository.salvar(novoPedido(1L));
        pedidoRepository.salvar(novoPedido(1L));
        pedidoRepository.salvar(novoPedido(2L));

        assertThat(pedidoRepository.listarTodos()).hasSize(3);
        assertThat(pedidoRepository.listarPorClienteId(1L)).hasSize(2);
        assertThat(pedidoRepository.listarPorClienteId(2L)).hasSize(1);
        assertThat(pedidoRepository.listarPorClienteId(99L)).isEmpty();
    }

    @Test
    void atualizar_persisteMudancaDeStatusESemDuplicarItens() {
        Pedido salvo = pedidoRepository.salvar(novoPedido(10L));
        salvo.cancelar();

        pedidoRepository.atualizar(salvo);

        Pedido recarregado = pedidoRepository.buscarPorId(salvo.getId()).orElseThrow();
        assertThat(recarregado.getStatus()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(recarregado.getItens()).hasSize(2);
        assertThat(pedidoJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void deletar_removePedidoEItensEmCascata() {
        Long id = pedidoRepository.salvar(novoPedido(10L)).getId();

        pedidoRepository.deletar(id);

        assertThat(pedidoRepository.buscarPorId(id)).isEmpty();
    }

    @Test
    void numeroPedido_eUnico() {
        Pedido a = novoPedido(1L);
        pedidoRepository.salvar(a);

        // reconstitui um pedido diferente com o mesmo número -> viola unique
        Pedido duplicado = new Pedido(null, a.getNumeroPedido(), 2L,
                List.of(new ItemPedido(1L, "X", 1, BigDecimal.TEN)),
                StatusPedido.PENDENTE, a.getDataCriacao(), a.getDataAtualizacao());

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> pedidoRepository.salvar(duplicado));
    }

    // ---------- Outbox ----------

    @Test
    void outbox_salvarEBuscarPorStatus() {
        OutboxEvent salvo = outboxRepository.salvar(new OutboxEvent("PEDIDO", 1L, "PEDIDO_CRIADO", "{\"a\":1}"));

        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(salvo.getRetryCount()).isZero();
        assertThat(outboxRepository.buscarPorStatus(OutboxStatus.PENDING)).hasSize(1);
        assertThat(outboxRepository.buscarPorStatus(OutboxStatus.PROCESSED)).isEmpty();
    }

    @Test
    void outbox_pendentesPaginado_respeitaLimiteEOrdemDeCriacao() {
        for (long i = 1; i <= 5; i++) {
            outboxRepository.salvar(new OutboxEvent("PEDIDO", i, "PEDIDO_CRIADO", "{\"n\":" + i + "}"));
        }

        List<OutboxEvent> pendentes = outboxRepository.buscarPendentesPaginado(3);

        assertThat(pendentes).hasSize(3);
        assertThat(pendentes).extracting(OutboxEvent::getAggregateId).containsExactly(1L, 2L, 3L);
    }

    @Test
    void outbox_atualizarPersisteStatusProcessadoEDataDeProcessamento() {
        OutboxEvent evento = outboxRepository.salvar(new OutboxEvent("PEDIDO", 1L, "PEDIDO_CRIADO", "{}"));
        evento.marcarComoProcessado();

        outboxRepository.atualizar(evento);

        assertThat(outboxRepository.buscarPorStatus(OutboxStatus.PROCESSED)).hasSize(1);
        assertThat(outboxRepository.buscarPorStatus(OutboxStatus.PROCESSED).get(0).getProcessedAt()).isNotNull();
        assertThat(outboxRepository.buscarPendentesPaginado(10)).isEmpty();
    }

    @Test
    void outbox_payloadLongoEhPersistidoIntegralmente() {
        String payload = "{\"d\":\"" + "x".repeat(20_000) + "\"}";
        OutboxEvent salvo = outboxRepository.salvar(new OutboxEvent("PEDIDO", 1L, "PEDIDO_CRIADO", payload));

        assertThat(outboxRepository.buscarPorStatus(OutboxStatus.PENDING).get(0).getPayload())
                .isEqualTo(payload);
        assertThat(salvo.getId()).isNotNull();
    }
}
