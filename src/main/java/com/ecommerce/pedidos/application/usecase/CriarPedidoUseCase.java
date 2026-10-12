package com.ecommerce.pedidos.application.usecase;

import com.ecommerce.pedidos.application.dto.ItemPedidoRequest;
import com.ecommerce.pedidos.application.dto.ProdutoDTO;
import com.ecommerce.pedidos.application.event.ItemPedidoEvent;
import com.ecommerce.pedidos.application.event.PedidoCriadoEvent;
import com.ecommerce.pedidos.application.service.ProdutoServicePort;
import com.ecommerce.pedidos.application.service.SalvarPedidoTransacionalPort;
import com.ecommerce.pedidos.domain.entity.ItemPedido;
import com.ecommerce.pedidos.domain.entity.OutboxEvent;
import com.ecommerce.pedidos.domain.entity.Pedido;
import com.ecommerce.pedidos.domain.exception.ProdutoIndisponivelException;
import com.ecommerce.pedidos.domain.repository.OutboxRepositoryPort;
import com.ecommerce.pedidos.domain.repository.PedidoRepositoryPort;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Caso de Uso: Criar um novo pedido
 * Implementa o padrão Outbox para garantir consistência eventual.
 * As chamadas de I/O externo são feitas fora de qualquer transação de banco de dados.
 */
public class CriarPedidoUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(CriarPedidoUseCase.class);
    
    private final PedidoRepositoryPort pedidoRepository;
    private final OutboxRepositoryPort outboxRepository;
    private final SalvarPedidoTransacionalPort salvarPedidoTransacionalPort;
    private final ProdutoServicePort produtoService;
    private final ObjectMapper objectMapper;
    
    public CriarPedidoUseCase(
            PedidoRepositoryPort pedidoRepository,
            OutboxRepositoryPort outboxRepository,
            SalvarPedidoTransacionalPort salvarPedidoTransacionalPort,
            ProdutoServicePort produtoService,
            ObjectMapper objectMapper) {
        this.pedidoRepository = pedidoRepository;
        this.outboxRepository = outboxRepository;
        this.salvarPedidoTransacionalPort = salvarPedidoTransacionalPort != null
                ? salvarPedidoTransacionalPort
                : (pedido, eventFactory) -> {
                    Pedido salvo = pedidoRepository.salvar(pedido);
                    outboxRepository.salvar(eventFactory.apply(salvo));
                    return salvo;
                };
        this.produtoService = produtoService;
        this.objectMapper = objectMapper;
    }

    public CriarPedidoUseCase(
            PedidoRepositoryPort pedidoRepository,
            OutboxRepositoryPort outboxRepository,
            ProdutoServicePort produtoService,
            ObjectMapper objectMapper) {
        this(pedidoRepository, outboxRepository, null, produtoService, objectMapper);
    }
    
    /**
     * Executa a criação de um pedido
     * 
     * @param clienteId ID do cliente
     * @param itensRequest Lista de itens (produtoId, quantidade)
     * @return Pedido criado
     */
    public Pedido executar(Long clienteId, List<ItemPedidoRequest> itensRequest) {
        logger.info("Iniciando criação de pedido para cliente {}", clienteId);
        
        // 1. Validar e buscar produtos (I/O externo, FORA de transação de banco)
        List<ItemPedido> itens = validarECriarItens(itensRequest);
        
        // 2. Criar pedido em memória
        Pedido pedido = new Pedido(clienteId, itens);
        
        // 3. Salvar pedido no banco e registrar Outbox atomicamente (transação de banco curta)
        Pedido pedidoSalvo = salvarPedidoTransacionalPort.salvar(pedido, this::gerarEventoOutbox);
        
        logger.info("Pedido {} criado e evento registrado na Outbox", pedidoSalvo.getNumeroPedido());
        
        return pedidoSalvo;
    }
    
    /**
     * Valida produtos e cria itens do pedido
     */
    private List<ItemPedido> validarECriarItens(List<ItemPedidoRequest> itensRequest) {
        List<ItemPedido> itens = new ArrayList<>();
        
        for (ItemPedidoRequest itemRequest : itensRequest) {
            // Busca produto no microserviço de produtos
            ProdutoDTO produto = produtoService.buscarProdutoPorId(itemRequest.produtoId())
            .orElseThrow(() -> new ProdutoIndisponivelException(itemRequest.produtoId(), "Produto não encontrado"));
            
            // Verifica estoque
            if (!produtoService.verificarEstoque(itemRequest.produtoId(), itemRequest.quantidade())) {
                throw new ProdutoIndisponivelException(
                        itemRequest.produtoId(), 
                        "Estoque insuficiente. Disponível: " + produto.estoque());
            }
            
            // Cria item do pedido com dados do produto
            ItemPedido item = new ItemPedido(
                    produto.id(),
                    produto.nome(),
                    itemRequest.quantidade(),
                    produto.preco()
            );
            
            itens.add(item);
        }
        
        return itens;
    }
    
    /**
     * Cria evento na tabela Outbox
     */
    private OutboxEvent gerarEventoOutbox(Pedido pedido) {
        try {
            // Cria evento de domínio
            PedidoCriadoEvent event = new PedidoCriadoEvent(
                    pedido.getId(),
                    pedido.getNumeroPedido(),
                    pedido.getClienteId(),
                    pedido.getItens().stream().map(item -> new ItemPedidoEvent(item.getProdutoId(), item.getNomeProduto(), item.getQuantidade(), item.getPrecoUnitario(), item.calcularSubtotal())).collect(Collectors.toList()),
                    pedido.calcularTotal(),
                    pedido.getStatus().name(),
                    pedido.getDataCriacao()
            );
            
            // Serializa evento para JSON
            String payload = objectMapper.writeValueAsString(event);
            
            // Cria registro na Outbox
            return new OutboxEvent(
                    "PEDIDO",
                    pedido.getId(),
                    "PEDIDO_CRIADO",
                    payload
            );
            
        } catch (Exception e) {
            logger.error("Erro ao criar evento Outbox: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao registrar evento", e);
        }
    }
    
    
}




