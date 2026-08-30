## Why

O histórico de lançamentos já registra compras e vendas, mas ainda não apresenta ao investidor a quantidade atualmente mantida nem o custo médio de aquisição de cada ativo. Esta mudança transforma os lançamentos efetivados em posições financeiras confiáveis e cria a base determinística para os futuros indicadores do dashboard.

## What Changes

- Adiciona uma consulta privada das posições atuais de uma carteira, agrupadas por ticker e mercado.
- Calcula dinamicamente quantidade, custo em custódia e preço médio ponderado a partir dos lançamentos `EFFECTIVE` em ordem cronológica.
- Inclui custos de compra no preço médio; vendas reduzem quantidade e custo em custódia proporcionalmente, sem alterar o preço médio.
- Omite posições encerradas e reinicia o preço médio quando uma nova compra ocorre após a liquidação total.
- Rejeita lançamentos retroativos de venda que deixariam a posição negativa em qualquer ponto do histórico recalculado.
- Não inclui cotação atual, patrimônio, rentabilidade, ganho realizado, dividendos ou gráficos.

## Capabilities

### New Capabilities

- `portfolio-positions`: consulta privada e cálculo dinâmico de quantidade, custo em custódia e preço médio por ativo da carteira.

### Modified Capabilities

- `portfolio-transactions`: a validação de venda passa a preservar saldo não negativo também na sequência cronológica quando uma operação retroativa é inserida.

## Impact

- Nova rota REST privada de posições no contexto da carteira e novos tipos de Domain, DTO, Mapper, Service e Controller.
- Reutilização do repositório e dos lançamentos existentes como fonte de verdade, sem nova tabela ou migração.
- Ajuste no serviço de lançamentos para validar vendas retroativas por ordem de data da transação e criação.
- Testes de cálculo, acesso privado, precisão decimal, liquidação/reabertura e compatibilidade com H2 e PostgreSQL.
