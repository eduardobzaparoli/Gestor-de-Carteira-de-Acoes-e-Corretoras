## MODIFIED Requirements

### Requirement: Fontes externas resilientes e reutilização temporária

O sistema SHALL consultar a Brapi para o mercado `BR` e a Twelve Data para identificação e cotação de ativos do mercado `US`. O sistema MUST reutilizar temporariamente resultados de identificação e cotações equivalentes para reduzir chamadas repetidas, sem exigir que o cliente informe ou receba chaves dos provedores. A Alpha Vantage MUST NOT ser usada como fonte de identificação ou cotação de ativos `US` e SHALL permanecer reservada à consulta de proventos americanos.

#### Scenario: Consulta repetida de identificação

- **WHEN** o investidor repete a mesma combinação de mercado, tipo e termo dentro da janela temporária configurada
- **THEN** o sistema reutiliza o resultado de identificação previamente obtido

#### Scenario: Consulta repetida de cotação

- **WHEN** o sistema precisa da cotação de um mesmo ticker dentro da janela temporária configurada
- **THEN** o sistema reutiliza a cotação previamente obtida

#### Scenario: Pesquisa de ativo americano

- **WHEN** o investidor pesquisa uma ação ou ETF no mercado `US`
- **THEN** o sistema consulta a Twelve Data, aceita somente instrumentos americanos do tipo solicitado e retorna preços em `USD`

#### Scenario: Provedor retorna o mesmo ticker mais de uma vez

- **WHEN** a Twelve Data devolve múltiplas ocorrências válidas do mesmo ticker em uma pesquisa
- **THEN** o sistema retorna somente um candidato para esse ticker, preservando a primeira ocorrência normalizada

#### Scenario: Provedor indisponível ou limitado

- **WHEN** a Brapi ou a Twelve Data excede o tempo limite, falha tecnicamente, rejeita a credencial ou informa limite de créditos
- **THEN** o sistema responde com status `503` e código público específico do provedor, sem expor credenciais, URLs internas ou conteúdo bruto
