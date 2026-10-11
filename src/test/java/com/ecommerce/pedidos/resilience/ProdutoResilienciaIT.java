package com.ecommerce.pedidos.resilience;

import com.ecommerce.pedidos.application.service.ProdutoServicePort;
import com.ecommerce.pedidos.application.dto.ProdutoDTO;
import com.ecommerce.pedidos.support.AbstractIntegrationTest;
import com.github.tomakehurst.wiremock.http.Fault;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * TESTES DE CARACTERIZAÇÃO da integração com o serviço de produtos (Feign + cache + Resilience4j + fallback).
 * Exercitam o adapter diretamente (ProdutoServicePort) para isolar a camada de cliente.
 */
class ProdutoResilienciaIT extends AbstractIntegrationTest {

    @Autowired
    private ProdutoServicePort produtoService;
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetarCircuitBreaker() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> cb.reset());
    }

    private static int requisicoesPrimario(long id) {
        return produtosApi.findAll(getRequestedFor(urlEqualTo("/api/produtos/" + id))).size();
    }

    private static int requisicoesFallback(long id) {
        return produtosApi.findAll(getRequestedFor(urlEqualTo("/fallback/produtos/" + id))).size();
    }

    @Test
    void sucesso_retornaProdutoMapeadoDoJson() {
        stubProduto(1L, "Notebook", "2500.00", 10);

        Optional<ProdutoDTO> produto = produtoService.buscarProdutoPorId(1L);

        assertThat(produto).isPresent();
        assertThat(produto.get().nome()).isEqualTo("Notebook");
        assertThat(produto.get().preco()).isEqualByComparingTo("2500.00");
        assertThat(produto.get().estoque()).isEqualTo(10);
    }

    @Test
    void buscarProduto_segundaChamadaVemDoCache() {
        stubProduto(1L, "Notebook", "2500.00", 10);

        produtoService.buscarProdutoPorId(1L);
        produtoService.buscarProdutoPorId(1L);
        produtoService.buscarProdutoPorId(1L);

        assertThat(requisicoesPrimario(1L)).isEqualTo(1);
    }

    @Test
    void verificarEstoque_trueQuandoSuficienteEFalseQuandoInsuficiente() {
        stubProduto(1L, "Notebook", "2500.00", 5);

        assertThat(produtoService.verificarEstoque(1L, 5)).isTrue();
        assertThat(produtoService.verificarEstoque(1L, 6)).isFalse();
    }

    @Test
    void verificarEstoque_usaCache_segundaChamadaNaoFazHttp() {
        // verificarEstoque usa o proxy de produtoService, portanto as chamadas subsequentes
        // são atendidas pelo cache sem gerar novas requisições HTTP.
        stubProduto(1L, "Notebook", "2500.00", 5);

        produtoService.verificarEstoque(1L, 1);
        produtoService.verificarEstoque(1L, 1);

        assertThat(requisicoesPrimario(1L)).isEqualTo(1);
    }

    @Test
    void criacaoDePedido_reutilizaCacheEEvitaChamadasDuplicadas() {
        // Após buscar o produto, a verificação de estoque reaproveita o cache
        stubProduto(1L, "Notebook", "2500.00", 10);

        produtoService.buscarProdutoPorId(1L);
        produtoService.verificarEstoque(1L, 1);
        assertThat(requisicoesPrimario(1L)).isEqualTo(1);

        produtoService.buscarProdutoPorId(1L);
        produtoService.verificarEstoque(1L, 1);
        assertThat(requisicoesPrimario(1L)).isEqualTo(1);
    }

    @Test
    void produto404_retornaVazioSemRetry() {
        produtosApi.stubFor(get(urlEqualTo("/api/produtos/999")).willReturn(aResponse().withStatus(404)));
        produtosApi.stubFor(get(urlEqualTo("/fallback/produtos/999")).willReturn(aResponse().withStatus(404)));

        Optional<ProdutoDTO> produto = produtoService.buscarProdutoPorId(999L);

        assertThat(produto).isEmpty();
        assertThat(requisicoesPrimario(999L)).isEqualTo(1);
    }

    private static void stubFallbackDisponivel() {
        produtosApi.stubFor(get(urlEqualTo("/fallback/produtos/1")).willReturn(aResponse()
                .withStatus(200).withHeader("Content-Type", "application/json")
                .withBody("{\"id\":1,\"nome\":\"Do Fallback\",\"preco\":10.00,\"estoque\":3}")));
    }

    @Test
    void erro500DoPrimario_acionaFallbackComSucesso() {
        produtosApi.stubFor(get(urlEqualTo("/api/produtos/1")).willReturn(aResponse().withStatus(500)));
        stubFallbackDisponivel();

        Optional<ProdutoDTO> produto = produtoService.buscarProdutoPorId(1L);

        assertThat(produto).isPresent();
        assertThat(produto.get().nome()).isEqualTo("Do Fallback");
        assertThat(requisicoesFallback(1L)).isGreaterThanOrEqualTo(1);
    }

    @Test
    void conexaoResetada_acionaFallbackComSucesso() {
        produtosApi.stubFor(get(urlEqualTo("/api/produtos/1"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
        stubFallbackDisponivel();

        Optional<ProdutoDTO> produto = produtoService.buscarProdutoPorId(1L);

        assertThat(produto).isPresent();
        assertThat(produto.get().nome()).isEqualTo("Do Fallback");
        assertThat(requisicoesFallback(1L)).isGreaterThanOrEqualTo(1);
    }

    @Test
    void timeoutDoPrimario_acionaFallbackComSucesso() {
        produtosApi.stubFor(get(urlEqualTo("/api/produtos/1"))
                .willReturn(aResponse().withFixedDelay(3000).withStatus(200)));
        stubFallbackDisponivel();

        Optional<ProdutoDTO> produto = produtoService.buscarProdutoPorId(1L);

        assertThat(produto).isPresent();
        assertThat(produto.get().nome()).isEqualTo("Do Fallback");
        assertThat(requisicoesFallback(1L)).isGreaterThanOrEqualTo(1);
    }


    @Test
    void falhaDoPrimarioEDoFallback_propagaExcecaoENaoCacheia() {
        produtosApi.stubFor(get(urlEqualTo("/api/produtos/1")).willReturn(aResponse().withStatus(500)));
        produtosApi.stubFor(get(urlEqualTo("/fallback/produtos/1")).willReturn(aResponse().withStatus(500)));

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> produtoService.buscarProdutoPorId(1L));

        // quando o serviço se recupera, a próxima chamada funciona (nada ruim foi cacheado)
        produtosApi.resetAll();
        stubProduto(1L, "Notebook", "2500.00", 10);
        assertThat(produtoService.buscarProdutoPorId(1L)).isPresent();
    }
}
