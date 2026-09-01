## 1. Câmbio histórico e domínio de custo

- [x] 1.1 Estender o contrato e a estratégia cambial para consultar a PTAX de compra de fechamento aplicável a uma data histórica, incluindo a busca do último fechamento anterior.
- [x] 1.2 Reutilizar ou adequar o cache cambial para distinguir moeda de origem, moeda-base e data efetiva da taxa histórica.
- [x] 1.3 Criar os modelos de domínio necessários para manter custo histórico em BRL por posição e registrar as taxas históricas utilizadas.

## 2. Cálculo e valorização consolidados

- [x] 2.1 Estender o replay cronológico dos lançamentos efetivados para acumular o custo das compras em BRL e USD convertido pela taxa histórica, incluindo custos adicionais.
- [x] 2.2 Reduzir o custo histórico em BRL de cada posição pelo custo médio das unidades vendidas, sem usar a receita da venda no valor investido.
- [x] 2.3 Calcular valor investido, patrimônio de mercado, ganho ou perda e rentabilidade consolidados em BRL, mantendo os subtotais nativos e a cotação atual existentes.
- [x] 2.4 Estender DTOs e mapeadores de valorização com os indicadores e as taxas históricas auditáveis de forma aditiva.
- [x] 2.5 Preservar a resposta atômica com `EXCHANGE_RATE_UNAVAILABLE` quando faltar taxa histórica necessária, sem resultados parciais.

## 3. Testes e compatibilidade

- [x] 3.1 Testar seleção de PTAX histórica, último fechamento anterior, cache por data, taxa inválida e indisponibilidade da fonte.
- [x] 3.2 Testar compras e vendas cronológicas em BRL e USD, custos adicionais, redução por custo médio histórico e exclusão de lançamentos pendentes.
- [x] 3.3 Testar os indicadores consolidados, rastreabilidade das taxas, autorização, propriedade e resposta aditiva do endpoint em H2.
- [x] 3.4 Executar a suíte Maven completa e a verificação opt-in no perfil PostgreSQL, corrigindo regressões relacionadas à mudança.
