## MODIFIED Requirements

### Requirement: Autenticação por credenciais
O sistema SHALL disponibilizar `POST /api/auth/login` para autenticar um usuário ativo por e-mail e senha e, em caso de sucesso, emitir um token JWT do tipo Bearer com expiração configurável.

#### Scenario: Login realizado com sucesso
- **WHEN** um usuário ativo informa e-mail normalizado para uma conta existente e a senha correta
- **THEN** o sistema responde com status `200`, token JWT, tipo `Bearer`, instante de expiração e dados públicos do usuário

#### Scenario: E-mail inexistente
- **WHEN** uma pessoa tenta entrar com um e-mail não cadastrado
- **THEN** o sistema responde com status `401` e código `INVALID_CREDENTIALS`

#### Scenario: Senha incorreta
- **WHEN** uma pessoa tenta entrar com a senha incorreta de uma conta existente
- **THEN** o sistema responde com o mesmo status `401`, código e mensagem usados para e-mail inexistente

#### Scenario: Conta inativa tenta entrar
- **WHEN** uma pessoa informa as credenciais corretas de uma conta inativa
- **THEN** o sistema responde com status `401` e código `ACCOUNT_INACTIVE`, sem emitir token

### Requirement: Autenticação stateless por JWT
O sistema MUST validar assinatura, expiração, identidade e estado ativo da conta de tokens JWT em requisições protegidas, sem criar sessão no servidor.

#### Scenario: Token válido
- **WHEN** uma requisição protegida apresenta um token Bearer válido e não expirado de uma conta ativa
- **THEN** o sistema autentica a identidade e disponibiliza seu identificador e papel durante a requisição

#### Scenario: Token ausente
- **WHEN** uma requisição protegida não apresenta token Bearer
- **THEN** o sistema rejeita o acesso com status `401` e resposta JSON

#### Scenario: Token inválido ou expirado
- **WHEN** uma requisição protegida apresenta token adulterado, malformado ou expirado
- **THEN** o sistema rejeita o acesso com status `401` e resposta JSON sem revelar detalhes criptográficos

#### Scenario: Token emitido antes da desativação
- **WHEN** uma requisição protegida apresenta token válido de uma conta que foi desativada após a emissão
- **THEN** o sistema rejeita o acesso com status `401` e código `ACCOUNT_INACTIVE`

### Requirement: Persistência compatível do estado de conta
O sistema SHALL manter o estado de conta persistido de forma compatível com H2 e PostgreSQL. Em produção, a evolução de `users.status` MUST ser aplicada por uma migração Flyway versionada, que preserve os usuários existentes como ativos e seja registrada no histórico do banco.

#### Scenario: Banco PostgreSQL existente recebe a migração
- **WHEN** uma versão da aplicação com esta mudança inicia em produção ou no perfil de integração PostgreSQL contra um banco existente que ainda não possui histórico Flyway
- **THEN** o sistema estabelece o baseline sem recriar dados e aplica uma única vez a migração que inclui `users.status` com valor `ACTIVE` às contas existentes

#### Scenario: Ambiente H2 de desenvolvimento ou teste
- **WHEN** a aplicação inicia com perfil de desenvolvimento ou teste baseado em H2
- **THEN** o Flyway não executa a migração de produção e o Hibernate continua responsável pelo ciclo de vida do esquema desse ambiente
