## MODIFIED Requirements

### Requirement: Consulta resiliente e sem persistência própria
O sistema SHALL reutilizar temporariamente cotações equivalentes já obtidas para o mesmo mercado e ticker, respeitando a janela de cache configurada. O sistema SHALL consultar a Brapi para posições `BR` e a Twelve Data para posições `US`. O sistema SHALL reutilizar temporariamente uma taxa cambial equivalente para a mesma moeda de origem, moeda-base e data de referência, respeitando sua janela de cache configurada. A consulta MUST ser atômica para a resposta: se qualquer cotação de ativo ou conversão cambial necessária falhar por indisponibilidade ou limite do provedor, o sistema MUST responder `503` com o código público específico e não retornar totais parciais. O sistema MUST NOT persistir cotações, taxas cambiais, posições ou indicadores de valorização.

#### Scenario: Cotação reutilizada dentro da janela temporária
- **WHEN** a mesma carteira ou outra carteira solicita valorização de um ticker no mesmo mercado dentro da janela de cache configurada
- **THEN** o sistema reutiliza a cotação temporária disponível antes de chamar o provedor novamente

#### Scenario: Taxa cambial reutilizada dentro da janela temporária
- **WHEN** uma segunda consulta de valorização precisa converter `USD` para `BRL` com a mesma data de referência dentro da janela de cache configurada
- **THEN** o sistema reutiliza a taxa cambial temporária disponível antes de chamar o provedor novamente

#### Scenario: Provedor indisponível ou limitado durante a valorização
- **WHEN** a Brapi ou a Twelve Data excede o tempo limite, falha tecnicamente, rejeita a credencial ou informa limite de créditos ao cotar qualquer posição aberta
- **THEN** o sistema responde com status `503` e o código público específico do provedor, sem expor credenciais, URLs internas, conteúdo bruto ou resultados parciais

#### Scenario: Provedor cambial indisponível durante a valorização
- **WHEN** a fonte cambial excede o tempo limite, falha tecnicamente ou não disponibiliza uma taxa PTAX de compra de fechamento válida
- **THEN** o sistema responde com status `503`, código `EXCHANGE_RATE_UNAVAILABLE`, sem expor detalhes internos e sem devolver resultados parciais

#### Scenario: Novo lançamento altera a base de cálculo
- **WHEN** uma compra ou venda efetivada altera o histórico da carteira
- **THEN** a próxima consulta recalcula a valorização a partir das posições abertas atualizadas, sem exigir persistência própria

#### Scenario: Banco suportado
- **WHEN** a aplicação usa H2 ou PostgreSQL
- **THEN** a consulta produz os mesmos indicadores para o mesmo histórico, as mesmas cotações e as mesmas taxas cambiais disponíveis

