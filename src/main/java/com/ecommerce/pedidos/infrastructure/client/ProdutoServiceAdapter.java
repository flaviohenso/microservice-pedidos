package com.ecommerce.pedidos.infrastructure.client;

import com.ecommerce.pedidos.application.dto.ProdutoDTO;
import com.ecommerce.pedidos.application.service.ProdutoServicePort;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Cliente REST para comunicação com o microserviço de produtos
 * Implementa cache e tratamento de erros
 */
@Component
public class ProdutoServiceAdapter implements ProdutoServicePort {

    private static final Logger logger = LoggerFactory.getLogger(ProdutoServiceAdapter.class);
    private static final String PRODUTO_SERVICE = "produtoService";
    
    private final ProdutoFeignClient produtoFeignClient;
    private final ObjectProvider<ProdutoServicePort> selfProvider;

    public ProdutoServiceAdapter(
            ProdutoFeignClient produtoFeignClient,
            ObjectProvider<ProdutoServicePort> selfProvider) {
        this.produtoFeignClient = produtoFeignClient;
        this.selfProvider = selfProvider;
    }

    @Override
    @Cacheable(value = "produtos", key = "#id", unless = "#result == null")
    @Retry(name = PRODUTO_SERVICE)
    @CircuitBreaker(name = PRODUTO_SERVICE)
    public Optional<ProdutoDTO> buscarProdutoPorId(Long id) {
        logger.debug("Buscando produto {} via Feign Client", id);
        
        try {
            ProdutoDTO produto = produtoFeignClient.buscarPorId(id);
            
            if (produto != null) {
                logger.info("Produto {} encontrado: {}", id, produto.nome());
                return Optional.of(produto);
            }
            
            return Optional.empty();
            
        } catch (FeignException.NotFound e) {
            logger.warn("Produto {} não encontrado", id);
            return Optional.empty();
            
        } catch (FeignException e) {
            logger.error("Erro ao buscar produto {}: {}", id, e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean verificarEstoque(Long produtoId, Integer quantidade) {
        logger.debug("Verificando estoque do produto {} (quantidade: {})", produtoId, quantidade);
        
        ProdutoServicePort self = selfProvider.getIfAvailable(() -> this);
        Optional<ProdutoDTO> produtoOpt = self.buscarProdutoPorId(produtoId);
        
        if (produtoOpt.isEmpty()) {
            logger.warn("Produto {} não encontrado para verificação de estoque", produtoId);
            return false;
        }

        ProdutoDTO produto = produtoOpt.get();
        boolean estoqueDisponivel = produto.estoque() != null && produto.estoque() >= quantidade;

        if (!estoqueDisponivel) {
            logger.warn("Estoque insuficiente para produto {}. Disponível: {}, Solicitado: {}",
                    produtoId, produto.estoque(), quantidade);
        } else {
            logger.info("Estoque OK para produto {}. Disponível: {}, Solicitado: {}",
                    produtoId, produto.estoque(), quantidade);
        }

        return estoqueDisponivel;
    }
}

