## Purpose

Permitir que investidores registrem e consultem o histórico imutável de compras e vendas de ativos em suas carteiras privadas, preservando o saldo disponível para negociação.

## ADDED Requirements

### Requirement: Acesso privado aos lançamentos da carteira
O sistema SHALL disponibilizar lançamentos somente no contexto de uma carteira do investidor autenticado. O proprietário MUST ser determinado exclusivamente pelo token, e administradores MUST NOT criar, consultar ou cancelar lançamentos privados.

#### Scenario: Investidor acessa lançamentos de carteira própria
- **WHEN** um investidor autenticado cria, consulta ou cancela um lançamento em uma de suas carteiras
- **THEN** o sistema executa a operação no contexto daquela carteira e retorna a resposta JSON correspondente

#### Scenario: Carteira inexistente ou de outro investidor
- **WHEN** um investidor informa o UUID de uma carteira inexistente ou pertencente a outra pessoa
- **THEN** o sistema responde com status `404` e código `PORTFOLIO_NOT_FOUND`, sem revelar a existência da carteira

#### Scenario: Requisição sem autenticação ou por administrador
- **WHEN** uma pessoa sem token válido ou um administrador chama uma operação de lançamento
- **THEN** o sistema responde respectivamente com `401` ou `403` em JSON e não altera nem revela lançamentos privados

### Requirement: Registro de compra e venda com dados do ativo
O sistema SHALL permitir registrar um lançamento de `BUY` ou `SELL` para uma ação ou ETF dos mercados `BR` ou `US`. O lançamento MUST conter ticker, nome do ativo, mercado, tipo, moeda, data da transação, quantidade positiva e preço unitário positivo; custos são opcionais e, quando informados, MUST ser iguais ou maiores que zero. O sistema MUST manter esses dados do ativo como retrato do lançamento, mesmo se a busca externa mudar posteriormente.

#### Scenario: Compra registrada com sucesso
- **WHEN** o investidor informa dados válidos de uma compra para sua carteira
- **THEN** o sistema persiste o lançamento e responde com status `201` contendo seu identificador, dados registrados, status e datas públicas

#### Scenario: Custos não informados
- **WHEN** o investidor registra um lançamento válido sem informar custos
- **THEN** o sistema considera o custo como zero e registra o lançamento

#### Scenario: Dados obrigatórios ou valores inválidos
- **WHEN** tipo, dados do ativo, data, quantidade, preço ou custos não atendem às regras do lançamento
- **THEN** o sistema responde com status `400` e identifica os campos inválidos no erro JSON

### Requirement: Ciclo de vida de lançamentos futuros
O sistema SHALL registrar como `EFFECTIVE` todo lançamento cuja data seja igual ou anterior à data atual e como `PENDING` aquele cuja data seja futura. Antes de qualquer consulta ou operação de lançamentos da carteira, o sistema MUST efetivar os lançamentos pendentes cuja data já tenha chegado. Um lançamento `PENDING` MAY ser cancelado; lançamentos `EFFECTIVE` e `CANCELLED` MUST permanecer imutáveis.

#### Scenario: Lançamento presente ou passado é efetivado
- **WHEN** o investidor registra um lançamento com data atual ou passada
- **THEN** o sistema o cria com status `EFFECTIVE`

#### Scenario: Lançamento futuro é efetivado na próxima interação
- **WHEN** existe um lançamento `PENDING` cuja data prevista já chegou e o investidor consulta ou realiza uma operação de lançamento naquela carteira
- **THEN** o sistema altera seu status para `EFFECTIVE` antes de processar ou retornar a operação solicitada

#### Scenario: Cancelamento antes da efetivação
- **WHEN** o investidor cancela um lançamento `PENDING` antes de sua data prevista
- **THEN** o sistema altera o status para `CANCELLED`, preserva o registro no histórico e responde com status `204`

#### Scenario: Cancelamento de lançamento não pendente
- **WHEN** o investidor tenta cancelar um lançamento `EFFECTIVE` ou `CANCELLED`
- **THEN** o sistema responde com status `409` e código `TRANSACTION_CANNOT_BE_CANCELLED`, sem alterar o histórico

### Requirement: Bloqueio de venda acima da posição disponível
O sistema MUST impedir que uma venda reduza a quantidade disponível de um ativo abaixo de zero. Para cada ticker e mercado na carteira, a quantidade disponível SHALL ser a posição dos lançamentos `EFFECTIVE` menos as quantidades de vendas futuras `PENDING`; compras futuras pendentes MUST NOT aumentar a quantidade disponível. A quantidade reservada por uma venda pendente MUST ser liberada quando ela for cancelada.

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

### Requirement: Histórico privado e imutável
O sistema SHALL disponibilizar o histórico de lançamentos de uma carteira própria em ordem da data de transação mais recente para a mais antiga, usando a data de criação como desempate. Cada item MUST expor identificador, tipo, status, retrato do ativo, data da transação, quantidade, preço unitário, custos e datas públicas. O sistema MUST NOT disponibilizar atualização ou exclusão física de lançamentos.

#### Scenario: Histórico ordenado
- **WHEN** o investidor consulta o histórico de uma carteira com lançamentos em datas diferentes
- **THEN** o sistema responde com status `200` e a coleção ordenada do lançamento mais recente para o mais antigo

#### Scenario: Histórico sem lançamentos
- **WHEN** o investidor consulta o histórico de uma carteira sem lançamentos
- **THEN** o sistema responde com status `200` e uma coleção vazia

#### Scenario: Lançamento efetivado não pode ser alterado
- **WHEN** um cliente tenta modificar ou remover fisicamente um lançamento efetivado
- **THEN** o sistema não oferece essa operação e preserva o lançamento no histórico

### Requirement: Persistência e respostas compatíveis
O sistema MUST persistir lançamentos e seus estados com o mesmo comportamento funcional no H2 e no PostgreSQL. Respostas e erros MUST ser JSON e MUST NOT expor entidades persistentes, proprietário interno ou detalhes de outros investidores.

#### Scenario: Banco local vazio
- **WHEN** a aplicação inicia sobre um banco H2 ou PostgreSQL local vazio com um perfil suportado
- **THEN** a estrutura de lançamentos fica pronta para registrar, consultar e cancelar operações conforme as regras desta especificação

