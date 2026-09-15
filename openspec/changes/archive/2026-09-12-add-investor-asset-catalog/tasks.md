## 1. Persistência e domínio do catálogo

- [x] 1.1 Criar a migration Flyway `V2` para o catálogo de ativos com UUID, proprietário, identidade normalizada, cotação, instante da consulta, auditoria, chave estrangeira, índice de listagem e unicidade por investidor/mercado/ticker.
- [x] 1.2 Implementar domínio, entidade JPA e repository do ativo cadastrado com precisão decimal e tipos compatíveis com H2 e PostgreSQL.
- [x] 1.3 Criar DTOs, mapper e erros públicos `ASSET_ALREADY_REGISTERED` e `REGISTERED_ASSET_NOT_FOUND`, mantendo respostas exclusivamente em JSON.
- [x] 1.4 Atualizar verificações de esquema e testes de migration para bancos vazios e já migrados nos perfis suportados.

## 2. Pesquisa e cotação de ativos

- [x] 2.1 Alterar a pesquisa externa para `GET /api/assets/search`, removendo a dependência de carteira e mantendo autorização exclusiva do investidor.
- [x] 2.2 Adaptar o cache de seleção temporária para escopo de proprietário, expiração e consumo no cadastro persistente, sem aceitar identidade livre do cliente.
- [x] 2.3 Extrair uma operação de cotação fresca que resolva Brapi ou Twelve Data, ignore valores previamente armazenados antes da chamada obrigatória e valide preço positivo e moeda compatível.
- [x] 2.4 Cobrir pesquisa global, isolamento da seleção, expiração, cotação fresca e falhas dos dois provedores com testes unitários e de integração simulados.

## 3. API e regras do catálogo

- [x] 3.1 Implementar service e controller para cadastrar um ativo por `assetSelectionId`, consultando cotação fresca e persistindo identidade, valor e instante de forma atômica.
- [x] 3.2 Implementar listagem de ativos próprios com filtro opcional `BR`/`US`, ordenação determinística e resposta contendo cotação e data/hora da consulta.
- [x] 3.3 Implementar atualização individual por `POST /api/assets/{assetId}/quote-refresh`, preservando os dados anteriores quando a chamada externa falhar.
- [x] 3.4 Implementar cotação transitória por `GET /api/assets/{assetId}/quote` para preparação do lançamento sem alterar o catálogo.
- [x] 3.5 Garantir unicidade concorrente no banco e traduzir conflitos de cadastro para `409 ASSET_ALREADY_REGISTERED` sem expor SQL.
- [x] 3.6 Cobrir autorização, isolamento entre investidores, filtros, duplicidade simples/concorrente e atomicidade em testes de serviço e integração.

## 4. Integração com lançamentos

- [x] 4.1 Substituir `assetSelectionId` por `registeredAssetId` no request de criação e resolver o ativo exclusivamente pelo identificador e proprietário autenticado.
- [x] 4.2 Manter a cópia do retrato do catálogo no lançamento sem chave estrangeira obrigatória, preservando a leitura de lançamentos anteriores à migration.
- [x] 4.3 Atualizar testes de criação, autorização, duas carteiras do mesmo investidor, ativo alheio/inexistente, validação de saldo e estados pendente/efetivo.

## 5. Tela React de ativos

- [x] 5.1 Adicionar rota protegida e carregada sob demanda `/app/ativos` e item Ativos na navegação do investidor, sem expor a tela ao administrador.
- [x] 5.2 Criar a página de catálogo com cabeçalho, estado vazio, cartões ou grade responsiva, filtros Todos/Brasileiros/Americanos, temas claro/escuro e os componentes visuais existentes.
- [x] 5.3 Criar o diálogo de cadastro com mercado, tipo, pesquisa externa, seleção única e confirmação por referência temporária, sem campo que permita persistir ticker livre.
- [x] 5.4 Exibir ticker, nome, mercado, tipo, moeda, cotação localizada e data/hora da consulta, reutilizando logo, badges e acessibilidade existentes.
- [x] 5.5 Implementar Atualizar cotação por item com carregamento isolado, notificação de sucesso e preservação visual do valor anterior em caso de erro.
- [x] 5.6 Cobrir navegação, filtros, cadastro, duplicidade, estados vazio/erro/carregamento, atualização e responsividade com testes React.

## 6. Novo fluxo de lançamento no frontend

- [x] 6.1 Remover a pesquisa externa do diálogo de lançamento e carregar somente o catálogo do investidor, oferecendo navegação para Ativos quando estiver vazio.
- [x] 6.2 Ao selecionar um ativo, limpar qualquer preço residual, consultar a cotação transitória e preencher o valor com formatação BRL e conversão existentes para ativos americanos.
- [x] 6.3 Enviar `registeredAssetId` com o preço confirmado pelo investidor e bloquear a conclusão enquanto a cotação obrigatória estiver ausente ou tiver falhado.
- [x] 6.4 Atualizar testes React de compras, vendas, ativos BR/US, troca de seleção, falha de cotação, nova abertura do formulário e compatibilidade com datas e moedas.

## 7. Documentação e validação final

- [x] 7.1 Atualizar `docs/product-spec.md`, guia da API e documentação de configuração com o cadastro prévio, novos endpoints e mudança incompatível do request de lançamento.
- [x] 7.2 Executar testes Maven relevantes e completos, incluindo gates de release H2 e PostgreSQL sem chamadas reais aos provedores.
- [x] 7.3 Executar lint, testes e build de produção do frontend React.
- [x] 7.4 Executar validação OpenSpec estrita, verificar formatação/diff e atualizar o mapa Graphify após a implementação.

## 8. Refinamentos de navegação, apresentação e exclusão

- [x] 8.1 Implementar exclusão individual e total do catálogo com validação de saldo positivo em todas as carteiras, atomicidade e erros públicos para ativo protegido.
- [x] 8.2 Adicionar ações e confirmações de exclusão na tela de ativos, incluindo tratamento da proteção por saldo e atualização da listagem.
- [x] 8.3 Tornar a marca Bom Investidor um link para a visão geral, exibir cotações americanas do catálogo em BRL e manter a cotação nativa apenas no contrato da API.
- [x] 8.4 Reutilizar as logos nos resultados do diálogo de lançamento e remover textos e links de atribuição Parqet de toda a aplicação.
- [x] 8.5 Atualizar documentação e testes de backend/frontend, executar as verificações disponíveis e atualizar o Graphify após os refinamentos.
