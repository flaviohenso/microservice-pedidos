package com.ecommerce.pedidos.infrastructure.persistence.entity;

import com.ecommerce.pedidos.domain.entity.StatusPedido;
import jakarta.persistence.*;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entidade JPA para Pedido
 * Representa a tabela de pedidos no banco de dados
 */
@Entity
@Table(name = "pedidos")
public class PedidoJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "numero_pedido", nullable = false, unique = true, length = 50)
    private String numeroPedido;
    
    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;
    
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemPedidoJpaEntity> itens = new ArrayList<>();
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPedido status;
    
    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;
    
    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;
    
    /**
     * Adiciona um item ao pedido (mantém sincronização bidirecional)
     */
    public void adicionarItem(ItemPedidoJpaEntity item) {
        itens.add(item);
        item.setPedido(this);
    }
    
    /**
     * Remove um item do pedido
     */
    public void removerItem(ItemPedidoJpaEntity item) {
        itens.remove(item);
        item.setPedido(null);
    }

    public Long getId() {
        return id;
    }

    public String getNumeroPedido() {
        return numeroPedido;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public List<ItemPedidoJpaEntity> getItens() {
        return itens;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNumeroPedido(String numeroPedido) {
        this.numeroPedido = numeroPedido;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public void setItens(List<ItemPedidoJpaEntity> itens) {
        this.itens = itens;
    }

    public void setStatus(StatusPedido status) {
        this.status = status;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PedidoJpaEntity other = (PedidoJpaEntity) obj;
        return Objects.equals(id, other.id) && Objects.equals(numeroPedido, other.numeroPedido) && Objects.equals(clienteId, other.clienteId) && Objects.equals(itens, other.itens) && Objects.equals(status, other.status) && Objects.equals(dataCriacao, other.dataCriacao) && Objects.equals(dataAtualizacao, other.dataAtualizacao);
    }

    public int hashCode() {
        return Objects.hash(id, numeroPedido, clienteId, itens, status, dataCriacao, dataAtualizacao);
    }
}

