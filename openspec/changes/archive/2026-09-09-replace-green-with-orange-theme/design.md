## Context

A identidade visual atual está distribuída entre tokens globais em `frontend/src/styles.css` e valores hexadecimais fixos em componentes de autenticação, carteiras e dashboard. Há ainda tons verdes embutidos em fundos, sombras, estados positivos, badges de sucesso, linhas e legendas de gráficos. Consulte `proposal.md` para a motivação e `specs/react-web-interface/spec.md` para o comportamento exigido.

## Goals / Non-Goals

**Goals:**

- Tornar a paleta laranja consistente em todas as jornadas e breakpoints.
- Centralizar a identidade em tokens semânticos para evitar novos valores verdes isolados.
- Manter contraste, foco visível e distinção dos estados financeiros e operacionais.
- Preservar a leitura dos gráficos após a alteração da cor inicial da série e da composição.

**Non-Goals:**

- Alterar estrutura, conteúdo, navegação ou regras funcionais das telas.
- Modificar o backend, contratos REST, dados ou cálculos financeiros.
- Redesenhar logotipos de empresas ou imagens externas, cujas cores pertencem às respectivas marcas.
- Remover vermelho de perdas/erros ou azul de informações quando essas cores têm função semântica.

## Decisions

### 1. Substituir tokens nominais por tokens semânticos

Os tokens `--emerald`, `--emerald-dark` e `--mint` serão substituídos por `--primary`, `--primary-dark` e `--primary-soft`. A base recomendada será laranja vivo, laranja escuro legível e laranja muito claro. Fundos, linhas, texto secundário e navegação também migrarão de matizes verde-acinzentadas para neutros quentes.

Isso evita conservar nomes incorretos depois da mudança e torna futuras alterações de tema mais simples. Manter nomes relacionados a verde e apenas trocar seus valores foi descartado porque perpetuaria uma representação enganosa no código.

### 2. Usar uma paleta quente com contraste verificável

A implementação usará como ponto de partida `#f97316` para a ação principal, `#c2410c` para texto e estados que exigem maior contraste e `#ffedd5` para superfícies suaves. A navegação escura migrará para marrons quase neutros, e o fundo claro passará a usar creme quente. Os valores finais poderão ser ajustados durante a inspeção visual para cumprir contraste e legibilidade.

Um laranja pastel como cor principal foi descartado porque teria contraste insuficiente em botões com texto branco e controles de foco.

### 3. Converter cores fixas e manter uma paleta categórica sem verde

Valores verdes fixos em gráficos, ícones e destaques serão substituídos por tokens ou constantes laranjas. A paleta do gráfico de composição começará pelo laranja e continuará com azul, roxo, vermelho, dourado, marrom e cinza azulado, sem variantes verdes. Logotipos externos não serão recoloridos.

Usar somente a troca dos três tokens foi descartado porque deixaria diversos resíduos verdes já identificados no código.

### 4. Preservar semântica com mais de um sinal visual

Ganho, sucesso, item ativo e ação principal poderão compartilhar a família laranja conforme solicitado. Perdas e erros continuarão vermelhos; informações continuarão azuis; avisos usarão âmbar suficientemente distinto. Badges, textos e ícones continuarão nomeando o estado para que a interpretação não dependa apenas de cor.

## Risks / Trade-offs

- [Laranja principal e aviso podem parecer semelhantes] → Usar tonalidades, fundos, ícones e rótulos distintos e revisar os componentes lado a lado.
- [A troca global pode reduzir contraste em texto pequeno] → Verificar foco, links, badges e textos sobre fundos claros e escuros durante a inspeção visual.
- [Cores verdes podem permanecer em valores fixos pouco óbvios] → Fazer varredura textual por tokens e códigos antigos depois da implementação.
- [Logotipos externos podem conter verde] → Excluir imagens de terceiros do critério, pois alterar marcas reduziria fidelidade e exigiria processamento adicional.

## Migration Plan

1. Introduzir os tokens semânticos da paleta laranja e migrar todas as referências CSS.
2. Atualizar cores fixas nos componentes e nas séries dos gráficos.
3. Varredura por tokens e códigos verdes antigos, mantendo apenas eventuais cores pertencentes a conteúdo externo.
4. Executar lint, testes, build, testes de navegador e inspeção visual em desktop e mobile.
5. Em caso de regressão, restaurar os tokens e constantes cromáticas anteriores sem impacto sobre dados ou API.
