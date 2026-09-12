# asset-search Specification

## Purpose

Permitir que investidores localizem ações ou ETFs de um mercado escolhido dentro de uma carteira própria e visualizem a cotação mais recente disponível antes de um futuro lançamento.

## Requirements

### Requirement: Busca privada para cadastro do investidor

O sistema SHALL disponibilizar a pesquisa externa de ativos somente a investidores autenticados e para preparar o cadastro no catálogo do próprio investidor. O proprietário MUST ser determinado exclusivamente pelo token, a pesquisa MUST NOT exigir uma carteira e administradores MUST NOT acessar essa capacidade.

#### Scenario: Investidor pesquisa em uma carteira própria

- **WHEN** um investidor autenticado pesquisa um ativo para seu catálogo, possuindo ou não uma carteira própria
- **THEN** o sistema executa a busca no contexto daquele investidor e responde com status `200`

#### Scenario: Carteira inexistente ou de outro investidor

- **WHEN** o investidor pesquisa um ativo pelo novo endpoint sem informar carteira, ainda que não possua carteira ou que uma carteira anteriormente usada tenha deixado de existir
- **THEN** o sistema não consulta nem valida carteira e executa a pesquisa para permitir o cadastro global do ativo

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
O sistema SHALL retornar uma coleção de resultados de busca sem persistir automaticamente ativos pesquisados. Cada resultado SHALL conter uma referência temporária de seleção, ticker, nome, mercado, tipo, moeda e a cotação mais recente disponível do ativo retornada pelo provedor selecionado, sem expor dados brutos ou detalhes internos da integração. A referência MUST ser utilizável somente pelo investidor autenticado, para cadastrar o ativo em seu catálogo e dentro de sua validade temporária.

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

O sistema SHALL consultar a Brapi para o mercado `BR` e a Twelve Data para identificação e cotação de ativos do mercado `US`. O sistema MUST reutilizar temporariamente resultados de identificação e cotações equivalentes para reduzir chamadas repetidas, sem exigir que o cliente informe ou receba chaves dos provedores. A Alpha Vantage MUST NOT ser usada como fonte de identificação ou cotação de ativos `US` e SHALL permanecer reservada à consulta de proventos americanos.

#### Scenario: Consulta repetida de identificação

- **WHEN** o investidor repete a mesma combinação de mercado, tipo e termo dentro da janela temporária configurada
- **THEN** o sistema reutiliza o resultado de identificação previamente obtido

#### Scenario: Consulta repetida de cotação

- **WHEN** o sistema precisa da cotação de um mesmo ticker dentro da janela temporária configurada
- **THEN** o sistema reutiliza a cotação previamente obtida

#### Scenario: Pesquisa de ativo americano

- **WHEN** o investidor pesquisa uma ação ou ETF no mercado `US`
- **THEN** o sistema consulta a Twelve Data, aceita somente instrumentos americanos do tipo solicitado e retorna preços em `USD`

#### Scenario: Provedor retorna o mesmo ticker mais de uma vez

- **WHEN** a Twelve Data devolve múltiplas ocorrências válidas do mesmo ticker em uma pesquisa
- **THEN** o sistema retorna somente um candidato para esse ticker, preservando a primeira ocorrência normalizada

#### Scenario: Provedor indisponível ou limitado

- **WHEN** a Brapi ou a Twelve Data excede o tempo limite, falha tecnicamente, rejeita a credencial ou informa limite de créditos
- **THEN** o sistema responde com status `503` e código público específico do provedor, sem expor credenciais, URLs internas ou conteúdo bruto
