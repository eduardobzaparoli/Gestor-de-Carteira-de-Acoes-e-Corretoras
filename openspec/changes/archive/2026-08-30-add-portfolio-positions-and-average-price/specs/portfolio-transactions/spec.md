## MODIFIED Requirements

### Requirement: Bloqueio de venda acima da posição disponível
O sistema MUST impedir que uma venda reduza a quantidade disponível de um ativo abaixo de zero. Para cada ticker e mercado na carteira, a quantidade disponível SHALL ser a posição dos lançamentos `EFFECTIVE` menos as quantidades de vendas futuras `PENDING`; compras futuras pendentes MUST NOT aumentar a quantidade disponível. A quantidade reservada por uma venda pendente MUST ser liberada quando ela for cancelada. Ao inserir uma venda com data presente ou passada, o sistema MUST recalcular a sequência por data da transação e data de criação e rejeitar a operação caso a quantidade fique negativa em qualquer ponto do histórico.

#### Scenario: Venda efetiva dentro da posição
- **WHEN** o investidor registra uma venda com quantidade igual ou menor que a posição disponível do ativo
- **THEN** o sistema efetiva a venda e mantém a quantidade da carteira não negativa

#### Scenario: Venda acima da posição
- **WHEN** o investidor tenta registrar uma venda com quantidade superior à posição disponível do ativo
- **THEN** o sistema responde com status `409` e código `INSUFFICIENT_ASSET_QUANTITY`, sem criar o lançamento

#### Scenario: Vendas futuras concorrentes são reservadas
- **WHEN** o investidor tenta registrar uma nova venda futura cuja soma com as vendas pendentes já reservadas excede a posição efetiva disponível
- **THEN** o sistema responde com status `409` e não cria a nova reserva

#### Scenario: Cancelamento libera a reserva
- **WHEN** o investidor cancela uma venda futura pendente
- **THEN** a quantidade daquela venda deixa de ser considerada para bloquear novas vendas

#### Scenario: Venda retroativa produziria saldo histórico negativo
- **WHEN** o investidor registra uma venda presente ou passada que, em ordem cronológica, antecede quantidade suficiente do ativo
- **THEN** o sistema responde com status `409` e código `INSUFFICIENT_ASSET_QUANTITY`, sem criar o lançamento
