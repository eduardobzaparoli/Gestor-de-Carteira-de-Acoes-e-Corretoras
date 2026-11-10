## 1. Contratos e autorização do backend

- [x] 1.1 Remover `password` de `AdminUserUpdateRequest`, criar o request exclusivo de redefinição e separar na entidade as operações de perfil e hash da senha.
- [x] 1.2 Ajustar `AdminUserService` para preservar a senha na edição cadastral e redefini-la em operação transacional própria usando `PasswordEncoder`.
- [x] 1.3 Proteger `AdminUserController` com `@PreAuthorize("hasRole('ADMIN')")` e expor `PUT /api/admin/users/{userId}/password` com resposta `204`.

## 2. Interface administrativa

- [x] 2.1 Manter senha apenas no cadastro e remover o campo do payload e formulário de edição cadastral.
- [x] 2.2 Adicionar ação separada de redefinição de senha usando os componentes e o design atuais, limpando o valor sensível ao fechar, concluir ou falhar.
- [x] 2.3 Atualizar os testes da tela para verificar os dois fluxos e a ausência de senha pré-carregada.

## 3. Verificação da política RBAC

- [x] 3.1 Atualizar testes do service para comprovar que a edição preserva o hash e que a redefinição armazena somente BCrypt válido.
- [x] 3.2 Atualizar os testes com JWT real para cobrir criação, edição, redefinição, senha antiga recusada, senha nova aceita e respostas sem campos sensíveis.
- [x] 3.3 Adicionar teste de segurança de método com `@WithMockUser`, demonstrando acesso administrativo permitido e investidor rejeitado com `403`.
- [x] 3.4 Confirmar pela suíte existente que `ADMIN` continua sem acesso financeiro, `INVESTOR` continua limitado aos próprios recursos e ausência de token retorna `401`.

## 4. Documentação e gates

- [x] 4.1 Documentar a matriz `Público`/`INVESTOR`/`ADMIN`, a diferença entre `401` e `403` e o endpoint administrativo separado de senha.
- [x] 4.2 Executar testes e build do frontend, suíte Maven/H2 e gate PostgreSQL quando o banco descartável estiver disponível.
- [x] 4.3 Atualizar o Graphify e validar a change com OpenSpec em modo estrito.
