## Context

O modelo de usuário já possui identificador, nome, e-mail único, hash de senha e papel (`INVESTOR` ou `ADMIN`). O cadastro público cria somente investidores; a segurança já reserva `/api/admin/**` para administradores. Usuários são proprietários de corretoras e carteiras, portanto a exclusão física de uma conta comprometeria referências e histórico.

As regras de comportamento estão em `specs/admin-user-management/spec.md` e as mudanças de autenticação em `specs/user-authentication/spec.md`.

## Goals / Non-Goals

**Goals:**

- Disponibilizar administração de contas sem expor recursos financeiros privados.
- Permitir desativação reversível, sem apagar histórico.
- Revogar imediatamente o acesso de uma conta desativada, mesmo com JWT ainda válido.
- Viabilizar o primeiro administrador sem abrir uma rota pública privilegiada.

**Non-Goals:**

- Não criar interface web administrativa.
- Não permitir que administradores atuem em carteiras, corretoras ou lançamentos de terceiros.
- Não implementar exclusão física de usuários, recuperação de senha por e-mail ou auditoria detalhada de alterações nesta mudança.
- Não alterar a emissão, algoritmo ou duração configurada do JWT.

## Decisions

### Estado de conta separado do papel

Será introduzido `UserStatus` com os valores `ACTIVE` e `INACTIVE`, persistido no usuário e incluído somente nas respostas administrativas. Papel define autorização; status define se a identidade pode autenticar e usar recursos protegidos.

Alternativa considerada: excluir fisicamente a conta. Foi descartada porque corretoras e carteiras possuem referência ao usuário, e a remoção destruiria ou inviabilizaria o histórico de investimentos.

### Endpoints administrativos e DTOs dedicados

Um controller sob `/api/admin/users` chamará um serviço administrativo próprio. Serão usados DTOs específicos para criação, atualização e resposta administrativa; o DTO público de autenticação permanecerá sem status e sem dados sensíveis. As operações serão:

- `GET /api/admin/users`
- `GET /api/admin/users/{userId}`
- `POST /api/admin/users`
- `PUT /api/admin/users/{userId}`
- `DELETE /api/admin/users/{userId}` para desativação lógica
- `POST /api/admin/users/{userId}/reactivate`

Alternativa considerada: reutilizar o controller e DTO de cadastro público. Foi descartada para impedir que a rota pública passe a aceitar papel, status ou operações administrativas por acidente.

### Revalidação da conta em cada requisição autenticada

Após a validação criptográfica do JWT, a autenticação consultará a conta pelo `subject` e exigirá que ela exista e esteja ativa. Assim, uma desativação tem efeito imediato em vez de aguardar a expiração do token. A verificação manterá a aplicação stateless: não haverá sessão nem lista de tokens revogados.

Alternativa considerada: aceitar o token até expirar. Foi descartada porque contraria a desativação efetiva da conta e deixaria até uma hora de acesso após a ação administrativa.

### Proteções de continuidade administrativa

O serviço executará as verificações em transação: o administrador autenticado não poderá desativar a si próprio, e qualquer desativação ou mudança de papel que removeria o último administrador ativo retornará conflito. A reativação de usuários não reduz essa proteção.

### Provisionamento do administrador inicial por ambiente

Uma configuração tipada receberá nome, e-mail e senha iniciais. Um inicializador transacional criará a conta apenas se ainda não existir usuário com o e-mail normalizado, usando o mesmo normalizador e codificador de senha já empregados no cadastro. As variáveis serão opcionais para desenvolvimento; quando ausentes, nenhum administrador é criado automaticamente.

Alternativa considerada: tornar o primeiro usuário público um administrador. Foi descartada por criar elevação de privilégio indevida.

### Migração versionada do estado de usuário em produção

O Flyway será responsável por aplicar a alteração estrutural de `users.status` em produção. A migração adicionará a coluna com valor padrão `ACTIVE`, para que todas as contas existentes continuem aptas a acessar o sistema após a implantação.

Como os bancos de produção atuais foram originalmente criados pelo Hibernate e ainda não possuem histórico de migrações, a configuração de produção fará o baseline do esquema existente antes de aplicar a primeira migração desta mudança. Isso preserva as tabelas e os dados já armazenados.

Os perfis H2 de desenvolvimento e teste permanecerão com Flyway desabilitado: eles usam bancos descartáveis ou mantidos pelo Hibernate e não devem executar uma migração que pressupõe a tabela `users` já existente. O perfil PostgreSQL de integração executará o mesmo baseline e as mesmas migrações de produção para validar a atualização de um esquema já existente antes dos testes de integração.

Alternativa considerada: permitir que o Hibernate atualize o banco de produção. Foi descartada porque `ddl-auto=validate` protege o ambiente produtivo contra alterações estruturais implícitas e não registra a versão aplicada do esquema.

## Risks / Trade-offs

- [Consulta ao banco em cada chamada autenticada] → A verificação usa busca simples por chave primária; testes de integração cobrirão a compatibilidade. Caso a carga futura exija, o estado poderá ser cacheado em mudança própria sem enfraquecer a regra.
- [Credencial inicial inadequadamente configurada] → A senha ficará exclusivamente em variável de ambiente, será validada antes do uso e jamais será registrada em logs ou respostas.
- [Concorrência ao alterar administradores] → As operações críticas serão transacionais e validarão a contagem de administradores ativos antes da persistência; testes cobrirão as proteções de autoalteração e último administrador.
- [Usuário inativo com dados históricos] → A desativação não altera entidades financeiras; o acesso do próprio usuário é que fica bloqueado.
- [Banco de produção sem histórico Flyway] → O baseline registra o ponto inicial sem recriar o esquema; a migração subsequente adiciona somente o estado de conta.

## Migration Plan

1. Adicionar Flyway e uma migração versionada que cria a coluna `users.status` com valor padrão `ACTIVE` para as contas existentes em PostgreSQL.
2. Configurar baseline do Flyway para que bancos PostgreSQL já criados pelo Hibernate recebam a nova migração sem recriar tabelas ou dados.
3. Manter Flyway desabilitado somente nos perfis H2 de desenvolvimento e teste; habilitá-lo também no perfil PostgreSQL de integração para validar a migração contra um esquema existente.
4. Configurar, em ambientes que precisem de administração, as variáveis do administrador inicial antes da primeira inicialização.
5. Implantar a aplicação; o Flyway aplica a migração uma única vez e o inicializador cria a conta somente quando o e-mail configurado ainda não existe.
6. Em caso de rollback do código, reativar administradores necessários diretamente no banco apenas se a versão anterior não reconhecer o novo estado; nenhum dado financeiro é removido pela mudança.

## Open Questions

Nenhuma. A política de desativação lógica, provisionamento por ambiente e revogação imediata do acesso foram definidos para esta mudança.
