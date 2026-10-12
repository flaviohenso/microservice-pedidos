package com.ecommerce.pedidos.api;

import com.ecommerce.pedidos.domain.entity.OutboxStatus;
import com.ecommerce.pedidos.infrastructure.persistence.entity.OutboxJpaEntity;
import com.ecommerce.pedidos.support.AbstractIntegrationTest;
import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TESTES DE CARACTERIZAÇÃO da API REST.
 * Registram o comportamento ATUAL (inclusive defeitos conhecidos, marcados com "DEFEITO CONHECIDO")
 * para garantir que a refatoração não quebre o que já funciona.
 * Quando um defeito for corrigido de propósito, o teste correspondente deve ser atualizado.
 */
class PedidoApiIT extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    // ---------- helpers ----------

    private org.springframework.http.HttpHeaders authHeaders(Long clienteId, String... roles) {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        String token = (roles.length > 0)
                ? jwtTestHelper.gerarToken(clienteId, roles)
                : jwtTestHelper.gerarToken(clienteId, "USER");
        headers.setBearerAuth(token);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return headers;
    }

    private org.springframework.http.HttpHeaders adminHeaders() {
        return authHeaders(999L, "ADMIN");
    }

    private ResponseEntity<JsonNode> post(Object body) {
        // Extrai clienteId se existir para autenticar como o próprio cliente
        Long clienteId = 7L;
        if (body instanceof Map<?, ?> map && map.get("clienteId") instanceof Number num) {
            clienteId = num.longValue();
        }
        var entity = new org.springframework.http.HttpEntity<>(body, authHeaders(clienteId));
        return rest.postForEntity("/api/pedidos", entity, JsonNode.class);
    }

    private Map<String, Object> pedidoRequest(Long clienteId, Object... produtoQtd) {
        List<Map<String, Object>> itens = new java.util.ArrayList<>();
        for (int i = 0; i < produtoQtd.length; i += 2) {
            itens.add(Map.of("produtoId", produtoQtd[i], "quantidade", produtoQtd[i + 1]));
        }
        Map<String, Object> req = new java.util.HashMap<>();
        req.put("clienteId", clienteId);
        req.put("itens", itens);
        return req;
    }

    private long criarPedidoValido() {
        stubProduto(1L, "Notebook", "2500.00", 10);
        ResponseEntity<JsonNode> resp = post(pedidoRequest(7L, 1L, 2));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().get("id").asLong();
    }

    // ---------- POST /api/pedidos ----------

    @Test
    void criarPedido_retorna201ComTotalEItensCalculados() {
        stubProduto(1L, "Notebook", "2500.00", 10);
        stubProduto(2L, "Mouse", "50.00", 5);

        ResponseEntity<JsonNode> resp = post(pedidoRequest(7L, 1L, 2, 2L, 1));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode body = resp.getBody();
        assertThat(body.get("id").asLong()).isPositive();
        assertThat(body.get("numeroPedido").asString()).matches("PED-[0-9A-F]{8}");
        assertThat(body.get("clienteId").asLong()).isEqualTo(7L);
        assertThat(body.get("status").asString()).isEqualTo("PENDENTE");
        assertThat(body.get("valorTotal").decimalValue()).isEqualByComparingTo("5050.00");
        assertThat(body.get("itens")).hasSize(2);
        assertThat(body.get("itens").get(0).get("nomeProduto").asString()).isEqualTo("Notebook");
        assertThat(body.get("itens").get(0).get("subtotal").decimalValue()).isEqualByComparingTo("5000.00");
        assertThat(body.get("dataCriacao")).isNotNull();
    }

    @Test
    void criarPedido_persistePedidoEEventoOutboxPendente() {
        long id = criarPedidoValido();

        assertThat(pedidoJpaRepository.findById(id)).isPresent();
        List<OutboxJpaEntity> outbox = outboxJpaRepository.findAll();
        assertThat(outbox).hasSize(1);
        OutboxJpaEntity evento = outbox.get(0);
        assertThat(evento.getEventType()).isEqualTo("PEDIDO_CRIADO");
        assertThat(evento.getAggregateType()).isEqualTo("PEDIDO");
        assertThat(evento.getAggregateId()).isEqualTo(id);
        assertThat(evento.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(evento.getRetryCount()).isZero();
        assertThat(evento.getPayload())
                .contains("\"numeroPedido\"")
                .matches("(?s).*\"clienteId\"\\s*:\\s*7.*");
    }

    @Test
    void criarPedido_usaPrecoEDadosDoServicoDeProdutos() {
        stubProduto(1L, "Teclado", "199.90", 3);

        ResponseEntity<JsonNode> resp = post(pedidoRequest(1L, 1L, 3));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode item = resp.getBody().get("itens").get(0);
        assertThat(item.get("precoUnitario").decimalValue()).isEqualByComparingTo("199.90");
        assertThat(item.get("subtotal").decimalValue()).isEqualByComparingTo("599.70");
    }

    @Test
    void criarPedido_estoqueInsuficiente_retorna422SemPersistir() {
        stubProduto(1L, "Notebook", "2500.00", 1);

        ResponseEntity<JsonNode> resp = post(pedidoRequest(7L, 1L, 5));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(resp.getBody().get("message").asString()).contains("Estoque insuficiente");
        assertThat(pedidoJpaRepository.count()).isZero();
        assertThat(outboxJpaRepository.count()).isZero();
    }

    @Test
    void criarPedido_produtoNaoEncontrado_retornaErroENaoPersiste() {
        produtosApi.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(
                        com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/api/produtos/999"))
                .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse().withStatus(404)));

        ResponseEntity<JsonNode> resp = post(pedidoRequest(7L, 999L, 1));

        // Status exato fixado em ProdutoResilienciaIT; aqui garantimos que nada é persistido
        assertThat(resp.getStatusCode().is2xxSuccessful()).isFalse();
        assertThat(pedidoJpaRepository.count()).isZero();
        assertThat(outboxJpaRepository.count()).isZero();
    }

    @Test
    void criarPedido_quantidadeZero_retorna400() {
        stubProduto(1L, "Notebook", "2500.00", 10);

        ResponseEntity<JsonNode> resp = post(pedidoRequest(7L, 1L, 0));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(pedidoJpaRepository.count()).isZero();
    }

    @Test
    void criarPedido_clienteIdNulo_retorna400() {
        stubProduto(1L, "Notebook", "2500.00", 10);

        ResponseEntity<JsonNode> resp = post(pedidoRequest(null, 1L, 1));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().toString()).containsIgnoringCase("cliente");
    }

    @Test
    void criarPedido_listaDeItensVazia_retorna400() {
        ResponseEntity<JsonNode> resp = post(pedidoRequest(7L));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void criarPedido_itensNulo_retorna400() {
        Map<String, Object> req = new java.util.HashMap<>();
        req.put("clienteId", 7L);
        req.put("itens", null);

        ResponseEntity<JsonNode> resp = post(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void criarPedido_clienteIdNegativo_retorna400() {
        stubProduto(1L, "Notebook", "2500.00", 10);

        ResponseEntity<JsonNode> resp = post(pedidoRequest(-5L, 1L, 1));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().toString()).containsIgnoringCase("positivo");
        assertThat(pedidoJpaRepository.count()).isZero();
    }

    // ---------- GET ----------

    @Test
    void buscarPorId_retorna200ComPedidoCompleto() {
        long id = criarPedidoValido();

        var entity = new org.springframework.http.HttpEntity<>(null, authHeaders(7L));
        ResponseEntity<JsonNode> resp = rest.exchange("/api/pedidos/" + id, HttpMethod.GET, entity, JsonNode.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("id").asLong()).isEqualTo(id);
        assertThat(resp.getBody().get("itens")).hasSize(1);
        assertThat(resp.getBody().get("valorTotal").decimalValue()).isEqualByComparingTo("5000.00");
    }

    @Test
    void buscarPorId_inexistente_retorna404ComCorpoPadronizado() {
        var entity = new org.springframework.http.HttpEntity<>(null, authHeaders(7L));
        ResponseEntity<JsonNode> resp = rest.exchange("/api/pedidos/999999", HttpMethod.GET, entity, JsonNode.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode body = resp.getBody();
        assertThat(body.get("status").asInt()).isEqualTo(404);
        assertThat(body.get("error").asString()).isEqualTo("Not Found");
        assertThat(body.get("message").asString()).contains("999999");
        assertThat(body.get("path").asString()).isEqualTo("/api/pedidos/999999");
        assertThat(body.get("timestamp")).isNotNull();
    }

    @Test
    void listarTodos_retornaTodosOsPedidos() {
        criarPedidoValido();
        criarPedidoValido();

        var entity = new org.springframework.http.HttpEntity<>(null, adminHeaders());
        ResponseEntity<JsonNode> resp = rest.exchange("/api/pedidos", HttpMethod.GET, entity, JsonNode.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().isArray()).isTrue();
        assertThat(resp.getBody()).hasSize(2);
    }

    @Test
    void listarPorCliente_filtraPorClienteId() {
        stubProduto(1L, "Notebook", "2500.00", 10);
        post(pedidoRequest(1L, 1L, 1));
        post(pedidoRequest(1L, 1L, 1));
        post(pedidoRequest(2L, 1L, 1));

        JsonNode cliente1 = rest.exchange("/api/pedidos/cliente/1", HttpMethod.GET, new org.springframework.http.HttpEntity<>(null, authHeaders(1L)), JsonNode.class).getBody();
        JsonNode cliente2 = rest.exchange("/api/pedidos/cliente/2", HttpMethod.GET, new org.springframework.http.HttpEntity<>(null, authHeaders(2L)), JsonNode.class).getBody();
        JsonNode cliente3 = rest.exchange("/api/pedidos/cliente/3", HttpMethod.GET, new org.springframework.http.HttpEntity<>(null, authHeaders(3L)), JsonNode.class).getBody();

        assertThat(cliente1).hasSize(2);
        assertThat(cliente2).hasSize(1);
        assertThat(cliente3).isEmpty();
    }

    // ---------- PUT /{id}/cancelar ----------

    @Test
    void cancelar_retorna200ComStatusCanceladoERegistraEventoOutbox() {
        long id = criarPedidoValido();

        var entity = new org.springframework.http.HttpEntity<>(null, authHeaders(7L));
        ResponseEntity<JsonNode> resp = rest.exchange(
                "/api/pedidos/" + id + "/cancelar", HttpMethod.PUT, entity, JsonNode.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("status").asString()).isEqualTo("CANCELADO");
        assertThat(outboxJpaRepository.findAll())
                .extracting(OutboxJpaEntity::getEventType)
                .containsExactlyInAnyOrder("PEDIDO_CRIADO", "PEDIDO_CANCELADO");

        // persistido
        JsonNode buscado = rest.exchange("/api/pedidos/" + id, HttpMethod.GET, entity, JsonNode.class).getBody();
        assertThat(buscado.get("status").asString()).isEqualTo("CANCELADO");
    }

    @Test
    void cancelar_jaCancelado_retorna400() {
        long id = criarPedidoValido();
        var entity = new org.springframework.http.HttpEntity<>(null, authHeaders(7L));
        rest.exchange("/api/pedidos/" + id + "/cancelar", HttpMethod.PUT, entity, JsonNode.class);

        ResponseEntity<JsonNode> resp = rest.exchange(
                "/api/pedidos/" + id + "/cancelar", HttpMethod.PUT, entity, JsonNode.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().get("message").asString()).contains("já está cancelado");
    }

    @Test
    void listarTodos_comPaginacao_retornaLotePaginado() {
        criarPedidoValido();
        criarPedidoValido();
        criarPedidoValido();

        var entity = new org.springframework.http.HttpEntity<>(null, adminHeaders());
        ResponseEntity<JsonNode> pag0 = rest.exchange("/api/pedidos?pagina=0&tamanho=2", HttpMethod.GET, entity, JsonNode.class);
        assertThat(pag0.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pag0.getBody().isArray()).isTrue();
        assertThat(pag0.getBody()).hasSize(2);

        ResponseEntity<JsonNode> pag1 = rest.exchange("/api/pedidos?pagina=1&tamanho=2", HttpMethod.GET, entity, JsonNode.class);
        assertThat(pag1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pag1.getBody().isArray()).isTrue();
        assertThat(pag1.getBody()).hasSize(1);
    }

    @Test
    void endpointSemToken_retorna401Unauthorized() {
        ResponseEntity<String> resp = rest.getForEntity("/api/pedidos", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void idor_tentativaAcessarPedidoDeOutroCliente_retorna403Forbidden() {
        long id = criarPedidoValido(); // cliente 7L

        // Cliente 88L tentando acessar pedido do cliente 7L
        var entity = new org.springframework.http.HttpEntity<>(null, authHeaders(88L));
        ResponseEntity<JsonNode> resp = rest.exchange("/api/pedidos/" + id, HttpMethod.GET, entity, JsonNode.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody().get("message").asString()).contains("não tem permissão");
    }

    @Test
    void actuator_exposicaoRestrita_healthOkEEndpointsSensiveisNaoExpostos() {
        ResponseEntity<JsonNode> health = rest.getForEntity("/actuator/health", JsonNode.class);
        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> env = rest.getForEntity("/actuator/env", String.class);
        assertThat(env.getStatusCode().value()).isIn(HttpStatus.NOT_FOUND.value(), HttpStatus.UNAUTHORIZED.value());

        ResponseEntity<String> heapdump = rest.getForEntity("/actuator/heapdump", String.class);
        assertThat(heapdump.getStatusCode().value()).isIn(HttpStatus.NOT_FOUND.value(), HttpStatus.UNAUTHORIZED.value());
    }
}
