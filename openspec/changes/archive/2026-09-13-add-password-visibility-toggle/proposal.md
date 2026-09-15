## Why

Os campos de senha permanecem sempre mascarados, dificultando a conferência do valor digitado e aumentando erros em login, cadastro e alterações de credenciais. Um controle explícito de visibilidade melhora a usabilidade sem alterar as regras de autenticação ou expor senhas por padrão.

## What Changes

- Adicionar um botão com símbolo de olho em todos os campos de senha da aplicação.
- Permitir alternar individualmente cada campo entre senha ocultada e texto visível, preservando seu valor, foco, validação e preenchimento automático.
- Manter todo campo de senha oculto por padrão e restaurar esse estado quando o formulário for reiniciado, fechado ou aberto novamente.
- Fornecer nome acessível, estado perceptível, foco visível e operação por teclado para o controle em ambos os temas e nos diferentes tamanhos de tela.
- Cobrir login, cadastro público, criação e edição administrativa de usuário e alteração de senha no perfil do investidor.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `react-web-interface`: acrescentar o comportamento uniforme e acessível de exibição temporária de senha em todos os formulários que solicitam credenciais.

## Impact

- Frontend React: componente compartilhado de campo, formulários de autenticação, gestão administrativa de usuários e perfil do investidor.
- Estilos dos campos e controles nos modos claro e escuro, inclusive em layout responsivo.
- Testes automatizados dos componentes e fluxos que contêm senha.
- Nenhuma alteração de API, banco de dados, contrato de autenticação ou dependência externa.
