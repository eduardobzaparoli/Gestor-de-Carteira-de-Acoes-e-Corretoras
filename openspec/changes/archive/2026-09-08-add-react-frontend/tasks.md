## 1. Fundação do frontend

- [x] 1.1 Criar o projeto `frontend/` com React, TypeScript e Vite, configuração estrita do TypeScript e estrutura inicial orientada a recursos.
- [x] 1.2 Adicionar e fixar as dependências de execução e teste definidas no design, com scripts para desenvolvimento, lint, testes, cobertura e build.
- [x] 1.3 Implementar tokens visuais, estilos globais, tipografia, breakpoints e componentes básicos acessíveis de botão, campo, seleção, cartão, badge, skeleton, alerta, toast, diálogo e estado vazio.
- [x] 1.4 Modelar em TypeScript os DTOs e enums atuais da API e implementar formatadores pt-BR de moeda, número, percentual, data e estado.
- [x] 1.5 Implementar o cliente HTTP central com URL-base configurável, Bearer token, `ApiError`, `fieldErrors`, cancelamento de requisições e tratamento global de `401`.
- [x] 1.6 Configurar os providers de consulta, sessão, notificações e roteamento, incluindo página não encontrada e limites de erro da interface.

## 2. Autenticação e navegação

- [x] 2.1 Implementar armazenamento de sessão em memória e `sessionStorage`, restauração por `/api/auth/me`, logout e limpeza do cache autenticado.
- [x] 2.2 Criar as telas responsivas de login e cadastro com validação local, erros remotos por campo, estados de envio e redirecionamento por papel.
- [x] 2.3 Criar guardas de autenticação e papel para separar os fluxos `INVESTOR` e `ADMIN` e impedir acesso direto a rotas incompatíveis.
- [x] 2.4 Implementar o shell autenticado com sidebar desktop, navegação móvel, cabeçalho, identificação do usuário e encerramento de sessão.
- [x] 2.5 Cobrir restauração, login, cadastro, expiração e autorização de rotas com testes de integração da interface.

## 3. Corretoras e primeiro uso

- [x] 3.1 Implementar consultas e mutação de corretoras com chaves de cache e invalidação adequadas.
- [x] 3.2 Criar a tela de listagem de corretoras com cartões responsivos, detalhes cadastrais e estado vazio orientando o primeiro uso.
- [x] 3.3 Criar o formulário de cadastro com máscaras de CNPJ e CEP, consulta de CEP, diferenciação de campos consultados e editáveis e preservação após erros externos.
- [x] 3.4 Testar listagem, consulta de CEP, cadastro validado e falhas de Brasil API, ViaCEP e CVM usando respostas simuladas.

## 4. Gestão de carteiras

- [x] 4.1 Implementar consultas, criação e exclusão de carteiras com atualização coordenada do cache.
- [x] 4.2 Criar a página principal do investidor com saudação, resumo visual, grade de carteiras e estados de carregamento, vazio e erro.
- [x] 4.3 Criar o fluxo de nova carteira com seleção de corretora e atalho obrigatório para cadastrar corretora quando nenhuma estiver disponível.
- [x] 4.4 Implementar exclusão de carteira com diálogo acessível, identificação do alvo, bloqueio de duplo envio e retorno visual do resultado.
- [x] 4.5 Testar primeiro uso, criação, navegação para a carteira e exclusão confirmada.

## 5. Dashboard BI da carteira

- [x] 5.1 Implementar consultas independentes para detalhes, posições, valorização, evolução, transações e resumo de proventos, com atualização conjunta sob demanda.
- [x] 5.2 Criar o cabeçalho da carteira e cartões de KPI para valor investido, patrimônio, ganho, rentabilidade e proventos, com variações positivas, negativas e indisponíveis.
- [x] 5.3 Criar resumos por moeda e painel de taxas de câmbio sem realizar consolidações financeiras não fornecidas pela API.
- [x] 5.4 Implementar o gráfico donut de composição com legenda e tabela acessível usando os percentuais retornados pela valorização.
- [x] 5.5 Implementar o gráfico de linhas de evolução patrimonial comparando valor investido e patrimônio, com tooltip formatado e alternativa textual acessível.
- [x] 5.6 Criar a tabela responsiva de posições com ticker, mercado, quantidade, preço médio, custo, preço atual, patrimônio, ganho e participação.
- [x] 5.7 Implementar isolamento de carregamento, vazio e erro por widget, incluindo mensagens específicas e nova tentativa para indisponibilidade ou limite dos provedores.
- [x] 5.8 Testar dashboard completo, carteira vazia, múltiplas moedas, atualização e falha parcial de integrações.

## 6. Pesquisa de ativos e lançamentos

- [x] 6.1 Implementar busca de ativos com atraso controlado, mercado BR/US, tipo STOCK/ETF e seleção explícita do resultado da API.
- [x] 6.2 Criar o formulário de compra e venda com data, quantidade, preço, custos, resumo da operação e indicação de lançamento futuro pendente.
- [x] 6.3 Integrar a criação de lançamento e invalidar histórico, posições, valorização e evolução após sucesso.
- [x] 6.4 Criar o histórico de lançamentos com busca, filtros de tipo e estado, ordenação e apresentação responsiva dos campos financeiros.
- [x] 6.5 Implementar cancelamento com confirmação e disponibilidade da ação coerente com o estado do lançamento.
- [x] 6.6 Testar pesquisa, compra, venda, lançamento futuro, saldo insuficiente, filtros e cancelamento.

## 7. Proventos

- [x] 7.1 Implementar consultas de histórico, resumo e candidatos por mercado e mutações de confirmação, cadastro manual e cancelamento.
- [x] 7.2 Criar os indicadores de proventos por moeda e consolidados, com apresentação das taxas cambiais fornecidas pela API.
- [x] 7.3 Criar a visualização de candidatos com estados confirmável, inelegível e já registrado e formulário de confirmação com motivo de ajuste.
- [x] 7.4 Criar o formulário de provento manual com campos condicionais, validações e explicações de origem e elegibilidade.
- [x] 7.5 Criar o histórico de proventos com filtros, badges de tipo, origem e estado e cancelamento confirmado quando permitido.
- [x] 7.6 Testar candidatos, confirmação, duplicidade, cadastro manual, resumo multimoeda e cancelamento.

## 8. Administração de usuários

- [x] 8.1 Implementar consultas e mutações administrativas para listar, consultar, criar, editar, desativar e reativar usuários.
- [x] 8.2 Criar a tela administrativa com indicadores, busca, filtros de papel e estado e tabela responsiva sem dados financeiros.
- [x] 8.3 Criar formulários de criação e edição com validação, seleção de papel e tratamento seguro de senha.
- [x] 8.4 Implementar confirmações de desativação e reativação e mensagens específicas para autodesativação e proteção do último administrador ativo.
- [x] 8.5 Testar o fluxo administrativo, mudanças de papel e estado, proteções da API e ausência de navegação financeira.

## 9. Responsividade, acessibilidade e acabamento

- [x] 9.1 Revisar todas as telas em larguras móvel, tablet e desktop, corrigindo overflow, hierarquia, densidade e posicionamento das ações.
- [x] 9.2 Validar navegação por teclado, foco de diálogos, rótulos, regiões vivas, contraste e conteúdo textual equivalente aos gráficos.
- [x] 9.3 Padronizar textos em português, microinterações, skeletons, transições discretas, confirmações e mensagens mapeadas por código público da API.
- [x] 9.4 Executar uma inspeção visual das jornadas completas com dados vazios, dados representativos, ganhos, perdas e falhas parciais.

## 10. Configuração, documentação e entrega

- [x] 10.1 Adicionar `frontend/.env.example`, ignorar arquivos locais e documentar `VITE_API_BASE_URL` sem incluir segredos no bundle.
- [x] 10.2 Atualizar a documentação do projeto com requisitos Node, instalação, execução simultânea, configuração de `CORS_ALLOWED_ORIGINS` e comandos de validação.
- [x] 10.3 Adicionar ao pipeline um job independente que instale dependências de forma reproduzível e execute lint, testes e build do frontend.
- [x] 10.4 Executar lint, testes unitários e integrados, testes de navegador e build de produção do frontend e corrigir todas as falhas.
- [x] 10.5 Executar novamente os gates completos H2 e PostgreSQL do backend para comprovar ausência de regressões.
- [x] 10.6 Atualizar o mapa Graphify, validar a mudança OpenSpec em modo estrito e registrar os resultados finais da implementação.

## 11. Refinamentos após validação visual

- [x] 11.1 Adicionar consulta de CNPJ e exibição somente leitura da razão social no cadastro de corretora, com mensagens públicas em português.
- [x] 11.2 Implementar exclusão segura de corretora na API e na interface, bloqueando corretoras vinculadas a carteiras e exigindo confirmação.
- [x] 11.3 Exibir somente o apelido da corretora no seletor de criação de carteira e uniformizar o espaçamento vertical do dashboard vazio.
- [x] 11.4 Reinicializar integralmente o formulário de lançamento em cada abertura.
- [x] 11.5 Implementar entrada de data `dd/mm/aaaa` e formatação monetária no desfoque para preço e custos.
- [x] 11.6 Implementar atualização de lançamentos pendentes na API e ação de edição no histórico, com revalidações de domínio e invalidação de cache.
- [x] 11.7 Cobrir os refinamentos com testes de frontend e backend, incluindo bloqueios de integridade e mensagens localizadas.
- [x] 11.8 Executar lint, testes, build, validação OpenSpec e atualização do Graphify.

## 12. Localização de formulários e conversão visual para BRL

- [x] 12.1 Substituir mensagens nativas de validação por mensagens próprias em português nos formulários do frontend.
- [x] 12.2 Criar um campo de data brasileiro reutilizável com seletor de calendário e conclusão do ano em entradas `dd/mm`.
- [x] 12.3 Aplicar o campo de data à criação e edição de lançamentos e ao cadastro de proventos manuais.
- [x] 12.4 Restringir o ticker do provento manual às posições atualmente custodiadas na carteira.
- [x] 12.5 Aplicar máscara em reais aos valores monetários de lançamentos e proventos manuais.
- [x] 12.6 Expor consulta cambial autenticada por carteira e converter valores estrangeiros somente para apresentação, preservando a moeda nativa nos comandos enviados à API.
- [x] 12.7 Cobrir mensagens localizadas, calendário, conclusão de datas, seleção de ticker, máscaras e conversão cambial com testes automatizados.
- [x] 12.8 Executar lint, testes, build, gates do backend, validação OpenSpec e atualização do Graphify.

## 13. Identificação visual dos ativos

- [x] 13.1 Implementar um componente reutilizável de logotipo por ticker, com fontes específicas para BR e US, carregamento preguiçoso, privacidade e fallback local.
- [x] 13.2 Aplicar o componente às posições, lançamentos, edição e proventos e incluir a atribuição obrigatória do provedor de logos americanos.
- [x] 13.3 Cobrir carregamento, URLs por mercado e fallback com testes automatizados.
- [x] 13.4 Executar lint, testes, build, validação OpenSpec e atualização do Graphify.

