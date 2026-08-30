# Portfolio Positions Specification

## Purpose

Permitir que o investidor consulte as quantidades atuais, o custo em custódia e o preço médio ponderado dos ativos mantidos em cada carteira privada.

## Requirements

### Requirement: Acesso privado às posições da carteira
O sistema SHALL disponibilizar as posições somente no contexto de uma carteira do investidor autenticado. O proprietário MUST ser determinado exclusivamente pelo token, e administradores MUST NOT consultar posições privadas.

#### Scenario: Investidor consulta posições de carteira própria
- **WHEN** um investidor autenticado consulta as posições de uma de suas carteiras
- **THEN** o sistema responde com status `200` e somente as posições daquela carteira

#### Scenario: Carteira inexistente ou de outro investidor
- **WHEN** o investidor consulta posições de uma carteira inexistente ou pertencente a outra pessoa
- **THEN** o sistema responde com status `404` e código `PORTFOLIO_NOT_FOUND`, sem revelar a existência da carteira

#### Scenario: Requisição sem autenticação ou por administrador
- **WHEN** uma pessoa sem token válido ou um administrador consulta posições
- **THEN** o sistema responde respectivamente com `401` ou `403` em JSON e não revela dados privados

### Requirement: Posições derivadas dos lançamentos efetivados
O sistema SHALL calcular as posições dinamicamente a partir dos lançamentos `EFFECTIVE`, agrupados por ticker e mercado. Antes do cálculo, o sistema MUST efetivar pendências vencidas; lançamentos que permaneçam `PENDING` ou estejam `CANCELLED` MUST NOT compor a posição.

#### Scenario: Carteira possui lançamentos efetivados
- **WHEN** o investidor consulta uma carteira com compras e vendas efetivadas
- **THEN** o sistema consolida quantidade, custo em custódia e preço médio por ticker e mercado

#### Scenario: Carteira não possui posição aberta
- **WHEN** a carteira não possui lançamentos efetivados ou todas as posições foram encerradas
- **THEN** o sistema responde com status `200` e uma coleção vazia

#### Scenario: Pendência chegou à data prevista
- **WHEN** um lançamento pendente já deveria estar efetivado na data da consulta
- **THEN** o sistema o efetiva antes de calcular e devolver as posições

### Requirement: Preço médio ponderado das compras
O sistema MUST processar os lançamentos efetivados da data de transação mais antiga para a mais recente, usando a data de criação como desempate. Cada compra SHALL aumentar a quantidade e o custo em custódia por `quantidade × preço unitário + custos da compra`; o preço médio SHALL ser o custo em custódia dividido pela quantidade atual.

#### Scenario: Primeira compra inicia a posição
- **WHEN** a primeira compra efetivada de um ativo é processada
- **THEN** o sistema define a quantidade comprada, o custo total da compra e o preço médio correspondente

#### Scenario: Nova compra recalcula a média ponderada
- **WHEN** uma posição aberta recebe outra compra efetivada por preço ou custos diferentes
- **THEN** o sistema soma o novo custo ao custo em custódia e divide pela quantidade total resultante

#### Scenario: Custos de compra participam da média
- **WHEN** uma compra efetivada possui custos maiores que zero
- **THEN** o sistema inclui esses custos no numerador do preço médio

### Requirement: Venda preserva o preço médio da posição restante
O sistema SHALL reduzir o custo em custódia de uma venda efetivada por `quantidade vendida × preço médio anterior`. O preço e os custos da venda MUST NOT alterar o preço médio das unidades restantes.

#### Scenario: Venda parcial
- **WHEN** uma venda efetivada reduz parcialmente uma posição
- **THEN** o sistema reduz quantidade e custo em custódia na mesma proporção e mantém o preço médio anterior

#### Scenario: Liquidação total
- **WHEN** uma venda efetivada consome toda a quantidade de uma posição
- **THEN** o sistema zera o custo em custódia e omite a posição da resposta

#### Scenario: Reabertura após liquidação
- **WHEN** uma nova compra ocorre depois da liquidação total do ativo
- **THEN** o sistema inicia uma nova posição e calcula o preço médio somente a partir dessa nova sequência de compras

### Requirement: Representação pública determinística e precisa
O sistema SHALL retornar para cada posição ticker, nome do ativo, mercado, tipo, moeda, quantidade, preço médio e custo em custódia. Os dados descritivos SHALL refletir o lançamento efetivado mais recente do grupo, a coleção MUST ser ordenada por mercado e ticker, e os cálculos MUST usar precisão decimal sem arredondamento monetário prematuro; o preço médio público SHALL usar até oito casas decimais com arredondamento `HALF_UP`.

#### Scenario: Posição é devolvida publicamente
- **WHEN** o investidor consulta uma carteira com posição aberta
- **THEN** a resposta contém somente os campos públicos da posição, sem entidade persistente ou proprietário interno

#### Scenario: Posições de mercados diferentes
- **WHEN** a carteira possui posições em mais de um mercado
- **THEN** o sistema mantém os grupos separados e os ordena por mercado e ticker

#### Scenario: Divisão produz decimal recorrente
- **WHEN** o preço médio não possui representação decimal finita dentro de oito casas
- **THEN** o sistema preserva maior precisão durante o cálculo e arredonda apenas a representação pública com `HALF_UP`

### Requirement: Cálculo sem persistência própria
O sistema MUST manter os lançamentos como fonte de verdade e MUST NOT exigir tabela própria de posições. O comportamento SHALL ser funcionalmente equivalente em H2 e PostgreSQL.

#### Scenario: Histórico é alterado por novo lançamento válido
- **WHEN** uma compra ou venda efetivada é adicionada à carteira
- **THEN** a próxima consulta recalcula a posição a partir do histórico atualizado

#### Scenario: Banco suportado
- **WHEN** a aplicação usa H2 ou PostgreSQL
- **THEN** a consulta produz os mesmos resultados para o mesmo histórico de lançamentos
