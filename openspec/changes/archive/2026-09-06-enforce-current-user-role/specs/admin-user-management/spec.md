## MODIFIED Requirements

### Requirement: Criação e atualização administrativa de usuários
O sistema SHALL permitir que um administrador ativo crie contas por `POST /api/admin/users` e atualize nome, e-mail, papel e senha de uma conta existente por `PUT /api/admin/users/{userId}`. Nome e e-mail MUST obedecer às mesmas regras de normalização, validação e unicidade do cadastro público; senhas MUST obedecer aos mesmos limites e ser persistidas somente como hash. Uma alteração de papel de uma conta ativa MUST produzir efeito na autorização de seus tokens ainda válidos já na próxima requisição protegida.

#### Scenario: Administrador cria investidor
- **WHEN** um administrador ativo envia dados válidos e o papel `INVESTOR` para `POST /api/admin/users`
- **THEN** o sistema cria uma conta ativa e responde `201` com os dados administrativos públicos

#### Scenario: Administrador cria outro administrador
- **WHEN** um administrador ativo envia dados válidos e o papel `ADMIN` para `POST /api/admin/users`
- **THEN** o sistema cria uma conta ativa com papel `ADMIN` e responde `201`

#### Scenario: Administrador atualiza dados de usuário
- **WHEN** um administrador ativo envia uma atualização válida para um usuário existente
- **THEN** o sistema persiste os campos informados e responde `200` com os dados administrativos atualizados

#### Scenario: Papel atualizado afeta token existente
- **WHEN** um administrador altera o papel de uma conta ativa que possui um token válido emitido antes da alteração
- **THEN** o token existente passa a ter as permissões do novo papel na próxima requisição protegida, sem novo login

#### Scenario: E-mail administrativo duplicado
- **WHEN** uma criação ou atualização administrativa resulta em e-mail já usado após normalização
- **THEN** o sistema responde `409` com o código `EMAIL_ALREADY_REGISTERED`
