## Context

As carteiras, a autenticação e a busca de ativos já estão separadas em camadas e usam H2 e PostgreSQL. Esta mudança adiciona dados persistentes e regras de posição, mantendo a busca de ativos como fonte temporária de seleção e cotação. A motivação e o comportamento público estão definidos em `proposal.md` e nas especificações desta change.

## Goals / Non-Goals

**Goals:**

- Introduzir o módulo de lançamentos na arquitetura existente, com domínio, persistência, mapeamento, serviços, DTOs e controlador próprios.
- Aplicar o padrão State ao ciclo `PENDING` → `EFFECTIVE` / `CANCELLED` e tornar a efetivação devida determinística em cada interação com lançamentos.
- Calcular posição e reservas de venda de forma transacional, sem consultar provedores externos.
- Preservar o histórico bloqueando a exclusão de carteiras com lançamentos.

**Non-Goals:**

- Não calcular preço médio, rentabilidade, patrimônio, dividendos, impostos ou gráficos.
- Não persistir o catálogo ou a cotação retornada pela pesquisa de ativos.
- Não adicionar job agendado: a efetivação de lançamentos futuros é reconciliada na próxima consulta ou operação da carteira.
- Não permitir edição, exclusão física ou reversão de lançamentos efetivados.

## Decisions

### Módulo persistente de lançamentos por carteira

Será criado um agregado de lançamento associado a uma única carteira, contendo o retrato do ativo (`ticker`, nome, mercado, tipo e moeda), tipo da operação, data, quantidade, preço, custos e status. `BigDecimal` será usado para quantidade, preço e custos, permitindo quantidades fracionárias quando aplicáveis sem uma regra especial por mercado nesta primeira versão.

A alternativa de manter somente saldos agregados foi descartada porque não preservaria o log imutável necessário para o histórico e para os cálculos futuros.

### Ciclo de vida explícito com State

O domínio terá estados e transições explícitas: `PENDING` pode ser efetivado ou cancelado; `EFFECTIVE` e `CANCELLED` não aceitam novas transições. O status será persistido como enumeração, enquanto objetos/estratégias de estado concentrarão as regras de transição no domínio.

Um relógio injetável definirá a data atual. Antes de listar, criar ou cancelar lançamentos, o serviço executará uma reconciliação transacional dos pendentes cuja data já chegou. Isso implementa a efetivação na próxima interação sem exigir infraestrutura de agendamento. A alternativa de um job diário foi descartada por ampliar a operação do projeto e introduzir a necessidade de monitoramento.

### Saldo disponível e reserva de vendas

O serviço calculará a posição por carteira, ticker e mercado a partir dos lançamentos efetivados. Para criar uma venda, subtrairá também as vendas pendentes da posição efetiva; compras pendentes não serão consideradas como saldo negociável. A validação, a reserva e a persistência ocorrerão na mesma transação.

As consultas que sustentam a criação de venda usarão bloqueio apropriado da carteira e/ou dos lançamentos relevantes para reduzir a chance de duas requisições concorrentes consumirem o mesmo saldo. A alternativa de registrar qualquer venda e corrigir o saldo posteriormente foi descartada por violar o bloqueio de venda a descoberto.

### Fronteiras de API e representação pública

O controlador exporá criação e listagem em `/api/portfolios/{portfolioId}/transactions` e cancelamento em `/api/portfolios/{portfolioId}/transactions/{transactionId}`. DTOs receberão e devolverão apenas os campos públicos; o proprietário continuará sendo derivado do token e a carteira será validada pelo serviço existente.

O ativo será recebido como retrato derivado de uma seleção de busca já exibida ao investidor. Não haverá chamada à Brapi ou AlphaVantage durante o registro, evitando falhas e limites de provedor no momento de gravar o histórico.

### Preservação do histórico no fluxo de exclusão

O repositório de lançamentos fornecerá uma verificação eficiente da existência de registros por carteira. O serviço de carteira fará essa verificação antes da exclusão e retornará um erro de conflito padronizado quando houver histórico.

Exclusão em cascata foi descartada porque destruiria lançamentos imutáveis, e exclusão lógica da carteira foi adiada porque altera a semântica atual de exclusão além do necessário.

## Risks / Trade-offs

- [Lançamentos pendentes só são efetivados após uma interação] → A API deixa esse comportamento explícito e a reconciliação é executada antes de toda operação ou consulta de lançamentos.
- [Concorrência entre vendas] → Consultas e gravações críticas serão transacionais e bloqueadas no escopo da carteira/ativo.
- [Snapshot de ativo recebido do cliente] → O serviço validará os valores estruturais e manterá o retrato como histórico; a origem é a pesquisa previamente autenticada da mesma carteira.
- [Sem paginação no histórico inicial] → A primeira versão retorna a coleção ordenada; paginação poderá ser adicionada quando o volume justificar, sem alterar os registros existentes.

## Migration Plan

1. Adicionar a entidade e as restrições de lançamentos preservando a criação automática de esquema usada nos perfis H2 e PostgreSQL.
2. Implementar o módulo e a proteção de exclusão de carteira.
3. Executar testes unitários, de integração web e a verificação opt-in do PostgreSQL com banco local separado.
4. Em caso de rollback antes de qualquer lançamento, remover o código da feature; após dados reais, preservar a tabela e desativar somente as rotas para não perder histórico.
