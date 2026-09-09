## MODIFIED Requirements

### Requirement: Experiência responsiva e acessível
O sistema MUST manter os fluxos essenciais utilizáveis em telas móveis e desktop, com navegação por teclado, foco visível, rótulos de formulário, contraste legível, identidade visual predominantemente laranja e alternativas textuais para informações representadas em gráficos. Os elementos anteriormente verdes MUST adotar laranja ou tons laranjados, preservando cores distintas para erros, perdas, avisos e informações quando necessárias à compreensão semântica.

#### Scenario: Uso em tela pequena
- **WHEN** a largura disponível não comporta a visualização de desktop
- **THEN** navegação, cartões, formulários, tabelas e gráficos se reorganizam sem ocultar ações essenciais nem produzir rolagem horizontal na página

#### Scenario: Navegação por teclado
- **WHEN** uma pessoa percorre controles por teclado
- **THEN** a ordem de foco é coerente e modais, menus e formulários podem ser operados sem mouse

#### Scenario: Informação apresentada em gráfico
- **WHEN** composição ou evolução é exibida visualmente
- **THEN** os mesmos valores relevantes também podem ser lidos em legenda, resumo ou tabela acessível

#### Scenario: Identidade cromática laranja
- **WHEN** qualquer tela pública, do investidor ou administrativa é apresentada
- **THEN** botões primários, navegação, destaques, foco, estados positivos ou de sucesso, gráficos e demais elementos da identidade principal usam laranja ou tons laranjados, sem resíduos visuais verdes e com contraste legível

#### Scenario: Diferenciação de estados semânticos
- **WHEN** a interface apresenta erro, perda, aviso ou informação secundária
- **THEN** ela mantém diferenciação visual suficiente em relação à cor principal laranja por meio de cor, texto, ícone ou rótulo
