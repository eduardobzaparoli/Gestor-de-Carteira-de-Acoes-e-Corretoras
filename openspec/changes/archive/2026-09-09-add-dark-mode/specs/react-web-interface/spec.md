## ADDED Requirements

### Requirement: Seleção e persistência do tema visual
O sistema MUST oferecer modos claro e escuro em toda a interface web. O modo claro MUST preservar a apresentação laranja e clara vigente, enquanto o modo escuro MUST usar superfícies pretas ou cinza-escuras, textos legíveis e o laranja como cor principal. A preferência explícita do usuário MUST ser persistida localmente e restaurada sem depender de autenticação ou da API.

#### Scenario: Primeiro acesso sem preferência salva
- **WHEN** a aplicação inicia sem uma preferência de tema previamente escolhida
- **THEN** ela usa a preferência de esquema de cores do sistema operacional e adota o modo claro quando essa informação não está disponível

#### Scenario: Preferência previamente salva
- **WHEN** a aplicação inicia com uma escolha válida de modo claro ou escuro armazenada no navegador
- **THEN** ela aplica essa escolha antes de apresentar a interface, sem exibir momentaneamente o tema incorreto

#### Scenario: Alternância manual de tema
- **WHEN** o usuário aciona o controle de tema em uma tela pública, do investidor ou administrativa
- **THEN** a interface alterna imediatamente entre claro e escuro, informa de forma acessível qual modo será ativado e persiste a nova escolha no navegador

#### Scenario: Apresentação no modo claro
- **WHEN** o modo claro está ativo
- **THEN** a interface preserva a identidade visual clara e laranja existente, incluindo fundos, navegação, componentes, indicadores e gráficos

#### Scenario: Apresentação no modo escuro
- **WHEN** o modo escuro está ativo
- **THEN** páginas, painéis, modais, formulários, tabelas, menus, estados de interação e gráficos usam superfícies pretas ou cinza-escuras com acentos laranja e contraste legível

#### Scenario: Diferenciação semântica nos dois temas
- **WHEN** qualquer tema apresenta erro, perda, aviso, sucesso ou informação secundária
- **THEN** o estado continua identificável por contraste, texto, ícone ou rótulo, sem depender exclusivamente da cor principal

#### Scenario: Uso responsivo e por teclado
- **WHEN** o controle de tema é utilizado em desktop, dispositivo móvel ou por navegação de teclado
- **THEN** ele permanece visível, operável, com foco perceptível e sem ocultar ações essenciais da tela
