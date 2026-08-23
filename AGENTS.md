# AGENTS.md

## Contexto do projeto

Este repositório contém uma API REST para gestão de carteiras de ações e corretoras.

Antes de planejar ou implementar funcionalidades que afetem requisitos do produto, consulte `docs/product-spec.md`.

## Fontes de verdade

- `docs/product-spec.md` contém a visão global e os requisitos aprovados do produto.
- `openspec/specs/` descreve o comportamento atual consolidado do sistema.
- `openspec/changes/` contém mudanças propostas ou em implementação.
- Não altere `docs/product-spec.md` sem solicitação explícita do usuário.

## Stack tecnológica

- Java 17
- Spring Boot
- Maven
- H2 para desenvolvimento
- PostgreSQL para produção

## Diretrizes de desenvolvimento

- Siga a arquitetura em camadas: Controller, Service, Repository, Entity e DTO.
- Mantenha regras de negócio na camada Service.
- Use DTOs nas entradas e saídas da API.
- Retorne respostas da API em JSON.
- Centralize o tratamento de erros com `@ControllerAdvice`.
- Isole integrações externas por meio de interfaces e estratégias.
- Não adicione dependências sem explicar a necessidade.
- Execute os testes relevantes após alterações no código.

## Fluxo de trabalho

Para novas funcionalidades ou mudanças de comportamento, use o OpenSpec para planejar a mudança antes de implementar o código.