## Context

Veja `proposal.md` para a motivação. O PostgreSQL já utiliza Flyway com `B1__initial_schema.sql`, `V1__add_user_status.sql`, baseline automática em versão `0` e Hibernate em `validate`. O perfil H2 usa `ddl-auto=update`, o perfil de teste usa `create-drop` e ambos desabilitam Flyway; por isso a suíte comum não comprova que o schema de produção é a única fonte de verdade. As migrations atuais já executam com sucesso no H2 em modo PostgreSQL por um teste isolado e não podem ser reescritas depois de publicadas.

## Goals / Non-Goals

**Goals:**

- Inicializar H2 e PostgreSQL com os mesmos arquivos em `db/migration`.
- Fazer Hibernate validar o resultado em todos os perfis persistentes.
- Tornar histórico, checksum, lock, baseline, imutabilidade e diagnóstico compreensíveis e verificáveis no projeto.
- Preservar bancos PostgreSQL vazios, legados e já versionados.

**Non-Goals:**

- Trocar Flyway por Liquibase.
- Alterar entidades, endpoints, dados financeiros ou regras de negócio.
- Editar `B1` ou `V1`, criar uma evolução de schema artificial ou introduzir dados de demonstração.
- Exigir a CLI externa do Flyway para executar a aplicação.

## Decisions

### Preservar as migrations publicadas

`B1` e `V1` permanecerão byte a byte inalteradas. A baseline migration `B1` cria o estado cumulativo em ambientes vazios; bancos legados sem histórico recebem o baseline `0` e executam `V1`; bancos já versionados ignoram a baseline cumulativa. Uma alteração estrutural futura deverá usar `V2` ou versão superior.

Alternativa considerada: consolidar ou renomear os arquivos para uma sequência mais didática. Foi rejeitada porque alteraria artefatos já publicados e poderia quebrar checksums e bancos existentes.

### Ativar Flyway diretamente nos perfis H2 e test

`application-h2.properties` e `application-test.properties` habilitarão Flyway e usarão `spring.jpa.hibernate.ddl-auto=validate`. O caminho padrão `classpath:db/migration` será compartilhado; não será criado um conjunto paralelo de scripts para H2. O modo de compatibilidade PostgreSQL continuará na URL H2.

Alternativa considerada: manter Hibernate nos testes e apenas executar um teste Flyway isolado. Foi rejeitada porque permite que a maior parte da suíte valide um esquema diferente do implantado.

### Manter baseline automática somente onde há legado

Os perfis PostgreSQL continuarão com `baseline-on-migrate=true` e versão `0` para suportar instalações anteriores. H2 e teste começam vazios e não precisam habilitar essa conveniência: o Flyway seleciona `B1` como ponto inicial automaticamente.

Alternativa considerada: ativar baseline automática em todos os perfis. Foi rejeitada por não agregar valor aos bancos descartáveis e por ampliar uma configuração que reduz a proteção contra apontar acidentalmente para o banco errado.

### Validar comportamento, histórico e mapeamento

Os testes existentes serão ajustados para confirmar que os contextos H2 realmente executam Flyway e Hibernate `validate`. O teste de migração verificará entradas e checksums não nulos em `flyway_schema_history`, repetirá `migrate` e confirmará ausência de reaplicação. O gate PostgreSQL existente continuará iniciando um serviço vazio e validando a aplicação real.

Não será criado um teste de concorrência artificial dependente de temporização; a proteção de lock é responsabilidade do mecanismo de migration e será documentada. A pipeline continuará sendo a evidência de compatibilidade nos dois bancos.

### Documentar a equivalência didática Liquibase–Flyway

A documentação explicará que o diretório ordenado do Flyway substitui o master/changelog, cada arquivo versionado equivale a uma evolução, `flyway_schema_history` concentra histórico e checksum e o próprio Flyway adquire lock durante alterações. Como o fluxo adotado é progressivo, correções serão feitas com nova migration; não haverá rollback automático destrutivo na inicialização.

Também serão documentados banco vazio, reinicialização, consulta do histórico, banco legado com backup e baseline, geração/revisão manual da baseline e diagnósticos de tabela existente, checksum, lock e divergência do Hibernate.

### Manter dados iniciais separados do schema

O Flyway continuará responsável apenas pela estrutura. O bootstrap opcional do administrador permanece idempotente por e-mail e não será movido para migration, pois contém credenciais configuráveis e pertence à inicialização da aplicação.

## Risks / Trade-offs

- [SQL compartilhado pode usar sintaxe não suportada por H2] → manter H2 em modo PostgreSQL e executar o gate H2 em cada pull request.
- [Contextos de teste passam a compartilhar histórico no banco em memória] → limpar apenas dados de domínio entre testes e nunca apagar `flyway_schema_history` durante a suíte.
- [Baseline automática pode aceitar um banco PostgreSQL incorreto] → restringi-la aos perfis PostgreSQL, exigir backup e documentar a conferência de URL e estrutura antes do primeiro início.
- [Versão H2 mais nova que a faixa verificada pelo Flyway gera aviso] → manter versões gerenciadas pelo Spring Boot e tratar qualquer incompatibilidade real como falha do gate, sem fixação manual prematura.
- [Flyway Community não oferece o mesmo modelo de rollback XML do exemplo Liquibase] → adotar correção progressiva por nova migration e documentar restauração por backup quando uma reversão de dados for necessária.

## Migration Plan

1. Ajustar os perfis H2 e test e executar os testes de inicialização direcionados.
2. Reforçar as asserções de histórico, checksum e segunda execução sem modificar `B1` ou `V1`.
3. Executar a suíte H2 completa e o gate PostgreSQL contra banco descartável vazio.
4. Atualizar a documentação operacional e validar que os exemplos não contêm credenciais.
5. Implantar normalmente: bancos PostgreSQL já versionados não recebem alteração estrutural, e bancos H2 descartáveis são recriados pelas migrations.

Em caso de regressão antes da entrega, revertem-se apenas as configurações e testes desta change. Não há migration nova a desfazer. Em banco real, qualquer correção estrutural futura deve avançar com nova versão ou restaurar um backup validado; arquivos aplicados e a tabela de histórico não devem ser editados manualmente.
