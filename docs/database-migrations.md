# Versionamento do banco de dados com Flyway

O esquema do Bom Investidor é versionado pelo Flyway. A aplicação usa a mesma sequência de migrations em H2 e PostgreSQL, e o Hibernate apenas valida se o resultado corresponde às entidades Java.

## Responsabilidades

| Componente | Responsabilidade |
| --- | --- |
| JPA | Define o modelo de persistência por meio de entidades e relacionamentos Java |
| Hibernate | Implementa a JPA e, neste projeto, valida o esquema com `ddl-auto=validate` |
| Flyway | Cria e evolui o esquema de forma ordenada, rastreável e repetível |

O Hibernate não cria nem atualiza tabelas. Se uma entidade exigir uma nova coluna, a alteração deve ser feita em uma nova migration e só então refletida ou validada pelo Hibernate.

No Liquibase, um arquivo mestre referencia changelogs compostos por `changeSet`s. No Flyway deste projeto, `classpath:db/migration` é o diretório compartilhado que o Spring Boot examina automaticamente, e cada arquivo SQL versionado equivale a uma alteração ordenada. A tabela `DATABASECHANGELOG` do Liquibase corresponde à `flyway_schema_history` do Flyway. O bloqueio registrado em `DATABASECHANGELOGLOCK` é realizado pelo Flyway usando o mecanismo de bloqueio do banco associado ao histórico de schema, sem exigir uma tabela de lock separada.

## Convenção e migrations atuais

Os arquivos seguem `<prefixo><versão>__<descrição>.sql`:

- `B1__initial_schema.sql` é a baseline migration usada para criar o esquema completo em um banco vazio.
- `V1__add_user_status.sql` é a migration incremental para bancos legados que já possuíam as tabelas antes da adoção do Flyway.
- uma mudança futura deve receber uma versão inédita, por exemplo `V2__add_portfolio_goal.sql`.

`B` identifica uma baseline migration e `V` uma migration versionada. As versões precisam ser únicas e são aplicadas em ordem. Os arquivos `B1` e `V1` já foram publicados: não devem ser renomeados, removidos nem editados. Qualquer correção deve entrar em uma nova migration.

## Histórico, checksum e concorrência

O Flyway cria `flyway_schema_history` e registra, entre outros dados, ordem de instalação, versão, descrição, tipo, nome do script, checksum, data e sucesso. No PostgreSQL, consulte:

```sql
SELECT installed_rank, version, description, type, script,
       checksum, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

O checksum permite detectar alterações em um script já aplicado. Uma divergência deve ser investigada; não se deve alterar o arquivo antigo para fazer a validação passar. Crie uma nova migration que leve o banco do estado atual ao estado desejado.

Durante a migração, o Flyway coordena o acesso ao histórico com os mecanismos de bloqueio do banco. Se duas instâncias iniciarem juntas, uma delas pode aguardar enquanto a outra migra. Uma espera persistente exige verificar sessões, transações e logs antes de encerrar qualquer processo.

## Primeira execução em banco vazio

1. Confirme `DB_URL`, `DB_USERNAME` e o banco de destino.
2. Faça backup se o banco não for descartável.
3. Inicie a aplicação. O Flyway cria o histórico e aplica `B1`.
4. O Hibernate valida o esquema criado.
5. Consulte `flyway_schema_history` e confirme `success=true` e um checksum preenchido.
6. Reinicie a aplicação. Nenhuma migration deve ser reaplicada e os dados existentes devem permanecer intactos.

O bootstrap opcional do administrador não é uma migration. Ele cria dados da aplicação de forma idempotente depois que o esquema está pronto; reiniciar não deve duplicar o administrador.

## Banco PostgreSQL legado

Um banco legado é um banco não vazio que contém o esquema anterior, mas não possui `flyway_schema_history`.

Para ambientes descartáveis, prefira recriar o banco vazio e deixar `B1` montar todo o esquema. Para um banco com dados que precisam ser preservados:

1. gere e valide um backup restaurável;
2. confira o servidor e o nome retornado por `SELECT current_database(), current_user;`;
3. compare o esquema existente com a baseline esperada;
4. inicie usando um perfil PostgreSQL (`dev`, `postgres` ou `prod`). Nesses perfis, `baseline-on-migrate=true` registra a baseline `0` para o esquema não vazio;
5. confirme que `V1` foi aplicada uma única vez e que os usuários existentes receberam `status=ACTIVE`;
6. reinicie e confirme que não houve nova aplicação.

Não ative baseline automaticamente em H2 ou no perfil `test`: esses bancos começam vazios. Usar baseline no banco ou servidor errado pode marcar um esquema incompatível como conhecido; por isso a conferência do alvo e o backup são obrigatórios.

## Preparação e revisão de novas migrations

O DDL pode ser escrito a partir das entidades e do requisito aprovado ou comparado com uma exportação somente do esquema, por exemplo:

```powershell
pg_dump --schema-only --no-owner --no-privileges `
  --dbname="$env:DB_URL"
```

DDL gerado é material de revisão, não uma migration pronta. Remova detalhes específicos do ambiente, confirme compatibilidade com H2 em modo PostgreSQL e PostgreSQL real, avalie dados existentes, restrições, índices e ordem das operações. Depois, crie o próximo arquivo `V<versão>__<descrição>.sql` e execute os dois gates de release.

O Flyway é iniciado pelo Spring Boot; não é necessário instalar a CLI do Flyway para executar a aplicação.

## Diagnóstico

| Sintoma | Verificação e ação |
| --- | --- |
| `relation already exists` / tabela já existe | Confirme se o banco é legado sem histórico e se o perfil PostgreSQL está aplicando baseline; não apague dados sem backup |
| checksum divergente | Compare o script versionado com o que foi aplicado; restaure o arquivo publicado e faça a correção em uma migration nova |
| aplicação aguardando lock | Verifique outra instância migrando, sessões e transações abertas; aguarde a migração ativa ou trate a sessão órfã de forma controlada |
| Hibernate informa coluna ou tipo ausente | A migration não foi aplicada ou não corresponde às entidades; confira perfil, histórico e DDL antes de iniciar a aplicação |
| histórico registra `success=false` | Leia o erro original, corrija a causa e siga o procedimento de recuperação apropriado ao banco antes de tentar novamente |

## Correção e recuperação

O fluxo adotado é progressivo: uma falha funcional de esquema é corrigida por uma migration de versão posterior. Não se desfaz uma migration publicada editando ou apagando o SQL antigo. Se a reversão envolver perda ou transformação de dados, restaure o backup em um banco separado, valide-o e só então decida o redirecionamento da aplicação.

## Validação

Execute o gate H2:

```powershell
.\mvnw.cmd -Prelease-h2 test
```

Depois execute o gate PostgreSQL apontando para um banco de teste vazio e descartável:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/bom_investidor_test"
$env:DB_USERNAME = "bom_investidor_test"
$env:DB_PASSWORD = "senha-de-teste"
.\mvnw.cmd -Prelease-postgres test
```

Os dois gates usam as migrations de `src/main/resources/db/migration`. O gate PostgreSQL não deve apontar para o banco oficial nem para um banco com dados importantes.
