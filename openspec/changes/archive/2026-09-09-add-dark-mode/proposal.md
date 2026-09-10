## Why

A interface possui apenas o tema claro, o que limita o conforto visual em ambientes com pouca luz e impede que o usuário adapte a apresentação à sua preferência. O modo escuro deve complementar a identidade laranja existente sem alterar fluxos, dados ou regras de negócio.

## What Changes

- Adicionar um modo escuro com superfícies pretas ou cinza-escuras e o laranja como cor principal de ações, destaques e gráficos.
- Manter o visual atual inalterado como modo claro.
- Disponibilizar um controle acessível para alternar o tema nas telas públicas, do investidor e administrativas.
- Persistir a escolha no navegador e, quando ainda não houver escolha, usar a preferência de esquema de cores do sistema operacional.
- Aplicar o tema antes da primeira renderização visível para evitar uma troca brusca de cores ao abrir ou recarregar a aplicação.
- Preservar contraste, foco visível e diferenciação semântica de erros, perdas, avisos e informações nos dois modos.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `react-web-interface`: incluir seleção, persistência e aplicação acessível dos modos claro e escuro em toda a interface web.

## Impact

- Afeta o gerenciamento de estado do frontend, a inicialização da aplicação, os componentes de navegação e autenticação, os tokens globais de estilo e as cores dos gráficos.
- Exige testes automatizados para resolução e persistência do tema, além de validação visual em desktop e mobile.
- Não altera endpoints, contratos da API, banco de dados, dependências externas nem regras financeiras.
