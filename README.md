# Microserviço de Pedidos

Microserviço de realização de pedidos para e-commerce, desenvolvido seguindo **Clean Architecture/Hexagonal Architecture** com **padrão Outbox** para garantir consistência eventual.

## 📋 Índice

- [Visão Geral](#visão-geral)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Padrões Implementados](#padrões-implementados)
- [Como Executar](#como-executar)
- [Endpoints da API](#endpoints-da-api)
- [Trade-offs e Decisões Arquiteturais](#trade-offs-e-decisões-arquiteturais)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Testes](#testes)

## 🎯 Visão Geral

O microserviço de pedidos é responsável por:
- Criar e gerenciar pedidos de clientes
- Validar disponibilidade de produtos (integração REST com microservice-produtos)
- Publicar eventos de pedidos via RabbitMQ (padrão Outbox)
- Garantir consistência eventual entre persistência e mensageria

## 🚀 Tecnologias

- **Java 21**
- **Spring Boot 3.2.0**
  - Spring Web
  - Spring Data JPA
  - Spring AMQP (RabbitMQ)
  - Spring Cache (Caffeine)
  - Spring Validation
- **PostgreSQL 15** - Banco de dados principal
- **RabbitMQ 3** - Sistema de mensageria
- **Maven** - Gerenciamento de dependências
- **Lombok** - Redução de boilerplate
- **SpringDoc OpenAPI** - Documentação Swagger
- **JaCoCo** - Cobertura de testes
- **Docker & Docker Compose** - Containerização

## 🏗️ Arquitetura

Este microserviço segue os princípios de **Clean Architecture (Arquitetura Hexagonal)**, garantindo separação de responsabilidades e independência de frameworks:

### Camadas

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
│  Controllers, DTOs, Exception Handlers, Mappers             │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER                         │
│  Use Cases, Service Ports, Events                           │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                            │
│  Entities, Business Rules, Repository Ports, Exceptions     │
└─────────────────────────────────────────────────────────────┘
                            ↑
┌─────────────────────────────────────────────────────────────┐
│                   INFRASTRUCTURE LAYER                       │
│  JPA Entities, Repositories, RabbitMQ, REST Clients, Config │
└─────────────────────────────────────────────────────────────┘
```

### Domain (Núcleo)
- **Entities**: `Pedido`, `ItemPedido`, `OutboxEvent`, `StatusPedido`, `OutboxStatus`
- **Repository Ports**: Interfaces que definem contratos de persistência
- **Exceptions**: `PedidoNotFoundException`, `ProdutoIndisponivelException`

### Application (Casos de Uso)
- `CriarPedidoUseCase`: Cria pedido + registro na Outbox (transação atômica)
- `BuscarPedidoPorIdUseCase`: Busca pedido por ID
- `ListarPedidosUseCase`: Lista pedidos (todos ou por cliente)
- `CancelarPedidoUseCase`: Cancela pedido + evento na Outbox

### Infrastructure (Implementações)
- **Persistence**: Implementações JPA dos repositórios
- **Messaging**: RabbitMQ publisher e Outbox processor
- **Client**: REST client para microservice-produtos (com cache)
- **Config**: Configurações Spring (Beans, RabbitMQ, Cache, OpenAPI)

### Presentation (API REST)
- **Controllers**: Endpoints REST
- **DTOs**: Objetos de transferência de dados
- **Exception Handlers**: Tratamento centralizado de erros

## 🔧 Padrões Implementados

### 1. Outbox Pattern
Garante consistência eventual entre banco de dados e mensageria:

**Fluxo:**
1. Pedido é salvo no banco
2. Evento é salvo na tabela `outbox` (mesma transação)
3. Job agendado lê eventos pendentes da `outbox`
4. Publica eventos no RabbitMQ
5. Marca eventos como processados

**Vantagens:**
- ✅ Garante que eventos não sejam perdidos
- ✅ Transação ACID (pedido + evento atômicos)
- ✅ Retry automático em caso de falha

**Trade-offs:**
- ⚠️ Latência adicional (eventos não são imediatos)
- ⚠️ Complexidade adicional (tabela Outbox + job)

### 2. Repository Pattern
Abstração da camada de persistência através de portas (interfaces) no domínio e adaptadores (implementações) na infraestrutura.

### 3. Cache Local
Cache com Caffeine para reduzir latência em consultas de produtos:
- TTL: 5 minutos
- Tamanho máximo: 100 entradas
- Cache invalidado automaticamente

### 4. REST Client Híbrido
Integração com microservice-produtos:
- Cache local para performance
- Validação em tempo real para estoque crítico
- Tratamento de erros e timeout configurável

## 📦 Como Executar

### Pré-requisitos
- Docker e Docker Compose
- Java 21 (opcional, se executar localmente)
- Maven (opcional, se executar localmente)

### Executar com Docker Compose

```bash
# Clone o repositório
cd /home/flavio/Projetos
git clone https://github.com/flaviohenso/microservice-pedidos.git
cd microservice-pedidos

# Suba todos os serviços
docker-compose up -d

# Verifique os logs
docker-compose logs -f microservice-pedidos
```

### Serviços Disponíveis

| Serviço | URL | Descrição |
|---------|-----|-----------|
| Microservice Pedidos | http://localhost:8081 | API de pedidos |
| Microservice Produtos | http://localhost:8080 | API de produtos |
| PostgreSQL | localhost:5432 | Banco de dados |
| RabbitMQ Management | http://localhost:15672 | Interface RabbitMQ (guest/guest) |
| Swagger Pedidos | http://localhost:8081/swagger-ui.html | Documentação API |
| Swagger Produtos | http://localhost:8080/swagger-ui.html | Documentação API |

### Executar Localmente (Desenvolvimento)

```bash
# Suba apenas as dependências (PostgreSQL + RabbitMQ)
docker-compose up -d postgres rabbitmq

# Execute a aplicação
mvn spring-boot:run

# Ou compile e execute o JAR
mvn clean package -DskipTests
java -jar target/microservice-pedidos-1.0.0.jar
```

## 📡 Endpoints da API

### Criar Pedido
```http
POST /api/pedidos
Content-Type: application/json

{
  "clienteId": 1,
  "itens": [
    {
      "produtoId": 1,
      "quantidade": 2
    },
    {
      "produtoId": 2,
      "quantidade": 1
    }
  ]
}
```

### Buscar Pedido por ID
```http
GET /api/pedidos/{id}
```

### Listar Todos os Pedidos
```http
GET /api/pedidos
```

### Listar Pedidos por Cliente
```http
GET /api/pedidos/cliente/{clienteId}
```

### Cancelar Pedido
```http
PUT /api/pedidos/{id}/cancelar
```

## ⚖️ Trade-offs e Decisões Arquiteturais

### ✅ Padrão Outbox

**Decisão:** Implementar Outbox Pattern para garantir consistência eventual.

**Prós:**
- Garante que eventos não sejam perdidos (transação ACID)
- Resiliência: retry automático em falhas do RabbitMQ
- Desacoplamento entre persistência e mensageria

**Contras:**
- Complexidade adicional (tabela + job scheduled)
- Latência: eventos não são publicados imediatamente (10s)
- Overhead de storage (tabela Outbox cresce)

**Mitigação:** 
- Job com intervalo curto (10s)
- Limpeza periódica de eventos processados

---

### ✅ Integração REST Híbrida (Cache + Validação)

**Decisão:** Cache local com validação em tempo real.

**Prós:**
- Performance: reduz latência em consultas repetidas
- Resiliência: tolera indisponibilidade temporária
- Simplicidade: sem necessidade de sincronização de eventos

**Contras:**
- Cache pode ficar desatualizado (staleness)
- Possível inconsistência temporária

**Mitigação:**
- TTL baixo (5 min)
- Validação crítica em tempo real (estoque)

---

### ✅ PostgreSQL vs H2

**Decisão:** PostgreSQL para produção.

**Prós:**
- Produção-ready
- Transações ACID robustas
- Persistência real (Outbox requer durabilidade)

**Contras:**
- Setup mais complexo
- Necessita infraestrutura

**Mitigação:** Docker Compose facilita setup local

---

### ⚠️ RabbitMQ vs Kafka

**Decisão:** RabbitMQ para MVP.

**RabbitMQ (escolhido):**
- Mais simples para começar
- Bom para comunicação request/reply
- Setup leve

**Kafka (alternativa futura):**
- Melhor para event sourcing e alta escala
- Log persistente de eventos
- Mais complexo

**Decisão:** RabbitMQ atende o caso de uso atual (notificações de pedido)

---

### ⚠️ Falta de Compensação (Saga)

**Limitação atual:** Não há compensação se o pedido for criado mas o estoque não for reservado.

**Cenário de risco:** Pedido salvo, evento publicado, mas produto esgotou entre validação e confirmação.

**Evolução futura:**
- Implementar Saga Pattern (orquestração ou coreografia)
- Reserva de estoque temporária no microservice-produtos
- Compensação automática (cancelar pedido se estoque indisponível)

**Decisão:** MVP sem compensação (aceitar edge case), documentar limitação

## 📂 Estrutura do Projeto

```
src/
├── main/
│   ├── java/com/ecommerce/pedidos/
│   │   ├── MicroservicePedidosApplication.java
│   │   ├── domain/
│   │   │   ├── entity/
│   │   │   ├── exception/
│   │   │   └── repository/
│   │   ├── application/
│   │   │   ├── usecase/
│   │   │   ├── service/
│   │   │   └── event/
│   │   ├── infrastructure/
│   │   │   ├── config/
│   │   │   ├── persistence/
│   │   │   ├── client/
│   │   │   └── messaging/
│   │   └── presentation/
│   │       ├── controller/
│   │       ├── dto/
│   │       ├── mapper/
│   │       └── exception/
│   └── resources/
│       └── application.properties
└── test/
    └── java/com/ecommerce/pedidos/
        ├── domain/entity/
        ├── application/usecase/
        └── infrastructure/
```

## 🧪 Testes

### Executar Testes

```bash
# Executar todos os testes
mvn test

# Executar testes com relatório de cobertura
mvn clean test jacoco:report

# Ver relatório de cobertura
open target/site/jacoco/index.html
```

### Cobertura de Testes

- **Testes Unitários**: Domain entities e use cases
- **Cobertura Mínima**: 70% (configurado no JaCoCo)

### Exemplos de Testes

- `ItemPedidoTest`: Validações de item de pedido
- `PedidoTest`: Regras de negócio de pedido
- `CriarPedidoUseCaseTest`: Fluxo completo de criação (com mocks)

## 🔐 Variáveis de Ambiente

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `SERVER_PORT` | Porta da aplicação | 8081 |
| `SPRING_DATASOURCE_URL` | URL do PostgreSQL | jdbc:postgresql://localhost:5432/pedidosdb |
| `SPRING_RABBITMQ_HOST` | Host do RabbitMQ | localhost |
| `PRODUTO_SERVICE_URL` | URL do microservice-produtos | http://localhost:8080/api/produtos |
| `OUTBOX_PROCESSOR_FIXED_DELAY` | Intervalo do job Outbox (ms) | 10000 |
| `OUTBOX_PROCESSOR_MAX_RETRIES` | Tentativas máximas de retry | 3 |

## 📈 Monitoramento

### RabbitMQ Management
- URL: http://localhost:15672
- Usuário: guest
- Senha: guest

### Métricas Disponíveis
- Eventos publicados
- Eventos pendentes na Outbox
- Taxa de sucesso/falha
- Estatísticas de cache

## 🤝 Contribuindo

1. Clone o repositório
2. Crie uma branch de feature: `git checkout -b feature/minha-feature`
3. Commit suas mudanças: `git commit -m 'feat: adiciona nova feature'`
4. Push para a branch: `git push origin feature/minha-feature`
5. Abra um Pull Request para `develop`

### GitFlow

- `main`: código em produção
- `develop`: branch de desenvolvimento principal
- `feature/*`: novas funcionalidades
- `release/*`: preparação de releases
- `hotfix/*`: correções urgentes

## 📝 Licença

Este projeto está sob a licença Apache 2.0.

## 👥 Autores

Equipe de Desenvolvimento - [contato@ecommerce.com](mailto:contato@ecommerce.com)

