## Context

O sistema já mantém lançamentos imutáveis de compra e venda, deriva posições por ticker e mercado e converte custos históricos USD/BRL pela PTAX. Não existe representação de renda recebida, e os provedores de ativos possuem contratos distintos: a Brapi expõe dividendos, JCP e outros eventos brasileiros, enquanto a Alpha Vantage expõe dividendos americanos. Consulte `proposal.md` e `specs/portfolio-income-events/spec.md` para motivação e comportamento aprovado.

O desenho precisa preservar a arquitetura em camadas, o isolamento das integrações por Strategy, o ciclo de vida por State, a privacidade determinada pelo token e a compatibilidade H2/PostgreSQL sem introduzir Flyway ou nova dependência.

## Goals / Non-Goals

**Goals:**

- Normalizar eventos monetários heterogêneos sem acoplar o domínio aos JSONs externos.
- Calcular elegibilidade histórica de forma determinística a partir dos lançamentos efetivados.
- Separar candidato temporário de provento confirmado e persistido.
- Preservar origem, cálculo previsto, ajuste confirmado e identidade contra duplicidade.
- Fornecer histórico e resumo próprios sem alterar a semântica da valorização de mercado.

**Non-Goals:**

- Processar bonificações, desdobramentos, grupamentos, subscrições ou qualquer alteração de quantidade.
- Calcular impostos, retenções ou ganho de capital realizado.
- Executar sincronização automática em segundo plano ou ao abrir a carteira.
- Atualizar silenciosamente registros confirmados quando um provedor corrigir seus dados.
- Persistir respostas brutas, cotações ou taxas cambiais.

## Decisions

### Separar descoberta, confirmação e persistência

A consulta retornará candidatos temporários com um `candidateId` opaco. Os candidatos normalizados ficarão em cache local com TTL configurável e vinculados ao proprietário e à carteira. A confirmação usará essa referência para impedir que dados atribuídos ao provedor sejam forjados ou reutilizados em outra carteira; referência ausente ou expirada exigirá nova sincronização.

Alternativas consideradas:

- Persistir candidatos como rascunhos: rejeitada para não poluir o histórico com eventos nunca confirmados.
- Reconsultar o provedor durante toda confirmação: rejeitada por duplicar consumo de limites e aumentar falhas.
- Aceitar todo o retrato enviado pelo cliente: rejeitada porque enfraquece a rastreabilidade da origem.

### Usar Strategy por mercado e um candidato de domínio comum

Uma interface de integração de proventos será resolvida por mercado. Adaptadores Brapi e Alpha Vantage converterão respostas externas em um candidato de domínio com tipo, valor unitário, elegibilidade, pagamento, origem e identidade externa. O filtro de eventos monetários ficará na integração/normalização, enquanto as regras de carteira permanecerão no serviço.

A abordagem reutiliza configurações, clientes HTTP, credenciais e tradução de falhas já existentes, sem adicionar bibliotecas. SDKs dos provedores não serão adicionados porque os contratos necessários são pequenos e o projeto já usa integração HTTP própria.

### Reproduzir posição histórica em componente de domínio dedicado

Um calculador de elegibilidade receberá lançamentos efetivados ordenados por data da transação e criação. Para `BR`, aplicará operações até o fim da data-com; para `US`, aplicará somente operações anteriores à data ex-dividendo. A regra não reutilizará a posição atual, pois ativos vendidos depois da elegibilidade ainda geram direito.

Eventos com data de elegibilidade futura serão apenas informativos. O cálculo definitivo ocorrerá após essa data para não prometer direito que ainda pode mudar.

### Persistir um retrato imutável e controlar estado com State

A entidade de provento guardará carteira, retrato do ativo, tipo, moeda, datas, quantidade elegível opcional, valor unitário opcional, valor previsto opcional, valor recebido, justificativa opcional, origem, identidade/fingerprint, status e datas públicas. O valor confirmado será a fonte do indicador; o previsto servirá apenas para auditoria.

O ciclo `PENDING -> EFFECTIVE` e `PENDING -> CANCELLED` seguirá o padrão State dos lançamentos. Registros efetivos e cancelados não terão atualização nem exclusão física. A reconciliação ocorrerá antes de consultas e operações do agregado.

### Aplicar unicidade por carteira e identidade do evento

Eventos de provedor usarão identidade externa quando estável; caso contrário, será calculado fingerprint canônico com mercado, ticker, tipo, elegibilidade, pagamento e valor unitário. A persistência terá restrição única por carteira, origem e identidade, protegendo também confirmações concorrentes. Registros manuais terão fingerprint de negócio próprio, derivado dos campos informados.

Uma nova consulta comparará seus candidatos com as identidades persistidas para marcá-los como já registrados. Mudança posterior da API não sobrescreverá o snapshot; correções de históricos efetivos exigirão uma futura estratégia explícita de ajuste ou reversão.

### Manter resumo de renda separado da valorização

O endpoint de resumo de proventos somará valores confirmados efetivos por moeda. Para USD, reutilizará o serviço cambial histórico na data de pagamento e retornará as taxas aplicadas. Falha em qualquer taxa produzirá `EXCHANGE_RATE_UNAVAILABLE` sem parcial.

O resumo será independente do endpoint de valorização para evitar que indisponibilidade cambial de renda histórica impeça a consulta das posições atuais e para não redefinir `investedValue`, `marketValue` ou `gainLoss`. O dashboard poderá consumir os dois contratos em paralelo.

### Contratos REST separados por intenção

Os contratos ficarão sob `/api/portfolios/{portfolioId}/income-events`:

- `GET /candidates` consulta candidatos de um mercado solicitado;
- `POST /confirmations` confirma candidato temporário;
- `POST /manual` registra contingência manual;
- `GET` lista o histórico;
- `DELETE /{incomeEventId}` cancela somente pendente;
- `GET /summary` retorna totais recebidos.

Separar confirmação e manual torna explícita a origem dos dados e permite validações diferentes sem DTO ambíguo.

## Risks / Trade-offs

- [Brapi pode exigir plano pago] → manter cadastro manual e traduzir falta de acesso para erro público sem vazar credenciais.
- [Alpha Vantage e Brapi possuem limites de requisição] → consulta explícita por mercado, cache temporário de candidatos e reaproveitamento das políticas de timeout/limite.
- [Cache de candidato se perde em reinício ou expira] → retornar erro de referência expirada e orientar nova sincronização; nenhum dado confirmado é perdido.
- [Data de pagamento ou elegibilidade pode faltar] → exibir candidato incompleto, impedir confirmação assistida e permitir registro manual quando o usuário possuir o dado correto.
- [Valor previsto pode ser bruto e divergir do crédito] → confirmar valor recebido e exigir justificativa para divergência, sem calcular impostos.
- [Fingerprint pode colidir em eventos externos sem ID] → incluir todos os campos estáveis normalizados e reforçar a restrição no banco; manter resposta de conflito recuperável.
- [Muitas posições históricas aumentam chamadas externas] → consultar somente tickers que tiveram compra efetivada no mercado solicitado e manter a operação fora da abertura automática da carteira.
- [Taxa PTAX histórica indisponível derruba o resumo consolidado] → manter totais nativos no histórico e responder falha atômica apenas no contrato de resumo consolidado.

## Migration Plan

1. Introduzir o modelo e a persistência de proventos, deixando o schema ser criado pelos perfis H2/PostgreSQL já suportados.
2. Adicionar domínio, repositório, mapper, DTOs e serviço com cadastro manual e ciclo de vida.
3. Adicionar calculador histórico, estratégias externas e cache de candidatos.
4. Expor os contratos privados e integrar o resumo cambial.
5. Validar comportamento unitário, H2 e teste PostgreSQL opt-in antes da liberação.

O rollback consiste em remover os novos contratos e componentes. A tabela nova é isolada das tabelas de lançamentos e posições; sua remoção física não será automatizada durante o desenvolvimento para evitar perda acidental de históricos confirmados.
