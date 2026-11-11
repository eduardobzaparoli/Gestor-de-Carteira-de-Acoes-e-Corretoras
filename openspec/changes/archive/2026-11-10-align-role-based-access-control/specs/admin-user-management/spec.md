## MODIFIED Requirements

### Requirement: Criação e atualização administrativa de usuários
O sistema SHALL permitir que um administrador ativo crie contas por `POST /api/admin/users`, atualize nome, e-mail e papel de uma conta existente por `PUT /api/admin/users/{userId}` e redefina sua senha exclusivamente por `PUT /api/admin/users/{userId}/password`. Nome e e-mail MUST obedecer às mesmas regras de normalização, validação e unicidade do cadastro público; a redefinição de senha MUST aceitar somente uma senha entre 8 e 72 caracteres, persistir apenas seu hash e responder `204` sem dados de usuário ou credenciais. Uma alteração de papel de uma conta ativa MUST produzir efeito na autorização de seus tokens ainda válidos já na próxima requisição protegida. Rebaixamentos concorrentes de contas `ADMIN` ativas MUST preservar ao menos uma conta `ADMIN` ativa ao término de todas as operações.

#### Scenario: Administrador cria investidor
- **WHEN** um administrador ativo envia dados válidos e o papel `INVESTOR` para `POST /api/admin/users`
- **THEN** o sistema cria uma conta ativa e responde `201` com os dados administrativos públicos

#### Scenario: Administrador cria outro administrador
- **WHEN** um administrador ativo envia dados válidos e o papel `ADMIN` para `POST /api/admin/users`
- **THEN** o sistema cria uma conta ativa com papel `ADMIN` e responde `201`

#### Scenario: Administrador atualiza dados de usuário
- **WHEN** um administrador ativo envia nome, e-mail e papel válidos para um usuário existente
- **THEN** o sistema persiste somente os dados cadastrais informados, mantém a senha atual e responde `200` com os dados administrativos atualizados

#### Scenario: Administrador redefine senha
- **WHEN** um administrador ativo envia uma nova senha válida para `PUT /api/admin/users/{userId}/password`
- **THEN** o sistema substitui o hash armazenado, responde `204` sem corpo e passa a aceitar apenas a nova senha em logins posteriores

#### Scenario: Redefinição administrativa inválida
- **WHEN** a nova senha possui menos de 8 ou mais de 72 caracteres
- **THEN** o sistema responde `400` em JSON, identifica o campo `password` e preserva o hash anterior

#### Scenario: Investidor tenta redefinir senha de outra conta
- **WHEN** um investidor autenticado chama `PUT /api/admin/users/{userId}/password`
- **THEN** o sistema responde `403` em JSON e não altera a credencial

#### Scenario: Papel atualizado afeta token existente
- **WHEN** um administrador altera o papel de uma conta ativa que possui um token válido emitido antes da alteração
- **THEN** o token existente passa a ter as permissões do novo papel na próxima requisição protegida, sem novo login

#### Scenario: Rebaixamentos concorrentes preservam um administrador
- **WHEN** operações concorrentes tentam rebaixar contas `ADMIN` ativas de modo que, juntas, removeriam todos os administradores ativos
- **THEN** o sistema permite somente as operações compatíveis com a permanência de ao menos um administrador ativo e rejeita as demais com `409` e código `LAST_ACTIVE_ADMIN`

#### Scenario: E-mail administrativo duplicado
- **WHEN** uma criação ou atualização administrativa resulta em e-mail já usado após normalização
- **THEN** o sistema responde `409` com o código `EMAIL_ALREADY_REGISTERED`
