## Context

Os lançamentos de carteira são a fonte persistente de compras, vendas, datas, custos e status. A nova consulta precisa derivar posições sem duplicar dados e, ao mesmo tempo, corrigir a lacuna de consistência causada por vendas inseridas retroativamente. Consulte `proposal.md` e as especificações da change para o comportamento aprovado.

## Goals / Non-Goals

**Goals:**

- Isolar o cálculo financeiro em domínio puro e testável, independente de HTTP e persistência.
- Reutilizar a reconciliação de pendências e a verificação de propriedade da carteira.
- Produzir resultado determinístico para H2 e PostgreSQL com precisão decimal controlada.
- Garantir que uma nova venda não torne negativa nenhuma etapa cronológica do histórico.

**Non-Goals:**

- Persistir projeções de posição ou criar migração de banco.
- Consultar Brapi ou AlphaVantage durante o cálculo.
- Calcular cotação atual, patrimônio, rentabilidade ou resultado realizado.

## Decisions

### Projeção dinâmica em domínio próprio

Será criado um modelo de posição e um calculador puro que receberá lançamentos efetivados ordenados. O serviço de posições coordenará propriedade, reconciliação, leitura e mapeamento público. A alternativa de armazenar saldo e preço médio na entidade de carteira foi descartada por duplicar a fonte de verdade e exigir sincronização em toda alteração de histórico.

### Ordem financeira por data da transação

O repositório fornecerá lançamentos em ordem ascendente de `transactionDate` e `createdAt`. Essa ordem será usada tanto na projeção quanto na simulação de vendas retroativas. A ordem de inserção isolada foi descartada porque produziria preço médio incorreto para lançamentos passados cadastrados posteriormente.

### Acumulador por ticker e mercado

Cada grupo manterá quantidade, custo em custódia, preço médio e o retrato público mais recente. Compras somam `quantidade × preço + custos`; vendas removem `quantidade × preço médio corrente`. Quando a quantidade chega a zero, quantidade e custo são forçados a zero para eliminar resíduos decimais; uma compra posterior inicia novo ciclo.

### Precisão decimal em duas etapas

O cálculo interno usará `BigDecimal` com contexto de alta precisão e nenhuma conversão para ponto flutuante. Divisões internas usarão precisão suficiente para operações sucessivas, enquanto o DTO arredondará `averagePrice` para até oito casas com `HALF_UP`. Arredondar cada lançamento para duas casas foi descartado porque acumularia desvios em ativos fracionários.

### Validação de venda por simulação

Antes de persistir uma venda efetiva, o serviço criará uma representação candidata, combinará com os lançamentos efetivos do mesmo ticker e mercado e percorrerá a sequência cronológica. Qualquer quantidade intermediária negativa causará `INSUFFICIENT_ASSET_QUANTITY`. A verificação atual de saldo disponível e reservas pendentes será preservada para vendas futuras e concorrentes.

### Sem mapper persistente

Como a posição não será uma entidade, o mapper converterá domínio calculado diretamente em DTO. O controlador seguirá o padrão privado existente em `/api/portfolios/{portfolioId}/positions`, derivando o proprietário do JWT.

## Risks / Trade-offs

- [Históricos extensos exigem recálculo completo] → Consultar somente lançamentos da carteira na ordem necessária; projeção persistida poderá ser adicionada futuramente sem alterar o contrato.
- [Divisões recorrentes podem gerar resíduos] → Usar alta precisão internamente e zerar explicitamente uma posição liquidada.
- [Dados descritivos podem mudar entre lançamentos] → Expor o retrato do lançamento efetivado mais recente do grupo.
- [Regra retroativa amplia a validação de criação] → Cobrir sequências fora de ordem e garantir que rejeições não persistam parcialmente.

## Migration Plan

1. Adicionar o cálculo e a rota sem modificar o esquema do banco.
2. Ajustar a validação de vendas efetivas retroativas mantendo os códigos de erro existentes.
3. Executar testes H2, verificação opt-in PostgreSQL e contrato REST.
4. Em rollback, remover a rota e o cálculo; os lançamentos persistidos permanecem intactos.
