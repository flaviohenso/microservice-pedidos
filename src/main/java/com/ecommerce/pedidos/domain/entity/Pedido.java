package com.ecommerce.pedidos.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Entidade de domínio - Representa um pedido no sistema
 * Importante: Não tem anotações de Framework (JPA, Hibernate, etc)
 */
public class Pedido {
    
    private Long id;
    private String numeroPedido;
    private Long clienteId;
    private List<ItemPedido> itens;
    private StatusPedido status;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
    
    // Construtor para criação (sem ID)
    public Pedido(Long clienteId, List<ItemPedido> itens) {
        this.validarCamposObrigatorios(clienteId, itens);
        this.validarRegrasDeNegocio(itens);
        
        this.numeroPedido = gerarNumeroPedido();
        this.clienteId = clienteId;
        this.itens = new ArrayList<>(itens);
        this.status = StatusPedido.PENDENTE;
        this.dataCriacao = LocalDateTime.now();
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    // Construtor para reconstituição (com ID - vindo do BD)
    public Pedido(Long id, String numeroPedido, Long clienteId, List<ItemPedido> itens, 
                  StatusPedido status, LocalDateTime dataCriacao, LocalDateTime dataAtualizacao) {
        this.id = id;
        this.numeroPedido = numeroPedido;
        this.clienteId = clienteId;
        this.itens = new ArrayList<>(itens);
        this.status = status;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
    }
    
    // ===== REGRAS DE NEGÓCIO =====
    
    private void validarCamposObrigatorios(Long clienteId, List<ItemPedido> itens) {
        if (clienteId == null) {
            throw new IllegalArgumentException("ID do cliente é obrigatório");
        }
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("O pedido deve conter pelo menos um item");
        }
    }
    
    private void validarRegrasDeNegocio(List<ItemPedido> itens) {
        if (clienteId != null && clienteId <= 0) {
            throw new IllegalArgumentException("ID do cliente deve ser positivo");
        }
    }
    
    /**
     * Gera um número de pedido único
     */
    private String gerarNumeroPedido() {
        return "PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Calcula o valor total do pedido (soma dos subtotais dos itens)
     */
    public BigDecimal calcularTotal() {
        return itens.stream()
                .map(ItemPedido::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Confirma o pedido
     */
    public void confirmar() {
        if (this.status != StatusPedido.PENDENTE) {
            throw new IllegalStateException("Apenas pedidos pendentes podem ser confirmados");
        }
        this.status = StatusPedido.CONFIRMADO;
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    /**
     * Cancela o pedido
     */
    public void cancelar() {
        if (this.status == StatusPedido.CANCELADO) {
            throw new IllegalStateException("O pedido já está cancelado");
        }
        this.status = StatusPedido.CANCELADO;
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    /**
     * Verifica se o pedido pode ser cancelado
     */
    public boolean podeCancelar() {
        return this.status != StatusPedido.CANCELADO;
    }
    
    /**
     * Verifica se o pedido está pendente
     */
    public boolean isPendente() {
        return this.status == StatusPedido.PENDENTE;
    }
    
    /**
     * Verifica se o pedido foi confirmado
     */
    public boolean isConfirmado() {
        return this.status == StatusPedido.CONFIRMADO;
    }
    
    /**
     * Verifica se o pedido foi cancelado
     */
    public boolean isCancelado() {
        return this.status == StatusPedido.CANCELADO;
    }
    
    // ===== GETTERS (sem setters para imutabilidade) =====
    
    public Long getId() {
        return id;
    }
    
    public String getNumeroPedido() {
        return numeroPedido;
    }
    
    public Long getClienteId() {
        return clienteId;
    }
    
    public List<ItemPedido> getItens() {
        return Collections.unmodifiableList(itens);
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
}




