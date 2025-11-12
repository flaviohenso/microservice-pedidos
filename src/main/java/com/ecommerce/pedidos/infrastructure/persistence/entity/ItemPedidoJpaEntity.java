package com.ecommerce.pedidos.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Entidade JPA para Item de Pedido
 * Representa a tabela de itens de pedido no banco de dados
 */
@Entity
@Table(name = "itens_pedido")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
}

