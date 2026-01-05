package com.ecommerce.pedidos.infrastructure.client;

import com.ecommerce.pedidos.infrastructure.client.dto.ProdutoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "produto-service",
    url = "${produto.service.url}",
    fallback = ProdutoFeignClientFallback.class
)
public interface ProdutoFeignClient {
    
    @GetMapping("/{id}")
    ProdutoDTO buscarPorId(@PathVariable("id") Long id);
}