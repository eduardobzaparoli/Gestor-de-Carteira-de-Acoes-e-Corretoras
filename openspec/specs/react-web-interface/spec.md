# react-web-interface Specification

## Purpose

Disponibilizar uma interface web responsiva e orientada a dados para que investidores e administradores utilizem integralmente os recursos existentes da API sem depender de ferramentas técnicas.

## Requirements

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
- **THEN** a interface solicita a exclusão, remove a carteira da listagem e comunica o resultado

### Requirement: Edição visual dos dados da carteira
O sistema SHALL oferecer, em cada cartão da visão geral de carteiras, uma ação identificada para editar a carteira. A ação MUST abrir um diálogo preenchido com o nome e a corretora atuais, permitir escolher somente entre as corretoras do investidor e enviar os dois campos à operação de atualização. Em caso de sucesso, a interface MUST refletir os novos dados sem exigir recarregamento manual; em caso de falha, MUST preservar o formulário e apresentar o erro público correspondente.

#### Scenario: Abertura do diálogo preenchido
- **WHEN** o investidor aciona Editar em um cartão de carteira
- **THEN** a interface abre um diálogo com o nome e a corretora atuais selecionados e sem dados residuais de outra carteira

#### Scenario: Edição concluída
- **WHEN** o investidor confirma nome e corretora válidos e a API responde com sucesso
- **THEN** a interface fecha o diálogo, informa a conclusão e atualiza o cartão e as consultas dependentes com os dados retornados

#### Scenario: Nenhuma corretora alternativa
- **WHEN** o investidor possui somente a corretora já vinculada
- **THEN** a interface ainda permite editar o nome mantendo essa corretora selecionada

#### Scenario: Formulário inválido
- **WHEN** o nome fica vazio ou nenhuma corretora válida está selecionada
- **THEN** a interface impede o envio e apresenta orientação em português no contexto do formulário

#### Scenario: Conflito ou recurso não encontrado
- **WHEN** a API rejeita a edição por nome duplicado, carteira inexistente ou corretora indisponível
- **THEN** a interface mantém os valores informados, apresenta a mensagem pública correspondente e permite corrigir ou cancelar

#### Scenario: Uso responsivo e acessível
- **WHEN** a edição é utilizada em tela pequena, modo claro, modo escuro ou por teclado
- **THEN** a ação, o diálogo, os campos, o foco e as mensagens permanecem visíveis e operáveis segundo o padrão atual da aplicação

### Requirement: Dashboard analítico da carteira
O sistema SHALL apresentar uma visão de BI da carteira com valor investido, patrimônio, ganho total, rentabilidade, posições, distribuição por ativo, evolução patrimonial, moedas e taxas cambiais retornadas pela API. Valores MUST ser formatados de acordo com a moeda e datas e percentuais MUST ter apresentação consistente em português do Brasil.

#### Scenario: Carteira com dados consolidados
- **WHEN** o investidor abre uma carteira com posições e dados de mercado disponíveis
- **THEN** a interface exibe indicadores consolidados, tabela de posições, gráfico de composição e gráfico de evolução comparando valor investido e patrimônio

#### Scenario: Identificação visual dos ativos
- **WHEN** a interface apresenta um ativo brasileiro ou americano em posições, lançamentos ou proventos
- **THEN** ela tenta exibir o logotipo correspondente ao ticker usando a fonte definida para o mercado, carrega a imagem sob demanda e apresenta um marcador local legível se a imagem estiver ausente ou falhar

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
O sistema MUST manter os fluxos essenciais utilizáveis em telas móveis e desktop, com navegação por teclado, foco visível, rótulos de formulário, contraste legível, identidade visual predominantemente laranja e alternativas textuais para informações representadas em gráficos. Os elementos anteriormente verdes MUST adotar laranja ou tons laranjados, preservando cores distintas para erros, perdas, avisos e informações quando necessárias à compreensão semântica.

#### Scenario: Uso em tela pequena
- **WHEN** a largura disponível não comporta a visualização de desktop
- **THEN** navegação, cartões, formulários, tabelas e gráficos se reorganizam sem ocultar ações essenciais nem produzir rolagem horizontal na página

#### Scenario: Navegação por teclado
- **WHEN** uma pessoa percorre controles por teclado
- **THEN** a ordem de foco é coerente e modais, menus e formulários podem ser operados sem mouse

#### Scenario: Informação apresentada em gráfico
- **WHEN** composição ou evolução é exibida visualmente
- **THEN** os mesmos valores relevantes também podem ser lidos em legenda, resumo ou tabela acessível

#### Scenario: Identidade cromática laranja
- **WHEN** qualquer tela pública, do investidor ou administrativa é apresentada
- **THEN** botões primários, navegação, destaques, foco, estados positivos ou de sucesso, gráficos e demais elementos da identidade principal usam laranja ou tons laranjados, sem resíduos visuais verdes e com contraste legível

#### Scenario: Diferenciação de estados semânticos
- **WHEN** a interface apresenta erro, perda, aviso ou informação secundária
- **THEN** ela mantém diferenciação visual suficiente em relação à cor principal laranja por meio de cor, texto, ícone ou rótulo

### Requirement: Seleção e persistência do tema visual
O sistema MUST oferecer modos claro e escuro em toda a interface web. O modo claro MUST preservar a apresentação laranja e clara vigente, enquanto o modo escuro MUST usar superfícies pretas ou cinza-escuras, textos legíveis e o laranja como cor principal. A preferência explícita do usuário MUST ser persistida localmente e restaurada sem depender de autenticação ou da API.

#### Scenario: Primeiro acesso sem preferência salva
- **WHEN** a aplicação inicia sem uma preferência de tema previamente escolhida
- **THEN** ela usa a preferência de esquema de cores do sistema operacional e adota o modo claro quando essa informação não está disponível

#### Scenario: Preferência previamente salva
- **WHEN** a aplicação inicia com uma escolha válida de modo claro ou escuro armazenada no navegador
- **THEN** ela aplica essa escolha antes de apresentar a interface, sem exibir momentaneamente o tema incorreto

#### Scenario: Alternância manual de tema
- **WHEN** o usuário aciona o controle de tema em uma tela pública, do investidor ou administrativa
- **THEN** a interface alterna imediatamente entre claro e escuro, informa de forma acessível qual modo será ativado e persiste a nova escolha no navegador

#### Scenario: Apresentação no modo claro
- **WHEN** o modo claro está ativo
- **THEN** a interface preserva a identidade visual clara e laranja existente, incluindo fundos, navegação, componentes, indicadores e gráficos

#### Scenario: Apresentação no modo escuro
- **WHEN** o modo escuro está ativo
- **THEN** páginas, painéis, modais, formulários, tabelas, menus, estados de interação e gráficos usam superfícies pretas ou cinza-escuras com acentos laranja e contraste legível

#### Scenario: Diferenciação semântica nos dois temas
- **WHEN** qualquer tema apresenta erro, perda, aviso, sucesso ou informação secundária
- **THEN** o estado continua identificável por contraste, texto, ícone ou rótulo, sem depender exclusivamente da cor principal

#### Scenario: Uso responsivo e por teclado
- **WHEN** o controle de tema é utilizado em desktop, dispositivo móvel ou por navegação de teclado
- **THEN** ele permanece visível, operável, com foco perceptível e sem ocultar ações essenciais da tela

### Requirement: Configuração e qualidade do frontend
O sistema MUST permitir configurar a URL-base da API sem editar o código-fonte e SHALL possuir verificações automatizadas para compilação, análise estática e fluxos críticos da interface.

#### Scenario: Ambiente local configurado
- **WHEN** o frontend inicia com uma URL-base válida e a origem está autorizada no backend
- **THEN** todas as chamadas usam essa configuração e os fluxos podem operar contra a API local

#### Scenario: Validação automatizada
- **WHEN** a validação do frontend é executada
- **THEN** análise estática, testes e build de produção terminam com resultado verificável e sem depender das integrações externas reais

### Requirement: Composição consolidada com fechamento visual
O sistema SHALL usar na tabela de posições e no gráfico de composição os percentuais consolidados em `BRL` devolvidos pela API. A interface MUST apresentar uma única distribuição para todos os mercados e moedas, manter correspondência entre fatias, legendas e linhas e exibir percentuais cuja soma seja `100%` quando houver patrimônio positivo.

#### Scenario: Carteira com ativos brasileiros e americanos
- **WHEN** o dashboard recebe posições em `BRL` e `USD`
- **THEN** tabela, gráfico e legenda exibem as mesmas participações consolidadas e a soma apresentada é `100%`

#### Scenario: Diferença residual de arredondamento
- **WHEN** os percentuais recebidos possuem casas além da precisão visual
- **THEN** a interface usa os valores públicos já reconciliados e não recalcula cada moeda como uma distribuição independente

#### Scenario: Carteira sem patrimônio positivo
- **WHEN** não há posições com valor de mercado positivo
- **THEN** a interface não fabrica percentuais nem uma composição enganosa e apresenta o estado vazio correspondente

### Requirement: Estados resilientes da descoberta de proventos
O sistema SHALL consumir o objeto enriquecido da consulta de candidatos e distinguir resultado atualizado, resultado reutilizado, falha parcial e indisponibilidade total. Candidatos confiáveis MUST permanecer operáveis quando existirem avisos parciais, e o cadastro manual MUST continuar acessível independentemente do estado dos provedores externos.

#### Scenario: Consulta totalmente atualizada
- **WHEN** todos os tickers necessários são consultados ou reutilizados dentro da janela normal sem avisos
- **THEN** a interface apresenta os candidatos ou o estado vazio sem mensagem de erro

#### Scenario: Dados anteriores reutilizados
- **WHEN** a API informa reutilização de último resultado por falha na atualização
- **THEN** a interface mantém os candidatos disponíveis e informa em português que os dados não puderam ser atualizados, exibindo a referência temporal disponível

#### Scenario: Falha parcial
- **WHEN** a API retorna candidatos e avisos para um ou mais tickers
- **THEN** a interface exibe os candidatos confiáveis, identifica os ativos não atualizados e oferece nova tentativa sem ocultar os resultados válidos

#### Scenario: Falha total
- **WHEN** a API responde com indisponibilidade total e nenhum resultado confiável
- **THEN** a interface apresenta o código público traduzido, oferece nova tentativa e mantém disponível a ação de registro manual

#### Scenario: Limite da Alpha Vantage
- **WHEN** a descoberta americana não pode ser atualizada porque o limite da Alpha Vantage foi atingido
- **THEN** a interface evita tentativas automáticas repetidas, usa dados reutilizáveis quando fornecidos e orienta o investidor a tentar novamente mais tarde
