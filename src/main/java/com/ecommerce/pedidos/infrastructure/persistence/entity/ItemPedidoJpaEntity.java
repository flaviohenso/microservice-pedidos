package com.ecommerce.pedidos.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entidade JPA para Item de Pedido
 * Representa a tabela de itens de pedido no banco de dados
 */
@Entity
@Table(name = "itens_pedido")
public class ItemPedidoJpaEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "produto_id", nullable = false)
    private Long produtoId;
    
    @Column(name = "nome_produto", nullable = false, length = 200)
    private String nomeProduto;
    
    @Column(nullable = false)
    private Integer quantidade;
    
    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoJpaEntity pedido;

    public void setPedido(PedidoJpaEntity pedido) {
        this.pedido = pedido;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setProdutoId(Long produtoId) {
        this.produtoId = produtoId;
    }

    public void setNomeProduto(String nomeProduto) {
        this.nomeProduto = nomeProduto;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ItemPedidoJpaEntity other = (ItemPedidoJpaEntity) obj;
        return Objects.equals(id, other.id) && Objects.equals(produtoId, other.produtoId) && Objects.equals(nomeProduto, other.nomeProduto) && Objects.equals(quantidade, other.quantidade) && Objects.equals(precoUnitario, other.precoUnitario) && Objects.equals(pedido, other.pedido);
    }

    public int hashCode() {
        return Objects.hash(id, produtoId, nomeProduto, quantidade, precoUnitario, pedido);
    }
}




