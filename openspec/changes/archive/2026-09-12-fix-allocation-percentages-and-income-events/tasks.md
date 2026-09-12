## 1. Percentuais consolidados da carteira

- [x] 1.1 Adaptar o cálculo de valorização para converter o valor de mercado de cada posição para BRL com as mesmas taxas do resumo consolidado antes de calcular `allocationPercentage`.
- [x] 1.2 Implementar o fechamento determinístico em 100% pela estratégia de maior resto, com desempate estável por mercado e ticker e sem arredondar valores monetários.
- [x] 1.3 Preservar os subtotais nativos por moeda e garantir os estados corretos para carteira vazia, patrimônio não positivo e falha cambial.
- [x] 1.4 Atualizar os testes do domínio e da API de valorização para uma moeda, múltiplas moedas, divisões periódicas, ordem diferente das posições e soma exata em 100%.

## 2. Integrações e cache de proventos

- [x] 2.1 Migrar a consulta brasileira para o endpoint dedicado de dividendos da Brapi v2, agrupando símbolos compatíveis em uma chamada e normalizando o novo envelope, respostas vazias e renomes de ticker.
- [x] 2.2 Manter a Alpha Vantage como fonte americana e revisar a tradução de limite, credencial, plano, payload inválido e indisponibilidade para códigos públicos específicos.
- [x] 2.3 Criar um cache temporário de eventos de provedor por mercado e ticker, separado das referências de confirmação, armazenando resultados válidos inclusive vazios, instante, TTL normal e janela de contingência.
- [x] 2.4 Deduplicar chamadas concorrentes equivalentes para que apenas uma consulta externa seja executada e seu resultado seja compartilhado com segurança.
- [x] 2.5 Adicionar configurações documentadas de TTL e contingência aos arquivos de configuração e ao `.env.example`, sem incluir credenciais reais nem nova dependência.

## 3. Orquestração e contrato dos candidatos

- [x] 3.1 Selecionar e deduplicar os tickers efetivados relevantes ao mercado solicitado antes de consultar os provedores, sem alterar as regras históricas de elegibilidade.
- [x] 3.2 Criar DTOs para o objeto de resposta com `candidates`, `updatedAt`, `stale` e avisos públicos por ticker e atualizar o controller do endpoint.
- [x] 3.3 Implementar resposta `200` com resultados confiáveis e avisos em falha parcial ou contingência, e manter `503` quando todos os tickers falharem sem dados reutilizáveis.
- [x] 3.4 Recalcular elegibilidade no contexto privado e emitir referências opacas novas e expiráveis também para eventos recuperados do cache, preservando isolamento e prevenção de duplicidade.
- [x] 3.5 Garantir que consulta sem ativos retorne objeto vazio sem chamar provedor e que uma resposta vazia válida seja distinguida de falha externa.

## 4. Interface React

- [x] 4.1 Atualizar tipos e cliente HTTP para consumir o novo objeto de candidatos e mapear todos os novos códigos públicos para mensagens em português.
- [x] 4.2 Usar diretamente os percentuais consolidados na tabela, gráfico e legenda, garantindo correspondência entre posições e apresentação total de 100%.
- [x] 4.3 Exibir estados distintos para candidatos atualizados, dados reutilizados, falha parcial por ticker, limite da Alpha Vantage e indisponibilidade total.
- [x] 4.4 Manter candidatos confiáveis confirmáveis durante avisos parciais, evitar novas tentativas automáticas após limite e conservar o botão de registro manual em todos os estados externos.
- [x] 4.5 Cobrir acessibilidade, modos claro e escuro, telas pequenas e nova tentativa manual sem apagar resultados válidos já exibidos.

## 5. Verificação e documentação

- [x] 5.1 Adicionar testes de contrato dos provedores para Brapi v2 e Alpha Vantage cobrindo sucesso, lote, vazio, campos inválidos, autenticação, plano, limite e indisponibilidade.
- [x] 5.2 Testar cache normal, cache vazio, expiração, contingência, deduplicação concorrente, falha parcial e falha total sem acessar integrações externas reais.
- [x] 5.3 Atualizar testes de integração do endpoint e executar os cenários relevantes em H2 e PostgreSQL, incluindo segurança das referências e compatibilidade do novo envelope.
- [x] 5.4 Atualizar testes do frontend para composição multimoeda e todos os estados da descoberta de proventos.
- [x] 5.5 Atualizar o guia da API com o contrato incompatível, exemplos, metadados, avisos, códigos de erro e orientações de configuração, sem alterar `docs/product-spec.md` nesta change.
- [x] 5.6 Executar testes Maven relevantes e completos, formatador, lint, testes e build do frontend; validar a change em modo estrito e atualizar o índice Graphify.
