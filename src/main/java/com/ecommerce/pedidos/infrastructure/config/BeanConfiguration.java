package com.ecommerce.pedidos.infrastructure.config;

import com.ecommerce.pedidos.application.service.ProdutoServicePort;
import com.ecommerce.pedidos.application.usecase.BuscarPedidoPorIdUseCase;
import com.ecommerce.pedidos.application.usecase.CancelarPedidoUseCase;
import com.ecommerce.pedidos.application.usecase.CriarPedidoUseCase;
import com.ecommerce.pedidos.application.usecase.ListarPedidosUseCase;
import com.ecommerce.pedidos.domain.repository.OutboxRepositoryPort;
import com.ecommerce.pedidos.domain.repository.PedidoRepositoryPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração dos Beans - Injeção de Dependências
 * Aqui criamos as instâncias dos Use Cases e outras dependências
 */
@Configuration
public class BeanConfiguration {
    
    /**
     * Bean para ObjectMapper (serialização/desserialização JSON)
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
    
    /**
     * Bean para CriarPedidoUseCase
     */
    @Bean
    public CriarPedidoUseCase criarPedidoUseCase(
            PedidoRepositoryPort pedidoRepository,
            OutboxRepositoryPort outboxRepository,
            ProdutoServicePort produtoService,
            ObjectMapper objectMapper) {
        return new CriarPedidoUseCase(pedidoRepository, outboxRepository, produtoService, objectMapper);
    }
    
    /**
     * Bean para BuscarPedidoPorIdUseCase
     */
    @Bean
    public BuscarPedidoPorIdUseCase buscarPedidoPorIdUseCase(PedidoRepositoryPort pedidoRepository) {
        return new BuscarPedidoPorIdUseCase(pedidoRepository);
    }
    
    /**
     * Bean para ListarPedidosUseCase
     */
    @Bean
    public ListarPedidosUseCase listarPedidosUseCase(PedidoRepositoryPort pedidoRepository) {
        return new ListarPedidosUseCase(pedidoRepository);
    }
    
    /**
     * Bean para CancelarPedidoUseCase
     */
    @Bean
    public CancelarPedidoUseCase cancelarPedidoUseCase(
            PedidoRepositoryPort pedidoRepository,
            OutboxRepositoryPort outboxRepository,
            ObjectMapper objectMapper) {
        return new CancelarPedidoUseCase(pedidoRepository, outboxRepository, objectMapper);
    }
}

