package com.ecommerce.pedidos.infrastructure.persistence.entity;

import com.ecommerce.pedidos.domain.entity.StatusPedido;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade JPA para Pedido
 * Representa a tabela de pedidos no banco de dados
 */
@Entity
@Table(name = "pedidos")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
}

