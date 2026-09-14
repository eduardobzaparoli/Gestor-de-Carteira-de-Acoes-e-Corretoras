## Context

O diálogo de lançamento é implementado em `PortfolioPage.tsx`. Ele consulta o catálogo privado, filtra por mercado, tipo e texto e renderiza cada resultado como um botão. A linha já instancia o componente compartilhado `AssetLogo`, mas também renderiza a última cotação do catálogo à direita. A cotação que realmente prepara o lançamento é obtida por uma operação separada somente após a seleção.

O `AssetLogo` centraliza as URLs por mercado, tenta formatos ou URLs alternativas e mantém um marcador local enquanto a imagem não carrega ou quando todas as tentativas falham. O grafo do Graphify agrupa `PortfolioPage.tsx`, `AssetLogo`, os ativos cadastrados e o requisito de pesquisa no mesmo núcleo de impacto.

## Goals / Non-Goals

**Goals:**

- simplificar visualmente cada opção do catálogo removendo apenas o valor da cotação;
- carregar a logo pela abstração compartilhada e preservar um fallback legível;
- manter seleção, acessibilidade, filtros, temas e layout responsivo;
- preservar a consulta fresca que preenche o preço unitário depois da escolha.

**Non-Goals:**

- alterar endpoints, DTOs, banco ou provedores de cotação;
- remover a cotação da tela de catálogo ou do campo de preço da operação;
- persistir imagens ou criar um serviço próprio de logos;
- alterar a edição de lançamentos pendentes.

## Decisions

### 1. Remover somente a apresentação da cotação na opção

O bloco monetário à direita de cada `asset-result` será removido. Os dados `lastQuote` e `currency` continuarão no contrato porque são usados em outras telas e não interferem na seleção.

Alternativa considerada: remover a cotação do DTO do catálogo. Rejeitada porque ampliaria a mudança para backend e prejudicaria a tela de ativos sem benefício para este fluxo.

### 2. Preservar a cotação corrente após a seleção

O clique continuará definindo o ativo selecionado e executando `/api/assets/{assetId}/quote`. Estados de carregamento, erro, validação e conversão cambial permanecerão inalterados.

Alternativa considerada: usar a cotação armazenada do catálogo. Rejeitada porque ela pode estar desatualizada e contraria a regra de obter preço corrente para preparar o lançamento.

### 3. Centralizar a identificação visual no `AssetLogo`

A opção continuará usando o componente compartilhado. A implementação verificará se as fontes por mercado e a troca de tentativas funcionam no contexto do botão; qualquer ajuste necessário será feito no componente comum para não criar URLs ou estados paralelos em `PortfolioPage`.

Alternativa considerada: renderizar uma tag `img` diretamente no diálogo. Rejeitada porque duplicaria regras de URL, carregamento, texto alternativo e fallback.

### 4. Validar comportamento observável sem depender da rede

Os testes do diálogo verificarão que ticker, nome, tipo e imagem são renderizados, que o preço do catálogo não aparece na lista e que selecionar o ativo ainda solicita a cotação fresca. Os testes do componente simularão sucesso e falha das imagens, sem chamar provedores reais.

Alternativa considerada: depender da resposta externa durante a suíte. Rejeitada por tornar os testes instáveis e consumir serviços de terceiros.

## Risks / Trade-offs

- [A remoção da coluna monetária altera a largura útil das opções] → manter a identidade ocupando o espaço disponível e validar temas e viewport estreito.
- [A disponibilidade da imagem continua dependendo de fonte externa] → preservar tentativas alternativas e fallback local, sem exibir imagem quebrada.
- [Um teste textual pode confundir a cotação da lista com o preço unitário] → limitar as asserções ao contêiner de resultados e verificar separadamente o preço preenchido após o clique.

## Migration Plan

1. ajustar a opção de ativo e, se necessário, a resolução de fontes do componente de logo;
2. atualizar os testes unitários e de interface;
3. executar formatação, lint, testes e build do frontend;
4. realizar inspeção visual nos temas claro e escuro para mercados BR e US.

O rollback consiste em restaurar a apresentação monetária da opção e os testes anteriores; não há migração de dados nem alteração de API.
