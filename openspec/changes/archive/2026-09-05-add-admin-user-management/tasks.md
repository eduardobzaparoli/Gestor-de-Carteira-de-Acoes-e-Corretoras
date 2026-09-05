## 1. Modelo de usuário e provisionamento

- [x] 1.1 Adicionar o estado ativo/inativo ao domínio, entidade, mapeador e respostas administrativas de usuário.
- [x] 1.2 Adaptar repositórios e validações para consultar usuários e contar administradores ativos.
- [x] 1.3 Criar propriedades tipadas e inicializador idempotente para o administrador inicial configurado por ambiente.
- [x] 1.4 Adicionar Flyway e configurar o baseline e a execução de migrações nos perfis de produção e integração PostgreSQL.
- [x] 1.5 Criar a migração versionada que adiciona `users.status` com padrão `ACTIVE` e manter somente os perfis H2 gerenciados pelo Hibernate.

## 2. Administração de usuários

- [x] 2.1 Criar DTOs administrativos de criação, atualização e resposta sem campos sensíveis.
- [x] 2.2 Implementar serviço administrativo para listar, consultar, criar e atualizar usuários reutilizando normalização, validação e hash de senha.
- [x] 2.3 Implementar desativação e reativação lógicas, preservando os recursos financeiros associados.
- [x] 2.4 Aplicar as proteções contra autodesativação e remoção ou rebaixamento do último administrador ativo.
- [x] 2.5 Criar controller com os endpoints `/api/admin/users` e respostas HTTP previstas na especificação.

## 3. Segurança e erros

- [x] 3.1 Revalidar existência e estado ativo da conta após a validação do JWT em toda requisição protegida.
- [x] 3.2 Bloquear login de conta inativa sem emitir token e manter o cadastro público limitado a `INVESTOR`.
- [x] 3.3 Adicionar exceções e mapeamentos JSON para usuário inexistente, conta inativa e conflitos de continuidade administrativa.
- [x] 3.4 Confirmar que rotas administrativas não retornam dados financeiros ou credenciais.

## 4. Verificação

- [x] 4.1 Criar testes unitários do serviço administrativo, incluindo normalização, duplicidade, status, senha e regras do último administrador.
- [x] 4.2 Criar testes web e de segurança para autorização de administrador, isolamento de investidores e contratos HTTP dos endpoints.
- [x] 4.3 Criar testes de autenticação para conta inativa no login e para JWT emitido antes da desativação.
- [x] 4.4 Verificar a migração em banco compatível e executar a suíte Maven completa e o teste de integração PostgreSQL quando o ambiente estiver configurado.
- [x] 4.5 Realizar roteiro manual: provisionar administrador, criar investidor, desativá-lo, confirmar bloqueio, reativá-lo e confirmar novo login.
