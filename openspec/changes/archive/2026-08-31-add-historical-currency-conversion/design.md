## Context

A consulta de valorização já oferece subtotais nativos por moeda e, pela change de conversão corrente, passa a expor o patrimônio de mercado em BRL com a PTAX atual. Essa taxa atual não pode ser usada para medir o custo de compras históricas em USD. Veja `proposal.md` para a motivação e a especificação delta para o contrato observável.

## Goals / Non-Goals

**Goals:**

- Reprocessar os lançamentos efetivados em ordem cronológica para manter, por posição, o custo histórico em sua moeda nativa e em BRL.
- Usar a PTAX de compra de fechamento aplicável à data de cada compra em USD, com busca do último fechamento anterior.
- Calcular os indicadores consolidados sem alterar os subtotais nativos, a cotação atual ou a regra de preço médio já existente.
- Permitir auditoria das taxas históricas usadas na resposta.

**Non-Goals:**

- Converter receitas de venda para determinar custo, realizar cálculo de imposto ou reconhecer lucro realizado separadamente.
- Incluir dividendos, taxas próprias de corretora, outras moedas, conversão intradiária ou persistência de taxas.
- Alterar lançamentos históricos, seu estado ou o esquema do banco.

## Decisions

### Acumular custo histórico em BRL durante o replay cronológico

O cálculo de posições será estendido com um acumulador de custo por posição na moeda-base. Cada compra efetivada em BRL adiciona diretamente seu custo; cada compra efetivada em USD consulta a taxa histórica e adiciona o custo convertido. A venda desconta quantidade e custo proporcional pelo custo médio histórico em BRL acumulado para aquela posição. Dessa forma, uma venda não faz a receita recebida se passar por valor investido.

Alternativa considerada: converter o custo em USD pelo câmbio atual ao consultar a valorização. Foi rejeitada porque substituiria o investimento histórico pelo valor presente da moeda.

### Manter cotação de patrimônio e taxa histórica como responsabilidades distintas

O patrimônio de mercado em USD continuará sendo convertido pela taxa atual já fornecida pela change anterior. A nova consulta histórica usa a mesma abstração cambial, mas solicita uma data de referência igual à data da compra e preserva a data do fechamento realmente selecionado. As duas taxas são devolvidas de forma identificável no resumo consolidado.

Alternativa considerada: usar somente uma taxa na resposta. Foi rejeitada porque custo histórico e patrimônio atual representam momentos distintos e precisam ser auditáveis.

### Cachear por par de moedas e data efetiva

O cache existente será reutilizado ou estendido para identificar uma taxa por moeda de origem, moeda-base e data de referência. O serviço resolve o último fechamento válido antes de guardar a taxa sob a data efetivamente usada, evitando consultas duplicadas no mesmo replay.

Alternativa considerada: cachear apenas por par de moedas. Foi rejeitada porque poderia reutilizar uma taxa de data incorreta para uma compra histórica.

### Resposta atômica

Caso falte qualquer cotação atual ou taxa histórica necessária, o endpoint falha integralmente com o código público cambial já definido. Isso mantém a resposta coerente e evita apresentar retorno consolidado calculado com somente parte das compras.

## Risks / Trade-offs

- [Uma carteira com muitas compras em datas diferentes demanda várias taxas históricas] → usar cache por data e testar o limite do provedor com respostas simuladas.
- [A PTAX pode não existir em fins de semana ou feriados] → selecionar o fechamento válido mais recente anterior e informar sua data.
- [O replay aumenta a complexidade do cálculo de posições] → manter o novo acumulador no domínio de cálculo, com testes cronológicos de compras e vendas.
- [Uma indisponibilidade histórica impede a valorização] → manter resposta `503` atômica e reutilizar taxas temporárias válidas.

## Migration Plan

1. Estender os modelos de cálculo e o contrato da taxa cambial, sem migração de banco.
2. Integrar o replay de custo histórico ao serviço de valorização e adicionar os campos ao resumo consolidado de forma aditiva.
3. Cobrir cálculos, cache, indisponibilidade, autorização e compatibilidade em H2 e PostgreSQL.
4. Em rollback, remover os campos consolidados históricos e o replay adicional; não haverá dados persistidos a reverter.
