package com.ecommerce.pedidos.application.event;

import java.math.BigDecimal;
import java.util.Objects;

public class ItemPedidoEvent {
    
    private Long produtoId;
    private String nomeProduto;
    private Integer quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal subtotal;

    public ItemPedidoEvent() {}
    
    public ItemPedidoEvent(Long produtoId, String nomeProduto, Integer quantidade, BigDecimal precoUnitario, BigDecimal subtotal) {
        this.produtoId = produtoId;
        this.nomeProduto = nomeProduto;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.subtotal = subtotal;
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
    
    public BigDecimal getSubtotal() {
        return subtotal;
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

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public String toString() {
        return "ItemPedidoEvent [produtoId=" + produtoId + ", nomeProduto=" + nomeProduto + ", quantidade=" + quantidade + ", precoUnitario=" + precoUnitario + ", subtotal=" + subtotal + "]";
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ItemPedidoEvent other = (ItemPedidoEvent) obj;
        return Objects.equals(produtoId, other.produtoId) && Objects.equals(nomeProduto, other.nomeProduto) && Objects.equals(quantidade, other.quantidade) && Objects.equals(precoUnitario, other.precoUnitario) && Objects.equals(subtotal, other.subtotal);
    }

    public int hashCode() {
        return Objects.hash(produtoId, nomeProduto, quantidade, precoUnitario, subtotal);
    }
}
