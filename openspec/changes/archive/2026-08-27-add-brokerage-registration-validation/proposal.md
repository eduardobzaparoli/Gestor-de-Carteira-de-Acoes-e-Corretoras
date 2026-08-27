## Why

O investidor precisa cadastrar corretoras confiáveis antes de vinculá-las às futuras carteiras. Para impedir registros inconsistentes, o cadastro deve confirmar que o CNPJ pertence a uma instituição ativa na CVM e que o CEP informado corresponde ao estabelecimento consultado, preservando ao mesmo tempo os campos de endereço que dependem de preenchimento humano.

## What Changes

- Adicionar cadastro de corretora autenticado com apelido, CNPJ, CEP, rua, bairro, número e complemento.
- Consultar o CEP pela ViaCEP para preencher ou atualizar rua e bairro e obter cidade e estado, sem substituir número ou complemento e permitindo ajustes manuais nos campos de endereço.
- Na confirmação, consultar novamente as fontes no backend, validar o CNPJ pela Brasil API, exigir que ele conste como participante ativo na CVM e comparar o CEP informado com o CEP cadastral do CNPJ.
- Impedir qualquer persistência parcial quando uma validação falhar ou uma integração obrigatória estiver indisponível.
- Persistir dados oficiais da instituição, o endereço validado, o apelido e o proprietário autenticado.
- Isolar corretoras por investidor, impedir CNPJ ou apelido duplicado para o mesmo proprietário e permitir que investidores diferentes cadastrem a mesma instituição.
- Disponibilizar consulta das corretoras pertencentes ao investidor para uso posterior no vínculo de carteiras.
- Padronizar em JSON os erros de entrada, conflito, divergência cadastral e indisponibilidade externa.

## Capabilities

### New Capabilities

- `brokerage-registration`: Consulta de endereço por CEP, cadastro e listagem de corretoras do investidor com validação integrada na Brasil API, ViaCEP e cadastro de participantes da CVM.

### Modified Capabilities

- Nenhuma.

## Impact

- Novos endpoints REST protegidos para consulta de CEP, cadastro e listagem de corretoras.
- Novos modelos Domain, Entity, DTOs, Mapper, Repository e Service para corretoras e endereços.
- Novas integrações externas isoladas por interfaces/estratégias para Brasil API, ViaCEP e dados cadastrais da CVM.
- Nova persistência relacionada ao usuário autenticado, com restrições de unicidade por proprietário compatíveis com H2 e PostgreSQL.
- Novas configurações de URLs, timeouts e política de atualização/consulta das fontes externas, sem credenciais versionadas.
- Testes unitários, de integração e de contrato para regras cadastrais, isolamento de dados e falhas das integrações.
