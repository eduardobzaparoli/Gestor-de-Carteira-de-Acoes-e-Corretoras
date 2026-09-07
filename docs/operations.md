# Operação

## Implantação e migração

Configure `JWT_SECRET`, `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`, faça backup do banco e execute o gate PostgreSQL antes da implantação. Ao iniciar com `prod`, o Flyway aplica migrações versionadas e o Hibernate valida o esquema. Considere a instância pronta somente após `/actuator/health` responder `UP`.

O procedimento completo para banco vazio, banco legado, histórico, checksum, concorrência e diagnóstico está no [guia de versionamento do banco](database-migrations.md).

## Rollback e recuperação

Em falha de aplicação, restaure a versão anterior do artefato. Migrações já aplicadas não devem ser apagadas nem alteradas; restaure o backup em um banco separado quando uma reversão de dados for necessária e valide-o antes de redirecionar tráfego. Registre a versão da aplicação, a versão do Flyway e o horário do backup usado.

## Backup

Use a ferramenta de backup compatível com a versão do PostgreSQL e valide periodicamente uma restauração. Proteja o arquivo com os mesmos controles do banco e nunca o inclua no repositório.

## Smoke tests opcionais

Após a implantação, valide health, cadastro/login e um fluxo isolado de carteira. Chamadas reais aos provedores devem ser executadas manualmente com chaves próprias e baixa frequência; elas não fazem parte dos gates obrigatórios porque estão sujeitas a rede, plano e limite de uso.

## Diagnóstico

Logs de produção usam níveis conservadores. Aumente o nível apenas temporariamente e nunca habilite registro de cabeçalhos `Authorization`, corpos de login, senhas ou chaves de integração.
