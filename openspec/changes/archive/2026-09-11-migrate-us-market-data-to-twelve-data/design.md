## Context

Veja `proposal.md` para a motivação. Hoje uma mesma integração Alpha Vantage implementa três responsabilidades `US`: busca/cotação, série histórica e dividendos. As interfaces Strategy já separam essas responsabilidades, mas propriedades, cliente HTTP, códigos de erro, documentação e testes ainda tratam a Alpha Vantage como provedora geral. A Twelve Data possui formatos e limites próprios e seu plano gratuito combina 800 créditos diários com apenas 8 créditos por minuto.

## Goals / Non-Goals

**Goals:**

- Trocar somente as estratégias `US` de identificação, cotação e série histórica pela Twelve Data.
- Preservar contratos REST, caches, cálculos financeiros e seleção temporária de ativos.
- Manter a Alpha Vantage isolada e funcional para dividendos `US`.
- Distinguir com segurança credenciais, falhas e limites dos dois provedores.

**Non-Goals:**

- Migrar dividendos para a Twelve Data ou alterar datas e regras de elegibilidade de proventos.
- Substituir Brapi, Brasil API/PTAX ou provedores de logotipos.
- Adicionar streaming, WebSocket, fundamentos, indicadores técnicos ou persistência de dados de mercado.
- Implementar fallback de cotação ou histórico para a Alpha Vantage.

## Decisions

### 1. Substituir as implementações `US` mantendo as interfaces Strategy

Serão criadas estratégias Twelve Data para `AssetSearchStrategy` e `HistoricalAssetPriceStrategy`. As implementações Alpha Vantage dessas duas interfaces deixarão de ser componentes ativos, evitando duas estratégias para o mesmo mercado. `AlphaVantageIncomeEventProviderStrategy` e seu cliente permanecerão inalterados no fluxo de proventos.

Alternativa considerada: manter Alpha Vantage como fallback. Rejeitada porque aumentaria ambiguidade operacional, manteria consumo não controlado da cota antiga e contrariaria a separação aprovada entre provedores.

### 2. Usar endpoints simples e preservar o cache existente

A identificação usará `/symbol_search`, filtrando país `US`, moeda `USD` e os tipos compatíveis com `STOCK` ou `ETF`. Como o provedor pode devolver o mesmo símbolo em mais de uma listagem, os candidatos serão normalizados em maiúsculas e deduplicados por ticker, preservando a primeira ocorrência. A cotação usará `/price` para um ticker por chamada. O serviço continuará limitando resultados e reutilizando seus caches de candidatos e cotações.

Alternativa considerada: alterar a interface para cotação em lote. Embora reduza conexões HTTP, ela amplia a mudança em todos os provedores e não reduz os créditos por símbolo. Pode ser tratada futuramente se medições demonstrarem necessidade.

### 3. Consultar séries por intervalo com preços não ajustados

O histórico usará `/time_series` com `interval=1day`, `start_date`, `end_date` e `adjust=none`. Serão aceitos apenas pontos com data e fechamento positivo dentro do intervalo solicitado. Isso mantém a regra consolidada que reproduz quantidades exclusivamente pelo log e exclui desdobramentos externos do cálculo.

Alternativa considerada: aceitar o ajuste padrão por splits. Rejeitada porque misturaria preços ajustados com quantidades históricas não ajustadas e mudaria silenciosamente a evolução patrimonial.

### 4. Traduzir falhas Twelve Data para códigos públicos próprios

HTTP `429`, respostas de erro por limite de créditos e indicadores equivalentes serão traduzidos para `TWELVE_DATA_RATE_LIMITED`; credencial ausente/inválida, plano sem acesso, timeout, transporte e respostas inválidas usarão `TWELVE_DATA_PROVIDER_UNAVAILABLE`. O tratamento centralizado continuará devolvendo JSON `503` sem corpo bruto, URL ou chave.

Alternativa considerada: reutilizar códigos `ALPHAVANTAGE_*`. Rejeitada porque prejudicaria diagnóstico e tornaria o contrato incompatível com a origem real da falha.

### 5. Separar configuração e ciclo de vida dos clientes

Serão adicionadas `TWELVE_DATA_BASE_URL` e `TWELVE_DATA_API_KEY`. O cliente Twelve Data terá os mesmos timeouts configuráveis usados pelas integrações existentes. `ALPHA_VANTAGE_BASE_URL` e `ALPHA_VANTAGE_API_KEY` permanecerão porque ainda são necessários para dividendos. As chaves continuarão opcionais na inicialização e obrigatórias apenas quando o respectivo fluxo for acionado.

### 6. Preservar o contrato externo e atualizar as fontes de verdade

Controllers, DTOs, URLs da API e cálculos não mudarão. As especificações consolidadas e `docs/product-spec.md` passarão a nomear Twelve Data para dados de mercado americanos e Alpha Vantage para dividendos americanos. Documentação de configuração explicará limites e finalidade de cada chave.

## Risks / Trade-offs

- [O plano Basic possui apenas 8 créditos por minuto] → preservar caches, limitar resultados a cinco, reconhecer `429` e orientar nova tentativa; batching fica como otimização futura baseada em medição.
- [A busca pode listar instrumentos sem acesso no plano atual] → filtrar metadados e omitir candidatos cuja cotação não possa ser obtida, distinguindo ausência individual de falha global.
- [Diferenças de nomenclatura de tipos podem classificar ativos incorretamente] → mapear explicitamente `Common Stock` e `ETF` e cobrir variações conhecidas com testes de contrato.
- [Preços Twelve Data podem divergir da Alpha Vantage por fonte e horário] → manter precisão e moeda, documentar a origem e validar apenas consistência contratual, não igualdade numérica entre provedores.
- [Uso público pode exigir plano e atribuição adequados] → documentar que a credencial e a licença devem corresponder ao ambiente; não expor chave nem prometer redistribuição com plano individual.
- [Remoção parcial da Alpha Vantage pode quebrar dividendos] → manter cliente, propriedades e testes específicos de `AlphaVantageIncomeEventProviderStrategy` e adicionar teste de separação entre fluxos.

## Migration Plan

1. Adicionar propriedades e cliente Twelve Data sem remover a configuração Alpha Vantage.
2. Implementar e testar as novas estratégias em isolamento.
3. Trocar os componentes `US` de busca/cotação e histórico, mantendo dividendos na Alpha Vantage.
4. Atualizar erros, configuração, documentação e requisitos globais.
5. Executar testes unitários, integrações H2/PostgreSQL e validações de release com respostas externas simuladas.
6. Configurar `TWELVE_DATA_API_KEY` no ambiente antes da implantação; em rollback, reverter o código e restaurar as estratégias Alpha anteriores, sem migração de dados.
