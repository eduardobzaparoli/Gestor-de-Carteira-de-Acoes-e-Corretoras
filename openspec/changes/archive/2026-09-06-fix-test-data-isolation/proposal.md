## Why

A suíte completa falha de forma dependente da ordem de execução porque testes administrativos limpam somente usuários, embora a base H2 compartilhada retenha corretoras e outros registros dependentes criados anteriormente. Isso torna o resultado de `mvn test` não confiável apesar de os testes passarem isoladamente.

## What Changes

- Padronizar a limpeza de dados de integração na ordem que respeita as chaves estrangeiras.
- Isolar os testes de concorrência administrativa de registros criados por outros testes.
- Confirmar que a suíte Maven completa passa de forma repetível.

## Capabilities

### New Capabilities

<!-- Nenhuma; não há comportamento novo de produto. -->

### Modified Capabilities

<!-- Nenhuma; a mudança é restrita à infraestrutura de testes. -->

## Impact

- Código de teste e sua infraestrutura de limpeza de dados H2.
- Nenhum endpoint, regra de negócio, dependência de aplicação ou esquema de produção.
