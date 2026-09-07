## Why

A configuração local ainda não segue a separação exigida entre arquivos versionados, valores locais e variáveis fornecidas ao processo. Precisamos impedir que segredos entrem no Git e tornar reproduzível a inicialização de desenvolvimento com PostgreSQL, sem perder o suporte existente a H2 e aos gates automatizados.

## What Changes

- Adicionar um `.env.example` versionado com nomes de variáveis e valores exclusivamente fictícios ou públicos.
- Ignorar `.env` e suas variantes locais, preservando a exceção versionável para `.env.example`.
- **BREAKING (ambiente de desenvolvimento):** fazer o perfil `dev` usar PostgreSQL e exigir `DB_PASSWORD`, em vez de iniciar com H2 implicitamente.
- Preservar H2 em um perfil explícito `h2` e no perfil `test` usado pela suíte automatizada.
- Manter os perfis `postgres` e `prod` com seus papéis atuais de integração e produção.
- Documentar que o Spring Boot não carrega `.env` automaticamente neste projeto e que IntelliJ ou terminal devem fornecer as variáveis ao processo.
- Verificar a ausência de segredos versionados e a compatibilidade dos gates H2 e PostgreSQL.

## Capabilities

### New Capabilities

- `environment-configuration`: separação segura entre modelo versionado, configuração local e variáveis de ambiente, incluindo os perfis de desenvolvimento e H2.

### Modified Capabilities

Nenhuma. Os contratos funcionais da API e das migrações PostgreSQL permanecem inalterados.

## Impact

- `.env.example` e `.gitignore` na raiz do repositório.
- Perfis em `src/main/resources/application-*.properties` e documentação de configuração e início rápido.
- Experiência de execução local no IntelliJ IDEA e PowerShell.
- Testes de configuração e gates Maven para H2 e PostgreSQL.
- Nenhuma nova dependência e nenhuma alteração de banco, endpoint ou regra de negócio.
