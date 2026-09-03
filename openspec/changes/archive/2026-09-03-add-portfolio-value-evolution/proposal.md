## Why

O dashboard precisa comparar valor investido e patrimônio ao longo do tempo, mas hoje a API calcula somente a fotografia atual da carteira. A mudança acrescenta uma série histórica coerente com os lançamentos, preços e câmbio de cada data, sem transformar vendas ou proventos em saldo de caixa inexistente no domínio atual.

## What Changes

- Expor uma evolução diária dos últimos 90 dias para carteira privada do investidor, consolidada em `BRL`.
- Reproduzir em cada data somente lançamentos `EFFECTIVE`, calculando o valor investido pelo custo histórico das posições ainda em custódia.
- Consultar fechamentos históricos não ajustados dos ativos `BR` e `US`, com Strategy por mercado e cache temporário das séries.
- Converter diariamente posições em `USD` pela PTAX histórica aplicável, usando o último fechamento anterior quando necessário.
- Reutilizar o último preço disponível quando apenas um dos mercados estiver fechado e incluir um ponto atual com as cotações correntes.
- Manter a resposta atômica: preço ou câmbio obrigatório ausente resulta em erro público, sem série parcial.
- Excluir do cálculo lançamentos pendentes ou cancelados, proventos, recursos obtidos em vendas e eventos que alterem quantidade.
- Não persistir preços, câmbio, posições, pontos do gráfico nem saldo de caixa.

## Capabilities

### New Capabilities

- `portfolio-value-evolution`: Série histórica privada de valor investido e patrimônio em BRL, com reprodução da custódia, preços e câmbio por data.

### Modified Capabilities

- Nenhuma.

## Impact

- Novo contrato REST no contexto privado da carteira para consultar a evolução patrimonial.
- Novos domínios, DTOs, mapper, controller e serviços para calcular e representar a série.
- Novas estratégias de preços históricos para Brapi e Alpha Vantage, com propriedades e cache próprios, sem nova dependência.
- Reutilização do histórico de lançamentos, das regras de custo médio, das cotações atuais e do serviço cambial histórico.
- Ampliação dos testes unitários, integrados H2 e PostgreSQL opt-in, incluindo limites e falhas das integrações externas.
