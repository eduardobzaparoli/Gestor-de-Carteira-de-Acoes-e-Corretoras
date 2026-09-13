## Context

A identidade autenticada já é obtida por `GET /api/auth/me` e exibida no contexto global e no rodapé da navegação. O cadastro público e a administração de usuários já possuem normalização, validação, hash de senha e tradução do conflito de e-mail, mas não existe uma operação privada para o investidor alterar a própria conta. Consulte `proposal.md` para a motivação e os deltas de `user-authentication` e `react-web-interface` para o contrato observável.

## Goals / Non-Goals

**Goals:**

- Reutilizar as regras centrais de identidade sem permitir que o investidor altere papel ou estado.
- Tornar a atualização atômica e segura diante de e-mails duplicados e troca de senha.
- Manter o contexto React como fonte única dos dados públicos da sessão após a atualização.
- Cobrir o fluxo em H2, PostgreSQL e testes da interface sem depender de serviços externos.

**Non-Goals:**

- Recuperação de senha por e-mail, verificação externa de endereço ou autenticação multifator.
- Alteração de papel, estado, exclusão da conta ou encerramento de outras sessões.
- Edição de usuários pelo investidor ou acesso administrativo aos dados financeiros.

## Decisions

### Operação privada em `PUT /api/auth/me`

O endpoint existente de identidade será mantido para leitura e receberá uma operação `PUT` no mesmo recurso. O identificador da conta continuará vindo exclusivamente do token, e o corpo aceitará `name`, `email`, `currentPassword` e `newPassword`, sem `id`, `role` ou `status`. Isso mantém a semântica de substituição dos campos editáveis e evita uma rota de usuário parametrizada que poderia facilitar acesso horizontal. Uma rota `/api/users/{id}` foi descartada por ampliar desnecessariamente a superfície de autorização.

### Senha nova opcional com confirmação da senha atual

Nome e e-mail poderão ser atualizados sem reenviar credenciais. Quando `newPassword` estiver presente, `currentPassword` será obrigatória, validada contra o hash atual, e a nova senha deverá ser diferente. A confirmação da nova senha será apenas uma validação do frontend. Exigir a senha atual em toda edição foi descartado por criar atrito para correções não relacionadas à credencial; permitir troca somente com o token foi descartado por aumentar o impacto de uma sessão abandonada ou comprometida.

### Regra única de normalização e unicidade

O serviço privado reutilizará o normalizador e os limites do cadastro. Antes de salvar, verificará se o e-mail normalizado pertence a outra conta; a restrição única do banco continuará sendo a defesa definitiva para concorrência e será traduzida para `EMAIL_ALREADY_REGISTERED`. A escrita será transacional para impedir atualização parcial quando qualquer validação falhar.

### Atualização do contexto autenticado no frontend

A tela de perfil utilizará os mesmos componentes, tokens visuais e tratamento de erros existentes. Após resposta bem-sucedida, o contexto da sessão e o cache de identidade serão atualizados com o objeto retornado, fazendo o rodapé refletir nome e e-mail imediatamente. Campos de senha nunca serão hidratados e serão apagados após sucesso ou falha de envio.

### Navegação sem ambiguidade no rodapé

O bloco atual de identidade será convertido em controle navegável com semântica de link, foco visível e indicação acessível. A rota privada do perfil será disponibilizada apenas a investidores; administradores mantêm sua navegação administrativa atual.

## Risks / Trade-offs

- [Token emitido antes da mudança de senha permanece válido] → Manter o escopo atual de JWT stateless e documentar que a nova senha vale para autenticações futuras; revogação de sessões fica fora desta mudança.
- [Verificação prévia de e-mail sofre condição de corrida] → Preservar a restrição única no banco e traduzir sua violação dentro da operação transacional.
- [Campos sensíveis podem permanecer no navegador após erro] → Limpar senha atual, nova senha e confirmação em qualquer conclusão da tentativa, mantendo apenas nome e e-mail.
- [Duplicação das regras entre cadastro, administração e perfil] → Extrair ou reutilizar validação compartilhada somente onde isso reduzir divergência sem ampliar o escopo funcional.

## Migration Plan

1. Publicar a operação de atualização e seus testes sem alterar o contrato de `GET /api/auth/me`.
2. Publicar a rota e a ação de perfil no frontend consumindo o novo endpoint.
3. Validar H2 e PostgreSQL, especialmente a tradução da restrição única sob concorrência.
4. Em rollback, remover a rota visual e o método `PUT`; nenhuma migração de esquema é necessária.
