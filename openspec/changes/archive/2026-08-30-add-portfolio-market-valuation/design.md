## Context

As posições já são calculadas dinamicamente a partir dos lançamentos efetivados, com validação de propriedade da carteira e reconciliação de pendências. A busca de ativos já possui estratégias por mercado, uma representação de cotação e cache temporário compartilhado. Esta mudança acrescenta uma projeção de leitura sobre essas duas capacidades; consulte `proposal.md` para a motivação e `specs/portfolio-market-valuation/spec.md` para o contrato externo.

## Goals / Non-Goals

**Goals:**

- Expor uma única leitura privada que entregue posições valorizadas e resumos por moeda.
- Reutilizar a lógica existente de posições, seleção de provedor e cache de cotação.
- Preservar precisão decimal durante os cálculos e centralizar falhas no tratamento de erros atual.
- Manter o mesmo comportamento funcional em H2 e PostgreSQL sem nova tabela ou migração.

**Non-Goals:**

- Converter BRL e USD, apresentar patrimônio consolidado ou armazenar taxa de câmbio.
- Calcular dividendos, impostos, ganho realizado, evolução histórica ou gráficos.
- Persistir a cotação ou transformar a consulta em uma atualização dos lançamentos.
- Alterar o contrato existente de posições, busca ou lançamentos.

## Decisions

### Novo serviço de valorização como composição de leituras existentes

Um `PortfolioMarketValuationService` receberá a carteira já autorizada e suas posições abertas do serviço de posições. Para cada posição, ele resolverá a estratégia do respectivo mercado e obterá a cotação pelo cache compartilhado antes de consultar o provedor. Um domínio de cálculo puro receberá posições e cotações e produzirá posições valorizadas e resumos por moeda.

Essa composição evita duplicar a reconciliação e a regra de preço médio. A alternativa de recalcular lançamentos novamente no serviço de valorização foi descartada porque criaria duas implementações para a mesma posição.

### Novo endpoint e DTOs de leitura, sem entidade persistente

Será criado `GET /api/portfolios/{portfolioId}/valuation`, um controlador dedicado e DTOs de resposta para a consulta, posição valorizada e resumo por moeda. O endpoint não aceitará corpo ou filtros nesta primeira versão.

DTOs explícitos preservam a arquitetura em camadas e não expõem entidades ou detalhes de integração. Uma extensão do DTO do endpoint de posições foi descartada para não mudar seu contrato nem forçar consumidores que só precisam de custódia a chamar provedores externos.

### Cálculos por moeda e precisão

Para cada posição, o custo investido será o `custo em custódia` calculado pelas posições existentes; o valor de mercado será `quantidade × preço atual`; o ganho ou perda não realizado será a diferença entre ambos; e a rentabilidade será essa diferença dividida pelo custo investido, em porcentagem. O resumo somará exclusivamente posições da mesma moeda. A alocação será o valor de mercado da posição dividido pelo total de mercado daquela moeda, em porcentagem.

Os cálculos internos usarão `BigDecimal` com precisão suficiente; a definição da escala pública será centralizada nos mappers, sem arredondamento intermediário. Não haverá soma de moedas: conversão cambial é uma change posterior. A alternativa de converter usando uma taxa implícita do provedor foi descartada porque criaria uma fonte de taxa, data de referência e regras de arredondamento ainda não aprovadas.

### Política atômica de cotação

A consulta só produzirá resposta depois de cotar todas as posições abertas. Uma cotação ausente, inválida, negativa ou com ticker/moeda incompatíveis interromperá a resposta com `ASSET_QUOTE_UNAVAILABLE` e status `503`; erros de conectividade e limite continuarão usando os códigos públicos específicos de Brapi ou AlphaVantage.

Essa política é preferível a devolver totais incompletos, que poderiam parecer indicadores confiáveis. O trade-off é indisponibilidade temporária da tela de valorização quando um único ativo falhar; o cache de cotações reduz essa ocorrência e a interface poderá tentar novamente depois.

### Segurança e reconciliação antes de integrações externas

A verificação de posse e a reconciliação de pendências ocorrerão antes de qualquer chamada a provedores. Carteira ausente/de outro investidor, token inválido e acesso de administrador continuarão usando os comportamentos de erro existentes.

Assim, o endpoint não revela a existência de carteiras nem consome a cota dos provedores para requisições não autorizadas.

## Risks / Trade-offs

- [Um provedor limita ou interrompe a cotação de um ativo] → Reutilizar o cache existente e devolver erro `503` explícito, sem inventar dados ou totais parciais.
- [Preço retornado não corresponde à posição registrada] → Validar ticker, moeda e valor positivo antes do cálculo; interromper a consulta se forem inconsistentes.
- [Posições de moedas distintas serem somadas por engano] → Modelar resumos por moeda e não disponibilizar campo de total consolidado nesta versão.
- [Divisões gerarem casas decimais recorrentes] → Manter precisão durante o cálculo e aplicar a escala pública apenas na resposta.

## Migration Plan

Não há mudança de esquema nem dados persistidos. A implantação adiciona um endpoint de leitura independente; a reversão consiste em removê-lo, sem migração ou limpeza de dados.

## Open Questions

Nenhuma. A conversão cambial foi deliberadamente adiada para uma change posterior, na qual serão definidos fonte, taxa, momento de referência e arredondamentos.
