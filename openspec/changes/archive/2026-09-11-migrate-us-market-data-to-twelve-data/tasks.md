## 1. Configuração e isolamento dos provedores

- [x] 1.1 Adicionar propriedades e cliente HTTP para `TWELVE_DATA_BASE_URL` e `TWELVE_DATA_API_KEY`, preservando timeouts e mantendo a configuração Alpha Vantage usada por dividendos.
- [x] 1.2 Atualizar `.env.example` e os testes de configuração com valores fictícios e com a finalidade separada das duas chaves.
- [x] 1.3 Garantir que ausência de cada credencial afete somente os fluxos do respectivo provedor e não impeça a inicialização da aplicação.

## 2. Pesquisa e cotação de ativos americanos

- [x] 2.1 Implementar a estratégia Twelve Data de pesquisa `US` via `/symbol_search`, normalizando ticker, nome, país, moeda e tipos `Common Stock`/`ETF`.
- [x] 2.2 Implementar cotação Twelve Data via `/price`, aceitando somente valor positivo em `USD` e preservando o cache e o limite atual de resultados.
- [x] 2.3 Traduzir limite de créditos, credencial inválida, plano sem acesso, timeout e respostas inválidas para códigos públicos `TWELVE_DATA_*` em JSON `503`.
- [x] 2.4 Retirar a estratégia Alpha Vantage de pesquisa e cotação do conjunto de componentes ativos, comprovando que ela permanece disponível apenas para dividendos.
- [x] 2.5 Deduplicar candidatos da Twelve Data pelo ticker normalizado e cobrir respostas com listagens repetidas.

## 3. Histórico e evolução patrimonial

- [x] 3.1 Implementar a estratégia histórica Twelve Data via `/time_series` com intervalo diário, datas inicial/final e `adjust=none`.
- [x] 3.2 Normalizar somente datas do intervalo e fechamentos positivos, mantendo cache, atomicidade e erro explícito para cobertura insuficiente.
- [x] 3.3 Retirar a estratégia histórica Alpha Vantage do conjunto de componentes ativos sem alterar cálculos, PTAX ou contratos da evolução.

## 4. Testes de contrato e regressão

- [x] 4.1 Cobrir pesquisa, classificação de ações/ETFs, cotação, séries diárias e respostas vazias da Twelve Data com servidor HTTP simulado.
- [x] 4.2 Cobrir `429`, erro no corpo, credencial ausente/inválida, plano sem acesso, timeout e dados malformados sem expor detalhes internos.
- [x] 4.3 Testar a seleção de estratégias por mercado e confirmar que dividendos `US` continuam usando `AlphaVantageIncomeEventProviderStrategy`.
- [x] 4.4 Executar as suítes Maven completas e os gates de release H2 e PostgreSQL sem depender das APIs externas reais.

## 5. Documentação e validação final

- [x] 5.1 Atualizar `docs/product-spec.md`, configuração e guia da API para identificar Twelve Data em pesquisa/cotação/histórico `US` e Alpha Vantage em dividendos `US`.
- [x] 5.2 Documentar limites, licenciamento, atribuição aplicável e configuração segura de `TWELVE_DATA_API_KEY`, sem incluir chaves reais.
- [x] 5.3 Executar validação OpenSpec estrita, verificar formatação e atualizar o mapa Graphify após a implementação.
