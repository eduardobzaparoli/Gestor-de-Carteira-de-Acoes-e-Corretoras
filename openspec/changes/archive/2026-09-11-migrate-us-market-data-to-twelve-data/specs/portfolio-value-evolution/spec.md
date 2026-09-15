## MODIFIED Requirements

### Requirement: Consulta histórica resiliente e atômica
O sistema SHALL consultar séries históricas na Brapi para ativos `BR` e na Twelve Data para ativos `US`, respeitando credenciais, limites e cobertura disponíveis. As séries da Twelve Data MUST usar intervalo diário, o período solicitado e preços de fechamento não ajustados para preservar a base de cálculo do histórico de lançamentos. O sistema MUST reutilizar temporariamente séries equivalentes em cache e MUST NOT persistir preços, taxas cambiais, posições ou pontos da evolução. Se faltar preço ou câmbio necessário para qualquer posição em custódia, a operação MUST responder com `503` e código público correspondente, sem retornar série parcial nem detalhes internos do provedor.

#### Scenario: Série histórica reutilizada
- **WHEN** uma nova consulta exige a mesma série de mercado ainda válida no cache
- **THEN** o sistema reutiliza os dados sem repetir a chamada externa equivalente

#### Scenario: Série americana consultada
- **WHEN** a evolução exige fechamentos históricos de uma ação ou ETF `US`
- **THEN** o sistema consulta a Twelve Data para o intervalo necessário e usa somente fechamentos diários não ajustados e válidos

#### Scenario: Cobertura histórica insuficiente
- **WHEN** o provedor não entrega preço suficiente para valorar uma posição existente na janela
- **THEN** o sistema responde com `503` e erro público de dados históricos indisponíveis sem fabricar ou truncar silenciosamente a série

#### Scenario: Câmbio histórico indisponível
- **WHEN** falta PTAX necessária para consolidar uma posição `US`
- **THEN** o sistema responde com `503` e código `EXCHANGE_RATE_UNAVAILABLE` sem pontos parciais

#### Scenario: Twelve Data indisponível ou limitada
- **WHEN** a Twelve Data excede o tempo limite, falha tecnicamente, rejeita a credencial ou informa limite de créditos durante a consulta histórica
- **THEN** o sistema responde com `503` e código público específico do provedor, sem recorrer à Alpha Vantage nem expor detalhes internos

