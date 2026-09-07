## Purpose

Definir uma configuração local reproduzível e segura, separando modelos versionados, segredos locais e variáveis efetivamente fornecidas ao processo da aplicação.

## ADDED Requirements

### Requirement: Modelo de ambiente seguro e versionável
O projeto SHALL fornecer um arquivo de exemplo versionado que identifique o perfil de desenvolvimento, a conexão PostgreSQL e os demais valores obrigatórios para iniciar a aplicação. Esse modelo MUST conter somente valores públicos ou inequivocamente fictícios e MUST NOT funcionar como repositório de segredos reais.

#### Scenario: Desenvolvedor prepara o ambiente local
- **WHEN** um desenvolvedor consulta o modelo versionado
- **THEN** encontra `SPRING_PROFILES_ACTIVE`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` e `JWT_SECRET` documentados sem credenciais reais

#### Scenario: Modelo é enviado ao repositório
- **WHEN** o arquivo de exemplo é incluído em um commit
- **THEN** nenhum segredo local, token de provedor ou senha verdadeira é publicado

### Requirement: Arquivos locais de ambiente protegidos
O projeto MUST excluir do versionamento `.env` e suas variantes locais, mantendo somente `.env.example` como exceção versionável. A documentação MUST alertar que remover posteriormente um segredo do arquivo não o elimina do histórico do Git.

#### Scenario: Arquivo local contém credenciais
- **WHEN** um desenvolvedor cria `.env` ou uma variante `.env.*`
- **THEN** o Git ignora esse arquivo e não o inclui entre as alterações versionáveis

#### Scenario: Arquivo de exemplo é mantido
- **WHEN** o repositório contém `.env.example`
- **THEN** a regra de exclusão não impede que esse modelo seja versionado

### Requirement: Desenvolvimento com PostgreSQL externalizado
O perfil `dev` SHALL configurar PostgreSQL por placeholders de ambiente, permitindo padrão local somente para URL e usuário e exigindo `DB_PASSWORD` sem valor padrão. O H2 SHALL permanecer disponível por um perfil local explícito e pelo perfil de testes automatizados.

#### Scenario: Perfil de desenvolvimento recebe configuração válida
- **WHEN** a aplicação inicia com `SPRING_PROFILES_ACTIVE=dev` e recebe as variáveis obrigatórias válidas
- **THEN** configura o datasource PostgreSQL sem depender de senha armazenada no repositório

#### Scenario: Senha do banco não é fornecida
- **WHEN** a aplicação inicia no perfil `dev` sem `DB_PASSWORD`
- **THEN** a inicialização falha em vez de assumir uma senha compartilhada ou conhecida

#### Scenario: Desenvolvedor escolhe H2 explicitamente
- **WHEN** a aplicação inicia com o perfil H2 e recebe os demais segredos obrigatórios da aplicação
- **THEN** utiliza um banco em memória sem exigir credenciais PostgreSQL

### Requirement: Arquivo `.env` não é carregado implicitamente
O projeto MUST depender das variáveis fornecidas pelo sistema operacional, pela IDE ou pelo processo de execução e MUST NOT adicionar uma biblioteca ou mecanismo implícito para transformar `.env` em configuração da aplicação. A documentação SHALL explicar como transferir os pares do arquivo local para o IntelliJ IDEA ou para o terminal.

#### Scenario: Execução pelo IntelliJ IDEA
- **WHEN** um desenvolvedor configura na execução da IDE os pares mantidos em seu `.env`
- **THEN** o Spring Boot recebe esses valores como variáveis do processo

#### Scenario: Execução pelo terminal
- **WHEN** um desenvolvedor exporta as variáveis na sessão atual antes de iniciar o Maven
- **THEN** a aplicação resolve os placeholders a partir dessa sessão sem ler diretamente o arquivo `.env`

### Requirement: Configuração preserva os gates de release
O sistema MUST manter verificações reproduzíveis para H2 e PostgreSQL após a reorganização dos perfis, sem exigir segredos reais no pipeline.

#### Scenario: Gate H2 é executado
- **WHEN** a suíte de release H2 é iniciada
- **THEN** os testes usam a configuração isolada de teste e não dependem do PostgreSQL local

#### Scenario: Gate PostgreSQL é executado
- **WHEN** a suíte de release PostgreSQL recebe credenciais descartáveis do banco de teste
- **THEN** as migrações e integrações de persistência são validadas sem utilizar credenciais pessoais
