## 1. Domínio e persistência de proventos

- [x] 1.1 Criar tipos de domínio para tipo, origem, status, candidato, evento confirmado e resumo de proventos, incluindo validações de precisão e moeda por mercado.
- [x] 1.2 Implementar estados `PENDING`, `EFFECTIVE` e `CANCELLED` com transições permitidas e bloqueio de alteração dos estados imutáveis.
- [x] 1.3 Criar `PortfolioIncomeEventEntity` com snapshot financeiro, vínculo à carteira, identidade/fingerprint, restrição única e datas públicas compatíveis com H2 e PostgreSQL.
- [x] 1.4 Criar repositório com consultas privadas, ordenação por pagamento/criação, busca de identidades existentes, reconciliação de pendentes e agregação dos efetivos.
- [x] 1.5 Criar mapper entre entidade, domínio e DTOs sem expor proprietário, entidade ou identidade interna sensível.

## 2. Elegibilidade e identidade histórica

- [x] 2.1 Implementar calculador de quantidade elegível que reproduza lançamentos efetivados em ordem cronológica por ticker e mercado.
- [x] 2.2 Aplicar inclusão de operações até a data-com para `BR` e somente anteriores à data ex-dividendo para `US`, ignorando pendentes e canceladas.
- [x] 2.3 Calcular valor bruto previsto com precisão decimal e classificar eventos futuros ou incompletos como não confirmáveis.
- [x] 2.4 Implementar identidade externa normalizada e fingerprint determinístico de contingência para eventos de provedor e registros manuais.
- [x] 2.5 Cobrir o calculador com testes de compra/venda na fronteira das datas, posição encerrada depois da elegibilidade, quantidade zero e lançamentos não efetivos.

## 3. Integrações e candidatos temporários

- [x] 3.1 Definir Strategy de consulta de proventos, resolvedor por mercado e modelo normalizado independente dos contratos externos.
- [x] 3.2 Implementar integração Brapi para dividendos, JCP e distribuições monetárias, filtrando eventos que alterem quantidade e tratando campos nulos.
- [x] 3.3 Implementar integração Alpha Vantage `DIVIDENDS`, normalizando datas, moeda, valor unitário, identidade e respostas de limite ou indisponibilidade.
- [x] 3.4 Reutilizar configurações, credenciais, timeouts e tradução pública de falhas existentes sem incluir nova dependência nem expor resposta bruta.
- [x] 3.5 Implementar cache com TTL para candidatos opacos vinculados ao proprietário e à carteira, incluindo expiração, isolamento e invalidação após confirmação.
- [x] 3.6 Testar parsing e normalização dos dois provedores, eventos ignorados, campos ausentes, erros técnicos, limites e falta de acesso ao recurso.

## 4. Serviços de consulta, confirmação e cadastro manual

- [x] 4.1 Implementar consulta explícita e atômica de candidatos por mercado usando apenas ativos com compra efetivada no histórico da carteira.
- [x] 4.2 Enriquecer candidatos com elegibilidade, valor previsto, condição de confirmação e indicação de evento já registrado.
- [x] 4.3 Implementar confirmação por `candidateId`, validando proprietário, carteira, expiração, completude, valor recebido e justificativa para divergência.
- [x] 4.4 Implementar cadastro manual sem chamada externa, validando ativo previamente comprado, tipo monetário, moeda, pagamento, valor e campos opcionais.
- [x] 4.5 Garantir idempotência e conflito recuperável em duplicidade sequencial ou concorrente, preservando snapshots já confirmados.
- [x] 4.6 Implementar reconciliação antes das operações, cancelamento lógico somente de pendente e histórico ordenado sem atualização ou exclusão física.

## 5. Resumo cambial de proventos

- [x] 5.1 Implementar totais de valores recebidos efetivos por moeda, excluindo pendentes e cancelados.
- [x] 5.2 Reutilizar a PTAX histórica da data de pagamento para converter USD em BRL e expor as taxas/data de referência aplicadas.
- [x] 5.3 Implementar resumo consolidado atômico, retornando `EXCHANGE_RATE_UNAVAILABLE` sem parcial e evitando consulta cambial quando não houver USD efetivo.
- [x] 5.4 Testar resumo vazio, BRL, USD, moedas combinadas, múltiplas datas, fallback ao fechamento anterior e indisponibilidade da taxa.

## 6. API, segurança e erros públicos

- [x] 6.1 Criar DTOs de consulta de candidatos, confirmação, cadastro manual, histórico e resumo com Bean Validation e respostas exclusivamente JSON.
- [x] 6.2 Criar controller privado sob `/api/portfolios/{portfolioId}/income-events` para candidatos, confirmações, manual, histórico, cancelamento e resumo.
- [x] 6.3 Reforçar autorização `INVESTOR`, proprietário derivado do JWT e comportamento indistinguível `PORTFOLIO_NOT_FOUND` para carteira alheia.
- [x] 6.4 Adicionar exceções e mapeamentos centralizados para candidato expirado/não confirmável, ativo sem histórico, duplicidade, cancelamento inválido e falhas dos provedores.
- [x] 6.5 Testar `401`, `403`, `404`, validações `400`, conflitos `409`, indisponibilidade `503` e ausência de detalhes internos nas respostas.

## 7. Testes integrados e compatibilidade

- [x] 7.1 Criar testes integrados H2 do fluxo completo: consulta simulada, confirmação, ajuste justificado, cadastro manual, duplicidade, histórico e estados.
- [x] 7.2 Testar que proventos não alteram posições, preço médio, custo em custódia nem os indicadores existentes de valorização.
- [x] 7.3 Ampliar o teste PostgreSQL opt-in para inicializar a nova estrutura e validar persistência, unicidade, ordenação, ciclo de vida e resumo.
- [x] 7.4 Executar a suíte Maven completa com integrações externas simuladas e corrigir regressões ou alertas relacionados à mudança.
- [x] 7.5 Executar a verificação PostgreSQL opt-in com credenciais locais controladas e registrar o resultado sem versionar segredos.

## 8. Validação da change

- [x] 8.1 Revisar contratos públicos, exemplos de erros e configuração local de TTL/credenciais, garantindo que nenhum segredo ou plano comercial seja presumido no código.
- [x] 8.2 Executar a validação OpenSpec estrita da change e manter proposta, spec, design e checklist coerentes com o comportamento implementado.
