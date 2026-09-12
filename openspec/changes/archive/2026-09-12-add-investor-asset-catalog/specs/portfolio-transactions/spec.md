## MODIFIED Requirements

### Requirement: Registro de compra e venda com dados do ativo
O sistema SHALL permitir registrar um lançamento de `BUY` ou `SELL` para uma ação ou ETF dos mercados `BR` ou `US` previamente cadastrada pelo investidor. O lançamento MUST conter o identificador de um ativo cadastrado válido, tipo, data da transação, quantidade positiva e preço unitário positivo; custos são opcionais e, quando informados, MUST ser iguais ou maiores que zero. O retrato do ativo MUST ser recuperado exclusivamente do catálogo do investidor, mantido no lançamento mesmo se o cadastro ou o provedor mudar posteriormente e nunca aceito livremente do cliente. Quantidade, preço unitário e custos MUST comportar no máximo 11 dígitos inteiros e 8 casas decimais. O ativo cadastrado MUST pertencer ao investidor autenticado, mas MAY ser usado em qualquer carteira desse mesmo investidor.

#### Scenario: Compra registrada com sucesso
- **WHEN** o investidor informa um ativo do próprio catálogo e dados válidos de uma compra para sua carteira
- **THEN** o sistema persiste o lançamento e responde com status `201` contendo seu identificador, retrato do ativo, dados registrados, status e datas públicas

#### Scenario: Mesmo ativo usado em carteiras diferentes
- **WHEN** o investidor possui mais de uma carteira e seleciona o mesmo ativo cadastrado para lançamentos nelas
- **THEN** o sistema aceita o ativo em todas as carteiras pertencentes ao mesmo investidor

#### Scenario: Custos não informados
- **WHEN** o investidor registra um lançamento válido sem informar custos
- **THEN** o sistema considera o custo como zero e registra o lançamento

#### Scenario: Referência de ativo inválida para o lançamento
- **WHEN** o identificador do ativo não existe ou não pertence ao investidor autenticado
- **THEN** o sistema responde com `404` e código `REGISTERED_ASSET_NOT_FOUND`, sem criar lançamento nem revelar o cadastro associado

#### Scenario: Dados obrigatórios ou valores inválidos
- **WHEN** tipo, identificador do ativo, data, quantidade, preço ou custos não atendem às regras, inclusive precisão ou escala compatíveis com o armazenamento
- **THEN** o sistema responde com status `400` e identifica os campos inválidos no erro JSON

#### Scenario: Histórico anterior ao catálogo
- **WHEN** o sistema consulta lançamentos criados antes da adoção do catálogo persistente
- **THEN** os retratos já armazenados permanecem válidos e são retornados sem exigir vínculo retroativo com um ativo cadastrado
