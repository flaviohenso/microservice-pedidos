package com.ecommerce.pedidos.infrastructure.config;

import com.ecommerce.pedidos.application.service.ProdutoServicePort;
import com.ecommerce.pedidos.application.usecase.BuscarPedidoPorIdUseCase;
import com.ecommerce.pedidos.application.usecase.CancelarPedidoUseCase;
import com.ecommerce.pedidos.application.usecase.CriarPedidoUseCase;
import com.ecommerce.pedidos.application.usecase.ListarPedidosUseCase;
import com.ecommerce.pedidos.domain.repository.OutboxRepositoryPort;
import com.ecommerce.pedidos.domain.repository.PedidoRepositoryPort;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração dos Beans - Injeção de Dependências
 * Aqui criamos as instâncias dos Use Cases e outras dependências
 */
@Configuration
public class BeanConfiguration {
    
    /**
     * Bean para ObjectMapper (serialização/desserialização JSON Jackson 3)
     */
    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder().build();
    }
    
    /**
     * Bean para CriarPedidoUseCase
     */
    @Bean
    public CriarPedidoUseCase criarPedidoUseCase(
            PedidoRepositoryPort pedidoRepository,
            OutboxRepositoryPort outboxRepository,
            com.ecommerce.pedidos.application.service.SalvarPedidoTransacionalPort salvarPedidoTransacionalPort,
            ProdutoServicePort produtoService,
            ObjectMapper objectMapper) {
        return new CriarPedidoUseCase(
                pedidoRepository,
                outboxRepository,
                salvarPedidoTransacionalPort,
                produtoService,
                objectMapper
        );
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




