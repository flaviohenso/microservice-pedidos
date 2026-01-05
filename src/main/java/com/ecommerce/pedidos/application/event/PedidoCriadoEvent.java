package com.ecommerce.pedidos.application.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Evento representando a criação de um pedido
 */

public class PedidoCriadoEvent {
    
    private Long pedidoId;
    private String numeroPedido;
    private Long clienteId;
    private List<ItemPedidoEvent> itens;
    private BigDecimal valorTotal;
    private String status;
    private LocalDateTime dataCriacao;
    
    public PedidoCriadoEvent() {}

    public PedidoCriadoEvent(Long pedidoId, String numeroPedido, Long clienteId, List<ItemPedidoEvent> itens, BigDecimal valorTotal, String status, LocalDateTime dataCriacao) {
        this.pedidoId = pedidoId;
        this.numeroPedido = numeroPedido;
        this.clienteId = clienteId;
        this.itens = itens;
        this.valorTotal = valorTotal;
        this.status = status;
        this.dataCriacao = dataCriacao;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public String getNumeroPedido() {
        return numeroPedido;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public List<ItemPedidoEvent> getItens() {
        return itens;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public String getStatus() {
        return status;
    }

   public LocalDateTime getDataCriacao() {
    return dataCriacao;
   }

    public void setPedidoId(Long pedidoId) {
        this.pedidoId = pedidoId;
    }

    public void setNumeroPedido(String numeroPedido) {
        this.numeroPedido = numeroPedido;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public void setItens(List<ItemPedidoEvent> itens) {
        this.itens = itens;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PedidoCriadoEvent other = (PedidoCriadoEvent) obj;
        return Objects.equals(pedidoId, other.pedidoId) && Objects.equals(numeroPedido, other.numeroPedido) && Objects.equals(clienteId, other.clienteId) && Objects.equals(itens, other.itens) && Objects.equals(valorTotal, other.valorTotal) && Objects.equals(status, other.status) && Objects.equals(dataCriacao, other.dataCriacao);
    }

    public int hashCode() {
        return Objects.hash(pedidoId, numeroPedido, clienteId, itens, valorTotal, status, dataCriacao);
    }

    public String toString() {
        return "PedidoCriadoEvent [pedidoId=" + pedidoId + ", numeroPedido=" + numeroPedido + ", clienteId=" + clienteId + ", itens=" + itens + ", valorTotal=" + valorTotal + ", status=" + status + ", dataCriacao=" + dataCriacao + "]";
    }
}




