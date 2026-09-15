## MODIFIED Requirements

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
