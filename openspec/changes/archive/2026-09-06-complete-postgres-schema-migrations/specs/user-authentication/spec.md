## MODIFIED Requirements

### Requirement: Persistência compatível do estado de conta
O sistema SHALL manter o estado de conta persistido de forma compatível com H2 e PostgreSQL. No PostgreSQL, o esquema de autenticação SHALL ser criado e evoluído por migrações Flyway versionadas. A evolução de `users.status` MUST preservar os usuários existentes como ativos e ser registrada no histórico do banco.

#### Scenario: Banco PostgreSQL vazio recebe o esquema de autenticação
- **WHEN** a aplicação inicia em produção ou no perfil de integração PostgreSQL contra um banco vazio
- **THEN** o Flyway cria a tabela de usuários com a restrição de unicidade do e-mail e a coluna `status`, e a aplicação fica pronta para cadastro e login

#### Scenario: Banco PostgreSQL existente recebe a migração
- **WHEN** uma versão da aplicação com esta mudança inicia em produção ou no perfil de integração PostgreSQL contra um banco existente que ainda não possui histórico Flyway
- **THEN** o sistema estabelece o baseline sem recriar dados e aplica uma única vez a migração que inclui `users.status` com valor `ACTIVE` às contas existentes

#### Scenario: Ambiente H2 de desenvolvimento ou teste
- **WHEN** a aplicação inicia com perfil de desenvolvimento ou teste baseado em H2
- **THEN** o Flyway não executa a migração de produção e o Hibernate continua responsável pelo ciclo de vida do esquema desse ambiente

