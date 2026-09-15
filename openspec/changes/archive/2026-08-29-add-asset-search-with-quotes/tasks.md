## 1. Contratos de domínio e configuração

- [x] 1.1 Criar os valores de domínio para mercado (`BR`, `US`) e tipo de ativo (`STOCK`, `ETF`), resultado normalizado de ativo e cotação sem dados de fornecedor.
- [x] 1.2 Criar DTOs imutáveis de resposta pública com ticker, nome, mercado, tipo, moeda e preço, sem expor credenciais, timestamps internos ou DTOs externos.
- [x] 1.3 Criar propriedades de integração e cache para Brapi e AlphaVantage, resolvidas por variáveis de ambiente e com valores locais seguros sem versionar tokens.
- [x] 1.4 Criar o componente de cache em memória com expiração de cinco minutos para identificação e um minuto para cotação, incluindo chaves normalizadas.
- [x] 1.5 Testar os valores de domínio, mapeamentos de resposta e expiração de cache com relógio controlado.

## 2. Integrações e estratégias de ativos

- [x] 2.1 Definir a interface Strategy de busca por mercado e os contratos internos para candidatos, classificação e cotações.
- [x] 2.2 Implementar o cliente e a estratégia Brapi para localizar ativos brasileiros, filtrar ações ou ETFs classificados e obter cotações.
- [x] 2.3 Implementar o cliente e a estratégia AlphaVantage para localizar ativos americanos, filtrar ações ou ETFs classificados e obter a cotação de cada candidato elegível.
- [x] 2.4 Implementar seleção de Strategy pelo mercado, normalização do termo, limite de cinco resultados cotados e exclusão de candidatos sem classificação ou preço.
- [x] 2.5 Traduzir timeout, resposta técnica inesperada e limite de uso de cada provedor para falhas públicas distintas sem expor tokens, URLs ou corpos externos.
- [x] 2.6 Testar clientes e estratégias com respostas simuladas, ausência de resultados, classificação divergente, cotação ausente, timeout e limitação externa.

## 3. Serviço, segurança e API

- [x] 3.1 Implementar o Service de busca que valida `market`, `assetType` e `query`, normaliza o termo e confirma a propriedade da carteira antes de chamar uma Strategy.
- [x] 3.2 Reutilizar resultados e cotações do cache segundo suas durações, sem persistir ativos ou preços no banco.
- [x] 3.3 Criar `GET /api/portfolios/{portfolioId}/assets` protegido para `INVESTOR`, com parâmetros obrigatórios `market`, `assetType` e `query`.
- [x] 3.4 Adicionar exceções e mapeamentos no `@ControllerAdvice` para entrada inválida, indisponibilidade e limitação da Brapi e da AlphaVantage em JSON padronizado.
- [x] 3.5 Garantir `401` sem token, `403` para administrador e `404 PORTFOLIO_NOT_FOUND` para carteira inexistente ou de outro investidor, sem consultar fornecedores nesses casos.
- [x] 3.6 Criar testes unitários do Service para autorização, normalização, filtros exclusivos, limite de resultados, cache e ausência de persistência.

## 4. Testes integrados e compatibilidade

- [x] 4.1 Criar testes de API para busca brasileira de ações e ETFs e busca americana de ações e ETFs, validando o contrato público com preço.
- [x] 4.2 Testar parâmetros ausentes, valores inválidos, termo vazio ou curto, coleção vazia e candidatos sem preço ou classificação compatível.
- [x] 4.3 Testar isolamento entre investidores, carteira inexistente, autenticação e autorização das operações de busca.
- [x] 4.4 Testar respostas `503` para indisponibilidade e limite dos dois provedores, confirmando que tokens e detalhes técnicos não são expostos.
- [x] 4.5 Executar a suíte Maven completa no perfil H2 e corrigir falhas relacionadas à mudança.
- [x] 4.6 Inicializar o perfil PostgreSQL local sem Docker e confirmar que a busca não cria tabelas ou registros de ativos e cotações.

## 5. Revisão final da mudança

- [x] 5.1 Executar `openspec validate add-asset-search-with-quotes --strict` e reconciliar proposta, especificação, desenho e tarefas.
- [x] 5.2 Revisar o diff para confirmar arquitetura em camadas, Strategy isolada, ausência de credenciais e preservação de alterações não relacionadas.
- [x] 5.3 Atualizar o Graphify em modo somente código após a implementação e revisar os impactos nas comunidades de carteira e integração.
