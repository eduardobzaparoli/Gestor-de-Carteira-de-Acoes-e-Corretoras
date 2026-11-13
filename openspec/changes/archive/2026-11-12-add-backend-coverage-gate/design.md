## Context

Veja `proposal.md` para a motivação. O Maven executa 214 testes locais com sucesso e já separa os gates `release-h2` e `release-postgres`, mas ambos terminam na fase `test`. O pipeline não instrumenta o backend, não produz relatório de cobertura e não possui limite de não regressão. O frontend já tem cobertura própria com Vitest e testes ponta a ponta com Playwright.

## Goals / Non-Goals

**Goals:**

- Medir cobertura de linhas e ramos do backend com uma ferramenta integrada ao Maven.
- Tornar o mesmo comando de validação reproduzível localmente e no GitHub Actions.
- Calibrar os limites com dados reais deste repositório e manter pelo menos 80% de linhas.
- Disponibilizar o relatório completo mesmo quando o gate falhar.
- Direcionar eventuais testes novos a comportamentos críticos, não a chamadas artificiais para aumentar números.

**Non-Goals:**

- Alterar contratos da API, regras de negócio, banco ou interface.
- Substituir Flyway, H2, PostgreSQL, Vitest ou Playwright.
- Introduzir PIT, Testcontainers, Newman ou outra ferramenta adicional nesta mudança.
- Reescrever testes existentes em AssertJ ou parametrizá-los sem necessidade comportamental.
- Excluir classes ou pacotes da medição somente para elevar o percentual.

## Decisions

### 1. JaCoCo como plugin de build, não dependência da aplicação

O `jacoco-maven-plugin` será configurado com `prepare-agent`, `report` e `check`. Relatório e verificação serão associados à fase `verify`, preservando `test` como execução rápida quando o desenvolvedor não precisar do relatório.

Alternativa considerada: ferramenta separada ou cobertura apenas pela IDE. Isso não produziria uma política reproduzível no Maven e no CI.

### 2. Calibração em duas etapas

Primeiro será gerado o relatório sem inventar percentuais. Depois os limites finais serão versionados:

- linhas: o maior valor entre 80% e a linha de base arredondada para baixo; se a linha de base estiver abaixo de 80%, testes de comportamento relevantes serão adicionados antes de ativar o gate;
- ramos: linha de base arredondada para baixo, registrada como piso inicial, com 80% mantido como objetivo de evolução.

Os percentuais do material da aula não serão reutilizados porque pertencem a outro projeto. Propriedades Maven permitirão demonstrar uma reprovação com um limite temporariamente maior sem editar o arquivo.

Alternativa considerada: fixar imediatamente 80% para linhas e ramos. Isso poderia bloquear o projeto antes de conhecer sua situação real e incentivar testes sem valor ou exclusões artificiais.

### 3. Sem exclusões de cobertura nesta primeira configuração

Todo o código de produção instrumentado pelo build participará da linha de base. Uma exclusão futura exigirá justificativa técnica própria, pois remover DTOs, configurações ou mapeadores apenas para melhorar o índice esconderia regressões.

Alternativa considerada: excluir código considerado trivial. A classificação seria subjetiva e desnecessária antes de observar o relatório.

### 4. `clean verify` nos dois gates de banco

Os jobs H2 e PostgreSQL passarão a executar seus perfis atuais até `verify`. Cada job publicará `target/site/jacoco/` com nome distinto e `if: always()`, permitindo examinar o relatório mesmo quando o limite reprovar o build.

Alternativa considerada: medir apenas H2. O gate PostgreSQL também deve usar o mesmo ciclo Maven, e seu relatório ajuda a confirmar quais caminhos adicionais dependem do banco real.

### 5. Testes novos guiados por risco e pelo relatório

Se houver lacunas necessárias para atingir o piso, serão priorizados autenticação e autorização, alteração de dados, cálculos financeiros, valores de fronteira e tratamento de exceções. Testes parametrizados serão usados somente quando várias entradas representarem a mesma regra, como limites de tamanho.

Alternativa considerada: criar testes para cada linha vermelha. Isso aumentaria manutenção sem garantir melhores verificações.

## Risks / Trade-offs

- [A cobertura inicial de linhas ficar abaixo de 80%] → Adicionar o menor conjunto de testes comportamentais em áreas críticas antes de ativar o limite.
- [A instrumentação aumentar o tempo do pipeline] → Restringir a cobertura ao backend e reutilizar os caches Maven existentes.
- [Diferença de cobertura entre H2 e PostgreSQL] → Manter artefatos separados e usar o job H2 como base geral, sem ocultar falhas do job PostgreSQL.
- [O gate incentivar testes sem asserções relevantes] → Revisar os cenários pelo comportamento protegido e proibir exclusões ou chamadas artificiais.
- [Falha no gate impedir a publicação do diagnóstico] → Enviar o artefato com `if: always()` e nome único por job.

## Migration Plan

1. Configurar o JaCoCo para instrumentar os testes e gerar o relatório no ciclo `verify`.
2. Executar o gate H2 para medir linhas e ramos reais sem copiar a linha de base do material.
3. Adicionar somente os testes necessários caso a cobertura de linhas esteja abaixo de 80% ou haja ramos críticos sem proteção.
4. Registrar os limites finais no `pom.xml` e comprovar uma execução aprovada e outra reprovada por limite temporário maior.
5. Atualizar os jobs H2 e PostgreSQL para `clean verify` e publicar seus relatórios.
6. Documentar uso, leitura do relatório e política de evolução no README.

A reversão remove o plugin, restaura os comandos `test` do workflow e retira a documentação correspondente. Não há migração nem risco para dados persistidos.
