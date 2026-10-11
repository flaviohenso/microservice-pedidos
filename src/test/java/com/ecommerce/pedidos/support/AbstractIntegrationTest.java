package com.ecommerce.pedidos.support;

import com.ecommerce.pedidos.infrastructure.persistence.repository.OutboxJpaRepository;
import com.ecommerce.pedidos.infrastructure.persistence.repository.PedidoJpaRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Base dos testes de integração: Spring completo + PostgreSQL/RabbitMQ reais + WireMock
 * no lugar do microserviço de produtos (e do serviço de fallback).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(ContainersConfig.class)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final WireMockServer produtosApi =
            new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static {
        produtosApi.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("produto.service.url", () -> produtosApi.baseUrl() + "/api/produtos");
        registry.add("fallback.service.url", () -> produtosApi.baseUrl() + "/fallback/produtos");
    }

    @Autowired
    protected PedidoJpaRepository pedidoJpaRepository;

    @Autowired
    protected OutboxJpaRepository outboxJpaRepository;

    @Autowired
    protected CacheManager cacheManager;

    @BeforeEach
    void limparEstado() {
        produtosApi.resetAll();
        outboxJpaRepository.deleteAll();
        pedidoJpaRepository.deleteAll();
        cacheManager.getCacheNames().forEach(n -> cacheManager.getCache(n).clear());
    }

    @AfterAll
    static void noop() {
        // WireMock é singleton para toda a JVM de testes; encerrado junto com ela
    }

    /** Stub do serviço de produtos: GET /api/produtos/{id} -> 200 com o produto. */
    protected static void stubProduto(long id, String nome, String preco, int estoque) {
        produtosApi.stubFor(get(urlEqualTo("/api/produtos/" + id))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id": %d, "nome": "%s", "descricao": "desc", "preco": %s,
                                 "estoque": %d, "categoria": "cat"}
                                """.formatted(id, nome, preco, estoque))));
    }
}
