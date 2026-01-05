package com.ecommerce.pedidos.infrastructure.messaging;

import com.ecommerce.pedidos.application.service.EventPublisherPort;
import com.ecommerce.pedidos.domain.entity.OutboxEvent;
import com.ecommerce.pedidos.domain.entity.OutboxStatus;
import com.ecommerce.pedidos.domain.repository.OutboxRepositoryPort;
import com.ecommerce.pedidos.infrastructure.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Processador de eventos da tabela Outbox
 * Executa periodicamente para publicar eventos pendentes no RabbitMQ
 */
@Component
public class OutboxProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(OutboxProcessor.class);
    
    private final OutboxRepositoryPort outboxRepository;
    private final EventPublisherPort eventPublisher;
    private final int maxRetries;
    private final int batchSize;
    
    public OutboxProcessor(
            OutboxRepositoryPort outboxRepository,
            EventPublisherPort eventPublisher,
            @Value("${outbox.processor.max-retries:3}") int maxRetries,
            @Value("${outbox.processor.batch-size:100}") int batchSize) {
        this.outboxRepository = outboxRepository;
        this.eventPublisher = eventPublisher;
        this.maxRetries = maxRetries;
        this.batchSize = batchSize;
    }
    
    /**
     * Job agendado que processa eventos pendentes da Outbox
     * Executa a cada 10 segundos (configurável via application.properties)
     */
    @Scheduled(fixedDelayString = "${outbox.processor.fixed-delay:10000}")
    @Transactional
    public void processarEventosPendentes() {
        try {
            // Busca eventos pendentes (limitado por batch)
            List<OutboxEvent> eventosPendentes = outboxRepository.buscarPendentesPaginado(batchSize);
            
            if (eventosPendentes.isEmpty()) {
                logger.debug("Nenhum evento pendente para processar");
                return;
            }
            
            logger.info("Processando {} eventos pendentes da Outbox", eventosPendentes.size());
            
            for (OutboxEvent evento : eventosPendentes) {
                processarEvento(evento);
            }
            
        } catch (Exception e) {
            logger.error("Erro ao processar eventos da Outbox: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Processa um evento individual
     */
    private void processarEvento(OutboxEvent evento) {
        try {
            logger.debug("Processando evento: id={}, tipo={}, agregado={}", 
                    evento.getId(), evento.getEventType(), evento.getAggregateId());
            
            // Define a routing key baseada no tipo de evento
            String routingKey = obterRoutingKey(evento.getEventType());
            
            // Publica o evento no RabbitMQ
            eventPublisher.publicarEvento(evento.getEventType(), routingKey, evento.getPayload());
            
            // Marca como processado
            evento.marcarComoProcessado();
            outboxRepository.atualizar(evento);
            
            logger.info("Evento processado com sucesso: id={}, tipo={}", 
                    evento.getId(), evento.getEventType());
            
        } catch (Exception e) {
            logger.error("Erro ao processar evento id={}: {}", evento.getId(), e.getMessage(), e);
            tratarFalha(evento, e.getMessage());
        }
    }
    
    /**
     * Trata falhas no processamento de eventos
     */
    private void tratarFalha(OutboxEvent evento, String errorMessage) {
        try {
            if (evento.podeReprocessar(maxRetries)) {
                // Marca como falho mas permite retry
                evento.marcarComoFalho(errorMessage);
                outboxRepository.atualizar(evento);
                
                logger.warn("Evento id={} falhou (tentativa {}/{}). Será reprocessado.", 
                        evento.getId(), evento.getRetryCount(), maxRetries);
            } else {
                // Excedeu número máximo de tentativas
                evento.marcarComoFalhoDefinitivo(errorMessage);
                outboxRepository.atualizar(evento);
                
                logger.error("Evento id={} falhou definitivamente após {} tentativas. Movido para FAILED.", 
                        evento.getId(), maxRetries);
            }
        } catch (Exception e) {
            logger.error("Erro ao atualizar status de falha do evento id={}: {}", 
                    evento.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * Obtém a routing key apropriada baseada no tipo de evento
     */
    private String obterRoutingKey(String eventType) {
        return switch (eventType) {
            case "PEDIDO_CRIADO" -> RabbitMQConfig.PEDIDO_CRIADO_ROUTING_KEY;
            case "PEDIDO_CANCELADO" -> RabbitMQConfig.PEDIDO_CANCELADO_ROUTING_KEY;
            default -> "pedido.evento";
        };
    }
    
    /**
     * Método para limpeza de eventos processados (executar periodicamente)
     * Executa uma vez por dia (configurável)
     */
    @Scheduled(cron = "${outbox.processor.cleanup-cron:0 0 2 * * ?}")
    @Transactional
    public void limparEventosProcessados() {
        try {
            logger.info("Iniciando limpeza de eventos processados da Outbox");
            
            // Busca eventos processados com mais de 7 dias
            List<OutboxEvent> eventosProcessados = outboxRepository.buscarPorStatus(OutboxStatus.PROCESSED);
            
            // Aqui você pode implementar lógica de remoção se desejar
            // Por exemplo: deletar eventos com mais de X dias
            
            logger.info("Limpeza de Outbox concluída. Total de eventos processados: {}", 
                    eventosProcessados.size());
            
        } catch (Exception e) {
            logger.error("Erro ao limpar eventos processados: {}", e.getMessage(), e);
        }
    }
}




