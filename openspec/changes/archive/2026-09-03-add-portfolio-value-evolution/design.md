## Context

A valorização atual já deriva posições, custo histórico em BRL, cotações correntes e conversão USD/BRL. O log de lançamentos é a fonte de verdade e não existe saldo de caixa nem persistência de posições. Esta mudança precisa reproduzir os mesmos conceitos em várias datas e consultar séries externas sem multiplicar chamadas de maneira descontrolada. Consulte `proposal.md` e `specs/portfolio-value-evolution/spec.md` para motivação e contrato.

## Goals / Non-Goals

**Goals:**

- Reproduzir custódia e custo histórico incrementalmente ao longo de uma janela fixa.
- Isolar séries históricas externas por Strategy e mercado.
- Consolidar todos os pontos em BRL com preços e câmbio coerentes com cada data.
- Reaproveitar os cálculos atuais sem criar duas interpretações para custo médio e valorização.
- Limitar chamadas externas e memória pela janela de 90 dias e por cache temporário.

**Non-Goals:**

- Manter saldo de caixa, fluxo de aportes, patrimônio líquido total ou retorno ponderado por tempo.
- Somar receitas de vendas ou proventos ao patrimônio.
- Ajustar automaticamente quantidades por desdobramentos, grupamentos ou bonificações.
- Oferecer períodos maiores, granularidade configurável ou benchmarks nesta primeira versão.
- Persistir snapshots, candles, câmbio ou pontos do gráfico.

## Decisions

### Criar um contrato privado próprio para a evolução

Um endpoint de leitura sob `/api/portfolios/{portfolioId}/value-evolution` devolverá a janela diária em ordem crescente. Ele ficará separado de `/valuation`, pois a fotografia atual e a série possuem dependências, custo e falhas diferentes.

Alternativa considerada: incluir os pontos na resposta da valorização atual. Rejeitada porque tornaria toda abertura do resumo dependente de várias séries históricas e ampliaria uma resposta hoje simples.

### Introduzir Strategy específica para preços históricos

Uma abstração de série histórica receberá ticker e intervalo e retornará fechamentos não ajustados datados. Um resolvedor escolherá Brapi para `BR` e Alpha Vantage para `US`. Os adaptadores compartilharão os `RestClient`, credenciais, timeouts e tradução pública de erros existentes, mas terão modelos normalizados e cache próprios.

Alternativa considerada: ampliar diretamente `AssetSearchStrategy`. Rejeitada porque busca, cotação atual e série histórica possuem contratos, volume e políticas de cache distintos.

### Usar fechamento não ajustado

O domínio multiplicará a quantidade realmente registrada no log pelo preço negociado no fechamento daquela data. Fechamento ajustado foi rejeitado porque altera retroativamente o preço sem ajustar a quantidade persistida e produziria valor patrimonial absoluto inconsistente. Eventos societários que alterem quantidade continuarão como limitação explícita até uma change própria.

### Reproduzir o estado cronologicamente uma única vez

O calculador receberá lançamentos efetivados ordenados, taxas históricas das compras, séries por ativo e taxas dos pontos. Ele avançará simultaneamente pelos eventos e pelas datas, mantendo quantidade e custo em BRL por ticker/mercado. Assim, compras aumentam custo, vendas retiram custo médio e liquidações zeram a posição sem recalcular todo o histórico para cada ponto.

A lógica comum de custo histórico deverá ser extraída ou reutilizada por composição para que valorização atual e evolução compartilhem as mesmas regras puras.

### Montar o calendário pela união dos pregões

As datas históricas serão a união ordenada das datas retornadas pelas séries necessárias dentro da janela. Quando um mercado estiver fechado e outro possuir pregão, cada ativo fechado usará seu último fechamento anterior disponível. Não haverá preenchimento anterior ao primeiro preço válido do ativo. A data atual será acrescentada e calculada com as cotações e o câmbio atuais quando existir custódia.

Alternativas consideradas: gerar todos os dias de calendário, rejeitada por duplicar fins de semana; usar somente a interseção dos mercados, rejeitada por omitir pregões válidos quando apenas uma bolsa abre.

### Resolver todos os insumos antes de expor pontos

O serviço identificará ativos que estiveram em custódia na janela, buscará todas as séries e taxas necessárias e somente então mapeará a resposta. Cobertura truncada, preço ausente durante custódia ou câmbio indisponível abortará a operação inteira. Isso preserva a política atômica dos demais indicadores financeiros.

### Cachear séries normalizadas sem persistência

O cache em memória usará mercado, ticker, início e fim como chave, com TTL configurável. Chamadas Brapi poderão agrupar tickers quando o contrato permitir; Alpha Vantage continuará por ticker. A janela de 90 dias cabe na saída compacta diária disponível para Alpha Vantage, mas respostas de limite ou plano serão tratadas como indisponibilidade explícita.

## Risks / Trade-offs

- [Limites de plano podem reduzir silenciosamente a série da Brapi] → validar cobertura necessária antes do cálculo e falhar explicitamente sem truncar o gráfico.
- [Alpha Vantage limita chamadas por ticker] → consultar apenas ativos que tiveram custódia na janela e reutilizar cache.
- [Uma carteira mista demanda muitas PTAX diárias] → deduplicar taxas por data e reutilizar o cache cambial existente; considerar integração em lote somente em mudança futura.
- [Calendários de BR e US divergem] → usar união de pregões e último fechamento conhecido de cada ativo.
- [Ticker renomeado ou evento societário pode distorcer a série] → manter a limitação explícita e não inventar ajustes sem evento de quantidade no domínio.
- [Venda faz o gráfico cair mesmo após lucro realizado] → documentar que a série mede custódia e não patrimônio líquido com caixa.
- [Cálculo pode crescer com muitos ativos] → limitar a 90 dias, processar eventos incrementalmente e buscar uma série por ativo.

## Migration Plan

1. Adicionar domínio, estratégias e cache de séries históricas sem alterar contratos existentes.
2. Introduzir o calculador puro e compartilhar as regras de custo histórico com a valorização atual.
3. Expor o endpoint privado e validar erros públicos com integrações simuladas.
4. Executar testes H2, PostgreSQL opt-in e suíte completa antes da liberação.

O rollback remove o novo endpoint e seus componentes. Como não há nova tabela nem alteração dos dados existentes, não é necessária migração destrutiva.
