package com.ecommerce.pedidos.infrastructure.client;

import com.ecommerce.pedidos.infrastructure.client.dto.ProdutoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ProdutoFeignClientFallback implements ProdutoFeignClient {
    
    private static final Logger logger = LoggerFactory.getLogger(ProdutoFeignClientFallback.class);
    
    private final RestTemplate restTemplate;
    private final String fallbackServiceUrl;
    
    public ProdutoFeignClientFallback(
            RestTemplate restTemplate,
            @Value("${fallback.service.url}") String fallbackServiceUrl) {
        this.restTemplate = restTemplate;
        this.fallbackServiceUrl = fallbackServiceUrl;
    }
    
    @Override
    public ProdutoDTO buscarPorId(Long id) {
        logger.warn("FALLBACK ativado para produto {}", id);
        String url = fallbackServiceUrl + "/" + id;
        return restTemplate.getForObject(url, ProdutoDTO.class);
    }
}