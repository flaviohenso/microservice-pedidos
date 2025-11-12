package com.ecommerce.pedidos.infrastructure.persistence.repository;

import com.ecommerce.pedidos.domain.entity.Pedido;
import com.ecommerce.pedidos.domain.repository.PedidoRepositoryPort;
import com.ecommerce.pedidos.infrastructure.persistence.mapper.PedidoMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementação do repositório de Pedido usando Spring Data JPA
 */
@Component
public class PedidoRepositoryImpl implements PedidoRepositoryPort {
    
    private final PedidoJpaRepository jpaRepository;
    
    public PedidoRepositoryImpl(PedidoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public Pedido salvar(Pedido pedido) {
        var jpaEntity = PedidoMapper.toJpaEntity(pedido);
        var savedEntity = jpaRepository.save(jpaEntity);
        return PedidoMapper.toDomain(savedEntity);
    }
    
    @Override
    public Optional<Pedido> buscarPorId(Long id) {
        return jpaRepository.findById(id)
                .map(PedidoMapper::toDomain);
    }
    
    @Override
    public Optional<Pedido> buscarPorNumeroPedido(String numeroPedido) {
        return jpaRepository.findByNumeroPedido(numeroPedido)
                .map(PedidoMapper::toDomain);
    }
    
    @Override
    public List<Pedido> listarTodos() {
        return jpaRepository.findAll().stream()
                .map(PedidoMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Pedido> listarPorClienteId(Long clienteId) {
        return jpaRepository.findByClienteId(clienteId).stream()
                .map(PedidoMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public Pedido atualizar(Pedido pedido) {
        var jpaEntity = PedidoMapper.toJpaEntity(pedido);
        var updatedEntity = jpaRepository.save(jpaEntity);
        return PedidoMapper.toDomain(updatedEntity);
    }
    
    @Override
    public void deletar(Long id) {
        jpaRepository.deleteById(id);
    }
}

