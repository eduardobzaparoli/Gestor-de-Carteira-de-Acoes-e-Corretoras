## Context

Consulte `proposal.md` para a motivação. O fluxo atual expõe criação, listagem, consulta e exclusão de carteiras; o nome normalizado e a corretora são definidos apenas na criação. A entidade já persiste `updatedAt`, o banco já possui a restrição única por proprietário e nome normalizado e a resposta pública já contém o resumo da corretora, portanto a mudança não exige migration nem novo armazenamento.

## Goals / Non-Goals

**Goals:**

- Atualizar nome e corretora em uma única transação autenticada e restrita ao proprietário.
- Reutilizar normalização, validação, mapeamento e erros públicos existentes sempre que o contrato for equivalente.
- Preservar todo o histórico financeiro e refletir imediatamente a alteração nas consultas do frontend.
- Manter a interface consistente com os cartões, diálogos, temas e acessibilidade atuais.

**Non-Goals:**

- Editar proprietário, identificador, datas de criação ou conteúdo dos lançamentos.
- Mover somente parte das posições ou transações para outra carteira.
- Cadastrar ou editar uma corretora dentro do diálogo da carteira.
- Alterar regras de exclusão ou cálculos financeiros.

## Decisions

### 1. Atualização completa por `PUT /api/portfolios/{id}`

A API receberá um DTO com `name` e `brokerageId`, ambos obrigatórios, e retornará o `PortfolioResponse` atualizado com status `200`. `PUT` representa adequadamente a substituição completa dos dados cadastrais editáveis e evita semântica ambígua de campos ausentes. A alternativa `PATCH` foi descartada porque aumentaria combinações de validação sem benefício para um formulário que sempre dispõe dos dois valores.

### 2. Validação e persistência atômicas na camada de serviço

O serviço localizará a carteira pelo par identificador/proprietário, normalizará a entrada, resolverá a corretora pelo mesmo proprietário e persistirá a atualização em uma transação. A verificação antecipada de nome duplicado deverá desconsiderar a própria carteira; a restrição única do banco continuará sendo a proteção definitiva contra concorrência e sua violação será traduzida para `PORTFOLIO_NAME_ALREADY_REGISTERED`. A alternativa de excluir e recriar a carteira foi descartada porque quebraria identidade e vínculos históricos.

### 3. Alteração controlada da entidade existente

A associação de corretora passará a aceitar atualização e a entidade oferecerá uma operação explícita para trocar nome, chave normalizada e corretora. Identificador, proprietário e `createdAt` continuarão imutáveis. Isso preserva o rastreamento do JPA e evita reconstruir uma entidade com o mesmo identificador.

### 4. Diálogo de edição separado do estado de criação

O cartão exibirá uma ação com ícone e nome acessível. Ao acioná-la, o frontend copiará os valores atuais para um estado de edição e abrirá um diálogo próprio, enviando `PUT`; no sucesso, invalidará a listagem e a consulta individual da carteira alterada. Separar os estados reduz risco de dados residuais entre criar e editar, embora os mesmos componentes visuais de campo e seleção sejam reutilizados.

### 5. Troca de corretora é somente cadastral

Lançamentos e posições pertencem à carteira, não diretamente à corretora. Assim, trocar a corretora atualiza a classificação organizacional da carteira sem reescrever o histórico. A resposta e a interface devem deixar esse comportamento implícito pela preservação dos dados, sem criar lançamentos compensatórios.

## Risks / Trade-offs

- **[Conflito concorrente de nomes]** → manter a restrição única e traduzir violações após `flush`, além da verificação antecipada que exclui o próprio ID.
- **[Corretora removida durante a edição]** → resolver a corretora dentro da transação e deixar a chave estrangeira impedir vínculo inválido, traduzindo o caso público de forma consistente.
- **[Cartão desatualizado após salvar]** → atualizar ou invalidar as chaves de consulta de listagem e detalhe antes de encerrar o fluxo visual.
- **[Ação de editar confundida com excluir]** → usar ícone, rótulo acessível, espaçamento e estilos semanticamente distintos.

## Migration Plan

1. Publicar backend e frontend juntos, pois o novo endpoint é aditivo e clientes antigos continuam funcionando.
2. Executar testes H2 e PostgreSQL para confirmar atualização do vínculo e restrição única sem alteração de esquema.
3. Em rollback, remover o endpoint e a ação da interface; carteiras já editadas permanecem válidas no esquema anterior.
