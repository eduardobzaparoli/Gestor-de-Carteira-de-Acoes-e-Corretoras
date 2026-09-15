## Purpose

Garantir que H2 e PostgreSQL sejam inicializados e validados pelo mesmo histórico versionado, evitando diferenças silenciosas entre desenvolvimento, testes e produção.

## ADDED Requirements

### Requirement: Histórico único de criação do esquema
O sistema SHALL usar o mesmo conjunto ordenado de migrations versionadas para criar o esquema persistente em bancos H2 e PostgreSQL vazios.

#### Scenario: Inicialização de H2 vazio
- **WHEN** a aplicação inicia com o perfil H2 contra um banco em memória vazio
- **THEN** o gerenciador de migrations cria toda a estrutura persistente antes da validação das entidades

#### Scenario: Inicialização de teste vazio
- **WHEN** a suíte automatizada inicia um banco H2 vazio
- **THEN** o banco recebe o mesmo histórico de migrations utilizado pelos perfis PostgreSQL

### Requirement: Hibernate somente valida o esquema
O sistema MUST configurar o Hibernate para validar, sem criar, atualizar ou remover objetos do banco, em todos os perfis de execução e teste que carregam persistência.

#### Scenario: Divergência em H2
- **WHEN** uma entidade exige uma tabela, coluna ou constraint incompatível com as migrations
- **THEN** a inicialização H2 falha sem o Hibernate corrigir automaticamente o esquema

### Requirement: Reinicialização idempotente
O sistema SHALL registrar as migrations aplicadas e SHALL preservar o schema e os dados existentes em reinicializações subsequentes do mesmo banco.

#### Scenario: Segunda inicialização
- **WHEN** a aplicação inicia novamente contra um banco cujo histórico está atualizado
- **THEN** nenhuma migration já registrada é reaplicada e os dados permanecem inalterados

### Requirement: Paridade validada na entrega
O sistema MUST executar no pipeline verificações de criação e compatibilidade do esquema em H2 e PostgreSQL.

#### Scenario: Validação de pull request
- **WHEN** uma alteração é submetida ao pipeline de entrega
- **THEN** os gates H2 e PostgreSQL validam as migrations e impedem a aprovação caso algum esquema seja incompatível
