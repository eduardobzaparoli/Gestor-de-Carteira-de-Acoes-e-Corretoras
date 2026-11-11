## Why

O sistema já possui autenticação JWT, papéis e isolamento por proprietário, mas a autorização administrativa ainda depende principalmente do padrão de URL e a edição cadastral mistura alteração de perfil com redefinição de senha. A Aula 14 pede uma política RBAC explícita, verificável em nível de método e com contratos que reduzam a exposição acidental de credenciais.

## What Changes

- Formalizar a matriz de acesso do domínio: rotas públicas explícitas, capacidades financeiras exclusivas de `INVESTOR` sobre seus próprios dados e gestão de usuários exclusiva de `ADMIN`.
- Proteger a API administrativa também em nível de método com `@PreAuthorize`, mantendo a regra HTTP existente como primeira barreira.
- **BREAKING**: remover a senha do contrato `PUT /api/admin/users/{userId}` e limitar essa operação a nome, e-mail e papel.
- Adicionar `PUT /api/admin/users/{userId}/password` para redefinição administrativa de senha, com resposta `204`, validação de 8 a 72 caracteres e persistência exclusiva do hash BCrypt.
- Separar, na interface administrativa, a edição cadastral da ação de redefinir senha, sem exibir senha ou hash em respostas ou formulários preenchidos.
- Cobrir permissões e negações com testes de segurança de método e com o fluxo JWT real, demonstrando respostas `200`/`201`/`204`, `401` e `403`.
- Preservar as proteções existentes de conta ativa, papel atualizado imediatamente, último administrador ativo e propriedade horizontal dos recursos financeiros.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `user-authentication`: explicitar a matriz RBAC do produto e a separação entre autenticação, autorização vertical e propriedade horizontal.
- `admin-user-management`: separar atualização cadastral e redefinição administrativa de senha, além de exigir autorização administrativa também no método.

## Impact

- Backend: `AdminUserController`, `AdminUserService`, DTOs administrativos, método de atualização da entidade e testes de segurança/integração.
- Frontend: formulário e chamadas da tela de gestão de usuários, mantendo o design atual e o componente de senha já existente.
- API: alteração compatível coordenada entre frontend e backend, sem migração de banco e sem nova dependência.
- Segurança: nenhuma ampliação de privilégio; `ADMIN` continua sem acesso aos dados financeiros privados e `INVESTOR` continua limitado aos próprios recursos.
