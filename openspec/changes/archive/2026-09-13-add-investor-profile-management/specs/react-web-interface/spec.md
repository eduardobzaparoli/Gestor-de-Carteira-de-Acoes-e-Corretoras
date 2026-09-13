## ADDED Requirements

### Requirement: Visualização e edição do perfil do investidor
O sistema SHALL transformar o bloco de identidade do investidor no rodapé da navegação em uma ação acessível para abrir sua tela de perfil. A tela MUST carregar os dados públicos atuais, permitir editar nome, e-mail e opcionalmente senha, manter o design vigente nos modos claro e escuro e atualizar a identidade exibida na aplicação imediatamente após o sucesso.

#### Scenario: Acesso pelo nome do investidor
- **WHEN** o investidor aciona por mouse ou teclado o bloco com seu nome e e-mail no canto inferior esquerdo
- **THEN** a aplicação navega para a tela de perfil e apresenta nome e e-mail preenchidos com os dados atuais

#### Scenario: Visualização sem edição de senha
- **WHEN** a tela de perfil é aberta
- **THEN** a senha não é exibida nem preenchida e os campos de senha atual e nova senha permanecem opcionais enquanto não houver intenção de alterá-la

#### Scenario: Edição concluída
- **WHEN** o investidor envia uma atualização válida e a API responde com sucesso
- **THEN** a tela apresenta confirmação em português e nome e e-mail são atualizados no formulário e no rodapé da navegação sem recarregamento manual

#### Scenario: Intenção de alterar senha
- **WHEN** o investidor informa uma nova senha
- **THEN** a interface exige a senha atual, permite confirmar a nova senha e não envia a confirmação como parte do contrato da API

#### Scenario: Confirmação da nova senha divergente
- **WHEN** a confirmação local não corresponde à nova senha
- **THEN** a interface impede o envio, identifica o campo em português e preserva os demais valores

#### Scenario: E-mail já utilizado
- **WHEN** a API rejeita a atualização com código `EMAIL_ALREADY_REGISTERED`
- **THEN** a interface mantém o formulário, associa a mensagem em português ao campo de e-mail e permite a correção

#### Scenario: Senha atual inválida
- **WHEN** a API rejeita a troca porque a senha atual está ausente ou incorreta
- **THEN** a interface mantém os dados não sensíveis, limpa os campos de senha e apresenta orientação em português junto ao campo correspondente

#### Scenario: Falha inesperada
- **WHEN** a atualização falha por rede ou erro público desconhecido
- **THEN** a interface preserva nome e e-mail informados, limpa valores de senha e oferece nova tentativa segura

#### Scenario: Perfil responsivo e acessível
- **WHEN** a tela é usada em tamanho reduzido, modo claro, modo escuro ou navegação por teclado
- **THEN** campos, ações, estados, foco, contraste e mensagens permanecem visíveis e operáveis segundo o padrão atual da aplicação
