## RENAMED Requirements

- FROM: `### Requirement: Busca privada no contexto da carteira`
- TO: `### Requirement: Busca privada para cadastro do investidor`

## MODIFIED Requirements

### Requirement: Busca privada para cadastro do investidor

O sistema SHALL disponibilizar a pesquisa externa de ativos somente a investidores autenticados e para preparar o cadastro no catálogo do próprio investidor. O proprietário MUST ser determinado exclusivamente pelo token, a pesquisa MUST NOT exigir uma carteira e administradores MUST NOT acessar essa capacidade.

#### Scenario: Investidor pesquisa em uma carteira própria

- **WHEN** um investidor autenticado pesquisa um ativo para seu catálogo, possuindo ou não uma carteira própria
- **THEN** o sistema executa a busca no contexto daquele investidor e responde com status `200`

#### Scenario: Carteira inexistente ou de outro investidor

- **WHEN** o investidor pesquisa um ativo pelo novo endpoint sem informar carteira, ainda que não possua carteira ou que uma carteira anteriormente usada tenha deixado de existir
- **THEN** o sistema não consulta nem valida carteira e executa a pesquisa para permitir o cadastro global do ativo

#### Scenario: Requisição sem autenticação ou por administrador

- **WHEN** uma pessoa sem token válido ou um administrador chama a pesquisa de ativos
- **THEN** o sistema responde respectivamente com `401` ou `403` em JSON e não consulta provedores de ativos

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
