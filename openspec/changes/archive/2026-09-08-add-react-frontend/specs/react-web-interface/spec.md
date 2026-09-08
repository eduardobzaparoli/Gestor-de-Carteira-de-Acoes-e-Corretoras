## Purpose

Disponibilizar uma interface web responsiva e orientada a dados para que investidores e administradores utilizem integralmente os recursos existentes da API sem depender de ferramentas técnicas.

## ADDED Requirements

### Requirement: Autenticação e sessão do usuário
O sistema SHALL apresentar login e cadastro como rotas públicas, validar os campos antes do envio e autenticar as chamadas protegidas com o token Bearer recebido da API. A sessão MUST ser encerrada quando o usuário sair, quando o token expirar ou quando a API responder que a autenticação não é mais válida.

#### Scenario: Cadastro de investidor
- **WHEN** uma pessoa informa nome, e-mail e senha válidos na tela de cadastro
- **THEN** a interface cria a conta, informa o sucesso e conduz a pessoa ao login

#### Scenario: Login bem-sucedido
- **WHEN** uma pessoa informa credenciais válidas na tela de login
- **THEN** a interface armazena a sessão, consulta a identidade atual e direciona o usuário para a área correspondente ao seu papel

#### Scenario: Credenciais ou campos inválidos
- **WHEN** a API rejeita o login ou cadastro com erro geral ou erros de campos
- **THEN** a interface mantém os dados não sensíveis preenchidos e apresenta mensagens claras junto ao formulário

#### Scenario: Sessão inválida ou expirada
- **WHEN** uma chamada protegida recebe resposta `401`
- **THEN** a interface remove a sessão local, impede o acesso às rotas privadas e direciona ao login com uma explicação

### Requirement: Navegação protegida por papel
O sistema MUST oferecer uma estrutura de navegação autenticada, responsiva e consistente e MUST exibir somente destinos permitidos ao papel atual retornado por `GET /api/auth/me`.

#### Scenario: Investidor autenticado
- **WHEN** um usuário com papel `INVESTOR` acessa a aplicação
- **THEN** a navegação apresenta carteiras e corretoras e não oferece acesso ao painel administrativo

#### Scenario: Administrador autenticado
- **WHEN** um usuário com papel `ADMIN` acessa a aplicação
- **THEN** a navegação direciona para a gestão de usuários e não expõe carteiras ou dados financeiros de investidores

#### Scenario: Rota incompatível com o papel
- **WHEN** um usuário tenta abrir diretamente uma rota que não pertence ao seu papel
- **THEN** a interface bloqueia a tela e o direciona para a página inicial autorizada

### Requirement: Gestão visual de corretoras
O sistema SHALL permitir ao investidor listar suas corretoras e cadastrar uma corretora pelo fluxo validado de CNPJ e CEP exigido pela API. Dados obtidos por consulta externa MUST ser diferenciados dos campos que o usuário pode preencher.

#### Scenario: Primeiro uso sem corretora
- **WHEN** o investidor ainda não possui corretoras
- **THEN** a interface explica que uma corretora é necessária para criar uma carteira e oferece a ação de cadastro

#### Scenario: Consulta de CEP
- **WHEN** o investidor informa um CEP válido no cadastro de corretora
- **THEN** a interface consulta a API e preenche rua, bairro, cidade e estado, permitindo completar número e complemento

#### Scenario: Cadastro validado de corretora
- **WHEN** o investidor envia apelido, CNPJ e endereço válidos
- **THEN** a interface apresenta o progresso da validação externa, inclui a corretora na listagem e a disponibiliza para novas carteiras

#### Scenario: Integração cadastral indisponível
- **WHEN** a API não consegue validar CNPJ, CEP ou participação na CVM
- **THEN** a interface preserva o formulário e apresenta o erro público sem permitir contornar manualmente a validação obrigatória

#### Scenario: Razão social consultada
- **WHEN** o investidor informa e consulta um CNPJ válido
- **THEN** a interface exibe a razão social oficial em campo somente leitura antes do cadastro

#### Scenario: Exclusão de corretora sem vínculo
- **WHEN** o investidor confirma a exclusão de uma corretora que não possui carteira vinculada
- **THEN** a API remove a corretora e a interface atualiza a listagem

#### Scenario: Exclusão de corretora vinculada
- **WHEN** o investidor tenta excluir uma corretora utilizada por alguma carteira
- **THEN** a API preserva a corretora e a interface explica em português que o vínculo deve ser removido primeiro

#### Scenario: Campos ausentes ou inválidos no cadastro
- **WHEN** o investidor tenta cadastrar uma corretora com informação obrigatória ausente ou inválida
- **THEN** a interface impede o envio e apresenta a orientação correspondente em português junto ao campo, sem usar a mensagem nativa do navegador

### Requirement: Visão geral e gestão de carteiras
O sistema SHALL listar as carteiras pertencentes ao investidor, identificar a corretora vinculada e permitir criar e excluir carteiras com confirmação explícita.

#### Scenario: Listagem com carteiras
- **WHEN** o investidor abre a visão geral e possui carteiras
- **THEN** a interface apresenta cartões navegáveis com nome, corretora e informações de atualização disponíveis

#### Scenario: Criação de carteira
- **WHEN** o investidor informa um nome válido e escolhe uma de suas corretoras
- **THEN** a interface identifica a corretora pelo apelido, cria a carteira, atualiza a listagem e oferece acesso ao novo dashboard

#### Scenario: Nenhuma corretora disponível
- **WHEN** o investidor tenta criar uma carteira sem possuir corretora
- **THEN** a interface impede o envio e oferece navegação direta para cadastrar uma corretora

#### Scenario: Exclusão confirmada
- **WHEN** o investidor confirma a exclusão de uma carteira
- **THEN** a interface solicita a exclusão à API, remove a carteira da listagem e comunica o resultado

### Requirement: Dashboard analítico da carteira
O sistema SHALL apresentar uma visão de BI da carteira com valor investido, patrimônio, ganho total, rentabilidade, posições, distribuição por ativo, evolução patrimonial, moedas e taxas cambiais retornadas pela API. Valores MUST ser formatados de acordo com a moeda e datas e percentuais MUST ter apresentação consistente em português do Brasil.

#### Scenario: Carteira com dados consolidados
- **WHEN** o investidor abre uma carteira com posições e dados de mercado disponíveis
- **THEN** a interface exibe indicadores consolidados, tabela de posições, gráfico de composição e gráfico de evolução comparando valor investido e patrimônio

#### Scenario: Identificação visual dos ativos
- **WHEN** a interface apresenta um ativo brasileiro ou americano em posições, lançamentos ou proventos
- **THEN** ela tenta exibir o logotipo correspondente ao ticker usando a fonte definida para o mercado, carrega a imagem sob demanda e apresenta um marcador local legível se a imagem estiver ausente ou falhar

#### Scenario: Transparência da fonte de logotipos
- **WHEN** logotipos fornecidos por um serviço que exige atribuição são apresentados
- **THEN** a interface mantém visível a atribuição e o link requeridos pelo provedor sem enviar credenciais ou dados da sessão

#### Scenario: Carteira com múltiplas moedas
- **WHEN** a valorização contém resumos em BRL e USD e um resumo consolidado
- **THEN** a interface permite compreender os valores por moeda, o consolidado em moeda-base e as taxas de câmbio aplicadas

#### Scenario: Atualização sob demanda
- **WHEN** o investidor aciona a atualização do dashboard
- **THEN** a interface busca novamente valorização, posições, evolução e resumos e indica quando os dados foram atualizados

#### Scenario: Carteira sem posições
- **WHEN** a carteira ainda não possui posição em custódia
- **THEN** a interface apresenta indicadores vazios de forma não enganosa, mantém espaçamento uniforme entre os painéis e destaca a ação para cadastrar o primeiro lançamento

#### Scenario: Provedor de mercado indisponível
- **WHEN** valorização ou evolução falha por indisponibilidade ou limite de uma integração externa
- **THEN** o painel mantém acessíveis os demais dados da carteira e apresenta no componente afetado uma mensagem com opção de tentar novamente

### Requirement: Pesquisa de ativos e cadastro de lançamentos
O sistema SHALL permitir pesquisar ações e ETFs nos mercados BR e US, selecionar somente um resultado válido da API e registrar compra ou venda com data, quantidade, preço unitário e custos. Datas MUST ser apresentadas e editadas no formato brasileiro `dd/mm/aaaa`. Campos de preço e custos MUST assumir formatação monetária da moeda selecionada ao perder o foco, sem enviar a representação localizada para a API.

#### Scenario: Pesquisa de ativo
- **WHEN** o investidor escolhe mercado e tipo e informa um termo de busca válido
- **THEN** a interface apresenta os ativos retornados com ticker, nome, moeda e cotação e permite selecionar um deles

#### Scenario: Compra efetiva
- **WHEN** o investidor seleciona um ativo e registra uma compra válida com data atual ou passada
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
- **WHEN** o investidor seleciona um ativo cuja moeda nativa não é BRL
- **THEN** cotação, preço, custos e resumo são apresentados e editados em BRL com indicação da taxa utilizada, enquanto a API recebe os valores convertidos de volta para a moeda nativa

### Requirement: Histórico e ciclo de vida dos lançamentos
O sistema SHALL exibir o log de compras e vendas da carteira com filtros de texto, tipo e estado, ordenação compreensível e ação de cancelamento somente quando aceita pelo contrato da API.

#### Scenario: Consulta do histórico
- **WHEN** a carteira possui lançamentos
- **THEN** a interface apresenta ticker, mercado, tipo, estado, data, quantidade, preço unitário, custos e valor bruto de cada item

#### Scenario: Cancelamento de lançamento pendente
- **WHEN** o investidor confirma o cancelamento de um lançamento pendente
- **THEN** a interface solicita o cancelamento, atualiza o item para cancelado e recalcula as consultas dependentes

#### Scenario: Edição de lançamento pendente
- **WHEN** o investidor escolhe editar um lançamento pendente e envia dados válidos
- **THEN** a API revalida e atualiza data, operação, quantidade, preço e custos e a interface atualiza os painéis afetados

#### Scenario: Histórico efetivado ou cancelado
- **WHEN** um lançamento está efetivado ou cancelado
- **THEN** a interface não oferece edição e a API rejeita tentativas diretas de alteração

#### Scenario: Histórico vazio ou sem correspondências
- **WHEN** não existem lançamentos ou os filtros não encontram resultados
- **THEN** a interface diferencia a ausência de dados da ausência de correspondências e oferece uma ação adequada

### Requirement: Gestão de proventos
O sistema SHALL permitir consultar resumo e histórico de proventos, buscar candidatos por mercado, confirmar candidatos elegíveis, registrar eventos manuais e cancelar eventos permitidos. Valores em moedas distintas MUST permanecer identificados e o consolidado MUST exibir o câmbio informado pela API.

#### Scenario: Confirmação de candidato
- **WHEN** o investidor seleciona um candidato confirmável e informa o valor recebido
- **THEN** a interface registra o provento, atualiza o histórico e impede confirmação duplicada do mesmo candidato

#### Scenario: Cadastro manual
- **WHEN** o investidor informa ticker, tipo, data de pagamento e valor válidos no formulário manual
- **THEN** a interface registra o evento manual e apresenta sua origem e estado no histórico

#### Scenario: Ativo elegível para provento manual
- **WHEN** o investidor abre o cadastro de provento manual
- **THEN** a interface permite escolher somente tickers com posição atualmente custodiada na carteira

#### Scenario: Entrada localizada de provento
- **WHEN** o investidor informa datas e valores no cadastro de provento manual
- **THEN** as datas são apresentadas em `dd/mm/aaaa` e os valores monetários são editados com máscara em reais, convertidos para a moeda nativa somente no envio quando necessário

#### Scenario: Candidato inelegível ou já registrado
- **WHEN** um candidato é retornado como não confirmável ou já registrado
- **THEN** a interface explica a condição e não disponibiliza a ação de confirmação

#### Scenario: Resumo de proventos
- **WHEN** existem proventos efetivos em uma ou mais moedas
- **THEN** a interface apresenta totais por moeda e, quando disponível, total consolidado e taxas cambiais utilizadas

### Requirement: Administração de usuários
O sistema SHALL fornecer aos administradores uma tela para listar, consultar, criar, editar, desativar e reativar usuários, apresentando papel e estado sem solicitar ou revelar dados financeiros.

#### Scenario: Listagem administrativa
- **WHEN** um administrador abre a gestão de usuários
- **THEN** a interface exibe nome, e-mail, papel, estado e datas cadastrais, com busca e filtros locais

#### Scenario: Criação ou edição de usuário
- **WHEN** o administrador envia dados válidos para criar ou editar uma conta
- **THEN** a interface atualiza a listagem e reflete o papel e o estado atuais retornados pela API

#### Scenario: Proteção do último administrador ativo
- **WHEN** a API rejeita uma ação que removeria o último administrador ativo ou a autodesativação proibida
- **THEN** a interface mantém o estado anterior e explica por que a operação não foi realizada

#### Scenario: Desativação e reativação
- **WHEN** o administrador confirma uma mudança de estado permitida
- **THEN** a interface executa a ação e atualiza imediatamente o estado visual da conta

### Requirement: Estados de interface, erros e confirmações
O sistema MUST representar de forma acessível carregamento, sucesso, vazio, erro e tentativa novamente. Erros da API MUST ser tratados pelo campo `code` e pelos `fieldErrors`, sem depender do texto da mensagem como identificador.

#### Scenario: Operação em andamento
- **WHEN** uma consulta ou mutação está em andamento
- **THEN** a interface sinaliza progresso, evita envios duplicados e mantém estável o conteúdo que ainda é válido

#### Scenario: Erro de campo
- **WHEN** a API devolve `fieldErrors`
- **THEN** cada mensagem é associada ao campo correspondente e um resumo acessível informa a falha

#### Scenario: Ação destrutiva
- **WHEN** o usuário solicita excluir, cancelar ou desativar um registro
- **THEN** a interface exige confirmação que identifique claramente o alvo e a consequência antes de enviar a ação

#### Scenario: Falha inesperada
- **WHEN** ocorre erro de rede ou código público desconhecido
- **THEN** a interface apresenta uma mensagem segura, preserva dados úteis e oferece nova tentativa quando aplicável

#### Scenario: Validação local compreensível
- **WHEN** um formulário contém campos ausentes, incompletos ou inválidos
- **THEN** todas as mensagens de validação apresentadas pela aplicação estão em português e o primeiro campo inválido pode receber foco

### Requirement: Experiência responsiva e acessível
O sistema MUST manter os fluxos essenciais utilizáveis em telas móveis e desktop, com navegação por teclado, foco visível, rótulos de formulário, contraste legível e alternativas textuais para informações representadas em gráficos.

#### Scenario: Uso em tela pequena
- **WHEN** a largura disponível não comporta a visualização de desktop
- **THEN** navegação, cartões, formulários, tabelas e gráficos se reorganizam sem ocultar ações essenciais nem produzir rolagem horizontal na página

#### Scenario: Navegação por teclado
- **WHEN** uma pessoa percorre controles por teclado
- **THEN** a ordem de foco é coerente e modais, menus e formulários podem ser operados sem mouse

#### Scenario: Informação apresentada em gráfico
- **WHEN** composição ou evolução é exibida visualmente
- **THEN** os mesmos valores relevantes também podem ser lidos em legenda, resumo ou tabela acessível

### Requirement: Configuração e qualidade do frontend
O sistema MUST permitir configurar a URL-base da API sem editar o código-fonte e SHALL possuir verificações automatizadas para compilação, análise estática e fluxos críticos da interface.

#### Scenario: Ambiente local configurado
- **WHEN** o frontend inicia com uma URL-base válida e a origem está autorizada no backend
- **THEN** todas as chamadas usam essa configuração e os fluxos podem operar contra a API local

#### Scenario: Validação automatizada
- **WHEN** a validação do frontend é executada
- **THEN** análise estática, testes e build de produção terminam com resultado verificável e sem depender das integrações externas reais
