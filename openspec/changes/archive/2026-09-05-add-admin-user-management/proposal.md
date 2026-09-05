## Why

O produto prevê que administradores gerenciem os usuários do sistema sem acessar os dados privados dos investidores. Embora o papel `ADMIN` e a proteção de rotas administrativas já existam, ainda não há uma capacidade para provisionar administradores ou administrar contas de forma segura.

## What Changes

- Adicionar uma API administrativa, exclusiva para `ADMIN`, para listar, consultar, criar, atualizar, desativar e reativar usuários.
- Adicionar o estado de conta ativa ou inativa, preservando os dados e o histórico financeiro do usuário desativado.
- Impedir login e acesso autenticado de usuários inativos, inclusive quando apresentam um JWT emitido antes da desativação.
- Provisionar de modo idempotente o primeiro administrador a partir de configuração de ambiente, sem expor credenciais em respostas ou código.
- Proteger a continuidade administrativa: um administrador não poderá desativar a própria conta nem remover o último administrador ativo.
- Manter o cadastro público restrito ao papel `INVESTOR` e preservar o isolamento dos dados financeiros dos investidores.
- Versionar a evolução do esquema de produção com Flyway, incluindo a migração do estado das contas já existentes.

## Capabilities

### New Capabilities

- `admin-user-management`: administração segura do ciclo de vida de contas de usuários por administradores.

### Modified Capabilities

- `user-authentication`: autenticação e acesso passam a considerar o estado ativo da conta e o provisionamento inicial de administrador.

## Impact

- Código afetado: domínio, entidade, DTOs, mapeadores, repositório, serviços de autenticação e administração, segurança e tratamento de erros.
- API: novos endpoints sob `/api/admin/users`; `POST /api/auth/login` e requisições protegidas passam a rejeitar contas inativas.
- Banco: inclusão do estado da conta de usuário, compatível com H2 e PostgreSQL, e migração versionada para bancos PostgreSQL existentes em produção e integração.
- Configuração: novas variáveis de ambiente para o provisionamento inicial de administrador.
