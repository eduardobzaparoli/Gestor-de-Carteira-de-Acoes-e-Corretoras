## Context

Veja `proposal.md` para a motivação. Hoje `PortfolioValuationCalculator` divide cada posição pelo total de sua própria moeda, enquanto `PortfolioPage` reúne todas as posições no mesmo gráfico. Assim, cada grupo monetário fecha em 100% de forma independente. Na descoberta de proventos, `PortfolioIncomeEventService` chama a estratégia uma vez para cada ticker histórico; `IncomeEventCandidateCache` guarda apenas a referência pronta para confirmação e não evita novas chamadas ao provedor. Uma exceção em qualquer ticker encerra toda a consulta.

A Brapi mantém autenticação Bearer, mas sua documentação atual recomenda o endpoint dedicado `/api/v2/stocks/dividends` e permite consultar vários símbolos em uma chamada. A Alpha Vantage continua sendo a fonte aprovada para dividendos americanos e possui limite de uso que precisa ser respeitado por cache e deduplicação.

## Goals / Non-Goals

**Goals:**

- Produzir uma distribuição única e determinística do patrimônio consolidado em BRL.
- Reduzir chamadas externas de proventos e impedir tempestades de requisições concorrentes.
- Preservar resultados válidos diante de falhas transitórias sem tratá-los como dados novos.
- Permitir resposta parcial transparente e manter o registro manual disponível.
- Validar os adaptadores com contratos reais representativos dos provedores.

**Non-Goals:**

- Trocar Brapi, Alpha Vantage, Twelve Data ou a fonte PTAX.
- Persistir cotações, respostas brutas ou candidatos não confirmados no banco.
- Alterar elegibilidade histórica, cálculo do valor previsto ou regras de confirmação.
- Criar rotina automática de proventos em segundo plano.

## Decisions

### 1. Calcular alocação em uma segunda passagem consolidada

O cálculo continuará produzindo valores monetários nativos e subtotais por moeda. Depois de obter as taxas exigidas pelo resumo consolidado, cada valor de mercado será convertido para BRL e dividido pelo total consolidado. Isso reutiliza a mesma base cambial já apresentada ao usuário e evita divergência entre indicadores.

Alternativa considerada: recalcular somente no frontend. Foi rejeitada porque duplicaria regra financeira, permitiria diferenças entre consumidores da API e manteria incorreto o campo público `allocationPercentage`.

### 2. Fechar percentuais com maior resto e desempate estável

Os percentuais exatos serão calculados com `DECIMAL128`. Para a escala pública de apresentação, serão truncados na unidade mínima e as unidades residuais serão distribuídas pelas maiores frações; empates serão resolvidos por mercado e ticker. Isso garante soma exata de 100% sem alterar nenhum valor monetário e sem depender da ordem de entrada.

Alternativa considerada: atribuir todo o resíduo à última posição. Foi rejeitada porque a posição afetada dependeria da ordenação e poderia concentrar erro de forma arbitrária.

### 3. Separar cache de dados do provedor do cache de confirmação

Será criado um cache temporário de `IncomeProviderEvent` por mercado e ticker, distinto das referências opacas de confirmação. Cada entrada guardará resultado, instante da consulta, expiração normal e limite de contingência. Respostas vazias bem-sucedidas também serão armazenadas. Ao montar a resposta, elegibilidade e identificadores de confirmação continuarão sendo recalculados no contexto do investidor e da carteira.

Alternativa considerada: reutilizar `IncomeEventCandidateCache`. Foi rejeitada porque seus dados são privados e contextualizados, enquanto eventos de provedor são públicos e podem ser compartilhados com segurança antes do cálculo de elegibilidade.

### 4. Deduplicar chamadas concorrentes e usar contingência somente em falha

Uma operação equivalente em andamento será compartilhada entre solicitações concorrentes. Dentro do TTL normal, o cache será servido diretamente. Após o TTL, tenta-se atualizar; apenas se essa tentativa falhar será aceito o último resultado ainda dentro da janela de contingência. O retorno indicará que não houve atualização nova.

Alternativa considerada: servir dados antigos até que uma atualização em segundo plano termine. Foi rejeitada porque adicionaria execução assíncrona e tornaria a percepção de atualização menos previsível nesta etapa.

### 5. Migrar a integração brasileira para o endpoint dedicado e agrupado da Brapi v2

A estratégia brasileira usará o endpoint dedicado de dividendos e enviará, quando suportado pelo conjunto, os símbolos distintos em uma única chamada autenticada. A normalização aceitará o envelope v2 e manterá chaves determinísticas por evento. A estratégia americana continuará consultando a Alpha Vantage por símbolo, protegida pelo cache compartilhado.

Alternativa considerada: manter `/api/quote/{ticker}?dividends=true`. Foi rejeitada porque mistura dados desnecessários, multiplica requisições e não acompanha o endpoint recomendado atualmente para dividendos.

### 6. Enriquecer explicitamente a resposta da consulta de candidatos

O endpoint retornará um objeto com `candidates`, `updatedAt`, `stale` e `warnings`. Cada aviso terá ao menos ticker, mercado e código público. Se houver qualquer resultado confiável, inclusive resposta vazia confirmada de outro ticker, a operação retorna 200 com avisos. Se todos os itens falharem e não houver contingência, mantém-se 503. A mudança é incompatível com clientes que esperam um array na raiz, por isso backend, frontend, testes e documentação serão migrados juntos.

Alternativa considerada: omitir silenciosamente tickers com falha. Foi rejeitada porque um estado vazio poderia ser interpretado incorretamente como ausência de proventos.

## Risks / Trade-offs

- [Taxa cambial indisponível impede a distribuição consolidada] → Manter a falha atômica já exigida para a valorização; não calcular percentuais misturando moedas sem conversão válida.
- [Dados reutilizados podem estar desatualizados] → Limitar a janela de contingência, devolver instante e marcador explícito e usar o cache antigo somente após falha de atualização.
- [Cache em memória é perdido no reinício] → Aceitar essa limitação para evitar migração e persistência de dados transitórios; após reinício, a primeira consulta volta ao provedor.
- [Mudança do envelope de candidatos quebra clientes antigos] → Atualizar a aplicação web e a documentação na mesma entrega e destacar a incompatibilidade nas notas da change.
- [Contrato externo da Brapi pode variar por plano] → Cobrir envelopes de sucesso, vazio, erro de autenticação, limite e campos ausentes com testes de contrato e manter tradução segura de erros.
- [Resposta parcial pode ocultar recorrência de falhas] → Exibir aviso por ticker e registrar diagnóstico interno sem expor conteúdo bruto ou credenciais.

## Migration Plan

1. Implementar e testar o cálculo consolidado sem mudar os valores monetários existentes.
2. Introduzir o cache de eventos, deduplicação e adaptadores atualizados atrás da interface de estratégia existente.
3. Alterar o DTO e o controller de candidatos e migrar o frontend no mesmo conjunto de alterações.
4. Executar testes unitários, integração H2/PostgreSQL, contratos simulados dos provedores e testes completos do frontend.
5. Implantar backend e frontend juntos. Em rollback, restaurar ambos para a versão anterior; não há migração de banco para reverter.
