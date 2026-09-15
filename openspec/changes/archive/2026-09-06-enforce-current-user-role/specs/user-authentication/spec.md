## MODIFIED Requirements

### Requirement: Autenticação stateless por JWT
O sistema MUST validar assinatura, expiração, identidade e estado ativo da conta de tokens JWT em requisições protegidas, sem criar sessão no servidor. O papel persistido atualmente para a conta MUST ser a fonte de verdade para autorização, inclusive quando divergir do papel registrado em um token válido emitido anteriormente.

#### Scenario: Token válido
- **WHEN** uma requisição protegida apresenta um token Bearer válido e não expirado de uma conta ativa
- **THEN** o sistema autentica a identidade e disponibiliza seu identificador e o papel atual da conta durante a requisição

#### Scenario: Papel alterado após a emissão do token
- **WHEN** uma conta ativa tem seu papel alterado depois da emissão de um token Bearer ainda válido
- **THEN** a próxima requisição protegida com esse mesmo token usa o novo papel da conta para autorizar o acesso

#### Scenario: Administrador rebaixado usa token anterior
- **WHEN** uma conta ativa é rebaixada de `ADMIN` para `INVESTOR` e usa um token válido emitido quando ainda era administradora para acessar uma rota administrativa
- **THEN** o sistema rejeita o acesso com status `403` e resposta JSON

#### Scenario: Investidor promovido usa token anterior
- **WHEN** uma conta ativa é promovida de `INVESTOR` para `ADMIN` e usa um token válido emitido quando ainda era investidora para acessar uma rota administrativa
- **THEN** o sistema permite o acesso sem exigir novo login

#### Scenario: Token ausente
- **WHEN** uma requisição protegida não apresenta token Bearer
- **THEN** o sistema rejeita o acesso com status `401` e resposta JSON

#### Scenario: Token inválido ou expirado
- **WHEN** uma requisição protegida apresenta token adulterado, malformado ou expirado
- **THEN** o sistema rejeita o acesso com status `401` e resposta JSON sem revelar detalhes criptográficos

#### Scenario: Token emitido antes da desativação
- **WHEN** uma requisição protegida apresenta token válido de uma conta que foi desativada após a emissão
- **THEN** o sistema rejeita o acesso com status `401` e código `ACCOUNT_INACTIVE`
