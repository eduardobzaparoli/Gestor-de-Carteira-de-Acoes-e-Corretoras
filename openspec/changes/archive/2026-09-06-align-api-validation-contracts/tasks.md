## 1. Seleção confiável de ativos

- [x] 1.1 Criar armazenamento temporário de seleções de ativos, com TTL e vínculo ao investidor, carteira e retrato normalizado.
- [x] 1.2 Incluir a referência de seleção em cada resultado de pesquisa e preservar o cache atual de identificação e cotação.
- [x] 1.3 Substituir o corpo de criação de lançamento pela referência do ativo e recuperar o retrato exclusivamente no servidor.
- [x] 1.4 Retornar `409 ASSET_SELECTION_EXPIRED` para referências ausentes, expiradas ou fora do contexto autenticado.

## 2. Validações de entrada e proventos manuais

- [x] 2.1 Aplicar limites de 11 dígitos inteiros e 8 casas decimais a quantidades e valores persistidos nos contratos de lançamento e provento manual.
- [x] 2.2 Centralizar e aplicar a compatibilidade `BR`/`BRL` e `US`/`USD` aos retratos usados pelos serviços.
- [x] 2.3 Simplificar o contrato manual de proventos, recuperar o retrato da compra efetiva mais recente e retornar `409 ASSET_NOT_ACQUIRED` quando ela não existir.

## 3. Testes e verificação

- [x] 3.1 Cobrir seleção válida, expirada, de outra carteira e de outro investidor nos testes de pesquisa e lançamentos.
- [x] 3.2 Cobrir limites decimais, combinações de moeda incompatíveis e garantia de resposta `400` antes da persistência.
- [x] 3.3 Cobrir provento manual com retrato derivado e ativo nunca adquirido com `409`.
- [x] 3.4 Executar os testes direcionados e a suíte Maven completa.
