## 1. Unificar a criação do esquema

- [x] 1.1 Habilitar Flyway e configurar Hibernate com `ddl-auto=validate` no perfil `h2`.
- [x] 1.2 Habilitar Flyway e configurar Hibernate com `ddl-auto=validate` no perfil `test`, preservando o H2 em modo PostgreSQL.
- [x] 1.3 Confirmar que `dev`, `postgres` e `prod` mantêm baseline apenas para PostgreSQL legado e que todos os perfis usam o mesmo diretório de migrations.
- [x] 1.4 Preservar os conteúdos publicados de `B1__initial_schema.sql` e `V1__add_user_status.sql` e registrar uma verificação contra alteração acidental nesta change.

## 2. Reforçar testes de migration

- [x] 2.1 Atualizar os testes de configuração para exigir Flyway habilitado e Hibernate em validação nos perfis H2 e test.
- [x] 2.2 Ajustar o teste de contexto H2 para comprovar a criação das cinco tabelas e da `flyway_schema_history` pela inicialização real do Spring Boot.
- [x] 2.3 Ampliar o teste de banco vazio para validar versão, descrição, sucesso e checksum registrados no histórico Flyway.
- [x] 2.4 Ampliar o cenário de reinicialização para confirmar que a segunda execução não reaplica migrations nem altera os dados existentes.
- [x] 2.5 Preservar e validar o cenário de PostgreSQL legado com baseline `0` e aplicação única de `V1`.

## 3. Documentar o fluxo da Aula 11 com Flyway

- [x] 3.1 Documentar as responsabilidades distintas de JPA, Hibernate e Flyway e a equivalência entre changelog/changeSet do Liquibase e migrations ordenadas do Flyway.
- [x] 3.2 Documentar convenções `B`/`V`, versões únicas, `flyway_schema_history`, checksum, lock e a regra de nunca editar migration aplicada.
- [x] 3.3 Documentar primeira execução em banco vazio, consulta do histórico, segunda inicialização e separação entre schema e bootstrap idempotente de dados.
- [x] 3.4 Documentar as estratégias para banco PostgreSQL legado, incluindo backup, conferência do banco alvo, baseline automática e quando preferir recriação.
- [x] 3.5 Documentar correção progressiva por nova migration, revisão manual de DDL gerado e diagnóstico de tabela existente, checksum, lock e divergência do Hibernate.

## 4. Validar entrega nos dois bancos

- [x] 4.1 Executar os testes direcionados de configuração, contexto H2, banco vazio, banco legado, histórico e checksum.
- [x] 4.2 Executar a suíte completa do gate H2 e corrigir qualquer teste que dependia implicitamente de `create-drop`.
- [x] 4.3 Executar o gate PostgreSQL contra banco descartável vazio e confirmar Flyway seguido de Hibernate `validate`.
- [x] 4.4 Revisar a pipeline para garantir que ambos os gates exercitam as migrations compartilhadas e ajustar somente se a evidência não estiver presente.
- [x] 4.5 Atualizar o mapa Graphify, executar a validação OpenSpec estrita e revisar que nenhuma credencial ou alteração de migration publicada entrou no diff.
