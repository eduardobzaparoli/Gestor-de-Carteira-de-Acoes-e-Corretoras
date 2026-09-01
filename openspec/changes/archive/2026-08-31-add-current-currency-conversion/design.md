## Context

A valorização atual obtém as posições abertas, consulta a cotação de cada ativo e produz subtotais independentes por moeda. As integrações externas usam estratégias, `RestClient`, propriedades configuráveis e caches em memória. A proposta define a consolidação pontual do patrimônio em BRL; os requisitos comportamentais estão no delta de `portfolio-market-valuation`.

## Goals / Non-Goals

**Goals:**

- Converter o valor de mercado atual de posições em USD para BRL com uma fonte pública e auditável.
- Manter intactos o cálculo de posições, os subtotais por moeda e a resposta existente para seus consumidores.
- Isolar a fonte cambial atrás de uma interface, com timeout, cache e tratamento centralizado de falhas.

**Non-Goals:**

- Converter custos de compras ou vendas pela taxa histórica de cada lançamento.
- Calcular valor investido, ganho, perda ou rentabilidade consolidados em BRL.
- Persistir cotações ou taxas cambiais, adicionar migrações de banco ou alterar lançamentos já registrados.
- Suportar moedas estrangeiras além de USD nesta etapa.

## Decisions

### Consolidar somente o patrimônio de mercado atual

O cálculo somará valores de mercado em BRL diretamente e multiplicará valores de mercado em USD pela taxa USD/BRL atual escolhida. Valor investido e retorno permanecerão somente nos resumos nativos por moeda, pois usar a taxa atual para custos históricos esconderia ou distorceria o efeito cambial.

Alternativa considerada: converter também custo, ganho e rentabilidade com a taxa atual. Foi rejeitada porque não representa o montante originalmente investido em reais.

### Usar BrasilAPI e fechamento PTAX de compra

A integração consultará a BrasilAPI para obter a PTAX do dólar, cuja fonte é o Banco Central. O algoritmo selecionará a cotação de compra do boletim `FECHAMENTO PTAX` da data mais recente disponível; se a data consultada não tiver fechamento, procurará o último dia anterior com fechamento válido. A cotação de compra representa o valor em reais recebido ao converter o patrimônio em dólares para reais.

Alternativa considerada: cotação intradiária ou taxa de venda. A cotação intradiária aumenta a instabilidade do indicador; a taxa de venda representa o custo de adquirir dólares, não o valor de conversão do patrimônio atual.

### Criar uma estratégia cambial independente

Será introduzida uma interface de consulta cambial e uma implementação da BrasilAPI. O serviço de valorização dependerá da interface, sem conhecer HTTP, URLs ou o formato da resposta. A configuração seguirá o padrão existente: URL-base, timeouts e TTL de cache em `app.integrations`.

Alternativa considerada: acoplar a consulta à estratégia de busca de ativos. Foi rejeitada porque câmbio tem contrato, cache e falhas distintos das cotações de ativos.

### Cache por par de moedas e data de referência

O cache em memória identificará uma taxa por moeda de origem, moeda-base e data efetiva do fechamento PTAX. Isso impede repetir consultas para várias posições em USD e evita misturar taxas de dias diferentes. Não haverá persistência: uma nova execução da aplicação pode consultar novamente a fonte.

### Resposta atômica e extensão compatível

O endpoint existente será estendido com um objeto opcional de resumo consolidado. Quando existem posições abertas que exigem câmbio, a ausência de uma taxa válida falha toda a valorização com `EXCHANGE_RATE_UNAVAILABLE`, seguindo a política atual para cotações de ativos. Quando não há posições ou só há BRL, não haverá consulta cambial.

Alternativa considerada: devolver subtotais nativos com um aviso de que faltou o consolidado. Foi rejeitada para manter uma resposta de valorização sem indicadores parcialmente calculados.

## Risks / Trade-offs

- [A BrasilAPI ou o Banco Central pode não oferecer fechamento para a data atual] → procurar retroativamente o último fechamento válido e expor a data usada.
- [Uma indisponibilidade cambial impede uma resposta que antes só dependia de cotações de ativos] → expor o código público específico e reutilizar cache temporário.
- [A taxa PTAX não equivale necessariamente à taxa aplicada pela corretora] → identificar a fonte, a taxa e a data na resposta; taxas de corretagem e câmbio próprio ficam fora desta etapa.
- [O cache é perdido ao reiniciar a aplicação] → aceitar a nova consulta por ser uma primeira etapa sem persistência; reavaliar persistência na mudança de desempenho histórico.

## Migration Plan

1. Adicionar a integração, o cache e os cálculos sem alterar o esquema do banco.
2. Estender o DTO de valorização de modo aditivo.
3. Validar com testes unitários e de integração usando respostas cambiais simuladas em H2 e PostgreSQL.
4. Em caso de rollback, remover o resumo consolidado e a integração; como não há dados persistidos, não há migração reversa.
