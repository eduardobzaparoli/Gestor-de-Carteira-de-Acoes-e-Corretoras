## 1. Contrato e domínio do perfil

- [x] 1.1 Criar o DTO de atualização privada com nome, e-mail, senha atual opcional e nova senha opcional, preservando normalização e limites já adotados no cadastro.
- [x] 1.2 Adicionar ao modelo e aos mapeamentos a operação necessária para atualizar nome, e-mail e hash de senha sem permitir alteração de papel ou estado.
- [x] 1.3 Implementar no serviço autenticado a atualização transacional do próprio investidor, incluindo validação da senha atual, proibição de reutilizar a mesma senha e resposta pública sem dados sensíveis.
- [x] 1.4 Reutilizar a validação de unicidade do e-mail sem rejeitar o endereço da própria conta e traduzir também a violação concorrente da restrição do banco para `EMAIL_ALREADY_REGISTERED`.

## 2. API privada e segurança

- [x] 2.1 Expor `PUT /api/auth/me` usando exclusivamente a identidade do token e retornar o mesmo formato público de `GET /api/auth/me`.
- [x] 2.2 Configurar autorização para permitir a operação somente a investidores autenticados e manter respostas JSON `401` e `403` nos demais casos.
- [x] 2.3 Integrar erros de senha atual, nova senha e e-mail duplicado ao tratamento centralizado, com códigos e campos estáveis e sem vazamento de credenciais.
- [x] 2.4 Atualizar o guia da API com requisição, resposta, validações e erros da edição de perfil, sem alterar `docs/product-spec.md` sem autorização explícita.

## 3. Tela de perfil do investidor

- [x] 3.1 Adicionar tipos e cliente HTTP para atualizar o perfil e permitir que o contexto autenticado substitua os dados públicos da sessão após o sucesso.
- [x] 3.2 Transformar o bloco de nome e e-mail do investidor no rodapé da navegação em um link acessível para uma rota privada de perfil, sem alterar o fluxo administrativo.
- [x] 3.3 Criar a tela de perfil seguindo os componentes e tokens visuais atuais, com nome e e-mail preenchidos e campos vazios para senha atual, nova senha e confirmação.
- [x] 3.4 Implementar validação local em português, confirmação da nova senha, associação de erros por campo, prevenção de envio duplicado e limpeza de valores sensíveis após cada tentativa.
- [x] 3.5 Atualizar imediatamente formulário, cabeçalho/rodapé e caches dependentes após sucesso, preservando nome e e-mail informados nas falhas corrigíveis.
- [x] 3.6 Garantir comportamento responsivo, navegação por teclado, foco visível e contraste adequado nos temas claro e escuro.

## 4. Verificação

- [x] 4.1 Adicionar testes unitários do serviço para atualização simples, manutenção do próprio e-mail, e-mail de terceiro, senha válida, senha atual ausente/incorreta, senha repetida e atomicidade.
- [x] 4.2 Adicionar testes de integração da API para autorização, normalização, validação, ausência de dados sensíveis e concorrência da unicidade em H2.
- [x] 4.3 Executar os cenários relevantes contra o banco PostgreSQL descartável, confirmando a tradução da restrição única e a persistência do novo hash.
- [x] 4.4 Adicionar testes da interface para navegação pelo usuário, preenchimento, atualização do contexto, validações, mensagens em português, limpeza de senhas, temas e tela pequena.
- [x] 4.5 Executar testes Maven relevantes e completos, formatação, lint, testes e build do frontend, validar a change em modo estrito e atualizar o índice Graphify.
