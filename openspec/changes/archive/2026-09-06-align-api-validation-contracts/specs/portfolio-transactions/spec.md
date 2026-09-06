## MODIFIED Requirements

### Requirement: Registro de compra e venda com dados do ativo
O sistema SHALL permitir registrar um lançamento de `BUY` ou `SELL` para uma ação ou ETF dos mercados `BR` ou `US`. O lançamento MUST conter uma referência de seleção de ativo válida, tipo, data da transação, quantidade positiva e preço unitário positivo; custos são opcionais e, quando informados, MUST ser iguais ou maiores que zero. O retrato do ativo MUST ser recuperado exclusivamente da referência, mantido no lançamento mesmo se a busca externa mudar posteriormente e nunca aceito livremente do cliente. Quantidade, preço unitário e custos MUST comportar no máximo 11 dígitos inteiros e 8 casas decimais. A referência MUST pertencer ao investidor autenticado, à carteira informada e ainda estar válida.

#### Scenario: Compra registrada com sucesso
- **WHEN** o investidor informa uma referência válida e dados válidos de uma compra para sua carteira
- **THEN** o sistema persiste o lançamento e responde com status `201` contendo seu identificador, dados registrados, status e datas públicas

#### Scenario: Custos não informados
- **WHEN** o investidor registra um lançamento válido sem informar custos
- **THEN** o sistema considera o custo como zero e registra o lançamento

#### Scenario: Referência de ativo inválida para o lançamento
- **WHEN** o investidor informa uma referência expirada, inexistente, emitida para outra carteira ou pertencente a outro investidor
- **THEN** o sistema responde com `409` e código `ASSET_SELECTION_EXPIRED`, sem criar lançamento nem revelar o retrato associado

#### Scenario: Dados obrigatórios ou valores inválidos
- **WHEN** tipo, referência, data, quantidade, preço ou custos não atendem às regras, inclusive precisão ou escala compatíveis com o armazenamento
- **THEN** o sistema responde com status `400` e identifica os campos inválidos no erro JSON
