## ADDED Requirements

### Requirement: Gestão autenticada do próprio perfil
O sistema SHALL permitir que um investidor autenticado consulte e atualize somente o próprio perfil por `GET /api/auth/me` e `PUT /api/auth/me`. A atualização MUST aceitar nome e e-mail e MAY aceitar uma nova senha; quando uma nova senha for informada, a senha atual MUST ser fornecida e validada. Nome, e-mail e senha MUST seguir as mesmas regras de normalização, tamanho, formato, unicidade e proteção criptográfica do cadastro. A resposta MUST conter apenas `id`, `name`, `email` e `role`, sem senha, hash ou dados de outra conta.

#### Scenario: Consulta do próprio perfil
- **WHEN** um investidor autenticado solicita sua identidade
- **THEN** o sistema responde com status `200` e os dados públicos atuais da conta vinculada ao token

#### Scenario: Atualização de nome e e-mail
- **WHEN** um investidor autenticado envia nome e e-mail válidos e o e-mail normalizado não pertence a outra conta
- **THEN** o sistema persiste os dados normalizados, responde com status `200` e mantém o papel e o estado da conta inalterados

#### Scenario: Manutenção do próprio e-mail
- **WHEN** o investidor atualiza o perfil mantendo o mesmo e-mail após normalização
- **THEN** o sistema aceita a atualização sem tratar o próprio registro como duplicado

#### Scenario: E-mail utilizado por outra conta
- **WHEN** o investidor tenta adotar um e-mail normalizado pertencente a outra pessoa
- **THEN** o sistema responde com status `409` e código `EMAIL_ALREADY_REGISTERED`, preservando todos os dados anteriores

#### Scenario: Atualizações concorrentes com o mesmo e-mail
- **WHEN** duas contas atualizam concorrentemente seus perfis para o mesmo e-mail normalizado
- **THEN** somente uma atualização é persistida e a outra recebe status `409` com código `EMAIL_ALREADY_REGISTERED`

#### Scenario: Alteração de senha válida
- **WHEN** o investidor informa a senha atual correta e uma nova senha válida e diferente
- **THEN** o sistema substitui o hash armazenado, não devolve credenciais na resposta e passa a aceitar a nova senha nos próximos logins

#### Scenario: Senha atual incorreta ou ausente
- **WHEN** uma nova senha é informada sem a senha atual ou com uma senha atual incorreta
- **THEN** o sistema responde com status `400`, identifica `currentPassword`, não altera nenhum dado do perfil e não revela informação sobre o hash

#### Scenario: Nova senha igual à atual
- **WHEN** a nova senha corresponde à senha atual
- **THEN** o sistema responde com status `400`, identifica `newPassword` e preserva o perfil

#### Scenario: Dados do perfil inválidos
- **WHEN** nome, e-mail ou nova senha não atende às regras cadastrais
- **THEN** o sistema responde com status `400`, identifica os campos inválidos e não persiste atualização parcial

#### Scenario: Acesso sem identidade de investidor
- **WHEN** a operação é chamada sem autenticação válida ou por uma conta que não possui papel `INVESTOR`
- **THEN** o sistema responde respectivamente com `401` ou `403` e não consulta nem altera o perfil de terceiros
