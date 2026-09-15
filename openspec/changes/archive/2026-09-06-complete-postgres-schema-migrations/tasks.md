## 1. Migração do esquema PostgreSQL

- [x] 1.1 Derivar das entidades atuais o DDL PostgreSQL das tabelas, relações, unicidades e colunas obrigatórias.
- [x] 1.2 Adicionar a baseline migration `B1__initial_schema.sql` com o esquema cumulativo, incluindo `users.status`.
- [x] 1.3 Preservar `V1__add_user_status.sql` como evolução de bancos legados e conferir sua compatibilidade com a baseline.

## 2. Configuração de inicialização

- [x] 2.1 Alterar o perfil `postgres` para validar, e não atualizar automaticamente, o esquema Hibernate.
- [x] 2.2 Manter o baseline Flyway para bancos legados e confirmar que os perfis H2 de desenvolvimento e teste continuam sem Flyway.

## 3. Testes de migração

- [x] 3.1 Criar teste Flyway para banco vazio, verificando as cinco tabelas, relações e restrições essenciais.
- [x] 3.2 Manter e ampliar o teste de banco legado para confirmar o baseline e a inclusão única de `users.status` com valor `ACTIVE`.
- [x] 3.3 Ajustar a suíte PostgreSQL opt-in para executar com Hibernate em validação e preservar a cobertura do ciclo de autenticação e carteira.

## 4. Verificação

- [x] 4.1 Executar a suíte Maven padrão e corrigir regressões.
- [x] 4.2 Executar a suíte PostgreSQL opt-in contra o banco de testes configurado e confirmar a inicialização por Flyway.
- [x] 4.3 Validar a change com `openspec validate complete-postgres-schema-migrations --strict`.
