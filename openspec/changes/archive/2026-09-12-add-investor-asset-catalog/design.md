## Context

Veja `proposal.md` para a motivação. Hoje, `AssetSearchController` pesquisa dentro de uma carteira, `AssetSearchService` grava um `SelectedAsset` no `AssetSelectionCache` com proprietário e carteira, e `PortfolioTransactionService` consome essa referência uma única vez para copiar o retrato do ativo ao lançamento. Não existe entidade persistente de ativo. O Graphify mostra esse serviço de pesquisa como um núcleo também reutilizado por valorização e evolução, enquanto a gestão de corretoras fornece o padrão de controller, service, repository, mapper e página privada por investidor.

A mudança atravessa persistência, autorização, contratos REST e React. Brapi continua responsável por `BR` e Twelve Data por `US`; nenhum novo provedor ou dependência é necessário. Os lançamentos existentes já contêm o retrato do ativo e precisam continuar legíveis sem migração retroativa.

## Goals / Non-Goals

**Goals:**

- Criar uma fonte persistente e isolada por investidor para os ativos selecionáveis em lançamentos.
- Separar a cotação histórica do catálogo das cotações correntes solicitadas durante um lançamento.
- Reutilizar as estratégias externas e o padrão arquitetural em camadas já existentes.
- Manter o contrato equivalente em H2 e PostgreSQL e garantir unicidade também sob concorrência.
- Introduzir a página React sem divergir dos temas claro/escuro, componentes e comportamento responsivo atuais.

**Non-Goals:**

- Editar a identidade de ativos cadastrados nesta etapa.
- Vincular retroativamente lançamentos antigos ao catálogo ou substituir seus retratos persistidos.
- Usar a cotação armazenada no catálogo para calcular o dashboard; valorização e evolução continuam com suas fontes correntes e históricas próprias.
- Alterar o fluxo de proventos manuais ou candidatos de dividendos.
- Automatizar atualização periódica de cotações em segundo plano.

## Decisions

### 1. Catálogo modelado como agregado privado do investidor

Será criada uma entidade `RegisteredAssetEntity` com UUID, `user_id`, ticker normalizado, nome, mercado, tipo, moeda, `last_quote`, `quoted_at`, `created_at` e `updated_at`. A restrição única será `(user_id, market, ticker)`, acompanhada de índice para listagem por proprietário e mercado.

O domínio, DTOs, mapper, repository, service e controller ficarão em um módulo de ativo cadastrado, mantendo autorização e regras de negócio no service. A alternativa de reutilizar o retrato embutido em lançamentos foi rejeitada porque ele só existe depois do primeiro lançamento, é duplicado por carteira e não representa um catálogo global.

### 2. Pesquisa passa a ser do investidor, não da carteira

O endpoint de pesquisa deixará de exigir `portfolioId`. O `AssetSelectionCache` continuará protegendo contra identidade livre enviada pelo cliente, mas sua entrada passará a conter somente `ownerId`, o retrato pesquisado e expiração. Essa seleção servirá exclusivamente ao cadastro persistente.

Rotas planejadas:

- `GET /api/assets/search?market=&assetType=&query=` pesquisa candidatos externos.
- `POST /api/assets` recebe apenas `assetSelectionId` e cadastra após cotação fresca.
- `GET /api/assets` lista o catálogo; `market` será filtro opcional.
- `POST /api/assets/{assetId}/quote-refresh` obtém e persiste uma nova cotação.
- `GET /api/assets/{assetId}/quote` obtém uma cotação fresca para preparar lançamento, sem atualizar o catálogo.

A alternativa de aceitar ticker, nome, tipo e moeda diretamente no `POST` foi rejeitada por reabrir a inserção livre que a mudança pretende eliminar.

### 3. Serviço explícito de cotação fresca

Será extraída uma operação de cotação que resolve a estratégia por mercado e chama o provedor sem consultar primeiro o cache de cotação. Cadastro, atualização explícita e seleção para lançamento devem representar uma nova consulta externa, conforme os requisitos. Depois de uma resposta válida, o serviço pode alimentar o cache compartilhado para beneficiar leituras subsequentes, mas não pode usar um valor já armazenado para evitar essa chamada obrigatória.

No cadastro e em `quote-refresh`, a cotação e o instante do `Clock` serão persistidos na mesma transação somente após resposta positiva e moeda compatível. No endpoint de preparação do lançamento, a resposta será transitória e não modificará `last_quote` nem `quoted_at`.

A alternativa de reutilizar diretamente `AssetSearchService.findQuote`, que prioriza cache, foi rejeitada porque não garante a nova consulta pedida nos dois momentos críticos.

### 4. Lançamento recebe o UUID do catálogo e mantém snapshot

`PortfolioTransactionCreateRequest` substituirá `assetSelectionId` por `registeredAssetId`. O service validará primeiro a carteira do investidor e depois buscará o ativo pelo par identificador/proprietário. O lançamento continuará copiando ticker, nome, mercado, tipo e moeda para sua própria entidade; não será criada uma chave estrangeira obrigatória entre lançamento e catálogo.

Isso mantém histórico antigo e novo independente de futuras mudanças no catálogo. A alternativa de tornar o lançamento dependente da entidade cadastrada foi rejeitada por acoplar a leitura histórica à vida útil do catálogo e exigir backfill dos registros existentes.

O preço unitário continuará sendo enviado no lançamento e poderá ser ajustado pelo usuário, pois a cotação corrente é uma sugestão de mercado e pode não coincidir com o preço real executado. A identidade do ativo, ao contrário, nunca será editável pelo cliente.

### 5. Concorrência protegida no banco e traduzida no serviço

O service fará uma verificação amigável de duplicidade antes de salvar, mas a garantia definitiva será a restrição única da migration. Uma violação concorrente será traduzida para `409 ASSET_ALREADY_REGISTERED`, sem expor SQL. Atualizações usarão a entidade pertencente ao usuário e commit atômico; falhas externas ocorrerão antes de qualquer mutação persistida.

### 6. Página React baseada no padrão de corretoras

Será criada a rota protegida `/app/ativos`, carregada sob demanda e adicionada ao menu de investidor. A página reutilizará `PageHeader`, `Card`, `Dialog`, `Button`, `Badge`, `Skeleton`, estados vazios/erro, `AssetLogo` e o sistema atual de notificações. Os filtros Todos, Brasileiros e Americanos serão controles acessíveis e responsivos; cada cartão mostrará identidade, mercado/tipo, moeda, cotação e data/hora local, além de `Atualizar cotação` com carregamento isolado.

O diálogo de cadastro fará pesquisa remota com mercado, tipo e termo, permitirá selecionar exatamente um resultado e então enviará sua referência temporária. Não haverá campo capaz de persistir ticker manualmente.

No diálogo de lançamento, a pesquisa externa será removida. A abertura carregará o catálogo; a seleção chamará a cotação transitória e preencherá o preço. Catálogo vazio mostrará uma chamada para `/app/ativos`. Trocar o ativo cancelará visualmente o preço anterior e exigirá que a nova cotação termine, evitando usar dados residuais.

### 7. Migration aditiva e compatibilidade

Uma migration Flyway `V2` criará a tabela e restrições sem alterar linhas existentes de lançamentos. Os mecanismos atuais que inicializam H2 e validam PostgreSQL serão atualizados para incluir a nova estrutura. Como a alteração do request é incompatível apenas para clientes do endpoint de criação, backend e frontend devem ser entregues juntos.

### 8. Exclusão protegida pelas posições consolidadas

O catálogo disponibilizará `DELETE /api/assets/{assetId}` e `DELETE /api/assets`. Antes de excluir, o service verificará as posições atuais de todas as carteiras do investidor pelo par mercado/ticker. Um saldo positivo produzirá `409 REGISTERED_ASSET_HAS_POSITION`; a exclusão total será atômica e não removerá nenhum item quando qualquer ativo estiver protegido. Lançamentos históricos não serão apagados porque mantêm seu próprio retrato e não possuem chave estrangeira para o catálogo.

### 9. Apresentação consistente de marca, logos e moeda

A marca no menu será um link para `/app/carteiras`. O catálogo converterá visualmente cotações `USD` para `BRL` usando o serviço de câmbio existente, preservando no backend a cotação nativa armazenada. `AssetLogo` será reutilizado tanto nos cartões quanto na seleção do lançamento, e os blocos de atribuição Parqet serão removidos da aplicação conforme a decisão de produto.

## Risks / Trade-offs

- [Mais chamadas externas ao cadastrar e selecionar] → manter busca e demais leituras em cache, mas cumprir a consulta fresca somente nos pontos exigidos e apresentar falhas recuperáveis.
- [Cotação muda entre seleção e envio] → tratar a cotação como preenchimento assistido e preservar o preço confirmado pelo usuário como preço real do lançamento.
- [Cadastro duplicado em corrida] → restrição única no banco e tradução determinística da violação.
- [Mudança incompatível no request de lançamento] → atualizar API, frontend, testes e documentação na mesma change; preservar respostas e dados históricos.
- [Falha do provedor durante atualização] → não alterar a entidade antes da resposta validada e manter último valor/data conhecidos.
- [Datas exibidas em fuso diferente] → persistir instante em UTC e formatar no frontend segundo o locale do usuário.
- [Exclusão de ativo ainda custodiado] → consolidar posições em todas as carteiras e bloquear individualmente ou toda a operação com conflito público.

## Migration Plan

1. Adicionar a migration `V2` e validar criação em bancos H2 e PostgreSQL vazios e já migrados.
2. Disponibilizar o catálogo e a pesquisa global mantendo temporariamente os componentes internos necessários à leitura dos lançamentos existentes.
3. Alterar o contrato de criação de lançamento e o frontend na mesma entrega.
4. Executar testes de integração, concorrência, gates H2/PostgreSQL e suíte do frontend antes do merge.
5. Em rollback de código, remover a funcionalidade sem apagar a tabela; a migration aditiva e os dados nela contidos podem permanecer inativos. Uma remoção física exigiria migration posterior explícita.
