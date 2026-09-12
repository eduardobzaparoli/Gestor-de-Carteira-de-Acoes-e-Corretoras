## Why

Depois de criada, uma carteira não pode ter seu nome ou sua corretora corrigidos, obrigando o investidor a conviver com dados desatualizados ou recriar a organização quando isso ainda é possível. A edição desses dados cadastrais completa o fluxo de gestão de carteiras sem interferir no histórico financeiro existente.

## What Changes

- Adicionar uma operação autenticada para editar o nome e a corretora vinculada de uma carteira pertencente ao investidor.
- Reutilizar na edição as regras atuais de normalização, limite e unicidade do nome e de propriedade da corretora.
- Preservar identificador, proprietário, data de criação, lançamentos, posições e histórico da carteira, atualizando apenas os dados cadastrais e `updatedAt`.
- Adicionar ao cartão da carteira uma ação de edição e um diálogo preenchido com os valores atuais, seguindo o design, os temas e os estados de interface existentes.
- Atualizar imediatamente a listagem e os dados dependentes após uma edição bem-sucedida, mantendo o formulário aberto e informativo quando a API rejeitar a alteração.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `portfolio-management`: passa a permitir a atualização privada do nome e da corretora de uma carteira existente, com as mesmas validações da criação e preservação do histórico.
- `react-web-interface`: passa a oferecer a ação e o diálogo de edição nos cartões da visão geral de carteiras.

## Impact

- API de carteiras: novo endpoint de atualização em `/api/portfolios/{id}` e novo DTO de entrada.
- Camadas de serviço e persistência: atualização transacional com validação de propriedade, corretora e nome único.
- Frontend React: cartão, diálogo, mutação, mensagens e invalidação das consultas de carteira.
- Testes: cobertura unitária, de integração H2/PostgreSQL e da interface para sucesso, propriedade, conflitos e preservação de dados.
- Documentação: contrato da API e especificações OpenSpec; não há dependência nova nem migration de banco prevista.
