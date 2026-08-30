## MODIFIED Requirements

### Requirement: Exclusão de carteira privada
O sistema SHALL permitir que o investidor exclua definitivamente uma de suas próprias carteiras somente quando ela não possuir lançamentos. Uma carteira que possua ao menos um lançamento, independentemente do seu status, MUST ser preservada para manter a integridade do histórico.

#### Scenario: Investidor exclui carteira própria
- **WHEN** o investidor exclui o UUID de uma carteira própria que não possui lançamentos
- **THEN** o sistema remove a carteira e responde com status `204`

#### Scenario: Investidor tenta excluir carteira com histórico
- **WHEN** o investidor exclui o UUID de uma carteira própria que possui um ou mais lançamentos
- **THEN** o sistema responde com status `409` e código `PORTFOLIO_HAS_TRANSACTIONS`, sem remover a carteira ou seus lançamentos

#### Scenario: Investidor tenta excluir carteira de outra pessoa ou inexistente
- **WHEN** o investidor exclui o UUID de uma carteira que não pertence a ele ou não existe
- **THEN** o sistema responde com status `404` e código `PORTFOLIO_NOT_FOUND`, sem revelar se o registro existe
