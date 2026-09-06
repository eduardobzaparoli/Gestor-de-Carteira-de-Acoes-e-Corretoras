## Context

Os contratos atuais transportam o retrato completo do ativo entre o cliente e os serviços de lançamentos e proventos. A pesquisa já dispõe de cache temporário para resultados e cotações; o provento manual já consulta o histórico efetivo da carteira. Veja `proposal.md` para a motivação e os deltas de especificação para os contratos observáveis.

## Goals / Non-Goals

**Goals:**
- Tornar o retrato usado em lançamentos verificável e vinculado ao contexto autenticado.
- Rejeitar dados decimais incompatíveis antes de alcançarem H2 ou PostgreSQL.
- Centralizar a coerência entre mercado e moeda para os pontos de entrada aplicáveis.
- Fazer o provento manual usar o retrato persistido e o erro de conflito aprovado.

**Non-Goals:**
- Persistir um catálogo de ativos, alterar cotações ou validar que o preço negociado é igual à cotação atual.
- Alterar lançamentos e proventos históricos existentes.
- Exigir pesquisa externa para o registro manual de proventos.

## Decisions

### Referência temporária vinculada ao contexto

A resposta de pesquisa receberá um identificador opaco associado ao investidor, à carteira e ao retrato normalizado do resultado. A criação do lançamento aceitará apenas esse identificador, mais tipo, data, quantidade, preço e custos. A referência terá TTL, não será persistida e será removida depois do uso bem-sucedido.

Uma referência ausente, expirada ou fora de seu contexto retorna `ASSET_SELECTION_EXPIRED` com `409`, no mesmo modelo de candidatos de proventos expirados. Isso evita que a API revele o ativo que foi selecionado por outra pessoa ou em outra carteira.

Alternativa considerada: reconsultar o provedor usando ticker enviado pelo cliente. Foi descartada porque mantém a possibilidade de combinar metadados inconsistentes, introduz uma chamada externa no registro e torna a operação dependente da disponibilidade do provedor.

Alternativa considerada: criar uma tabela de ativos. Foi descartada por ampliar persistência e manutenção de catálogo sem necessidade para o retrato imutável do lançamento.

### Contratos de entrada enxutos e limites no DTO

O DTO de lançamento deixará de receber nome, mercado, tipo e moeda. O DTO manual de provento manterá ticker para localizar a compra efetiva, mas deixará de receber os demais campos do retrato. As entradas `DECIMAL(19,8)` usarão validação declarativa de até 11 dígitos inteiros e 8 casas decimais, além das regras atuais de sinal.

O servidor centralizará a correspondência `BR`/`BRL` e `US`/`USD` como defesa adicional para dados recuperados de referências e histórico.

Alternativa considerada: capturar erros de precisão emitidos pelo banco. Foi descartada porque varia entre H2 e PostgreSQL e transforma uma falha previsível do cliente em erro de infraestrutura.

### Retrato manual derivado da compra efetiva

O registro manual localizará uma compra efetiva do ticker na carteira do investidor e reutilizará seu retrato. A ausência dessa compra gerará `ASSET_NOT_ACQUIRED` com `409`. Caso haja mais de uma compra, o retrato mais recente será usado, pois as regras desta change garantem que futuras compras tenham identidade coerente; registros legados permanecem preservados.

## Risks / Trade-offs

- [Clientes que usam o corpo antigo de lançamento deixam de funcionar] → documentar a quebra de contrato e cobrir o novo formato com testes de integração.
- [Uma seleção pode expirar antes do envio] → TTL explícito, erro público recuperável e nova pesquisa sem persistência de estado.
- [Histórico legado pode ter retratos inconsistentes] → não reescrever registros imutáveis; a escolha manual será determinística e limitada à carteira do solicitante.

## Migration Plan

1. Publicar o novo contrato de pesquisa e criação de lançamento na mesma versão da API.
2. Implantar sem migração de dados, pois referências temporárias não são persistidas.
3. Em rollback, restaurar os DTOs e serviços anteriores; lançamentos criados sob o novo contrato continuam compatíveis por preservarem o mesmo retrato persistido.
