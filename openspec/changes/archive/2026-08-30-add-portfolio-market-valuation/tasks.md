## 1. Domínio e contratos de valorização

- [x] 1.1 Criar os objetos de domínio puros para posição valorizada, resumo por moeda e cálculo decimal de valor de mercado, ganho ou perda, rentabilidade e alocação.
- [x] 1.2 Criar DTOs de resposta públicos para a consulta de valorização, posições valorizadas e subtotais por moeda, sem expor entidades ou detalhes internos.
- [x] 1.3 Criar mapper(s) para converter o domínio de valorização nos DTOs com escala pública consistente, preservando precisão nos cálculos internos.

## 2. Cotação e regras de negócio

- [x] 2.1 Expor no serviço de ativos uma operação reutilizável para obter a cotação de um ticker por mercado, reaproveitando o cache e as estratégias existentes.
- [x] 2.2 Implementar o serviço de valorização que obtém posições abertas autorizadas, garante a reconciliação de pendências e consulta todas as cotações necessárias antes de produzir a resposta.
- [x] 2.3 Validar ticker, moeda e preço positivo de cada cotação e mapear ausência ou incompatibilidade para `ASSET_QUOTE_UNAVAILABLE` com resposta `503`.
- [x] 2.4 Agrupar indicadores por moeda, calcular alocação somente dentro da mesma moeda e impedir qualquer total consolidado ou conversão cambial.

## 3. API e tratamento de erros

- [x] 3.1 Adicionar o controlador `GET /api/portfolios/{portfolioId}/valuation` com autenticação e autorização compatíveis com as consultas privadas de carteira.
- [x] 3.2 Integrar a nova falha pública de cotação ao tratamento centralizado de erros JSON, preservando os códigos existentes de indisponibilidade e limite dos provedores.

## 4. Testes e verificação

- [x] 4.1 Criar testes unitários para valor de mercado, ganho ou perda, rentabilidade, alocação, agrupamento por moeda, precisão decimal e carteira sem posição aberta.
- [x] 4.2 Criar testes de serviço para reutilização de cache, validação de cotação, política sem resultado parcial e propagação de erros de provedor.
- [x] 4.3 Criar testes de integração H2 para autorização, carteira inexistente/de outro investidor, posições BRL e USD, resposta vazia e contrato do endpoint.
- [x] 4.4 Estender a suíte opt-in do PostgreSQL para validar a mesma valorização sem tabela ou migração própria.
- [x] 4.5 Executar a suíte Maven padrão e a verificação opt-in do PostgreSQL com as variáveis locais necessárias, corrigindo apenas falhas relacionadas à change.
