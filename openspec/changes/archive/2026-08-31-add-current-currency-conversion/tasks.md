## 1. Contratos e integração cambial

- [x] 1.1 Criar os contratos de domínio e Strategy para consultar uma taxa de câmbio para uma moeda de origem, moeda-base e data de referência.
- [x] 1.2 Implementar a estratégia da BrasilAPI que selecione a cotação de compra do `FECHAMENTO PTAX` mais recente disponível para USD/BRL.
- [x] 1.3 Configurar cliente HTTP, URL-base, timeouts e TTL de cache da integração cambial sem introduzir credenciais ou dependências novas.
- [x] 1.4 Implementar o cache em memória por par de moedas e data de referência.

## 2. Valorização consolidada

- [x] 2.1 Criar os modelos de domínio para a taxa aplicada e o resumo consolidado em BRL.
- [x] 2.2 Estender o cálculo de valorização para somar posições em BRL e converter apenas valores de mercado em USD, preservando os subtotais e alocações nativos.
- [x] 2.3 Estender DTOs e mapeadores da resposta de valorização com o resumo consolidado, taxa e data de referência, sem incluir investimento, ganho ou rentabilidade consolidados.
- [x] 2.4 Integrar a consulta cambial ao serviço de valorização e impedir consulta cambial para carteiras vazias ou compostas somente por BRL.
- [x] 2.5 Criar o erro público `EXCHANGE_RATE_UNAVAILABLE` e mapeá-lo para resposta JSON `503` sem resultados parciais.

## 3. Testes

- [x] 3.1 Cobrir seleção da PTAX de compra de fechamento, busca do último fechamento disponível, moedas não suportadas, timeout e resposta inválida da fonte cambial.
- [x] 3.2 Cobrir cache de taxa por moeda e data de referência, inclusive expiração.
- [x] 3.3 Cobrir o cálculo consolidado para carteiras somente em BRL, somente em USD e com ambas as moedas, confirmando que os indicadores históricos permanecem segregados.
- [x] 3.4 Cobrir a falha atômica de valorização quando o câmbio necessário não estiver disponível.
- [x] 3.5 Atualizar os testes de integração do endpoint de valorização para validar autorização, propriedade, resposta aditiva e erro público em H2.
- [x] 3.6 Executar a suíte Maven completa e a verificação opt-in no perfil PostgreSQL, corrigindo regressões relacionadas à mudança.
