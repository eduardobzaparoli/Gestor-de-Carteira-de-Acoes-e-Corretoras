## ADDED Requirements

### Requirement: Gestão visual do catálogo de ativos
O sistema SHALL oferecer ao investidor uma tela de ativos consistente com a gestão visual de corretoras. A tela MUST permitir cadastrar ativos por pesquisa validada, listar ticker, nome, mercado, tipo, última cotação e data e hora da consulta, filtrar por todos, brasileiros ou americanos, atualizar individualmente cada cotação e excluir um ou todos os cadastros permitidos. Cotações americanas MUST ser apresentadas em reais.

#### Scenario: Primeiro uso sem ativos
- **WHEN** o investidor ainda não possui ativos cadastrados
- **THEN** a interface apresenta um estado vazio explicativo e oferece a ação de adicionar ativo

#### Scenario: Cadastro de ativo
- **WHEN** o investidor pesquisa, seleciona e confirma um ativo válido
- **THEN** a interface acompanha a consulta externa, adiciona o registro à lista e exibe sua cotação e data de consulta

#### Scenario: Filtros de mercado
- **WHEN** o investidor escolhe Todos, Brasileiros ou Americanos
- **THEN** a interface exibe respectivamente o catálogo completo ou somente os registros do mercado correspondente

#### Scenario: Atualização individual
- **WHEN** o investidor aciona Atualizar cotação em um ativo e a API responde com sucesso
- **THEN** a interface atualiza somente o valor e a data daquele item e informa a conclusão

#### Scenario: Falha ao atualizar cotação
- **WHEN** a API não consegue obter a nova cotação
- **THEN** a interface mantém o último valor e a data visíveis e apresenta o erro público com opção de nova tentativa

#### Scenario: Exclusão individual ou total
- **WHEN** o investidor confirma a exclusão de um ativo ou de todo o catálogo
- **THEN** a interface solicita a operação correspondente, atualiza a lista em caso de sucesso e apresenta a proteção por saldo quando a API responder com conflito

#### Scenario: Cotação americana no catálogo
- **WHEN** o catálogo contém um ativo cuja moeda nativa é `USD`
- **THEN** a interface converte e apresenta sua cotação em `BRL` com a taxa disponível

## MODIFIED Requirements

### Requirement: Navegação protegida por papel
O sistema MUST oferecer uma estrutura de navegação autenticada, responsiva e consistente e MUST exibir somente destinos permitidos ao papel atual retornado por `GET /api/auth/me`.

#### Scenario: Investidor autenticado
- **WHEN** um usuário com papel `INVESTOR` acessa a aplicação
- **THEN** a navegação apresenta carteiras, corretoras e ativos e não oferece acesso ao painel administrativo

#### Scenario: Retorno pela marca
- **WHEN** o investidor aciona a marca Bom Investidor no menu
- **THEN** a interface navega para `/app/carteiras`, correspondente à visão geral

#### Scenario: Administrador autenticado
- **WHEN** um usuário com papel `ADMIN` acessa a aplicação
- **THEN** a navegação direciona para a gestão de usuários e não expõe carteiras, catálogos de ativos ou dados financeiros de investidores

#### Scenario: Rota incompatível com o papel
- **WHEN** um usuário tenta abrir diretamente uma rota que não pertence ao seu papel
- **THEN** a interface bloqueia a tela e o direciona para a página inicial autorizada

### Requirement: Pesquisa de ativos e cadastro de lançamentos
O sistema SHALL permitir registrar compra ou venda escolhendo somente uma ação ou ETF previamente cadastrada no catálogo do investidor. Ao selecionar o ativo, a interface MUST solicitar uma cotação corrente à API, preencher o preço unitário e manter o campo editável para representar o preço real da operação. Datas MUST ser apresentadas e editadas no formato brasileiro `dd/mm/aaaa`. Campos de preço e custos MUST assumir formatação monetária da moeda selecionada ao perder o foco, sem enviar a representação localizada para a API.

#### Scenario: Pesquisa de ativo
- **WHEN** o investidor abre um novo lançamento em qualquer uma de suas carteiras
- **THEN** a interface lista somente os ativos previamente cadastrados por ele, com mercado, ticker, nome e logo quando disponível

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
