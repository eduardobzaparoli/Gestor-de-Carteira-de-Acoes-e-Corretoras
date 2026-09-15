## Context

Veja `proposal.md` para a motivação e `specs/environment-configuration/spec.md` para o contrato. Atualmente, `dev` é o perfil padrão e contém H2, enquanto `postgres` e `prod` já usam placeholders para o datasource. A aplicação também exige `JWT_SECRET`, e a suíte integrada escolhe explicitamente os perfis `test` ou `postgres`.

## Goals / Non-Goals

**Goals:**

- Reproduzir a separação didática entre modelo versionado, arquivo local e ambiente do processo.
- Tornar PostgreSQL o ambiente normal de desenvolvimento sem incorporar credenciais ao repositório.
- Preservar uma execução H2 explícita e os dois gates de release.
- Validar por testes e inspeções automatizadas as regras críticas de segurança da configuração.

**Non-Goals:**

- Carregar `.env` automaticamente.
- Adicionar biblioteca dotenv ou gerenciador de segredos.
- Alterar configuração de produção, esquema de banco ou comportamento dos endpoints.
- Criar, versionar ou preencher o `.env` pessoal do desenvolvedor.
- Reescrever histórico Git; a inspeção atual não encontrou `.env` previamente rastreado.

## Decisions

### 1. `dev` representará o desenvolvimento integrado com PostgreSQL

Mover a configuração PostgreSQL para `application-dev.properties`, com padrão público para URL e usuário e sem padrão para `DB_PASSWORD`. Isso segue a estrutura solicitada e faz falhar cedo uma inicialização sem senha.

Alternativa considerada: manter `dev` em H2 e usar `SPRING_PROFILES_ACTIVE=postgres` no exemplo. Embora segura, essa alternativa divergiria da convenção apresentada pelo professor e manteria o desenvolvimento principal distante do banco de produção.

### 2. H2 ficará em perfil explícito

Criar `application-h2.properties` a partir da configuração H2 atual. `application-test.properties` continua isolando a suíte, e os testes PostgreSQL mantêm o perfil `postgres` existente. Assim, o requisito global de suporte a H2 permanece atendido sem torná-lo implícito em `dev`.

Alternativa considerada: remover H2 da execução local. Isso reduziria opções úteis e contrariaria a especificação global do produto.

### 3. `.env.example` documentará somente a inicialização essencial

O modelo conterá `SPRING_PROFILES_ACTIVE`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` e `JWT_SECRET`. Senha e JWT usarão textos fictícios inequívocos; chaves opcionais de integrações continuarão documentadas em `docs/configuration.md`, evitando transformar o modelo mínimo em catálogo difícil de manter.

Alternativa considerada: listar todas as variáveis opcionais. Isso aumenta ruído e incentiva cópias de valores desnecessários para a inicialização básica.

### 4. `.env` continuará sendo referência, não fonte automática do Spring

Não será usado `spring.config.import` nem biblioteca dotenv. O IntelliJ ou o shell fornecerá as variáveis ao processo, reproduzindo exatamente a separação conceitual da orientação recebida e evitando nova dependência.

### 5. Regras de segurança serão verificadas no repositório

Adicionar testes focados na configuração versionada para assegurar a presença dos placeholders obrigatórios, a ausência de senha padrão em `dev`, a permanência do perfil H2 e a proteção de `.env`. Os gates existentes continuarão sendo a validação final de execução.

## Risks / Trade-offs

- [Quem executava sem escolher perfil passará a precisar do PostgreSQL] → Documentar a mudança incompatível e o uso explícito de `h2`.
- [Valor fictício do exemplo ser reutilizado fora do ambiente local] → Usar marcadores inequívocos e alertar que devem ser substituídos.
- [Duplicação entre `dev`, `postgres` e `prod`] → Manter cada perfil simples e com finalidade explícita; evitar abstração adicional nesta change.
- [`.env` já ter sido publicado] → Verificar rastreamento e histórico; se um segredo real for encontrado, interromper e exigir rotação antes de prosseguir.
- [Configuração local divergir da CI] → Executar gates H2 e PostgreSQL após a mudança.

## Migration Plan

1. Adicionar as regras de exclusão e o modelo versionável antes de qualquer configuração local.
2. Criar o perfil H2 explícito e converter `dev` para PostgreSQL externalizado.
3. Atualizar README e guia de configuração com IntelliJ, PowerShell, Linux/macOS e verificação do Git.
4. Executar testes direcionados, gate H2, gate PostgreSQL, validação OpenSpec e varredura de segredos.
5. Para rollback, restaurar o conteúdo H2 de `application-dev.properties` e remover o perfil `h2`; `.env.example` e as proteções do Git podem permanecer por serem compatíveis e seguras.
