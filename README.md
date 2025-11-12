# Microserviço de Pedidos

Microserviço de realização de pedidos para e-commerce, desenvolvido seguindo Clean Architecture/Hexagonal Architecture.

## Tecnologias

- Java 21
- Spring Boot 3.2.0
- PostgreSQL
- RabbitMQ
- Maven

## Status

🚧 Em desenvolvimento

## Arquitetura

Este microserviço segue os princípios de Clean Architecture (Arquitetura Hexagonal), separando as responsabilidades em camadas:

- **Domain**: Entidades e regras de negócio
- **Application**: Casos de uso e orquestração
- **Infrastructure**: Implementações técnicas (persistência, mensageria, clientes HTTP)
- **Presentation**: APIs REST e DTOs

## Padrões Implementados

- **Outbox Pattern**: Garante consistência eventual entre persistência e publicação de eventos
- **Repository Pattern**: Abstração da camada de persistência
- **DTO Pattern**: Separação entre modelos de domínio e APIs

## Como Executar

Instruções detalhadas serão adicionadas em breve.

