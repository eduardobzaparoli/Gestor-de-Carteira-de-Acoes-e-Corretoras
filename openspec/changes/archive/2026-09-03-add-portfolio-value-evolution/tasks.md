## 1. Domínio e reprodução histórica

- [x] 1.1 Criar os tipos de domínio para preço histórico, série normalizada, chave de ativo e ponto diário consolidado em BRL, com validações de data, moeda e valores positivos.
- [x] 1.2 Extrair ou compor um acumulador de quantidade e custo histórico em BRL reutilizável pela valorização atual e pela evolução, preservando as regras existentes de compra, venda, liquidação e reabertura.
- [x] 1.3 Implementar o calculador incremental da evolução para reproduzir lançamentos efetivados até cada data sem recalcular todo o histórico por ponto.
- [x] 1.4 Implementar a união ordenada dos pregões BR/US, o uso do último fechamento anterior para mercado fechado e o ponto atual com cotação corrente.

## 2. Integrações de preços históricos

- [x] 2.1 Adicionar propriedades de janela, TTL e configuração das séries históricas reutilizando clientes, credenciais e timeouts existentes sem versionar segredos.
- [x] 2.2 Definir Strategy de preços históricos e resolvedor por mercado independentes da busca e da cotação atual.
- [x] 2.3 Implementar o adaptador Brapi para consultar a janela exata diária, normalizar fechamentos não ajustados, moeda e datas e detectar cobertura truncada pelo plano.
- [x] 2.4 Implementar o adaptador Alpha Vantage com `TIME_SERIES_DAILY` e saída compacta, normalizando fechamentos não ajustados, moeda e datas.
- [x] 2.5 Implementar cache concorrente com TTL por mercado, ticker e intervalo, incluindo normalização de chaves e expiração determinística.
- [x] 2.6 Traduzir ausência de credencial, plano sem acesso, limite, timeout, transporte, payload inválido e cobertura insuficiente para erros públicos sem conteúdo bruto.
- [x] 2.7 Criar testes determinísticos dos dois adaptadores e do cache para parsing, ordenação, fechamento não ajustado, expiração, limites e falhas técnicas.

## 3. Serviço de evolução patrimonial

- [x] 3.1 Identificar, a partir de todo o log efetivado, somente os ativos que estiveram em custódia na janela inclusiva de 90 dias.
- [x] 3.2 Orquestrar a obtenção das séries por ativo e das cotações atuais somente quando houver custódia a valorar.
- [x] 3.3 Resolver e deduplicar PTAX histórica das compras em USD e das datas de patrimônio, reutilizando o cache cambial e o último fechamento anterior.
- [x] 3.4 Produzir pontos crescentes com `investedValue` e `marketValue` em BRL, excluindo caixa de vendas, proventos e lançamentos não efetivos.
- [x] 3.5 Garantir resolução atômica dos insumos e erro `503` sem pontos parciais quando preço, cobertura ou câmbio necessário estiver indisponível.
- [x] 3.6 Garantir que carteira sem custódia na janela retorne coleção vazia sem chamadas externas e que nenhum ponto, preço, posição ou câmbio seja persistido.
- [x] 3.7 Criar testes de serviço para seleção de ativos, janela, cache, ponto atual, operação atômica, ausência de custódia e isolamento por proprietário.

## 4. API privada e respostas públicas

- [x] 4.1 Criar DTOs e mapper da evolução com data, valor investido e patrimônio em BRL, mantendo precisão e ordem determinística.
- [x] 4.2 Criar endpoint `GET /api/portfolios/{portfolioId}/value-evolution` protegido para `INVESTOR`, derivando o proprietário exclusivamente do JWT.
- [x] 4.3 Adicionar exceções e tratamento centralizado para preço histórico indisponível, cobertura insuficiente e falhas dos provedores em JSON.
- [x] 4.4 Testar respostas `200`, coleção vazia, `401`, `403`, `404` indistinguível, `503` atômico e ausência de detalhes internos.

## 5. Testes integrados e compatibilidade

- [x] 5.1 Criar teste integrado H2 do fluxo brasileiro com compras, custos, venda parcial e ponto atual, usando preços simulados.
- [x] 5.2 Testar carteira mista BR/US com calendários diferentes, PTAX histórica por ponto e consolidação integral em BRL.
- [x] 5.3 Testar que venda total zera custódia sem criar caixa e que proventos, pendências e cancelamentos não alteram a série.
- [x] 5.4 Testar cobertura truncada, preço ausente, câmbio indisponível e limite externo sem retorno parcial.
- [x] 5.5 Ampliar o teste PostgreSQL opt-in para validar o mesmo cálculo sem criar tabela própria para evolução.
- [x] 5.6 Executar a suíte Maven completa com integrações simuladas e corrigir regressões relacionadas à mudança.
- [x] 5.7 Executar a verificação PostgreSQL opt-in com credenciais locais controladas e registrar o resultado sem versionar segredos.

## 6. Validação da change

- [x] 6.1 Revisar contratos, limites de 90 dias, configuração de cache, códigos públicos e escopo sem caixa ou eventos societários contra o PRD e as specs consolidadas.
- [x] 6.2 Atualizar a análise estrutural do Graphify após a implementação e verificar que as novas dependências preservam as camadas e Strategies planejadas.
- [x] 6.3 Executar a validação OpenSpec estrita e manter proposta, especificação, design e checklist coerentes com o comportamento final.
