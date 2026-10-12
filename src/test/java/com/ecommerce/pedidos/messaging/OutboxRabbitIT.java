package com.ecommerce.pedidos.messaging;

import com.ecommerce.pedidos.domain.entity.OutboxStatus;
import com.ecommerce.pedidos.infrastructure.config.RabbitMQConfig;
import com.ecommerce.pedidos.infrastructure.messaging.OutboxProcessor;
import com.ecommerce.pedidos.infrastructure.persistence.entity.OutboxJpaEntity;
import com.ecommerce.pedidos.support.AbstractIntegrationTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpMethod;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TESTES DE CARACTERIZAÇÃO do fluxo Outbox -> RabbitMQ (broker real).
 * O job é acionado manualmente (fixed-delay do perfil de teste é 1h) para ser determinístico.
 */
class OutboxRabbitIT extends AbstractIntegrationTest {

    @Autowired
    private OutboxProcessor outboxProcessor;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RabbitAdmin rabbitAdmin;
    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void limparFilas() {
        rabbitAdmin.purgeQueue(RabbitMQConfig.PEDIDOS_CRIADOS_QUEUE, false);
        rabbitAdmin.purgeQueue(RabbitMQConfig.PEDIDOS_DLQ, false);
    }

    private long criarPedido() {
        stubProduto(1L, "Notebook", "2500.00", 10);
        Map<String, Object> req = Map.of("clienteId", 7L,
                "itens", List.of(Map.of("produtoId", 1L, "quantidade", 2)));
        return rest.postForEntity("/api/pedidos", req, JsonNode.class).getBody().get("id").asLong();
    }

    private OutboxJpaEntity eventoDoTipo(String tipo) {
        return outboxJpaRepository.findAll().stream()
                .filter(e -> e.getEventType().equals(tipo)).findFirst().orElseThrow();
    }

    @Test
    void processar_publicaPedidoCriadoNaFilaEMarcaComoProcessado() throws Exception {
        long id = criarPedido();

        outboxProcessor.processarEventosPendentes();

        OutboxJpaEntity evento = eventoDoTipo("PEDIDO_CRIADO");
        assertThat(evento.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        assertThat(evento.getProcessedAt()).isNotNull();

        Message msg = rabbitTemplate.receive(RabbitMQConfig.PEDIDOS_CRIADOS_QUEUE, 5000);
        assertThat(msg).as("mensagem na fila pedidos.criados.queue").isNotNull();
        assertThat(msg.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(msg.getMessageProperties().getHeaders())
                .containsEntry("event_type", "PEDIDO_CRIADO")
                .containsKeys("timestamp", "correlation_id");

        JsonNode payload = objectMapper.readTree(new String(msg.getBody(), StandardCharsets.UTF_8));
        assertThat(payload.get("pedidoId").asLong()).isEqualTo(id);
        assertThat(payload.get("clienteId").asLong()).isEqualTo(7L);
        assertThat(payload.get("numeroPedido").asString()).startsWith("PED-");
        assertThat(payload.get("status").asString()).isEqualTo("PENDENTE");
        assertThat(payload.get("valorTotal").decimalValue()).isEqualByComparingTo("5000.00");
        assertThat(payload.get("itens")).hasSize(1);
    }

    @Test
    void processar_semPendentes_naoFazNada() {
        outboxProcessor.processarEventosPendentes();

        assertThat(rabbitTemplate.receive(RabbitMQConfig.PEDIDOS_CRIADOS_QUEUE, 500)).isNull();
    }

    @Test
    void processar_eventoJaProcessadoNaoEhRepublicado() {
        criarPedido();
        outboxProcessor.processarEventosPendentes();
        assertThat(rabbitTemplate.receive(RabbitMQConfig.PEDIDOS_CRIADOS_QUEUE, 5000)).isNotNull();

        outboxProcessor.processarEventosPendentes();

        assertThat(rabbitTemplate.receive(RabbitMQConfig.PEDIDOS_CRIADOS_QUEUE, 500)).isNull();
    }

    @Test
    void processar_respeitaTamanhoDoLote() {
        // batch-size configurado em application.properties = 10
        for (long i = 1; i <= 12; i++) {
            OutboxJpaEntity e = new OutboxJpaEntity();
            e.setAggregateType("PEDIDO");
            e.setAggregateId(i);
            e.setEventType("PEDIDO_CRIADO");
            e.setPayload("{\"n\":" + i + "}");
            e.setStatus(OutboxStatus.PENDING);
            e.setCreatedAt(LocalDateTime.now().plusSeconds(i));
            e.setRetryCount(0);
            outboxJpaRepository.save(e);
        }

        outboxProcessor.processarEventosPendentes();

        assertThat(outboxJpaRepository.findByStatus(OutboxStatus.PROCESSED)).hasSize(10);
        assertThat(outboxJpaRepository.findByStatus(OutboxStatus.PENDING)).hasSize(2);
    }

    @Test
    void cancelamento_eventoPublicadoEEntregueNaFilaDeCancelados() {
        long id = criarPedido();
        rest.exchange("/api/pedidos/" + id + "/cancelar", HttpMethod.PUT, null, JsonNode.class);

        outboxProcessor.processarEventosPendentes();

        assertThat(eventoDoTipo("PEDIDO_CANCELADO").getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        assertThat(rabbitTemplate.receive(RabbitMQConfig.PEDIDOS_CRIADOS_QUEUE, 5000)).isNotNull();
        Message msgCancelado = rabbitTemplate.receive(RabbitMQConfig.PEDIDOS_CANCELADOS_QUEUE, 5000);
        assertThat(msgCancelado).isNotNull();
    }

    @Test
    void limpeza_removeEventosProcessadosComSucesso() {
        criarPedido();
        outboxProcessor.processarEventosPendentes();
        assertThat(outboxJpaRepository.findByStatus(OutboxStatus.PROCESSED)).hasSize(1);

        outboxProcessor.limparEventosProcessados();

        // Com o expurgo implementado e retenção 0 no perfil de teste, eventos processados são removidos
        assertThat(outboxJpaRepository.findByStatus(OutboxStatus.PROCESSED)).isEmpty();
    }

    @Test
    void topologia_exchangeFilasEDlqExistemNoBroker() {
        assertThat(rabbitAdmin.getQueueProperties(RabbitMQConfig.PEDIDOS_CRIADOS_QUEUE)).isNotNull();
        assertThat(rabbitAdmin.getQueueProperties(RabbitMQConfig.PEDIDOS_CANCELADOS_QUEUE)).isNotNull();
        assertThat(rabbitAdmin.getQueueProperties(RabbitMQConfig.PEDIDOS_DLQ)).isNotNull();
    }
}
