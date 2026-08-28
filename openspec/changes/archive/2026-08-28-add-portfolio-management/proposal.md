## Why

Após autenticar-se, o investidor precisa organizar seus investimentos em carteiras privadas antes de registrar lançamentos ou consultar indicadores. A carteira estabelece o vínculo operacional com uma única corretora já aprovada, preservando o isolamento entre investidores.

## What Changes

- Adicionar criação de carteira autenticada com nome e vínculo obrigatório a uma corretora do próprio investidor.
- Validar nome obrigatório, normalizado, com até 100 caracteres e único por investidor sem distinção entre maiúsculas e minúsculas.
- Reutilizar a corretora já cadastrada como evidência de validação, sem repetir consultas à Brasil API, ViaCEP ou CVM ao criar uma carteira.
- Adicionar consulta individual, listagem cronológica da mais antiga para a mais recente e exclusão de carteiras do investidor.
- Padronizar respostas e erros JSON para validação, conflito, corretora inexistente e acesso a recursos de outro investidor.

## Capabilities

### New Capabilities

- `portfolio-management`: Criação, consulta, listagem e exclusão de carteiras privadas vinculadas a uma corretora já validada do investidor.

### Modified Capabilities

- Nenhuma.

## Impact

- Novos endpoints protegidos para gestão de carteiras.
- Novos modelos Domain, Entity, DTOs, Mapper, Repository, Service e Controller para carteiras.
- Novo relacionamento persistente entre carteira, usuário proprietário e corretora.
- Testes de validação, unicidade, ordenação cronológica, exclusão e isolamento de dados em H2 e PostgreSQL.
- Nenhuma nova integração externa ou dependência de biblioteca.
