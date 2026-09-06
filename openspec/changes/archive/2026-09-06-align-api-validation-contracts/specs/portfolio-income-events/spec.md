## MODIFIED Requirements

### Requirement: Registro manual de contingência
O sistema SHALL permitir registro manual de provento para ativo com ao menos uma compra efetivada na carteira. O registro MUST conter ticker e tipo monetário suportado, data de pagamento e valor recebido positivo; data de elegibilidade, quantidade elegível, valor unitário e observação MAY ser informados. O sistema MUST recuperar nome, mercado, tipo e moeda do retrato da compra efetiva da própria carteira, em vez de aceitá-los do cliente. Valores monetários e quantitativos informados MUST comportar no máximo 11 dígitos inteiros e 8 casas decimais. O registro manual MUST ser identificado com origem `MANUAL` e MUST NOT exigir consulta externa nem cálculo de impostos.

#### Scenario: Provento manual válido
- **WHEN** o investidor informa um provento manual válido para ativo presente no histórico efetivado da carteira
- **THEN** o sistema cria o registro com origem `MANUAL` e preserva os valores informados e o retrato histórico do ativo

#### Scenario: Ativo sem compra efetivada na carteira
- **WHEN** o investidor tenta registrar manualmente provento de ativo que nunca possuiu compra efetivada naquela carteira
- **THEN** o sistema responde com `409` e código `ASSET_NOT_ACQUIRED`, não cria o registro e não revela dados de outras carteiras

#### Scenario: Dados manuais inválidos
- **WHEN** tipo, pagamento, valor recebido, quantidade elegível ou valor unitário não atendem às regras, inclusive precisão ou escala compatíveis com o armazenamento
- **THEN** o sistema responde com `400`, identifica os campos inválidos e não cria o provento
