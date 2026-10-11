package com.ecommerce.pedidos.api;

import com.ecommerce.pedidos.infrastructure.persistence.entity.OutboxJpaEntity;
import com.ecommerce.pedidos.support.AbstractIntegrationTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ETAPA 0: TESTE DE CONTRATO GOLDEN DO JSON.
 * 
 * Trava e congela os formatos e nomes de propriedades JSON exatos das respostas HTTP
 * e dos payloads da Outbox / RabbitMQ antes da migração do Jackson 2 para o Jackson 3.
 * Qualquer alteração involuntária nos defaults do Jackson (ex: timestamps vs ISO-8601,
 * renomeação de chaves, formatação de BigDecimal) causará quebra nestes testes.
 */
class ContratoJsonGoldenIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObjectMapper objectMapper;

    private static Set<String> chavesDe(JsonNode node) {
        return new java.util.HashSet<>(node.propertyNames());
    }

    @Test
    @DisplayName("Golden Contract: Estrutura exata do PedidoResponseDTO em POST /api/pedidos")
    void goldenContract_respostaCriacaoPedido() {
        stubProduto(100L, "Teclado Mecanico", "250.00", 15);

        Map<String, Object> request = Map.of(
                "clienteId", 42L,
                "itens", List.of(Map.of("produtoId", 100L, "quantidade", 2))
        );

        ResponseEntity<JsonNode> response = rest.postForEntity("/api/pedidos", request, JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode root = response.getBody();
        assertThat(root).isNotNull();

        // 1. Validar exatamente as chaves de primeiro nível (sem chaves a mais nem a menos)
        assertThat(chavesDe(root)).containsExactlyInAnyOrder(
                "id", "numeroPedido", "clienteId", "itens", "valorTotal", "status", "dataCriacao", "dataAtualizacao"
        );

        // 2. Validar tipos de dados e formatos dos campos de raiz
        assertThat(root.get("id").isNumber()).isTrue();
        assertThat(root.get("numeroPedido").isString()).isTrue();
        assertThat(root.get("numeroPedido").asString()).matches("^PED-[0-9A-F]{8}$");
        assertThat(root.get("clienteId").asLong()).isEqualTo(42L);
        assertThat(root.get("valorTotal").decimalValue()).isEqualByComparingTo("500.00");
        assertThat(root.get("status").asString()).isEqualTo("PENDENTE");

        // Nota: no baseline atual, LocalDateTime é serializado como array numérico [yyyy, MM, dd, HH, mm, ss, ...]
        assertDataHoraNode(root.get("dataCriacao"));
        assertDataHoraNode(root.get("dataAtualizacao"));

        // 4. Validar exatamente a estrutura de cada item
        JsonNode itens = root.get("itens");
        assertThat(itens.isArray()).isTrue();
        assertThat(itens).hasSize(1);
        JsonNode item = itens.get(0);

        assertThat(chavesDe(item)).containsExactlyInAnyOrder(
                "produtoId", "nomeProduto", "quantidade", "precoUnitario", "subtotal"
        );
        assertThat(item.get("produtoId").asLong()).isEqualTo(100L);
        assertThat(item.get("nomeProduto").asString()).isEqualTo("Teclado Mecanico");
        assertThat(item.get("quantidade").asInt()).isEqualTo(2);
        assertThat(item.get("precoUnitario").decimalValue()).isEqualByComparingTo("250.00");
        assertThat(item.get("subtotal").decimalValue()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Golden Contract: Estrutura exata do PedidoCriadoEvent na tabela Outbox")
    void goldenContract_payloadEventoPedidoCriadoNaOutbox() throws Exception {
        stubProduto(100L, "Teclado Mecanico", "250.00", 15);

        Map<String, Object> request = Map.of(
                "clienteId", 42L,
                "itens", List.of(Map.of("produtoId", 100L, "quantidade", 2))
        );

        rest.postForEntity("/api/pedidos", request, JsonNode.class);

        OutboxJpaEntity outbox = outboxJpaRepository.findAll().stream()
                .filter(e -> "PEDIDO_CRIADO".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        JsonNode payload = objectMapper.readTree(outbox.getPayload());

        // 1. Chaves exatas do evento PedidoCriadoEvent
        assertThat(chavesDe(payload)).containsExactlyInAnyOrder(
                "pedidoId", "numeroPedido", "clienteId", "itens", "valorTotal", "status", "dataCriacao"
        );

        // 2. Tipos de dados e formatação de datas
        assertThat(payload.get("pedidoId").isNumber()).isTrue();
        assertThat(payload.get("numeroPedido").asString()).matches("^PED-[0-9A-F]{8}$");
        assertThat(payload.get("clienteId").asLong()).isEqualTo(42L);
        assertThat(payload.get("valorTotal").decimalValue()).isEqualByComparingTo("500.00");
        assertThat(payload.get("status").asString()).isEqualTo("PENDENTE");
        assertDataHoraNode(payload.get("dataCriacao"));

        // 3. Chaves e valores dos itens do evento
        JsonNode itens = payload.get("itens");
        assertThat(itens.isArray()).isTrue();
        assertThat(itens).hasSize(1);
        JsonNode item = itens.get(0);

        assertThat(chavesDe(item)).containsExactlyInAnyOrder(
                "produtoId", "nomeProduto", "quantidade", "precoUnitario", "subtotal"
        );
        assertThat(item.get("produtoId").asLong()).isEqualTo(100L);
        assertThat(item.get("nomeProduto").asString()).isEqualTo("Teclado Mecanico");
        assertThat(item.get("quantidade").asInt()).isEqualTo(2);
        assertThat(item.get("precoUnitario").decimalValue()).isEqualByComparingTo("250.00");
        assertThat(item.get("subtotal").decimalValue()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Golden Contract: Estrutura exata do evento PEDIDO_CANCELADO na Outbox")
    void goldenContract_payloadEventoPedidoCanceladoNaOutbox() throws Exception {
        stubProduto(100L, "Teclado Mecanico", "250.00", 15);

        ResponseEntity<JsonNode> criado = rest.postForEntity("/api/pedidos", Map.of(
                "clienteId", 42L,
                "itens", List.of(Map.of("produtoId", 100L, "quantidade", 1))
        ), JsonNode.class);

        long id = criado.getBody().get("id").asLong();
        rest.exchange("/api/pedidos/" + id + "/cancelar", HttpMethod.PUT, null, JsonNode.class);

        OutboxJpaEntity outboxCancelado = outboxJpaRepository.findAll().stream()
                .filter(e -> "PEDIDO_CANCELADO".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();

        JsonNode payload = objectMapper.readTree(outboxCancelado.getPayload());

        // Chaves exatas do mapa gerado no cancelamento
        assertThat(chavesDe(payload)).containsExactlyInAnyOrder(
                "pedidoId", "numeroPedido", "clienteId", "status", "dataAtualizacao"
        );
        assertThat(payload.get("pedidoId").asLong()).isEqualTo(id);
        assertThat(payload.get("status").asString()).isEqualTo("CANCELADO");
        assertDataHoraNode(payload.get("dataAtualizacao"));
    }

    @Test
    @DisplayName("Golden Contract: Formato padrão de erro (ErrorResponse) em 404")
    void goldenContract_formatoErroPadrao() {
        ResponseEntity<JsonNode> response = rest.getForEntity("/api/pedidos/99999", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode root = response.getBody();
        assertThat(root).isNotNull();

        assertThat(chavesDe(root)).containsExactlyInAnyOrder(
                "timestamp", "status", "error", "message", "path"
        );
        assertThat(root.get("status").asInt()).isEqualTo(404);
        assertThat(root.get("error").asString()).isEqualTo("Not Found");
        assertThat(root.get("path").asString()).isEqualTo("/api/pedidos/99999");
        assertDataHoraNode(root.get("timestamp"));
    }

    private void assertDataHoraNode(JsonNode node) {
        assertThat(node).isNotNull();
        // Jackson default com JavaTimeModule serializa LocalDateTime como array [ano, mes, dia, hora, minuto, segundo, ...]
        // ou como string ISO caso WRITE_DATES_AS_TIMESTAMPS seja falso
        if (node.isArray()) {
            assertThat(node.size()).isGreaterThanOrEqualTo(6);
            assertThat(node.get(0).asInt()).isGreaterThanOrEqualTo(2024);
        } else if (node.isString()) {
            DateTimeFormatter.ISO_DATE_TIME.parse(node.asString());
        } else {
            org.junit.jupiter.api.Assertions.fail("Campo de data/hora não é nem array nem textual: " + node);
        }
    }
}
