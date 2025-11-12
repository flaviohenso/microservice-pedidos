package com.ecommerce.pedidos.infrastructure.client;

import com.ecommerce.pedidos.application.service.ProdutoServicePort;
import com.ecommerce.pedidos.infrastructure.client.dto.ProdutoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * Cliente REST para comunicação com o microserviço de produtos
 * Implementa cache e tratamento de erros
 */
@Component
public class ProdutoRestClient implements ProdutoServicePort {
    
    private static final Logger logger = LoggerFactory.getLogger(ProdutoRestClient.class);
    
    private final RestTemplate restTemplate;
    private final String produtoServiceUrl;
    
    public ProdutoRestClient(
            RestTemplate restTemplate,
            @Value("${produto.service.url}") String produtoServiceUrl) {
        this.restTemplate = restTemplate;
        this.produtoServiceUrl = produtoServiceUrl;
    }
    
    @Override
    @Cacheable(value = "produtos", key = "#id", unless = "#result == null || #result.isEmpty()")
    public Optional<ProdutoDTO> buscarProdutoPorId(Long id) {
        try {
            String url = produtoServiceUrl + "/" + id;
            logger.debug("Buscando produto via REST: {}", url);
            
            ProdutoDTO produto = restTemplate.getForObject(url, ProdutoDTO.class);
            
            if (produto != null) {
                logger.info("Produto {} encontrado: {}", id, produto.getNome());
                return Optional.of(produto);
            }
            
            logger.warn("Produto {} não retornou dados", id);
            return Optional.empty();
            
        } catch (HttpClientErrorException.NotFound e) {
            logger.warn("Produto {} não encontrado no microserviço de produtos", id);
            return Optional.empty();
            
        } catch (ResourceAccessException e) {
            logger.error("Erro de comunicação com microserviço de produtos: {}", e.getMessage());
            throw new RuntimeException("Microserviço de produtos indisponível", e);
            
        } catch (Exception e) {
            logger.error("Erro ao buscar produto {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Erro ao buscar produto", e);
        }
    }
    
    @Override
    public boolean verificarEstoque(Long produtoId, Integer quantidade) {
        try {
            Optional<ProdutoDTO> produtoOpt = buscarProdutoPorId(produtoId);
            
            if (produtoOpt.isEmpty()) {
                logger.warn("Produto {} não encontrado para verificação de estoque", produtoId);
                return false;
            }
            
            ProdutoDTO produto = produtoOpt.get();
            boolean estoqueDisponivel = produto.getEstoque() != null && produto.getEstoque() >= quantidade;
            
            if (!estoqueDisponivel) {
                logger.warn("Estoque insuficiente para produto {}. Disponível: {}, Solicitado: {}", 
                        produtoId, produto.getEstoque(), quantidade);
            }
            
            return estoqueDisponivel;
            
        } catch (Exception e) {
            logger.error("Erro ao verificar estoque do produto {}: {}", produtoId, e.getMessage());
            return false;
        }
    }
}

