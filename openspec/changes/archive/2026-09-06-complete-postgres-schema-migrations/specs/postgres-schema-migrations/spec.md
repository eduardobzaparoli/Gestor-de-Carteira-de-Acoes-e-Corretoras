## Purpose

Garantir que o PostgreSQL seja inicializado e evoluído de forma versionada, reproduzível e compatível com bancos já existentes.

## ADDED Requirements

### Requirement: Esquema PostgreSQL completo em banco vazio
O sistema SHALL criar, por migrações versionadas, toda a estrutura persistente exigida pelas funcionalidades atuais quando iniciar contra um banco PostgreSQL vazio. A estrutura MUST incluir usuários, corretoras, carteiras, transações e eventos de proventos, com suas colunas obrigatórias, relações e restrições de unicidade.

#### Scenario: Inicialização de banco PostgreSQL vazio
- **WHEN** a aplicação inicia com o perfil PostgreSQL contra um banco sem tabelas nem histórico de migrações
- **THEN** o banco recebe o esquema completo e a aplicação inicia com a validação de persistência aprovada

#### Scenario: Restrições do esquema inicial
- **WHEN** a aplicação tenta persistir dados que violam uma chave estrangeira ou uma unicidade definida para usuários, corretoras, carteiras ou eventos de proventos
- **THEN** o banco rejeita a operação de acordo com a restrição correspondente

### Requirement: Evolução segura de bancos PostgreSQL legados
O sistema SHALL reconhecer um banco PostgreSQL não vazio sem histórico de migrações como legado e preservar suas tabelas e dados. Após registrar o baseline, SHALL aplicar somente as migrações posteriores a esse baseline.

#### Scenario: Banco legado recebe evolução pendente
- **WHEN** a aplicação inicia contra um banco legado que contém o esquema anterior e não possui histórico Flyway
- **THEN** o histórico é criado sem recriar tabelas existentes e cada migração posterior pendente é aplicada uma única vez

#### Scenario: Banco já versionado é reiniciado
- **WHEN** a aplicação inicia contra um banco cujo histórico já registra as migrações aplicadas
- **THEN** nenhuma migração já registrada é executada novamente e os dados existentes são preservados

### Requirement: Hibernate valida o esquema PostgreSQL versionado
O sistema MUST usar as migrações versionadas como fonte de verdade para o esquema PostgreSQL e MUST apenas validar o mapeamento persistente ao iniciar nesse perfil.

#### Scenario: Divergência entre mapeamento e banco
- **WHEN** o esquema PostgreSQL não satisfaz o mapeamento persistente da aplicação após a execução das migrações
- **THEN** a inicialização falha sem o Hibernate criar ou alterar tabelas automaticamente

