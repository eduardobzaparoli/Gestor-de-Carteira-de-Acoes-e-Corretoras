# postgres-schema-migrations Specification

## Purpose

Garantir que o PostgreSQL seja inicializado e evoluído de forma versionada, reproduzível e compatível com bancos já existentes.

## Requirements

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

### Requirement: Histórico verificável e migrations imutáveis
O sistema SHALL registrar versão, descrição, ordem, resultado e checksum das migrations aplicadas ao PostgreSQL e MUST rejeitar divergências entre esse histórico e os arquivos versionados.

#### Scenario: Migration aplicada permanece inalterada
- **WHEN** a aplicação inicia e os arquivos correspondem aos checksums registrados
- **THEN** a validação do histórico é aprovada e somente migrations pendentes podem ser executadas

#### Scenario: Migration aplicada foi modificada
- **WHEN** o conteúdo de uma migration já registrada é alterado
- **THEN** a validação falha por divergência de checksum sem reescrever silenciosamente o histórico

#### Scenario: Correção posterior do esquema
- **WHEN** uma estrutura já implantada precisa ser corrigida ou ampliada
- **THEN** a mudança é entregue por uma nova migration de versão superior, preservando os arquivos aplicados

### Requirement: Execução concorrente protegida
O sistema MUST serializar tentativas concorrentes de alteração do mesmo esquema para impedir que duas instâncias apliquem migrations simultaneamente.

#### Scenario: Duas instâncias iniciam juntas
- **WHEN** duas instâncias tentam migrar o mesmo banco PostgreSQL ao mesmo tempo
- **THEN** apenas uma aplica cada migration enquanto a outra aguarda ou reconhece o histórico já atualizado
