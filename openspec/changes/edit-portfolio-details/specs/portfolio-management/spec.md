## ADDED Requirements

### Requirement: Edição privada dos dados da carteira
O sistema SHALL permitir que o investidor atualize o nome e a corretora vinculada de uma carteira própria. A atualização MUST exigir `name` e `brokerageId`, aplicar as mesmas regras de normalização, limite e unicidade usadas na criação e aceitar somente uma corretora pertencente ao investidor autenticado. O sistema MUST preservar o identificador, o proprietário, a data de criação, os lançamentos e os demais dados financeiros da carteira, alterando apenas o nome, a corretora e a data de atualização.

#### Scenario: Nome e corretora atualizados
- **WHEN** o investidor envia um nome válido e o identificador de outra corretora própria para uma de suas carteiras
- **THEN** o sistema atualiza os dois dados, responde com status `200` e devolve a carteira com o novo resumo da corretora e uma nova data de atualização

#### Scenario: Apenas o nome efetivamente muda
- **WHEN** o investidor envia um novo nome válido mantendo a corretora atual
- **THEN** o sistema atualiza a carteira sem exigir recriação nem alterar seus lançamentos ou sua data de criação

#### Scenario: Apenas a corretora efetivamente muda
- **WHEN** o investidor mantém o nome atual e informa outra corretora própria
- **THEN** o sistema atualiza o vínculo cadastral sem mover, recriar ou excluir lançamentos da carteira

#### Scenario: Carteira inexistente ou alheia
- **WHEN** o investidor tenta atualizar uma carteira inexistente ou pertencente a outra pessoa
- **THEN** o sistema responde com status `404` e código `PORTFOLIO_NOT_FOUND`, sem revelar nem alterar o registro

#### Scenario: Corretora inexistente ou alheia
- **WHEN** o investidor tenta atualizar sua carteira com uma corretora inexistente ou pertencente a outra pessoa
- **THEN** o sistema responde com status `404` e código `BROKERAGE_NOT_FOUND`, preservando os dados atuais da carteira

#### Scenario: Nome inválido ou duplicado
- **WHEN** o novo nome é vazio, excede 100 caracteres ou equivale ao nome de outra carteira do mesmo investidor após normalização
- **THEN** o sistema responde com o mesmo erro de validação ou conflito utilizado na criação e não realiza atualização parcial

#### Scenario: Atualizações concorrentes produzem nome duplicado
- **WHEN** atualizações concorrentes tentam atribuir o mesmo nome normalizado a carteiras diferentes do mesmo investidor
- **THEN** no máximo uma atualização é persistida e a outra recebe `409 PORTFOLIO_NAME_ALREADY_REGISTERED`

#### Scenario: Requisição sem autenticação ou por administrador
- **WHEN** uma pessoa sem token válido ou um administrador tenta atualizar uma carteira
- **THEN** o sistema responde respectivamente com `401` ou `403` em JSON e não altera dados privados
