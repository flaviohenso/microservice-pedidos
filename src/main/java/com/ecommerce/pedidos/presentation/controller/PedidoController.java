package com.ecommerce.pedidos.presentation.controller;

import com.ecommerce.pedidos.application.usecase.BuscarPedidoPorIdUseCase;
import com.ecommerce.pedidos.application.usecase.CancelarPedidoUseCase;
import com.ecommerce.pedidos.application.usecase.CriarPedidoUseCase;
import com.ecommerce.pedidos.application.usecase.ListarPedidosUseCase;
import com.ecommerce.pedidos.domain.entity.Pedido;
import com.ecommerce.pedidos.presentation.dto.PedidoRequestDTO;
import com.ecommerce.pedidos.presentation.dto.PedidoResponseDTO;
import com.ecommerce.pedidos.presentation.mapper.PedidoDTOMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller REST para gerenciamento de pedidos
 */
@RestController
@RequestMapping("/api/pedidos")
@Tag(name = "Pedidos", description = "API para gerenciamento de pedidos")
public class PedidoController {
    
    private final CriarPedidoUseCase criarPedidoUseCase;
    private final BuscarPedidoPorIdUseCase buscarPedidoPorIdUseCase;
    private final ListarPedidosUseCase listarPedidosUseCase;
    private final CancelarPedidoUseCase cancelarPedidoUseCase;
    
    public PedidoController(
            CriarPedidoUseCase criarPedidoUseCase,
            BuscarPedidoPorIdUseCase buscarPedidoPorIdUseCase,
            ListarPedidosUseCase listarPedidosUseCase,
            CancelarPedidoUseCase cancelarPedidoUseCase) {
        this.criarPedidoUseCase = criarPedidoUseCase;
        this.buscarPedidoPorIdUseCase = buscarPedidoPorIdUseCase;
        this.listarPedidosUseCase = listarPedidosUseCase;
        this.cancelarPedidoUseCase = cancelarPedidoUseCase;
    }
    
    @Operation(summary = "Criar novo pedido")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso",
                    content = @Content(schema = @Schema(implementation = PedidoResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "422", description = "Produto indisponível ou estoque insuficiente")
    })
    @PostMapping
    public ResponseEntity<PedidoResponseDTO> criarPedido(
            @Valid @RequestBody PedidoRequestDTO request) {
        
        // Converte itens do DTO para o formato esperado pelo use case
        var itensRequest = request.getItens().stream()
                .map(item -> new CriarPedidoUseCase.ItemPedidoRequest(
                        item.getProdutoId(), 
                        item.getQuantidade()))
                .collect(Collectors.toList());
        
        Pedido pedido = criarPedidoUseCase.executar(request.getClienteId(), itensRequest);
        
        PedidoResponseDTO response = PedidoDTOMapper.toResponseDTO(pedido);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @Operation(summary = "Buscar pedido por ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido encontrado",
                    content = @Content(schema = @Schema(implementation = PedidoResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> buscarPedidoPorId(
            @Parameter(description = "ID do pedido") @PathVariable Long id) {
        
        Pedido pedido = buscarPedidoPorIdUseCase.executar(id);
        PedidoResponseDTO response = PedidoDTOMapper.toResponseDTO(pedido);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Listar todos os pedidos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pedidos retornada com sucesso")
    })
    @GetMapping
    public ResponseEntity<List<PedidoResponseDTO>> listarTodosPedidos() {
        List<Pedido> pedidos = listarPedidosUseCase.executar();
        
        List<PedidoResponseDTO> response = pedidos.stream()
                .map(PedidoDTOMapper::toResponseDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Listar pedidos por cliente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pedidos do cliente retornada com sucesso")
    })
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<PedidoResponseDTO>> listarPedidosPorCliente(
            @Parameter(description = "ID do cliente") @PathVariable Long clienteId) {
        
        List<Pedido> pedidos = listarPedidosUseCase.executarPorCliente(clienteId);
        
        List<PedidoResponseDTO> response = pedidos.stream()
                .map(PedidoDTOMapper::toResponseDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Cancelar pedido")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido cancelado com sucesso",
                    content = @Content(schema = @Schema(implementation = PedidoResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado"),
            @ApiResponse(responseCode = "400", description = "Pedido não pode ser cancelado")
    })
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<PedidoResponseDTO> cancelarPedido(
            @Parameter(description = "ID do pedido") @PathVariable Long id) {
        
        Pedido pedido = cancelarPedidoUseCase.executar(id);
        PedidoResponseDTO response = PedidoDTOMapper.toResponseDTO(pedido);
        
        return ResponseEntity.ok(response);
    }
}

