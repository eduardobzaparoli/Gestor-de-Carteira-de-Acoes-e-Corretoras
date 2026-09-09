## Why

A interface utiliza verde e tons esverdeados como identidade predominante, mas a direção visual desejada para o produto passou a ser laranja. A mudança deve ser global e consistente para evitar que componentes, gráficos ou estados mantenham resíduos da paleta anterior.

## What Changes

- Substituir a paleta principal verde/esmeralda por uma paleta laranja com variantes clara, principal e escura.
- Atualizar fundos, navegação, botões, links, foco, ícones, realces, sombras e microinterações derivados da identidade principal.
- Atualizar cores fixas usadas em gráficos e componentes que não passam atualmente pelos tokens globais.
- Representar estados positivos e de sucesso com tons laranjados, conforme a solicitação de eliminar as tonalidades verdes da interface.
- Preservar as cores semânticas de erro/perda em vermelho, aviso em tom distinto e informação em azul, mantendo contraste e diferenciação visual.
- Validar a nova identidade em telas públicas, área do investidor, área administrativa, desktop e dispositivos móveis.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `react-web-interface`: alterar a identidade cromática global da interface para que o laranja seja a cor principal e nenhuma tonalidade verde permaneça nos elementos do sistema.

## Impact

- Afeta os tokens e estilos globais em `frontend/src/styles.css`.
- Afeta cores fixas presentes em componentes React, especialmente gráficos, ícones, destaques de autenticação e cartões de carteira.
- Não altera endpoints, contratos da API, banco de dados, regras financeiras ou dependências.
- Exige atualização de testes visuais/estruturais relevantes, lint, testes automatizados, build e inspeção responsiva.
