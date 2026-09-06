## Why

O PostgreSQL ainda depende de tabelas criadas historicamente pelo Hibernate: a única migração Flyway versionada adiciona `users.status`. Por isso, um banco PostgreSQL vazio não pode inicializar a aplicação de forma reproduzível e o perfil de integração ainda permite alteração automática do esquema.

## What Changes

- Adicionar uma migração-base Flyway cumulativa para criar o esquema PostgreSQL atual completo em bancos vazios, incluindo tabelas, colunas, chaves estrangeiras e restrições de unicidade.
- Preservar a migração `V1__add_user_status.sql` e o baseline em versão `0`, para que bancos legados sem histórico recebam somente a evolução de `users.status` sem recriar nem apagar dados.
- Configurar o perfil PostgreSQL para o Hibernate somente validar o esquema; o Flyway passa a ser a fonte de verdade para sua evolução.
- Cobrir com testes os caminhos de banco vazio, banco legado e integração PostgreSQL opt-in.

## Capabilities

### New Capabilities

- `postgres-schema-migrations`: inicialização e evolução reproduzíveis do esquema PostgreSQL por migrações Flyway.

### Modified Capabilities

- `user-authentication`: bancos PostgreSQL vazios também passam a obter o esquema de autenticação por Flyway, mantendo a atualização segura de bancos legados.
- `brokerage-registration`: a persistência de corretoras no PostgreSQL deixa de depender de geração automática de esquema fora dos perfis H2 locais.

## Impact

- Afeta as migrações em `src/main/resources/db/migration/`, os perfis `postgres` e `prod` e os testes de migração e integração PostgreSQL.
- Não altera endpoints, contratos JSON, dados já persistidos, dependências ou o gerenciamento automático de esquema em H2 de desenvolvimento e teste.
