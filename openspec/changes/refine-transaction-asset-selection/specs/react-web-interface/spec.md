## MODIFIED Requirements

### Requirement: Pesquisa de ativos e cadastro de lançamentos
O sistema SHALL permitir registrar compra ou venda escolhendo somente uma ação ou ETF previamente cadastrada no catálogo do investidor. A lista de seleção MUST identificar cada opção por logo, ticker, nome e tipo, MUST manter um marcador local legível quando a logo externa estiver indisponível e MUST NOT apresentar a cotação armazenada ou convertida antes da escolha. Ao selecionar o ativo, a interface MUST solicitar uma cotação corrente à API, preencher o preço unitário e manter o campo editável para representar o preço real da operação. Datas MUST ser apresentadas e editadas no formato brasileiro `dd/mm/aaaa`. Campos de preço e custos MUST assumir formatação monetária da moeda selecionada ao perder o foco, sem enviar a representação localizada para a API.

#### Scenario: Pesquisa de ativo
- **WHEN** o investidor abre um novo lançamento em qualquer uma de suas carteiras
- **THEN** a interface lista somente os ativos previamente cadastrados por ele, com mercado, ticker, nome, tipo e logo quando disponível, sem apresentar cotação nas opções

#### Scenario: Logo disponível na seleção
- **WHEN** a fonte correspondente ao mercado fornece a logo de um ativo listado
- **THEN** a interface carrega e apresenta a imagem junto ao ticker e ao nome daquele ativo

#### Scenario: Logo indisponível na seleção
- **WHEN** a logo de um ativo não existe ou todas as fontes configuradas falham
- **THEN** a opção permanece identificável por um marcador local, ticker, nome e tipo, sem imagem quebrada

#### Scenario: Ausência de atribuição visual de logos
- **WHEN** qualquer tela apresenta logos de ativos
- **THEN** a interface não exibe textos ou links de atribuição do fornecedor de logos

#### Scenario: Cotação ao selecionar o ativo
- **WHEN** o investidor seleciona um ativo cadastrado
- **THEN** a interface consulta uma nova cotação, preenche o preço unitário atualizado e permite que o investidor ajuste esse preço antes do envio

#### Scenario: Catálogo vazio
- **WHEN** o investidor tenta criar um lançamento sem possuir ativos cadastrados
- **THEN** a interface explica que o cadastro é obrigatório e oferece navegação para a tela de ativos

#### Scenario: Falha na cotação para o lançamento
- **WHEN** a API não consegue consultar a cotação corrente do ativo selecionado
- **THEN** a interface preserva o formulário, apresenta o erro público e não permite concluir até obter uma cotação válida ou o investidor selecionar outro ativo

#### Scenario: Compra efetiva
- **WHEN** o investidor seleciona um ativo cadastrado e registra uma compra válida com data atual ou passada
- **THEN** a interface cria o lançamento, informa seu estado e atualiza histórico, posições e indicadores relacionados

#### Scenario: Lançamento futuro
- **WHEN** o investidor registra compra ou venda com data futura válida
- **THEN** a interface identifica o lançamento como pendente e explica que ele pode ser cancelado antes da efetivação

#### Scenario: Venda acima do saldo
- **WHEN** a API rejeita uma venda porque a quantidade excede a posição disponível na data
- **THEN** a interface mantém o formulário e apresenta uma mensagem específica de saldo insuficiente

#### Scenario: Nova abertura do formulário
- **WHEN** o investidor fecha e abre novamente o formulário de lançamento
- **THEN** a interface apresenta um formulário novo sem dados residuais do lançamento anterior

#### Scenario: Data brasileira assistida
- **WHEN** o investidor abre o calendário ou informa uma data parcial válida no formato `dd/mm`
- **THEN** a interface permite selecionar a data visualmente ou completa o ano corrente ao perder o foco e mantém a apresentação `dd/mm/aaaa`

#### Scenario: Valores de ativo estrangeiro em reais
- **WHEN** o investidor seleciona um ativo cadastrado cuja moeda nativa não é BRL
- **THEN** cotação, preço, custos e resumo são apresentados e editados em BRL com indicação da taxa utilizada, enquanto a API recebe os valores convertidos de volta para a moeda nativa
