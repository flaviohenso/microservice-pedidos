-- ===================================================================
-- V1: Criação da estrutura inicial de tabelas do Microserviço Pedidos
-- ===================================================================

-- Tabela de Pedidos
CREATE TABLE IF NOT EXISTS pedidos (
    id BIGSERIAL PRIMARY KEY,
    numero_pedido VARCHAR(50) NOT NULL CONSTRAINT uk_pedidos_numero_pedido UNIQUE,
    cliente_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    data_criacao TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    data_atualizacao TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_pedidos_cliente_id ON pedidos (cliente_id);
CREATE INDEX IF NOT EXISTS idx_pedidos_status ON pedidos (status);

-- Tabela de Itens de Pedido
CREATE TABLE IF NOT EXISTS itens_pedido (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL REFERENCES pedidos (id) ON DELETE CASCADE,
    produto_id BIGINT NOT NULL,
    nome_produto VARCHAR(200) NOT NULL,
    quantidade INTEGER NOT NULL,
    preco_unitario NUMERIC(10, 2) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_itens_pedido_pedido_id ON itens_pedido (pedido_id);
CREATE INDEX IF NOT EXISTS idx_itens_pedido_produto_id ON itens_pedido (produto_id);

-- Tabela de Eventos Outbox (Transactional Outbox Pattern)
CREATE TABLE IF NOT EXISTS outbox (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITHOUT TIME ZONE,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox (status);
CREATE INDEX IF NOT EXISTS idx_outbox_created_at ON outbox (created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate ON outbox (aggregate_type, aggregate_id);
