## Why

Os testes já cobrem regras críticas em H2 e PostgreSQL, mas sua execução ainda depende de comandos e configuração local. Um pipeline de entrega determinístico reduz o risco de integrar uma mudança sem validar o banco, os contratos das integrações e o build limpo.

## What Changes

- Criar uma rotina única de validação de release que execute a suíte Maven em H2 e a suíte PostgreSQL sob configuração explícita.
- Disponibilizar um fluxo de CI que execute essa rotina em cada alteração proposta e na integração com `dev`.
- Separar testes determinísticos, que usam dublês de integrações externas, de smoke tests reais opcionais para não tornar o pipeline dependente de credenciais ou limites de provedores.
- Documentar pré-requisitos, variáveis de ambiente e os comandos locais equivalentes ao pipeline.
- Remover dependências de produção comprovadamente não utilizadas, caso a análise do build as identifique.

## Capabilities

### New Capabilities

Nenhuma. Esta mudança automatiza verificações de entrega e não altera um comportamento observável do produto.

### Modified Capabilities

Nenhuma. Os contratos funcionais existentes permanecem inalterados.

## Impact

- Arquivos de build e automação de CI.
- Configurações e documentação de execução de testes H2 e PostgreSQL.
- Testes de integração existentes e possíveis dublês de provedores externos.
- `pom.xml`, somente se a análise confirmar uma dependência sem uso.
