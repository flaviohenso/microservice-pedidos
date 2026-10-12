# Microserviço de Pedidos

Microserviço de realização e gerenciamento de pedidos para e-commerce, desenvolvido com **Java 21**, **Spring Boot 4.0.8**, seguindo os princípios de **Clean Architecture / Arquitetura Hexagonal**, **Transactional Outbox Pattern** com concorrência segura (`SKIP LOCKED`) para garantia de consistência eventual, e **Virtual Threads** para alta escalabilidade.

---

## 📋 Índice

- [Visão Geral](#-visão-geral)
- [Tecnologias](#-tecnologias)
- [Arquitetura Hexagonal](#️-arquitetura-hexagonal)
- [Diagrama de Sequência & Virtual Threads](#-diagrama-de-sequência--virtual-threads)
- [Resiliência & Tolerância a Falhas](#-resiliência--tolerância-a-falhas)
- [Padrões e Diferenciais Implementados](#-padrões-e-diferenciais-implementados)
- [Banco de Dados & Migrações (Flyway)](#-banco-de-dados--migrações-flyway)
- [Como Executar](#-como-executar)
- [Endpoints da API](#-endpoints-da-api)
- [Monitoramento & Actuator](#-monitoramento--actuator)
- [Trade-offs e Decisões Arquiteturais](#-trade-offs-e-decisões-arquiteturais)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [Testes Automatizados](#-testes-automatizados)

---

## 🎯 Visão Geral

O microserviço de pedidos é o núcleo do processamento de compras da plataforma, responsável por:
- Criar, consultar e cancelar pedidos de clientes com validação estrita de dados e regras de negócio.
- Validar disponibilidade de catálogo e estoque de forma resiliente junto ao `microservice-produtos` via **OpenFeign 5** com **Circuit Breaker**, **Retry**, **Cache** e **Fallback**.
- Garantir que chamadas de rede externas e I/O bloqueante ocorram **fora de transações de banco de dados**, liberando conexões do pool Hikari imediatamente.
- Publicar eventos de domínio (`pedido.criado`, `pedido.cancelado`) via RabbitMQ através de **Transactional Outbox** com concorrência distribuída multi-instância (`FOR UPDATE SKIP LOCKED`) e expurgo periódico configurável.
- Evolução de esquema controlada e versionada por **Flyway**, validada rigorosamente pelo Hibernate (`ddl-auto=validate`).

---

## 🚀 Tecnologias

- **Linguagem & Runtime**: [Java 21](https://openjdk.org/projects/jdk/21/) (com Virtual Threads ativado)
- **Framework Principal**: [Spring Boot 4.0.8](https://spring.io/projects/spring-boot) / Spring Framework 7
  - Spring Web (MVC)
  - Spring Data JPA / Hibernate 7
  - Spring AMQP 4 (RabbitMQ)
  - Spring Cache (Caffeine)
  - Spring Validation (Jakarta Validation)
  - Spring Cloud 2025 (OpenFeign 5.0.3 & CircuitBreaker Resilience4j 5.0.3)
- **Serialização JSON**: [Jackson 3](https://github.com/FasterXML/jackson) (`tools.jackson`)
- **Resiliência**: [Resilience4j 2.4.0](https://resilience4j.readme.io/) (Circuit Breaker, Retry, TimeLimiter)
- **Banco de Dados**: [PostgreSQL 16](https://www.postgresql.org/)
- **Migrações de Banco**: [Flyway 11](https://flywaydb.org/) (`spring-boot-starter-flyway`)
- **Mensageria**: [RabbitMQ 3.13+](https://www.rabbitmq.com/) (com Dead Letter Queue e filas dedicadas)
- **Documentação de API**: [SpringDoc OpenAPI 3.0](https://springdoc.org/) (Swagger UI)
- **Testes & Infraestrutura de Teste**:
  - JUnit 5 & AssertJ
  - [Testcontainers 2.0.5](https://testcontainers.com/) (PostgreSQL & RabbitMQ gerenciados via `@ServiceConnection`)
  - [WireMock](https://wiremock.org/) para isolamento de chamadas HTTP externas
  - JaCoCo para cobertura de código

---

## 🏗️ Arquitetura Hexagonal

O projeto segue estritamente a **Clean Architecture (Portas e Adaptadores)**. As camadas internas de negócio (Domain e Application) possuem **zero dependências** de bibliotecas de infraestrutura, clientes HTTP, Spring AMQP ou anotações invasivas de persistência:

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                            🌐 EXTERNAL CLIENTS & BROKER                             │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐                   │
│  │   HTTP Client    │  │   PostgreSQL     │  │    RabbitMQ      │                   │
│  │   (Swagger UI)   │  │   (Database)     │  │    (Broker)      │                   │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘                   │
└───────────┼─────────────────────┼─────────────────────┼──────────────────────────────┘
            │                     │                     │
            ▼                     ▼                     ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                         📦 PRESENTATION LAYER                                        │
│  Controllers: PedidoController, ResilienceHealthController                          │
│  DTOs: PedidoRequestDTO, PedidoResponseDTO, ItemPedidoDTO                           │
│  Exception Handling: GlobalExceptionHandler                                         │
│  Mapper: PedidoDTOMapper                                                            │
└───────────────────────────────────────┬─────────────────────────────────────────────┘
                                        │ invoca Use Cases
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                          ⚙️ APPLICATION LAYER                                        │
│  Use Cases: CriarPedidoUseCase, BuscarPedidoPorIdUseCase,                           │
│             CancelarPedidoUseCase, ListarPedidosUseCase                             │
│  Ports Out: ProdutoServicePort, EventPublisherPort, SalvarPedidoTransacionalPort    │
│  DTOs de Aplicação: ProdutoDTO, ItemPedidoRequest                                   │
│  Eventos de Aplicação: PedidoCriadoEvent, ItemPedidoEvent                           │
└───────────────────────────────────────┬─────────────────────────────────────────────┘
                                        │ usa entidades e regras de domínio
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                           🎯 DOMAIN LAYER (Core)                                     │
│  Entities: Pedido, ItemPedido, OutboxEvent, StatusPedido, OutboxStatus              │
│  Ports: PedidoRepositoryPort, OutboxRepositoryPort                                  │
│  Exceptions: PedidoNotFoundException, ProdutoIndisponivelException                  │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                        ▲ implementa interfaces (Ports)
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                        🔧 INFRASTRUCTURE LAYER                                       │
│  Persistence: PedidoRepositoryImpl, OutboxRepositoryImpl, SalvarPedidoTransacional  │
│  Client HTTP: ProdutoFeignClient, ProdutoFeignClientFallback, ProdutoServiceAdapter  │
│  Mensageria: RabbitMQPublisher, OutboxProcessor (@Scheduled)                        │
│  Config: BeanConfiguration, CacheConfig, RabbitMQConfig, Flyway, OpenApiConfig      │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

### Regra de Dependência (DIP)

```
Presentation ──────► Application ──────► Domain ◄────────── Infrastructure
```

- **Domain**: Regras de negócio puras (cálculo de totais, validação de itens, transições de status).
- **Application**: Orquestração dos casos de uso, fronteiras de transação atômica (`SalvarPedidoTransacionalPort`) e interfaces de portas de saída.
- **Infrastructure**: Implementações JPA, clientes OpenFeign, listeners, publicadores RabbitMQ e migrações Flyway.
- **Presentation**: Endpoints REST, tratamento de exceções global e documentação OpenAPI.

---

## 🔄 Diagrama de Sequência & Virtual Threads

### Fluxo de Criação de Pedido com Isolamento de Transação

Uma das melhorias mais críticas da arquitetura foi a **remoção de I/O de rede de dentro de `@Transactional`**: a validação do produto ocorre fora de qualquer transação de banco. A transação só é aberta no momento de persistir o pedido e o evento na Outbox:

```
Client         Tomcat(VThread)   PedidoController   CriarPedidoUseCase   ProdutoServiceAdapter   SalvarPedidoTransacional   PostgreSQL
  │                   │                  │                  │                      │                        │                   │
  │ POST /api/pedidos │                  │                  │                      │                        │                   │
  │──────────────────►│                  │                  │                      │                        │                   │
  │                   │ criarPedido()    │                  │                      │                        │                   │
  │                   │─────────────────►│                  │                      │                        │                   │
  │                   │                  │ executar()       │                      │                        │                   │
  │                   │                  │─────────────────►│                      │                        │                   │
  │                   │                  │                  │ buscarProdutoPorId() │                        │                   │
  │                   │                  │                  │─────────────────────►│ (I/O HTTP Externo)     │                   │
  │                   │                  │                  │                      │ SEM TRANSAÇÃO DE BANCO │                   │
  │                   │                  │                  │◄─────────────────────│                        │                   │
  │                   │                  │                  │                      │                        │                   │
  │                   │                  │                  │ salvarPedidoEEventoOutbox()                   │                   │
  │                   │                  │                  │──────────────────────────────────────────────►│                   │
  │                   │                  │                  │                      │                        │ ── BEGIN TX ────  │
  │                   │                  │                  │                      │                        │ INSERT INTO pedido│
  │                   │                  │                  │                      │                        │ INSERT INTO outbox│
  │                   │                  │                  │                      │                        │ ── COMMIT TX ───  │
  │                   │                  │                  │◄──────────────────────────────────────────────│                   │
  │                   │                  │◄─────────────────│                      │                        │                   │
  │                   │◄─────────────────│                  │                      │                        │                   │
  │ HTTP 201 Created  │                  │                  │                      │                        │                   │
  │◄──────────────────│                  │                  │                      │                        │                   │
```

### Virtual Threads (Java 21 Loom)
Com `spring.threads.virtual.enabled=true`:
1. Cada requisição HTTP roda em uma Virtual Thread leve.
2. Durante bloqueios de I/O (espera de resposta HTTP ou chamadas JDBC), a Virtual Thread desassocia-se da Carrier Thread do SO, liberando-a para atender outras conexões.
3. Garante altíssimo throughput concorrente com consumo mínimo de recursos.

---

## 🛡️ Resiliência & Tolerância a Falhas

A integração com serviços externos (`microservice-produtos`) conta com múltiplos mecanismos de defesa gerenciados pelo **Resilience4j**:

```
Requisição ──► Cache Caffeine ──► Retry (3x) ──► Circuit Breaker ──► OpenFeign HTTP ──► microservice-produtos
                     │                                   │                  │
                     └─ Retorna se em cache              └─ Fallback se OPEN└─ Fallback em falha de conexão/timeout
```

1. **Cache Local (Caffeine)**: Consultas a produtos recentes são cacheadas em memória por 5 minutos, eliminando chamadas de rede desnecessárias.
2. **Retry com Backoff Exponencial**: Até 3 tentativas automáticas para falhas temporárias de rede (`ConnectException`, `SocketTimeoutException`).
3. **Circuit Breaker**: Janela deslizante de 10 chamadas. Se a taxa de erro atingir 50%, o circuito abre por 30s, rejeitando chamadas com desvio imediato para o fallback sem saturar o destino.
4. **Fallback Transparente via Spring Cloud**: Caso o circuito esteja aberto ou ocorra indisponibilidade persistente, o `ProdutoFeignClientFallback` consulta uma rota alternativa ou degrada com segurança.

---

## 🔧 Padrões e Diferenciais Implementados

### 1. Transactional Outbox com `FOR UPDATE SKIP LOCKED`
- A gravação do pedido e do evento de negócio na tabela `outbox` é atômica no banco de dados.
- O `OutboxProcessor` executa periodicamente para publicar mensagens no RabbitMQ.
- **Concorrência Segura Multi-Instância**: A busca de eventos pendentes utiliza `SELECT ... FOR UPDATE SKIP LOCKED` nativo do PostgreSQL. Múltiplos nós do microserviço em cluster consomem lotes disjuntos simultaneamente, eliminando riscos de deadlocks ou processamento duplicado.
- **Expurgo Automático de Eventos Processados**: Limpeza periódica via job scheduled que descarta registros processados antigos com base na propriedade `outbox.processor.retention-days` (evitando crescimento indefinido da tabela).

### 2. Validações e Tratamento de Erros Padronizado
- Validação profunda com Jakarta Bean Validation (`@NotNull`, `@Positive`, `@NotEmpty`, `@Valid`) em todos os DTOs.
- `clienteId <= 0` ou dados inconsistentes retornam `400 Bad Request`.
- Rotas não encontradas e recursos estáticos ausentes disparam `NoResourceFoundException`, mapeada centralmente para `404 Not Found` no `GlobalExceptionHandler`.
- Entidades JPA utilizam regras de `equals`/`hashCode` estritamente baseadas no identificador ou chave de negócio, prevenindo `StackOverflowError` e consultas desnecessárias a coleções.

### 3. Topologia de Mensageria Completa
- Exchange: `pedidos.exchange` (topic).
- Fila `pedidos.criados.queue` com routing key `pedido.criado`.
- Fila `pedidos.cancelados.queue` com routing key `pedido.cancelado`.
- Dead Letter Exchange (`pedidos.dlx`) e Dead Letter Queue (`pedidos.dlq`) configuradas para mensagens não roteáveis ou rejeitadas.

---

## 🗄️ Banco de Dados & Migrações (Flyway)

O microserviço utiliza migrações versionadas pelo **Flyway**:
- **Local das Migrações**: `src/main/resources/db/migration/`
- **Arquivo Inicial**: `V1__init_schema.sql` (Criação de `pedidos`, `itens_pedido`, `outbox` e criação de índices otimizados em `cliente_id`, `status`, `pedido_id` e `produto_id`).
- **Validação Estrita do Hibernate**: `spring.jpa.hibernate.ddl-auto=validate` — o Hibernate atua estritamente validando o esquema gerado pelas migrações, garantindo previsibilidade absoluta entre ambientes.

---

## 📦 Como Executar

### Pré-requisitos
- Docker e Docker Compose
- Java 21 e Maven 3.9+ (caso queira compilar/executar localmente)

### 1. Executar com Docker Compose (Completo)

```bash
# Clone o repositório
git clone https://github.com/flaviohenso/microservice-pedidos.git
cd microservice-pedidos

# Suba a infraestrutura completa (PostgreSQL, RabbitMQ, Produtos e Pedidos)
docker-compose up -d

# Acompanhe os logs
docker-compose logs -f microservice-pedidos
```

### 2. Executar Localmente em Modo Desenvolvimento

```bash
# Suba apenas os serviços de apoio
docker-compose up -d postgres rabbitmq microservice-produtos

# Execute o microserviço de pedidos
mvn spring-boot:run
```

Acesse:
- **Swagger UI**: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8081/api-docs](http://localhost:8081/api-docs)
- **RabbitMQ Management**: [http://localhost:15672](http://localhost:15672) (usuário: `guest` / senha: `guest`)

---

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
**Resposta (201 Created)**:
```json
{
  "id": 1,
  "numeroPedido": "PED-A1B2C3D4",
  "clienteId": 1,
  "status": "PENDENTE",
  "valorTotal": 5050.00,
  "itens": [
    {
      "id": 1,
      "produtoId": 1,
      "nomeProduto": "Notebook",
      "quantidade": 2,
      "precoUnitario": 2500.00,
      "subtotal": 5000.00
    }
  ],
  "dataCriacao": "2026-10-11T14:30:00"
}
```

### Buscar Pedido por ID
```http
GET /api/pedidos/{id}
```

### Listar Pedidos (com Paginação Opcional)
```http
# Lista todos (retrocompatível)
GET /api/pedidos

# Lista com paginação
GET /api/pedidos?pagina=0&tamanho=10
```

### Listar Pedidos por Cliente
```http
GET /api/pedidos/cliente/{clienteId}
```

### Cancelar Pedido
```http
PUT /api/pedidos/{id}/cancelar
```

---

## 📈 Monitoramento & Actuator

Para conformidade com as melhores práticas de segurança de produção, os endpoints do Spring Boot Actuator foram restritos, expondo apenas métricas essenciais sem vazamento de dados sensíveis do ambiente:

| Endpoint | Descrição | Status |
|----------|-----------|:------:|
| `/actuator/health` | Status de saúde da aplicação e de dependências | Exposto (`200 OK`) |
| `/actuator/info` | Informações da aplicação, Java, Git e Build | Exposto |
| `/actuator/metrics` | Métricas de JVM, garbage collection, threads e HTTP | Exposto |
| `/actuator/prometheus` | Coleta de métricas formatadas para Prometheus | Exposto |
| `/actuator/env` | Variáveis de ambiente e segredos de sistema | **Bloqueado (404)** |
| `/actuator/heapdump` | Dump de memória da JVM | **Bloqueado (404)** |
| `/actuator/threaddump`| Dump de threads do sistema operacional | **Bloqueado (404)** |

---

## ⚖️ Trade-offs e Decisões Arquiteturais

| Decisão | Alternativa Avaliada | Motivação & Benefício |
|---|---|---|
| **Virtual Threads (Java 21)** | Spring WebFlux / Reativo | Mantém simplicidade de código imperativo/síncrono e rastreabilidade total de stack traces, atingindo escalabilidade comparável sem introduzir o overhead cognitivo do ecossistema reativo. |
| **I/O Fora de Transação** | `@Transactional` cobrindo o Use Case | Evita exaustão do pool Hikari durante chamadas HTTP lentas ao serviço externo de produtos. A transação dura milissegundos, restrita ao salvamento de `Pedido` + `OutboxEvent`. |
| **Outbox com `SKIP LOCKED`** | Consumo por lock otimista / sem lock | Permite escalabilidade horizontal com múltiplos pods/instâncias concorrentes sem colisão ou duplicação de eventos publicados no RabbitMQ. |
| **Flyway + Hibernate Validate** | `ddl-auto=update` | Garante que alterações estruturais no banco sejam rastreáveis por código, testadas e auditadas em esteiras de CI/CD, eliminando surpresas em produção. |
| **Jackson 3 Nativo** | Jackson 2 Legado | Alinhamento com o Spring Boot 4 / Spring Framework 7, serialização moderna com suporte nativo a tipos `java.time.*` sem dependências obsoletas. |

---

## 📂 Estrutura do Projeto

```
src/
├── main/
│   ├── java/com/ecommerce/pedidos/
│   │   ├── MicroservicePedidosApplication.java
│   │   ├── domain/                              # Camada de Domínio Puro
│   │   │   ├── entity/                          # Pedido, ItemPedido, OutboxEvent...
│   │   │   ├── exception/                       # PedidoNotFoundException...
│   │   │   └── repository/                      # PedidoRepositoryPort, OutboxRepositoryPort
│   │   ├── application/                         # Camada de Casos de Uso e Portas
│   │   │   ├── usecase/                         # CriarPedidoUseCase, ListarPedidosUseCase...
│   │   │   ├── port/out/                        # SalvarPedidoTransacionalPort
│   │   │   ├── service/                         # ProdutoServicePort, EventPublisherPort
│   │   │   ├── dto/                             # ProdutoDTO, ItemPedidoRequest
│   │   │   └── event/                           # PedidoCriadoEvent, ItemPedidoEvent
│   │   ├── infrastructure/                      # Adaptadores Técnicos
│   │   │   ├── persistence/                     # Repositories JPA, Entidades JPA, Adaptadores
│   │   │   ├── client/                          # OpenFeign, Adaptadores, Fallbacks
│   │   │   ├── messaging/                       # OutboxProcessor, RabbitMQPublisher
│   │   │   └── config/                          # Configurações do Spring, Cache, AMQP, OpenAPI
│   │   └── presentation/                        # Adaptadores de Entrada (Web)
│   │       ├── controller/                      # PedidoController, ResilienceHealthController
│   │       ├── dto/                             # DTOs de Request e Response da API
│   │       ├── mapper/                          # PedidoDTOMapper
│   │       └── exception/                       # GlobalExceptionHandler
│   └── resources/
│       ├── application.properties               # Configurações gerais
│       └── db/migration/
│           └── V1__init_schema.sql              # DDL inicial Flyway
└── test/
    ├── java/com/ecommerce/pedidos/
    │   ├── api/                                 # ContratoJsonGoldenIT, PedidoApiIT
    │   ├── messaging/                           # OutboxRabbitIT, OutboxConcorrenciaIT
    │   ├── persistence/                         # PersistenciaIT
    │   ├── resilience/                          # ProdutoResilienciaIT
    │   ├── transaction/                         # TransacaoBoundaryIT
    │   └── support/                             # ContainersConfig (Testcontainers PostgreSQL & RabbitMQ)
    └── resources/
        └── application-test.properties          # Perfil de testes
```

---

## 🧪 Testes Automatizados

O projeto conta com uma pirâmide robusta de testes unitários e de integração real executados com **Testcontainers 2.0.5** e **WireMock**:

### Executar a Suíte Completa

```bash
# Executa testes unitários e testes de integração com Testcontainers
mvn clean verify

# Executa e gera relatório JaCoCo
mvn test jacoco:report
```

### Categorias de Testes:
- **Testes Unitários**: Testes puros de domínio e orquestração de use cases sem subir contexto do Spring.
- **Testes de Contrato Golden JSON (`ContratoJsonGoldenIT`)**: Protegem a API e o formato dos eventos publicados contra breaking changes de serialização Jackson.
- **Testes de Concorrência (`OutboxConcorrenciaIT`)**: Validam o processamento concorrente do Outbox com `SELECT FOR UPDATE SKIP LOCKED` em múltiplas threads contra PostgreSQL real.
- **Testes de Fronteira Transacional (`TransacaoBoundaryIT`)**: Garantem que chamadas HTTP remotas ocorram estritamente com zero transações de banco ativas.
- **Testes de Resiliência (`ProdutoResilienciaIT`)**: Simulam cenários de falhas de rede, lentidão, timeouts e fallback com WireMock.
- **Testes de API e Flyway (`PedidoApiIT`, `PersistenciaIT`)**: Testam o ciclo completo de ponta a ponta, paginação e validação do schema com Hibernate.

---

## 👥 Autor

Desenvolvido e refatorado por **[Flávio Henrique](https://github.com/flaviohenso)**.
