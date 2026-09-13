## ADDED Requirements

### Requirement: Controle de visibilidade em campos de senha
O sistema SHALL apresentar em todo campo que solicita senha um controle com símbolo de olho para alternar somente aquele campo entre conteúdo mascarado e visível. Todo campo de senha MUST iniciar oculto, o controle MUST preservar o valor, a validação e a finalidade de preenchimento automático do campo, e a interface MUST restaurar o estado oculto quando o respectivo formulário for reiniciado ou aberto novamente.

#### Scenario: Campos abrangidos
- **WHEN** a interface apresenta um campo de senha no login, cadastro público, criação ou edição administrativa de usuário ou perfil do investidor
- **THEN** o campo exibe um controle de visibilidade associado exclusivamente a ele

#### Scenario: Exibição temporária da senha
- **WHEN** a pessoa aciona o controle de um campo de senha oculto
- **THEN** o conteúdo daquele campo se torna legível sem alterar seu valor, foco, validação ou dados enviados pelo formulário

#### Scenario: Nova ocultação da senha
- **WHEN** a pessoa aciona o controle de um campo cuja senha está visível
- **THEN** o conteúdo daquele campo volta a ser mascarado e permanece inalterado

#### Scenario: Alternância independente entre campos
- **WHEN** um formulário possui mais de um campo de senha e a pessoa altera a visibilidade de um deles
- **THEN** os demais campos mantêm seus próprios estados de visibilidade

#### Scenario: Estado seguro ao iniciar ou reiniciar
- **WHEN** um formulário com senha é aberto, reaberto, cancelado ou reiniciado após uma operação
- **THEN** todos os seus campos de senha são apresentados no estado oculto, independentemente do estado anterior

#### Scenario: Controle acessível
- **WHEN** a pessoa percorre ou aciona o controle por teclado ou tecnologia assistiva
- **THEN** o controle recebe foco visível, informa em português se a ação disponível é mostrar ou ocultar a senha e comunica seu estado atual sem depender apenas do ícone

#### Scenario: Apresentação responsiva nos dois temas
- **WHEN** um campo de senha é exibido em tela reduzida, modo claro ou modo escuro
- **THEN** o controle permanece alinhado dentro do campo, não encobre o texto nem as mensagens de validação e mantém contraste legível conforme o design da aplicação
