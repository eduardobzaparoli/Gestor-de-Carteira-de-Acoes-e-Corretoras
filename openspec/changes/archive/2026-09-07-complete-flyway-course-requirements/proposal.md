## Why

O PostgreSQL já possui esquema versionado pelo Flyway, mas os perfis H2 e de teste ainda delegam a criação do banco ao Hibernate. Essa diferença impede que a suíte padrão valide o mesmo histórico usado em produção e deixa sem documentação operacional conceitos obrigatórios da Aula 11, como histórico, checksum, lock, baseline e imutabilidade das migrations.

## What Changes

- Tornar o Flyway a fonte única de criação e evolução do esquema nos perfis `dev`, `h2`, `test`, `postgres` e `prod`.
- Executar as mesmas migrations SQL em H2 com modo de compatibilidade PostgreSQL e em PostgreSQL real.
- Configurar o Hibernate apenas com `ddl-auto=validate` também nos perfis H2 e de teste.
- Ampliar os testes para comprovar criação de banco vazio, histórico Flyway, checksum, reaplicação idempotente e compatibilidade do esquema com as entidades.
- Preservar a estratégia já adotada para bancos PostgreSQL legados: baseline em versão `0`, baseline migration cumulativa `B1` e evolução `V1` sem alteração retroativa.
- Documentar o fluxo Flyway equivalente ao conteúdo da Aula 11: responsabilidades, convenções, histórico, lock, imutabilidade, nova evolução, banco vazio, banco legado, diagnóstico e estratégia de correção progressiva.
- Manter a carga opcional do administrador inicial idempotente e separada da responsabilidade estrutural do Flyway.

## Capabilities

### New Capabilities

- `cross-database-schema-migrations`: criação, validação e evolução do mesmo esquema versionado por Flyway em H2 e PostgreSQL, incluindo histórico e reinicialização idempotente.

### Modified Capabilities

- `postgres-schema-migrations`: explicitar a governança das migrations aplicadas, a preservação de checksums e a validação operacional do histórico em bancos novos, legados e já versionados.

## Impact

- Perfis Spring em `src/main/resources` e `src/test/resources`.
- Migrations em `src/main/resources/db/migration`, preservando os arquivos já publicados.
- Testes de configuração, migração, inicialização H2 e integração PostgreSQL.
- Pipeline de validação H2/PostgreSQL e documentação de configuração/operação.
- Nenhuma mudança nos endpoints, DTOs ou regras financeiras da API.
- Nenhuma troca de Flyway por Liquibase e nenhuma nova dependência prevista.
