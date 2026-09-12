## ADDED Requirements

### Requirement: Edição visual dos dados da carteira
O sistema SHALL oferecer, em cada cartão da visão geral de carteiras, uma ação identificada para editar a carteira. A ação MUST abrir um diálogo preenchido com o nome e a corretora atuais, permitir escolher somente entre as corretoras do investidor e enviar os dois campos à operação de atualização. Em caso de sucesso, a interface MUST refletir os novos dados sem exigir recarregamento manual; em caso de falha, MUST preservar o formulário e apresentar o erro público correspondente.

#### Scenario: Abertura do diálogo preenchido
- **WHEN** o investidor aciona Editar em um cartão de carteira
- **THEN** a interface abre um diálogo com o nome e a corretora atuais selecionados e sem dados residuais de outra carteira

#### Scenario: Edição concluída
- **WHEN** o investidor confirma nome e corretora válidos e a API responde com sucesso
- **THEN** a interface fecha o diálogo, informa a conclusão e atualiza o cartão e as consultas dependentes com os dados retornados

#### Scenario: Nenhuma corretora alternativa
- **WHEN** o investidor possui somente a corretora já vinculada
- **THEN** a interface ainda permite editar o nome mantendo essa corretora selecionada

#### Scenario: Formulário inválido
- **WHEN** o nome fica vazio ou nenhuma corretora válida está selecionada
- **THEN** a interface impede o envio e apresenta orientação em português no contexto do formulário

#### Scenario: Conflito ou recurso não encontrado
- **WHEN** a API rejeita a edição por nome duplicado, carteira inexistente ou corretora indisponível
- **THEN** a interface mantém os valores informados, apresenta a mensagem pública correspondente e permite corrigir ou cancelar

#### Scenario: Uso responsivo e acessível
- **WHEN** a edição é utilizada em tela pequena, modo claro, modo escuro ou por teclado
- **THEN** a ação, o diálogo, os campos, o foco e as mensagens permanecem visíveis e operáveis segundo o padrão atual da aplicação
