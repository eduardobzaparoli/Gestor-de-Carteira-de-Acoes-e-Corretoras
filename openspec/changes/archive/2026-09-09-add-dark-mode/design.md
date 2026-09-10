## Context

Veja `proposal.md` para a motivação. O frontend React concentra a identidade visual em `styles.css`, mas ainda contém algumas cores de gráficos e destaques nos componentes. Não existe gerenciamento de tema, e o visual atual deve permanecer como referência exata do modo claro.

## Goals / Non-Goals

**Goals:**

- Aplicar o tema de modo global e independente da sessão autenticada.
- Evitar a exibição momentânea do tema incorreto durante a inicialização.
- Centralizar as cores dependentes de tema em tokens semânticos reutilizáveis.
- Manter controles, gráficos e estados acessíveis e legíveis em desktop e mobile.
- Implementar sem nova dependência de produção.

**Non-Goals:**

- Salvar a preferência no perfil do usuário ou no backend.
- Permitir paletas personalizadas além de claro e escuro.
- Alterar logotipos externos, regras financeiras, dados ou contratos da API.
- Redesenhar a composição e a navegação das telas.

## Decisions

### Estado global de tema com API própria

Será criado um provedor de tema com o tipo restrito `light | dark`, responsável por resolver o valor inicial, aplicar a seleção no elemento raiz e expor a alternância aos componentes. Isso evita estado duplicado entre autenticação, área do investidor e administração.

Alternativa considerada: controlar o tema apenas com CSS e um botão isolado. Foi descartada porque dificultaria persistência, testes e sincronização do estado visual e do texto acessível do controle.

### Persistência local e preferência do sistema somente como valor inicial

A escolha explícita será salva sob uma chave versionável do aplicativo no `localStorage`. Na ausência dela, a inicialização consultará `prefers-color-scheme: dark`; se o recurso não existir, usará claro. Depois de uma escolha manual, mudanças do sistema operacional não substituirão a preferência do usuário.

Alternativa considerada: sempre acompanhar o sistema operacional. Foi descartada porque contraria a expectativa de que uma escolha manual seja estável.

### Aplicação antecipada no elemento raiz

Um pequeno inicializador executado antes da montagem do React definirá `data-theme` e `color-scheme` no elemento `html`. O provedor reutilizará a mesma regra de resolução, evitando flash do tema incorreto e mantendo os controles nativos coerentes.

Alternativa considerada: aplicar o tema somente em um efeito do React. Foi descartada porque a primeira pintura poderia ocorrer com o modo claro antes da correção.

### Tokens semânticos com sobrescrita por tema

Os valores atuais permanecerão nos tokens padrão de `:root`, preservando o modo claro. O seletor `:root[data-theme="dark"]` sobrescreverá fundos, superfícies, textos, bordas, sombras, campos e estados interativos com pretos e cinzas escuros, mantendo as variantes laranja como acento. Literais dependentes do tema serão migrados para tokens; cores de erro, aviso e informação terão variantes legíveis em ambos os modos.

Alternativa considerada: duplicar todas as regras CSS sob um seletor escuro. Foi descartada pelo risco de divergência e pelo custo de manutenção.

### Gráficos e componentes sensíveis ao tema

As cores de séries, eixos, legendas, grades e tooltips serão obtidas de tokens do tema, diretamente por CSS ou por uma pequena camada que leia os valores computados. A troca de tema provocará nova renderização quando necessária. Logotipos de ativos permanecerão inalterados.

Alternativa considerada: manter as cores atuais fixas. Foi descartada porque eixos e textos claros podem perder contraste sobre superfícies escuras.

### Controle único e acessível

Um componente reutilizável com ícones de sol e lua será inserido nas telas públicas e na estrutura autenticada, incluindo o cabeçalho móvel. O rótulo acessível descreverá a ação de destino, como “Ativar modo escuro”, e o foco seguirá o padrão visual da aplicação.

## Risks / Trade-offs

- [Cores literais escaparem da tematização] → Cobrir fontes React e CSS com varredura e testes de contrato dos tokens.
- [Contraste insuficiente em gráficos ou estados secundários] → Validar visualmente fluxos representativos e usar tokens específicos para texto, grade e estados semânticos.
- [Flash de tema incorreto na abertura] → Aplicar a preferência antes do carregamento do React e testar recarga com tema persistido.
- [Ambientes sem `localStorage` ou `matchMedia`] → Tratar falhas de acesso e usar modo claro como fallback seguro.
- [Diferenças entre controles nativos dos navegadores] → Definir `color-scheme` e executar testes de navegador em desktop e mobile.

## Migration Plan

1. Introduzir o resolvedor, o provedor e o inicializador antecipado sem alterar o tema padrão atual.
2. Converter os estilos para tokens semânticos e adicionar as sobrescritas escuras.
3. Integrar o seletor nas estruturas públicas e autenticadas e adaptar gráficos.
4. Executar testes automatizados e inspeção visual nos dois temas e resoluções.

O rollback consiste em remover o provedor, o inicializador, o controle e as sobrescritas escuras; os valores padrão de `:root` continuam representando integralmente o modo claro.
