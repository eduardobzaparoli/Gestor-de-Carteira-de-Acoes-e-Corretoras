## Context

As carteiras já são privadas, acessadas por UUID e protegidas pelo papel `INVESTOR`. A busca de ativos atende à jornada dentro da carteira, mas ainda não há entidade de ativo, lançamento, posição ou dashboard. As integrações cadastrais existentes já usam interfaces de estratégia, propriedades centralizadas, `RestClient`, timeouts e tratamento de falhas padronizado.

## Goals / Non-Goals

**Goals:**

- Consultar ações e ETFs brasileiros ou americanos com preço no retorno, sem persistir o resultado.
- Reutilizar a verificação de propriedade da carteira antes de qualquer chamada externa.
- Isolar Brapi e AlphaVantage para que o serviço de busca não dependa de protocolos ou DTOs de fornecedor.
- Respeitar limites externos com cache local e respostas de erro previsíveis.

**Non-Goals:**

- Persistir ativos, preços, favoritos, posições ou histórico de cotações.
- Criar lançamentos, calcular saldo, preço médio, rentabilidade ou indicadores de dashboard.
- Exibir ao cliente o horário, a modalidade ou uma garantia de tempo real da cotação nesta versão.
- Aceitar uma categoria que misture ações e ETFs.

## Decisions

### 1. Endpoint de busca subordinado à carteira

Será exposto `GET /api/portfolios/{portfolioId}/assets` com `market`, `assetType` e `query` obrigatórios. O Controller extrai o investidor do JWT e o Service confirma a propriedade pelo mecanismo de carteiras antes de delegar a consulta.

Alternativa considerada: endpoint global de busca. Foi rejeitada porque a busca existe na tela da carteira e um caminho subordinado mantém a autorização e o fluxo de seleção claros.

### 2. Contrato normalizado e filtros exclusivos

O Domain terá valores de mercado `BR` e `US`, de tipo `STOCK` e `ETF`, e um resultado público contendo ticker, nome, mercado, tipo, moeda e preço. O Service elimina resultados que não possam ser classificados de forma segura ou que não tenham cotação disponível. O termo será removido de espaços nas extremidades, terá no mínimo dois caracteres e a resposta será limitada a cinco resultados cotados por busca.

Alternativa considerada: entregar a resposta bruta ou misturar tipos. Foi rejeitada para que a interface possa sempre respeitar a seleção explícita do investidor e permaneça independente dos formatos dos provedores.

### 3. Estratégias por mercado e composição de busca com cotação

Uma interface de busca definirá a operação normalizada por mercado. A estratégia brasileira usará Brapi; a americana usará AlphaVantage. Cada estratégia resolve candidatos e suas cotações conforme as capacidades do respectivo provedor, mas devolve apenas o contrato normalizado. Para o mercado americano, a estratégia pode precisar consultar a cotação por ticker após localizar candidatos.

Alternativa considerada: um cliente único com condicionais de provedor no Service. Foi rejeitada para preservar o padrão Strategy e permitir troca ou inclusão de provedores sem alterar as regras de carteira.

### 4. Cache local com durações separadas

Um componente em memória, sem nova dependência, manterá resultados de identificação por cinco minutos usando mercado, tipo e termo normalizado como chave. Cotações serão mantidas por um minuto usando mercado e ticker como chave. O cache será por processo, terá expiração preguiçosa e não será fonte de persistência ou histórico.

Alternativa considerada: cache único de cinco minutos para toda a resposta. Foi rejeitada porque permitiria que o preço exibido ficasse defasado por mais tempo do que o necessário.

### 5. Propriedades locais e falhas externas públicas

URLs, tokens e tempos de espera serão fornecidos por propriedades e variáveis de ambiente. Credenciais não serão versionadas. Falhas técnicas e limites externos serão convertidos em exceções específicas, mapeadas pelo `@ControllerAdvice` para `503`; entradas inválidas retornam `400`; uma carteira inacessível retorna o `404` já definido.

Alternativa considerada: repassar status e corpos externos. Foi rejeitada para não vazar dados de infraestrutura nem acoplar consumidores da API a fornecedores.

## Risks / Trade-offs

- [A AlphaVantage pode exigir uma consulta adicional de cotação para cada candidato] → limitar resultados cotados, usar cache de um minuto e testar limites e falhas.
- [A cotação mais recente disponível pode não representar tempo real em todos os planos ou mercados] → retornar o último preço disponibilizado pelo provedor, sem prometer modalidade de mercado nesta versão.
- [Cache local se perde em reinicialização e não é compartilhado entre instâncias] → aceitável para a primeira versão; uma solução distribuída só será considerada quando houver necessidade operacional.
- [Classificação de tipo pode ser incompleta] → excluir candidatos sem classificação confirmada em vez de apresentá-los na categoria errada.

## Migration Plan

1. Adicionar configurações de desenvolvimento e testes sem incluir tokens em arquivos versionados.
2. Implantar a busca sem migração de banco, pois a capacidade não persiste ativos ou cotações.
3. Em rollback, remover os componentes e a rota; nenhum dado de busca precisa ser migrado ou removido.
