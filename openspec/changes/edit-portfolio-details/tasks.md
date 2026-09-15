## 1. Contrato e modelo de carteira

- [x] 1.1 Criar o DTO de atualização de carteira com `name` e `brokerageId` obrigatórios e documentar o contrato `PUT /api/portfolios/{id}`.
- [x] 1.2 Reutilizar ou adaptar a normalização de entrada para aplicar ao nome editado as mesmas regras de formato, limite e chave normalizada usadas na criação.
- [x] 1.3 Permitir a troca controlada de nome, chave normalizada e corretora na entidade, mantendo imutáveis identificador, proprietário e data de criação.

## 2. Serviço, persistência e API

- [x] 2.1 Adicionar ao repositório a consulta de nome duplicado por proprietário que desconsidere a própria carteira editada.
- [x] 2.2 Implementar a atualização transacional na camada de serviço, localizando a carteira e a corretora pelo usuário autenticado e preservando lançamentos, posições e demais dados financeiros.
- [x] 2.3 Persistir e descarregar a atualização para que `updatedAt` seja renovado e conflitos concorrentes de nome sejam traduzidos para `409 PORTFOLIO_NAME_ALREADY_REGISTERED`.
- [x] 2.4 Expor `PUT /api/portfolios/{id}` no controller, retornando `200` com `PortfolioResponse` e mantendo os erros JSON existentes para validação, propriedade e autenticação.

## 3. Interface React

- [x] 3.1 Adicionar uma ação de edição no rodapé de cada cartão, entre o acesso ao dashboard e a exclusão, com ícone, rótulo acessível e distinção visual adequada.
- [x] 3.2 Criar um diálogo de edição separado do formulário de criação, preenchido com o nome e a corretora atuais e sem reaproveitar dados residuais de outra carteira.
- [x] 3.3 Carregar no seletor somente as corretoras do investidor, validar nome e seleção no formulário e apresentar orientações em português.
- [x] 3.4 Integrar o diálogo ao endpoint de atualização, preservar os valores e exibir o erro público em falhas, e fechar com confirmação em caso de sucesso.
- [x] 3.5 Atualizar ou invalidar as consultas da listagem e do detalhe após salvar para refletir nome, corretora e data de atualização sem recarregamento manual.
- [x] 3.6 Garantir que ação, diálogo, foco, campos e mensagens sigam o padrão visual atual nos modos claro e escuro, em telas pequenas e na navegação por teclado.

## 4. Verificação e documentação

- [x] 4.1 Cobrir no backend os casos de sucesso, alteração isolada de cada campo, nome inválido ou duplicado, recursos alheios ou inexistentes, autenticação e preservação do histórico.
- [x] 4.2 Adicionar teste de integração da proteção contra nomes concorrentes e executar a persistência relevante tanto com H2 quanto com PostgreSQL.
- [x] 4.3 Cobrir no frontend a abertura preenchida, edição bem-sucedida, única corretora disponível, validação local, erros da API, atualização da listagem e ausência de estado residual.
- [x] 4.4 Atualizar a documentação da API com o novo endpoint, seus exemplos e respostas, sem alterar `docs/product-spec.md` nesta change.
- [x] 4.5 Executar os testes Maven relevantes e completos, além de lint, testes e build do frontend, corrigindo qualquer regressão encontrada.
- [x] 4.6 Validar a change OpenSpec em modo estrito e atualizar o índice Graphify para refletir os novos artefatos e relacionamentos implementados.
