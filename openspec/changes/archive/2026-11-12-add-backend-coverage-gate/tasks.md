## 1. Instrumentação e linha de base

- [x] 1.1 Configurar no `pom.xml` o JaCoCo com versão explícita e as execuções `prepare-agent`, `report` e `check` associadas ao ciclo Maven apropriado.
- [x] 1.2 Executar `clean verify` com o perfil H2, confirmar a geração dos relatórios HTML, XML e CSV e registrar as coberturas reais de linhas e ramos.
- [x] 1.3 Examinar o relatório para identificar ramos não cobertos em autenticação, autorização, alterações de dados, cálculos financeiros, fronteiras e tratamento de exceções.

## 2. Portão de cobertura e testes relevantes

- [x] 2.1 Adicionar o menor conjunto de testes comportamentais necessário caso a cobertura de linhas esteja abaixo de 80% ou existam caminhos críticos sem proteção, usando parametrização somente quando várias entradas representarem a mesma regra.
- [x] 2.2 Versionar limites de linhas e ramos calibrados pela medição real, sem exclusões arbitrárias e sem reutilizar os percentuais do projeto-exemplo.
- [x] 2.3 Comprovar que o gate aprovado termina com sucesso e que um limite temporariamente maior reprova o build sem exigir edição permanente do `pom.xml`.

## 3. Integração contínua

- [x] 3.1 Atualizar os jobs H2 e PostgreSQL do GitHub Actions para executar os perfis existentes com `clean verify`.
- [x] 3.2 Publicar `target/site/jacoco/` em cada job backend com `if: always()` e nomes de artefato distintos.

## 4. Documentação e verificação final

- [x] 4.1 Documentar no README os comandos locais, a localização e leitura do relatório, os limites iniciais e a regra para sua evolução.
- [x] 4.2 Executar o gate H2 completo e confirmar testes, relatório e verificação de cobertura.
- [x] 4.3 Executar o gate PostgreSQL contra banco vazio e descartável e confirmar testes, migrações, relatório e verificação de cobertura.
- [x] 4.4 Revisar o diff final para confirmar que não houve alteração funcional, exclusão de cobertura, segredo versionado ou dependência de produção adicionada.
