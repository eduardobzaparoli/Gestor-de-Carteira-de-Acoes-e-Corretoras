## MODIFIED Requirements

### Requirement: Ciclo de vida de lançamentos futuros
O sistema SHALL registrar como `EFFECTIVE` todo lançamento cuja data seja igual ou anterior à data atual e como `PENDING` aquele cuja data seja futura. Antes de qualquer consulta ou operação de lançamentos da carteira, o sistema MUST efetivar os lançamentos pendentes cuja data já tenha chegado. Antes de efetivar uma venda pendente, o sistema MUST revalidar a sequência cronológica e as reservas ainda aplicáveis; se a efetivação reduzir a quantidade do ativo abaixo de zero, o sistema MUST responder com status `409` e código `INSUFFICIENT_ASSET_QUANTITY`, mantendo o lançamento como `PENDING`. Um lançamento `PENDING` MAY ser cancelado; lançamentos `EFFECTIVE` e `CANCELLED` MUST permanecer imutáveis.

#### Scenario: Lançamento presente ou passado é efetivado
- **WHEN** o investidor registra um lançamento com data atual ou passada
- **THEN** o sistema o cria com status `EFFECTIVE`

#### Scenario: Lançamento futuro é efetivado na próxima interação
- **WHEN** existe um lançamento `PENDING` cuja data prevista já chegou e o investidor consulta ou realiza uma operação de lançamento naquela carteira
- **THEN** o sistema altera seu status para `EFFECTIVE` antes de processar ou retornar a operação solicitada

#### Scenario: Venda pendente vencida não pode causar saldo negativo
- **WHEN** uma venda `PENDING` cuja data prevista já chegou causaria saldo negativo ao ser efetivada
- **THEN** o sistema responde com status `409` e código `INSUFFICIENT_ASSET_QUANTITY` e preserva o lançamento como `PENDING`

#### Scenario: Cancelamento antes da efetivação
- **WHEN** o investidor cancela um lançamento `PENDING` antes de sua data prevista
- **THEN** o sistema altera o status para `CANCELLED`, preserva o registro no histórico e responde com status `204`

#### Scenario: Cancelamento de lançamento não pendente
- **WHEN** o investidor tenta cancelar um lançamento `EFFECTIVE` ou `CANCELLED`
- **THEN** o sistema responde com status `409` e código `TRANSACTION_CANNOT_BE_CANCELLED`, sem alterar o histórico

### Requirement: Bloqueio de venda acima da posição disponível
O sistema MUST impedir que uma venda reduza a quantidade disponível de um ativo abaixo de zero. Para cada ticker e mercado na carteira, a quantidade disponível SHALL ser a posição dos lançamentos `EFFECTIVE` menos as quantidades de vendas futuras `PENDING`; compras futuras pendentes MUST NOT aumentar a quantidade disponível. A quantidade reservada por uma venda pendente MUST ser liberada quando ela for cancelada. Toda venda, inclusive presente ou passada, MUST respeitar as vendas futuras já reservadas. Ao inserir uma venda com data presente ou passada, o sistema MUST recalcular a sequência por data da transação e data de criação e rejeitar a operação caso a quantidade fique negativa em qualquer ponto do histórico. A validação e a persistência de operações que afetam o saldo da mesma carteira MUST ocorrer de forma atômica, para que vendas concorrentes não consumam a mesma quantidade.

#### Scenario: Venda efetiva dentro da posição
- **WHEN** o investidor registra uma venda com quantidade igual ou menor que a posição disponível do ativo
- **THEN** o sistema efetiva a venda e mantém a quantidade da carteira não negativa

#### Scenario: Venda efetiva respeita reserva futura
- **WHEN** existe uma venda futura `PENDING` reservando parte da posição e o investidor registra uma venda presente ou passada que consumiria essa reserva
- **THEN** o sistema responde com status `409` e código `INSUFFICIENT_ASSET_QUANTITY`, sem criar o lançamento

#### Scenario: Venda acima da posição
- **WHEN** o investidor tenta registrar uma venda com quantidade superior à posição disponível do ativo
- **THEN** o sistema responde com status `409` e código `INSUFFICIENT_ASSET_QUANTITY`, sem criar o lançamento

#### Scenario: Vendas futuras concorrentes são reservadas
- **WHEN** o investidor tenta registrar uma nova venda futura cuja soma com as vendas pendentes já reservadas excede a posição efetiva disponível
- **THEN** o sistema responde com status `409` e não cria a nova reserva

#### Scenario: Vendas simultâneas disputam o mesmo saldo
- **WHEN** duas vendas da mesma carteira e do mesmo ativo são processadas simultaneamente e a soma das quantidades excede a posição disponível
- **THEN** o sistema cria no máximo uma venda e responde para a outra com status `409` e código `INSUFFICIENT_ASSET_QUANTITY`

#### Scenario: Cancelamento libera a reserva
- **WHEN** o investidor cancela uma venda futura pendente
- **THEN** a quantidade daquela venda deixa de ser considerada para bloquear novas vendas

#### Scenario: Venda retroativa produziria saldo histórico negativo
- **WHEN** o investidor registra uma venda presente ou passada que, em ordem cronológica, antecede quantidade suficiente do ativo
- **THEN** o sistema responde com status `409` e código `INSUFFICIENT_ASSET_QUANTITY`, sem criar o lançamento
