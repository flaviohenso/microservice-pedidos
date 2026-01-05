# Microserviço de Pedidos

Microserviço de realização de pedidos para e-commerce, desenvolvido seguindo **Clean Architecture/Hexagonal Architecture** com **padrão Outbox** para garantir consistência eventual e **Virtual Threads** para alta escalabilidade.

## 📋 Índice

- [Visão Geral](#visão-geral)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Diagrama de Sequência](#diagrama-de-sequência)
- [Resiliência](#resiliência)
- [Padrões Implementados](#padrões-implementados)
- [Como Executar](#como-executar)
- [Endpoints da API](#endpoints-da-api)
- [Trade-offs e Decisões Arquiteturais](#trade-offs-e-decisões-arquiteturais)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Testes](#testes)

## 🎯 Visão Geral

O microserviço de pedidos é responsável por:
- Criar e gerenciar pedidos de clientes
- Validar disponibilidade de produtos (integração REST com microservice-produtos via **Feign Client**)
- Publicar eventos de pedidos via RabbitMQ (padrão Outbox)
- Garantir consistência eventual entre persistência e mensageria
- Alta disponibilidade com **Circuit Breaker**, **Retry** e **Fallback**

## 🚀 Tecnologias

- **Java 21** (com Virtual Threads habilitado)
- **Spring Boot 3.3.1**
  - Spring Web (MVC)
  - Spring Data JPA
  - Spring AMQP (RabbitMQ)
  - Spring Cache (Caffeine)
  - Spring Validation
  - Spring Cloud OpenFeign
- **Resilience4j** - Circuit Breaker, Retry, Rate Limiter
- **PostgreSQL 15** - Banco de dados principal
- **RabbitMQ 3** - Sistema de mensageria
- **Maven** - Gerenciamento de dependências
- **Lombok** - Redução de boilerplate
- **SpringDoc OpenAPI** - Documentação Swagger
- **JaCoCo** - Cobertura de testes
- **Docker & Docker Compose** - Containerização

## 🏗️ Arquitetura

Este microserviço segue os princípios de **Clean Architecture (Arquitetura Hexagonal)**, garantindo separação de responsabilidades e independência de frameworks.

> 📄 **Diagramas para impressão disponíveis em:**
> - [Arquitetura em Camadas (HTML)](docs/arquitetura-camadas.html)
> - [Diagrama de Sequência (HTML)](docs/diagrama-sequencia.html)
> - [Arquitetura de Resiliência (HTML)](docs/resiliencia.html)

### Diagrama de Arquitetura em Camadas

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                            🌐 EXTERNAL SYSTEMS                                       │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐                   │
│  │   HTTP Client    │  │   PostgreSQL     │  │    RabbitMQ      │                   │
│  │   (Swagger UI)   │  │   (Database)     │  │    (Message)     │                   │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘                   │
└───────────┼─────────────────────┼─────────────────────┼──────────────────────────────┘
            │                     │                     │
            ▼                     ▼                     ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                         📦 PRESENTATION LAYER                                        │
│  ┌─────────────────────────────────────────────────────────────────────────────┐    │
│  │ Controllers: PedidoController, ResilienceHealthController                   │    │
│  │ DTOs: PedidoRequestDTO, PedidoResponseDTO, ItemPedidoDTO                   │    │
│  │ Exception Handler: GlobalExceptionHandler                                   │    │
│  │ Mapper: PedidoDTOMapper                                                     │    │
│  └─────────────────────────────────────────────────────────────────────────────┘    │
└───────────────────────────────────────┬─────────────────────────────────────────────┘
                                        │ chama Use Cases
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                          ⚙️ APPLICATION LAYER                                        │
│  ┌─────────────────────────────────────────────────────────────────────────────┐    │
│  │ Use Cases: CriarPedidoUseCase, BuscarPedidoPorIdUseCase,                   │    │
│  │            CancelarPedidoUseCase, ListarPedidosUseCase                     │    │
│  │ Ports: ProdutoServicePort, EventPublisherPort                              │    │
│  │ Events: PedidoCriadoEvent, ItemPedidoEvent                                 │    │
│  └─────────────────────────────────────────────────────────────────────────────┘    │
└───────────────────────────────────────┬─────────────────────────────────────────────┘
                                        │ usa entidades e ports do domain
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                           🎯 DOMAIN LAYER (Core)                                     │
│  ┌─────────────────────────────────────────────────────────────────────────────┐    │
│  │ Entities: Pedido, ItemPedido, OutboxEvent, StatusPedido, OutboxStatus      │    │
│  │ Repository Ports: PedidoRepositoryPort, OutboxRepositoryPort               │    │
│  │ Exceptions: PedidoNotFoundException, ProdutoIndisponivelException          │    │
│  └─────────────────────────────────────────────────────────────────────────────┘    │
└───────────────────────────────────────┬─────────────────────────────────────────────┘
                                        ▲ implementa interfaces do domain
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                        🔧 INFRASTRUCTURE LAYER                                       │
│  ┌─────────────────────────────────────────────────────────────────────────────┐    │
│  │ Persistence: PedidoRepositoryImpl, OutboxRepositoryImpl, JPA Entities      │    │
│  │ External Clients: ProdutoFeignClient, ProdutoServiceAdapter                │    │
│  │ Messaging: RabbitMQPublisher, OutboxProcessor                              │    │
│  │ Config: BeanConfiguration, CacheConfig, RabbitMQConfig, OpenApiConfig      │    │
│  └─────────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

### Regra de Dependência (DIP)

```
Presentation ──────► Application ──────► Domain ◄────────── Infrastructure
```

As camadas internas definem interfaces (Ports) e as camadas externas as implementam (Adapters).

### Resumo das Camadas

| Camada | Responsabilidade | Componentes Principais |
|--------|------------------|------------------------|
| **Presentation** | Entrada/Saída HTTP, validação, conversão DTOs | Controllers, DTOs, ExceptionHandler |
| **Application** | Orquestração de casos de uso, lógica de aplicação | Use Cases, Ports, Events |
| **Domain** | Regras de negócio, entidades, contratos | Entities, Repository Ports, Exceptions |
| **Infrastructure** | Implementações técnicas, adaptadores externos | JPA, Feign Client, RabbitMQ, Configs |

## 🔄 Diagrama de Sequência

### Fluxo: POST /api/pedidos (Criar Pedido) com Virtual Threads

```
┌──────────┐     ┌────────────┐     ┌─────────────────┐     ┌──────────────────┐     ┌───────────────────┐     ┌──────────────────┐     ┌─────────────┐
│  Client  │     │   Tomcat   │     │ PedidoController│     │CriarPedidoUseCase│     │ProdutoServiceAdapt│     │ PedidoRepository │     │ Produtos API│
│          │     │  (VThread) │     │                 │     │                  │     │     (Feign)       │     │                  │     │  (External) │
└────┬─────┘     └─────┬──────┘     └───────┬─────────┘     └────────┬─────────┘     └─────────┬─────────┘     └────────┬─────────┘     └──────┬──────┘
     │                 │                    │                        │                         │                        │                      │
     │ POST /api/pedidos                    │                        │                         │                        │                      │
     │────────────────►│                    │                        │                         │                        │                      │
     │                 │                    │                        │                         │                        │                      │
     │                 │ ⚡ Cria Virtual Thread                      │                         │                        │                      │
     │                 │────────────────────►                        │                         │                        │                      │
     │                 │                    │                        │                         │                        │                      │
     │                 │                    │ criarPedido(request)   │                         │                        │                      │
     │                 │                    │───────────────────────►│                         │                        │                      │
     │                 │                    │                        │                         │                        │                      │
     │                 │                    │                        │ buscarProdutoPorId(id)  │                        │                      │
     │                 │                    │                        │────────────────────────►│                        │                      │
     │                 │                    │                        │                         │                        │                      │
     │                 │                    │                        │                         │ 🔄 HTTP GET (I/O)      │                      │
     │                 │                    │                        │                         │───────────────────────────────────────────────►
     │                 │                    │                        │                         │                        │                      │
     │                 │                    │                        │    ┌────────────────────┤                        │                      │
     │                 │                    │                        │    │ ⏸️ VThread SUSPENDE │                        │                      │
     │                 │                    │                        │    │ Carrier thread     │                        │                      │
     │                 │                    │                        │    │ liberada!          │                        │                      │
     │                 │                    │                        │    └────────────────────┤                        │                      │
     │                 │                    │                        │                         │                        │                      │
     │                 │                    │                        │                         │◄──────────────────────────────────────────────│
     │                 │                    │                        │                         │       ProdutoDTO       │                      │
     │                 │                    │                        │    ┌────────────────────┤                        │                      │
     │                 │                    │                        │    │ ▶️ VThread RETOMA   │                        │                      │
     │                 │                    │                        │    └────────────────────┤                        │                      │
     │                 │                    │                        │                         │                        │                      │
     │                 │                    │                        │◄────────────────────────│                        │                      │
     │                 │                    │                        │   Optional<ProdutoDTO>  │                        │                      │
     │                 │                    │                        │                         │                        │                      │
     │                 │                    │                        │ pedidoRepository.salvar(pedido)                  │                      │
     │                 │                    │                        │────────────────────────────────────────────────►│                      │
     │                 │                    │                        │                         │                        │                      │
     │                 │                    │                        │    ┌──────────────────────────────────────────────┤                      │
     │                 │                    │                        │    │ ⏸️ VThread SUSPENDE (aguardando DB - JDBC)   │                      │
     │                 │                    │                        │    └──────────────────────────────────────────────┤                      │
     │                 │                    │                        │                         │                        │                      │
     │                 │                    │                        │                         │    ┌───────────────────┤                      │
     │                 │                    │                        │                         │    │ INSERT pedido     │                      │
     │                 │                    │                        │                         │    │ INSERT itens      │                      │
     │                 │                    │                        │                         │    │ INSERT outbox     │                      │
     │                 │                    │                        │                         │    └───────────────────┤                      │
     │                 │                    │                        │                         │                        │                      │
     │                 │                    │                        │    ┌──────────────────────────────────────────────┤                      │
     │                 │                    │                        │    │ ▶️ VThread RETOMA                            │                      │
     │                 │                    │                        │    └──────────────────────────────────────────────┤                      │
     │                 │                    │                        │                         │                        │                      │
     │                 │                    │                        │◄───────────────────────────────────────────────────                      │
     │                 │                    │                        │       Pedido (salvo)    │                        │                      │
     │                 │                    │                        │                         │                        │                      │
     │                 │                    │◄───────────────────────│                         │                        │                      │
     │                 │                    │         Pedido         │                         │                        │                      │
     │                 │                    │                        │                         │                        │                      │
     │                 │◄───────────────────│                        │                         │                        │                      │
     │                 │ ResponseEntity<DTO>│                        │                         │                        │                      │
     │                 │                    │                        │                         │                        │                      │
     │◄────────────────│                    │                        │                         │                        │                      │
     │ HTTP 201 Created│                    │                        │                         │                        │                      │
     │                 │                    │                        │                         │                        │                      │
```

### Virtual Threads - Como Funciona

Com **Java 21 Virtual Threads** habilitado (`spring.threads.virtual.enabled=true`):

1. Cada requisição HTTP é processada em uma **Virtual Thread** (VThread)
2. Quando há operação de I/O bloqueante (HTTP call, JDBC), a VThread **suspende**
3. A **carrier thread** (thread do SO) é liberada para processar outras VThreads
4. Quando o I/O completa, a VThread **retoma** em qualquer carrier disponível

**Resultado:** Milhares de requisições simultâneas com poucas threads do SO!

## 🛡️ Resiliência

O microserviço implementa múltiplos padrões de resiliência usando **Resilience4j**:

### Arquitetura de Resiliência

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         FLUXO DE RESILIÊNCIA                                 │
│                                                                              │
│  Request ──► Retry ──► Circuit Breaker ──► Cache ──► Feign Client ──► API   │
│                │              │               │              │               │
│                │              │               │              │               │
│                ▼              ▼               ▼              ▼               │
│           [3 tentativas] [CLOSED/OPEN]  [Caffeine]    [Fallback]            │
│                              │                              │               │
│                              └──────────────────────────────┘               │
│                                         │                                    │
│                                         ▼                                    │
│                                    [Fallback Service]                        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1. Retry (Tentativas Automáticas)

Realiza múltiplas tentativas em caso de falha temporária.

**Configuração:**
```properties
resilience4j.retry.instances.produtoService.max-attempts=3
resilience4j.retry.instances.produtoService.wait-duration=1s
resilience4j.retry.instances.produtoService.enable-exponential-backoff=true
resilience4j.retry.instances.produtoService.exponential-backoff-multiplier=2
```

**Comportamento:**
- **Tentativa 1:** Falha → aguarda 1s
- **Tentativa 2:** Falha → aguarda 2s (exponential backoff)
- **Tentativa 3:** Falha → Circuit Breaker/Fallback

### 2. Circuit Breaker (Disjuntor)

Protege o sistema de sobrecarga quando o serviço externo está indisponível.

**Estados do Circuit Breaker:**

```
     ┌─────────────┐
     │   CLOSED    │ ◄── Estado normal
     │ (Funcional) │     Requisições passam
     └──────┬──────┘
            │
            │ Taxa de falha > 50%
            ▼
     ┌─────────────┐
     │    OPEN     │ ◄── Serviço indisponível
     │ (Bloqueado) │     Requisições rejeitadas
     └──────┬──────┘     (vai direto pro Fallback)
            │
            │ Após 30 segundos
            ▼
     ┌─────────────┐
     │ HALF-OPEN   │ ◄── Testando recuperação
     │  (Teste)    │     Permite 3 requisições
     └─────────────┘
            │
            ├── Sucesso → CLOSED
            └── Falha → OPEN
```

**Configuração:**
```properties
resilience4j.circuitbreaker.instances.produtoService.sliding-window-size=10
resilience4j.circuitbreaker.instances.produtoService.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.produtoService.wait-duration-in-open-state=30s
resilience4j.circuitbreaker.instances.produtoService.permitted-number-of-calls-in-half-open-state=3
```

### 3. Fallback (Serviço Alternativo)

Quando todas as tentativas falham ou o Circuit Breaker está aberto, aciona o serviço de fallback.

**Implementação:**
```java
@FeignClient(
    name = "produto-service",
    url = "${produto.service.url}",
    fallback = ProdutoFeignClientFallback.class
)
public interface ProdutoFeignClient {
    @GetMapping("/{id}")
    ProdutoDTO buscarPorId(@PathVariable("id") Long id);
}
```

**Fallback:**
```java
@Component
public class ProdutoFeignClientFallback implements ProdutoFeignClient {
    @Override
    public ProdutoDTO buscarPorId(Long id) {
        logger.warn("FALLBACK ativado para produto {}", id);
        // Chama serviço alternativo ou retorna cache
        return fallbackService.buscarPorId(id);
    }
}
```

### 4. Cache (Caffeine)

Reduz latência e carga no serviço externo.

**Configuração:**
```properties
spring.cache.type=caffeine
spring.cache.cache-names=produtos
spring.cache.caffeine.spec=expireAfterWrite=5m,maximumSize=100
```

**Uso:**
```java
@Cacheable(value = "produtos", key = "#id", unless = "#result == null")
public Optional<ProdutoDTO> buscarProdutoPorId(Long id) {
    return produtoFeignClient.buscarPorId(id);
}
```

### Resumo da Resiliência

| Mecanismo | Função | Quando Atua |
|-----------|--------|-------------|
| **Retry** | Tenta novamente | Falhas temporárias (timeout, conexão) |
| **Circuit Breaker** | Bloqueia chamadas | Serviço degradado (>50% falhas) |
| **Fallback** | Serviço alternativo | Todas tentativas falharam |
| **Cache** | Reduz latência | Consultas repetidas |

## 🔧 Padrões Implementados

### 1. Outbox Pattern

Garante consistência eventual entre banco de dados e mensageria:

```
┌─────────────────────────────────────────────────────────────────┐
│                      OUTBOX PATTERN                              │
│                                                                  │
│  1. Pedido criado ─────► 2. Evento salvo na Outbox (TX atômica) │
│                                      │                           │
│                                      ▼                           │
│                          3. OutboxProcessor (Job @Scheduled)     │
│                                      │                           │
│                                      ▼                           │
│                          4. Publica no RabbitMQ                  │
│                                      │                           │
│                                      ▼                           │
│                          5. Marca como PROCESSED                 │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**Vantagens:**
- ✅ Garante que eventos não sejam perdidos (transação ACID)
- ✅ Resiliência: retry automático em falhas do RabbitMQ
- ✅ Desacoplamento entre persistência e mensageria

### 2. Repository Pattern

Abstração da camada de persistência através de portas (interfaces) no domínio e adaptadores (implementações) na infraestrutura.

### 3. Feign Client

Cliente HTTP declarativo para comunicação entre microserviços.

```java
@FeignClient(
    name = "produto-service",
    url = "${produto.service.url}",
    fallback = ProdutoFeignClientFallback.class
)
public interface ProdutoFeignClient {
    @GetMapping("/{id}")
    ProdutoDTO buscarPorId(@PathVariable("id") Long id);
}
```

### 4. Virtual Threads (Project Loom)

Threads leves do Java 21 que permitem alta concorrência sem complexidade de código reativo.

```properties
spring.threads.virtual.enabled=true
```

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
| Actuator Health | http://localhost:8081/actuator/health | Health check |

### Executar Localmente (Desenvolvimento)

```bash
# Suba apenas as dependências (PostgreSQL + RabbitMQ + Produtos)
docker-compose up -d postgres rabbitmq microservice-produtos

# Execute a aplicação
mvn spring-boot:run

# Ou compile e execute o JAR
mvn clean package -DskipTests
java -jar target/microservice-pedidos-1.0.0.jar
```

## 📡 Endpoints da API

### Criar Pedido
```http
POST http://localhost:8081/api/pedidos
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
GET http://localhost:8081/api/pedidos/{id}
```

### Listar Todos os Pedidos
```http
GET http://localhost:8081/api/pedidos
```

### Listar Pedidos por Cliente
```http
GET http://localhost:8081/api/pedidos/cliente/{clienteId}
```

### Cancelar Pedido
```http
PUT http://localhost:8081/api/pedidos/{id}/cancelar
```

### Health Check
```http
GET http://localhost:8081/actuator/health
```

## ⚖️ Trade-offs e Decisões Arquiteturais

### ✅ Virtual Threads vs WebFlux

**Decisão:** Virtual Threads (Java 21) em vez de WebFlux reativo.

**Prós:**
- Código síncrono/imperativo (mais simples de entender)
- Compatível com todo ecossistema existente (JPA, RestTemplate, etc.)
- Debug mais fácil (stack traces legíveis)
- Alta escalabilidade (milhares de threads virtuais)

**Contras:**
- Requer Java 21+
- Blocos `synchronized` podem causar "pinning"

---

### ✅ Feign vs RestTemplate

**Decisão:** Feign Client para comunicação entre microserviços.

**Prós:**
- Código declarativo (interface + anotações)
- Integração nativa com Spring Cloud
- Suporte a fallback integrado
- Menos boilerplate

**Contras:**
- Dependência do Spring Cloud
- Menos controle granular que RestTemplate

---

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

### ✅ Resilience4j

**Decisão:** Usar Resilience4j para resiliência em vez de Hystrix (deprecated).

**Prós:**
- Ativo e mantido
- Suporte a Java 21 e Spring Boot 3
- Configuração via properties
- Métricas integradas

## 📂 Estrutura do Projeto

```
src/
├── main/
│   ├── java/com/ecommerce/pedidos/
│   │   ├── MicroservicePedidosApplication.java
│   │   ├── domain/
│   │   │   ├── entity/
│   │   │   │   ├── Pedido.java
│   │   │   │   ├── ItemPedido.java
│   │   │   │   ├── OutboxEvent.java
│   │   │   │   ├── StatusPedido.java
│   │   │   │   └── OutboxStatus.java
│   │   │   ├── exception/
│   │   │   │   ├── PedidoNotFoundException.java
│   │   │   │   └── ProdutoIndisponivelException.java
│   │   │   └── repository/
│   │   │       ├── PedidoRepositoryPort.java
│   │   │       └── OutboxRepositoryPort.java
│   │   ├── application/
│   │   │   ├── usecase/
│   │   │   │   ├── CriarPedidoUseCase.java
│   │   │   │   ├── BuscarPedidoPorIdUseCase.java
│   │   │   │   ├── ListarPedidosUseCase.java
│   │   │   │   └── CancelarPedidoUseCase.java
│   │   │   ├── service/
│   │   │   │   ├── ProdutoServicePort.java
│   │   │   │   └── EventPublisherPort.java
│   │   │   ├── dto/
│   │   │   │   └── ItemPedidoRequest.java
│   │   │   └── event/
│   │   │       ├── PedidoCriadoEvent.java
│   │   │       └── ItemPedidoEvent.java
│   │   ├── infrastructure/
│   │   │   ├── config/
│   │   │   │   ├── BeanConfiguration.java
│   │   │   │   ├── CacheConfig.java
│   │   │   │   ├── RabbitMQConfig.java
│   │   │   │   ├── RestClientConfig.java
│   │   │   │   └── OpenApiConfig.java
│   │   │   ├── persistence/
│   │   │   │   ├── entity/
│   │   │   │   ├── mapper/
│   │   │   │   └── repository/
│   │   │   ├── client/
│   │   │   │   ├── ProdutoFeignClient.java
│   │   │   │   ├── ProdutoFeignClientFallback.java
│   │   │   │   ├── ProdutoServiceAdapter.java
│   │   │   │   └── dto/
│   │   │   └── messaging/
│   │   │       ├── RabbitMQPublisher.java
│   │   │       └── OutboxProcessor.java
│   │   └── presentation/
│   │       ├── controller/
│   │       │   ├── PedidoController.java
│   │       │   └── ResilienceHealthController.java
│   │       ├── dto/
│   │       ├── mapper/
│   │       └── exception/
│   │           └── GlobalExceptionHandler.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/com/ecommerce/pedidos/
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

### Teste de Carga (Virtual Threads)

```bash
# Usando hey (instalar: go install github.com/rakyll/hey@latest)
hey -n 1000 -c 200 http://localhost:8081/api/pedidos

# Usando curl para criar pedido
curl -X POST http://localhost:8081/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{"clienteId": 1, "itens": [{"produtoId": 1, "quantidade": 2}]}'
```

## 🔐 Variáveis de Ambiente

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `SERVER_PORT` | Porta da aplicação | 8081 |
| `SPRING_DATASOURCE_URL` | URL do PostgreSQL | jdbc:postgresql://localhost:5432/pedidosdb |
| `SPRING_RABBITMQ_HOST` | Host do RabbitMQ | localhost |
| `PRODUTO_SERVICE_URL` | URL do microservice-produtos | http://localhost:8080/api/produtos |
| `FALLBACK_SERVICE_URL` | URL do serviço de fallback | http://localhost:8083/api/produtos |
| `OUTBOX_PROCESSOR_FIXED_DELAY` | Intervalo do job Outbox (ms) | 10000 |
| `SPRING_THREADS_VIRTUAL_ENABLED` | Habilitar Virtual Threads | true |

## 📈 Monitoramento

### Actuator Endpoints

| Endpoint | Descrição |
|----------|-----------|
| `/actuator/health` | Status da aplicação |
| `/actuator/info` | Informações da aplicação |
| `/actuator/metrics` | Métricas do sistema |
| `/actuator/caches` | Status dos caches |
| `/actuator/threaddump` | Dump de threads |

### RabbitMQ Management
- URL: http://localhost:15672
- Usuário: guest
- Senha: guest

## 🤝 Contribuindo

1. Clone o repositório
2. Crie uma branch de feature: `git checkout -b feature/minha-feature`
3. Commit suas mudanças: `git commit -m 'feat: adiciona nova feature'`
4. Push para a branch: `git push origin feature/minha-feature`
5. Abra um Pull Request para `develop`

## 📝 Licença

Este projeto está sob a licença Apache 2.0.

## 👥 Autor

Desenvolvido por [Flávio Henrique](https://github.com/flaviohenso)
