## Why

O backend possui uma suíte relevante de testes, mas o build ainda não mede quais linhas e decisões são exercitadas nem impede regressões de cobertura. A mudança torna essa evidência reproduzível localmente e no CI, adaptando a Aula 15 ao projeto sem importar métricas ou exemplos de outro domínio.

## What Changes

- Integrar o JaCoCo ao ciclo Maven para gerar relatórios HTML, XML e CSV durante `verify`.
- Medir a linha de base real do projeto antes de definir limites e registrar pisos de cobertura de linhas e ramos que não excedam a cobertura existente.
- Preservar o objetivo acadêmico mínimo de 80% de linhas; caso a medição inicial fique abaixo desse valor, acrescentar somente testes de comportamento relevantes até alcançá-lo.
- Fazer o build falhar quando a cobertura ficar abaixo dos limites versionados, sem exclusões arbitrárias de classes ou pacotes.
- Executar o mesmo gate nos jobs H2 e PostgreSQL do GitHub Actions e publicar o relatório JaCoCo como artefato para diagnóstico.
- Documentar os comandos locais, a localização do relatório, a interpretação das métricas e a política de evolução dos limites.
- Adicionar testes de fronteira ou de caminhos alternativos apenas quando o relatório revelar uma lacuna relevante em regras críticas.

## Capabilities

### New Capabilities

Nenhuma. A mudança introduz uma política de qualidade do build e não altera comportamento observável do produto.

### Modified Capabilities

Nenhuma. Os contratos funcionais e as APIs existentes permanecem inalterados.

## Impact

- `pom.xml`, com o plugin JaCoCo e propriedades dos limites versionados.
- `.github/workflows/release-validation.yml`, para executar `clean verify` e disponibilizar o relatório.
- `README.md`, com instruções de cobertura e diagnóstico.
- Testes backend, somente se a medição inicial indicar cobertura de linhas abaixo de 80% ou ramos críticos sem proteção.
- Nenhuma alteração de banco, API, interface, dependência de produção ou `docs/product-spec.md`.
