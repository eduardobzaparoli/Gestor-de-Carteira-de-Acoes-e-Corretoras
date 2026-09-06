# asset-search Specification

## Purpose

Permitir que investidores localizem ações ou ETFs de um mercado escolhido dentro de uma carteira própria e visualizem a cotação mais recente disponível antes de um futuro lançamento.

## Requirements

### Requirement: Busca privada no contexto da carteira

O sistema SHALL disponibilizar uma busca de ativos somente para uma carteira pertencente ao investidor autenticado. O proprietário MUST ser determinado exclusivamente pelo token, e administradores MUST NOT acessar essa capacidade.

#### Scenario: Investidor pesquisa em uma carteira própria

- **WHEN** um investidor autenticado pesquisa ativos usando o UUID de uma de suas carteiras
- **THEN** o sistema executa a busca no contexto daquela carteira e responde com status `200`

#### Scenario: Carteira inexistente ou de outro investidor

- **WHEN** um investidor pesquisa usando o UUID de uma carteira inexistente ou que pertence a outra pessoa
- **THEN** o sistema responde com status `404` e código `PORTFOLIO_NOT_FOUND`, sem consultar provedores de ativos

#### Scenario: Requisição sem autenticação ou por administrador

- **WHEN** uma pessoa sem token válido ou um administrador chama a busca de ativos
- **THEN** o sistema responde respectivamente com `401` ou `403` em JSON e não consulta provedores de ativos

### Requirement: Seleção obrigatória de mercado e tipo

O sistema SHALL exigir um mercado `BR` ou `US`, um tipo `STOCK` ou `ETF` e um termo de pesquisa não vazio. O sistema MUST retornar somente ativos cuja classificação confirmada corresponde ao mercado e ao tipo selecionados; não SHALL existir uma opção agregada de todos os tipos.

#### Scenario: Busca de ação brasileira

- **WHEN** o investidor seleciona `BR`, `STOCK` e informa um termo válido
- **THEN** o sistema retorna somente ações brasileiras compatíveis com o termo

#### Scenario: Busca de ETF americano

- **WHEN** o investidor seleciona `US`, `ETF` e informa um termo válido
- **THEN** o sistema retorna somente ETFs americanos compatíveis com o termo

#### Scenario: Mercado, tipo ou termo inválido

- **WHEN** mercado ou tipo não pertence aos valores aceitos, ou o termo fica vazio após normalização
- **THEN** o sistema responde com status `400` e identifica os campos inválidos no erro JSON

### Requirement: Resultados com cotação mais recente disponível
O sistema SHALL retornar uma coleção de resultados de busca sem persistir ativos pesquisados. Cada resultado SHALL conter uma referência temporária de seleção, ticker, nome, mercado, tipo, moeda e a cotação mais recente disponível do ativo retornada pelo provedor selecionado, sem expor dados brutos ou detalhes internos da integração. A referência MUST ser utilizável somente pelo investidor autenticado, na carteira consultada, e dentro de sua validade temporária.

#### Scenario: Resultado com cotação
- **WHEN** o provedor encontra um ativo classificado no mercado e tipo solicitados e fornece sua cotação
- **THEN** o sistema inclui o ativo, sua cotação e uma referência temporária de seleção no resultado da busca

#### Scenario: Nenhum ativo compatível é encontrado
- **WHEN** não existe ativo classificado no mercado e tipo solicitados que corresponda ao termo
- **THEN** o sistema responde com status `200` e uma coleção vazia

#### Scenario: Cotação indisponível para um resultado candidato
- **WHEN** um candidato não possui cotação disponível na resposta do provedor
- **THEN** o sistema não o inclui entre os resultados cotados e não persiste dados parciais

### Requirement: Fontes externas resilientes e reutilização temporária

O sistema SHALL consultar a Brapi para o mercado `BR` e a AlphaVantage para o mercado `US`. O sistema MUST reutilizar temporariamente resultados de identificação e cotações equivalentes para reduzir chamadas repetidas, sem exigir que o cliente informe ou receba chaves dos provedores.

#### Scenario: Consulta repetida de identificação

- **WHEN** o investidor repete a mesma combinação de mercado, tipo e termo dentro da janela temporária configurada
- **THEN** o sistema reutiliza o resultado de identificação previamente obtido

#### Scenario: Consulta repetida de cotação

- **WHEN** o sistema precisa da cotação de um mesmo ticker dentro da janela temporária configurada
- **THEN** o sistema reutiliza a cotação previamente obtida

#### Scenario: Provedor indisponível ou limitado

- **WHEN** a Brapi ou a AlphaVantage excede o tempo limite, falha tecnicamente ou informa limite de uso
- **THEN** o sistema responde com status `503` e código público específico do provedor, sem expor credenciais, URLs internas ou conteúdo bruto
