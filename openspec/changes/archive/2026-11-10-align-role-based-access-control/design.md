## Context

O projeto já usa `@EnableMethodSecurity`, converte o papel persistido em `ROLE_INVESTOR` ou `ROLE_ADMIN` a cada requisição e restringe os controllers financeiros ao investidor. A administração possui DTOs seguros e regras adicionais para conta ativa e continuidade administrativa, mas sua autorização está concentrada no padrão `/api/admin/**` e o mesmo contrato de edição cadastral exige uma senha nova.

## Goals / Non-Goals

**Goals:**

- Aplicar defesa em profundidade à administração sem duplicar regras em cada método.
- Separar a atualização cadastral da redefinição de credencial em backend e frontend.
- Demonstrar a matriz RBAC do produto com testes de método e JWT real.
- Preservar o isolamento horizontal já implementado pelos services financeiros.

**Non-Goals:**

- Renomear `INVESTOR` para o papel didático `USER`.
- Permitir que administradores consultem ou alterem investimentos.
- Introduzir permissões granulares, revogação de tokens, auditoria ou rate limiting.
- Alterar banco de dados, claims JWT ou dependências.

## Decisions

### Manter dois níveis complementares de autorização

O matcher `/api/admin/**` continuará bloqueando o acesso antes do controller, e `AdminUserController` receberá `@PreAuthorize("hasRole('ADMIN')")` na classe. Os controllers financeiros manterão a proteção de classe por `INVESTOR` e os services continuarão validando o proprietário.

Alternativa considerada: anotar individualmente todos os métodos. Foi descartada porque todos os métodos do controller administrativo possuem a mesma política e a anotação de classe é menor e menos sujeita a omissões.

### Preservar os papéis do domínio

A matriz usará `INVESTOR` e `ADMIN`, não `USER` e `ADMIN`. `INVESTOR` administra os próprios investimentos; `ADMIN` administra identidades e não herda acesso financeiro. Essa separação segue o produto e evita que o papel privilegiado exponha dados privados desnecessariamente.

Alternativa considerada: fazer `ADMIN` herdar permissões de investidor. Foi descartada por contrariar o princípio do menor privilégio e a especificação do produto.

### Separar perfil administrativo e senha

`AdminUserUpdateRequest` manterá somente nome, e-mail e papel. Um novo request com apenas `password` atenderá `PUT /api/admin/users/{userId}/password`; o service terá uma operação transacional específica e a entidade exporá operações distintas para dados cadastrais e hash da senha. A criação continuará recebendo senha normalmente.

Alternativa considerada: tornar a senha opcional no update atual. Foi descartada porque mantém dois objetivos no mesmo contrato e dificulta distinguir ausência de senha de intenção de redefinição.

### Coordenar a mudança com a interface

O modal de criação continuará contendo senha. Na edição, o formulário mostrará apenas nome, e-mail e papel, com uma ação separada para redefinir senha usando o componente de campo existente. Após sucesso ou erro, valores de senha serão limpos.

### Testar cada camada pelo mecanismo adequado

Um teste com `@WithMockUser` verificará a anotação de método sem depender da emissão JWT. Os testes de integração existentes continuarão usando tokens reais para comprovar `401`, `403`, papel atualizado, BCrypt e ausência de campos sensíveis. O teste da redefinição confirmará que a senha antiga deixa de autenticar e a nova passa a autenticar.

## Risks / Trade-offs

- [Frontend antigo envia `password` no update] → Alterar backend e frontend na mesma change e tratar o contrato como breaking até a implantação conjunta.
- [Redefinir senha não revoga tokens já emitidos] → Documentar a limitação; tokens continuam sujeitos à expiração, conta ativa e papel persistido.
- [Proteções HTTP e de método divergem no futuro] → Manter testes específicos de autorização para as duas barreiras.
- [Senha permanece em memória no navegador após uma tentativa] → Limpar o estado do campo ao concluir, falhar ou fechar o formulário.

## Migration Plan

1. Publicar backend e frontend juntos, pois o contrato de edição muda.
2. Executar testes H2 e, no pipeline, o gate PostgreSQL descartável.
3. Não há migração de dados; rollback consiste em restaurar o contrato e a interface anteriores.
