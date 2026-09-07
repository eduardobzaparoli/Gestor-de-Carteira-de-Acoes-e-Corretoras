## 1. Proteção e modelo de ambiente

- [x] 1.1 Adicionar ao `.gitignore` as regras para ignorar `.env` e variantes, preservando `.env.example`.
- [x] 1.2 Criar `.env.example` com perfil `dev`, conexão PostgreSQL, senha fictícia e JWT fictício de tamanho válido, sem incluir segredos reais.
- [x] 1.3 Verificar pelo Git que `.env` é ignorado, `.env.example` é versionável e nenhum arquivo local de ambiente foi rastreado.

## 2. Perfis Spring

- [x] 2.1 Criar o perfil explícito `h2` a partir da configuração em memória atualmente usada por `dev`.
- [x] 2.2 Converter `application-dev.properties` para PostgreSQL com padrões somente para `DB_URL` e `DB_USERNAME` e com `DB_PASSWORD` obrigatório.
- [x] 2.3 Preservar as diferenças necessárias entre `dev`, `postgres`, `prod` e `test`, incluindo Flyway, validação Hibernate, console H2 e níveis de log.

## 3. Verificações automatizadas

- [x] 3.1 Adicionar testes que validem os placeholders obrigatórios do perfil `dev` e a ausência de senha padrão.
- [x] 3.2 Testar que o perfil `h2` permanece autocontido e não exige variáveis PostgreSQL.
- [x] 3.3 Testar as regras de exclusão de `.env` e a segurança dos valores publicados em `.env.example`.

## 4. Documentação de execução

- [x] 4.1 Atualizar o início rápido e a documentação de perfis para refletir PostgreSQL em `dev` e H2 como escolha explícita.
- [x] 4.2 Documentar a criação local de `.env`, sua função de referência e a configuração equivalente no IntelliJ IDEA.
- [x] 4.3 Documentar os comandos de execução no PowerShell e em Linux/macOS, sem carregar `.env` implicitamente.
- [x] 4.4 Incluir orientações de segurança, verificação com `git check-ignore` e resposta a eventual exposição de segredo.

## 5. Validação final

- [x] 5.1 Executar os testes direcionados de configuração e revisar que nenhuma dependência dotenv ou carregamento implícito foi adicionado.
- [x] 5.2 Executar os gates completos H2 e PostgreSQL e validar a change OpenSpec estritamente.
- [x] 5.3 Atualizar o mapa local do Graphify e executar uma varredura final por segredos e arquivos `.env` rastreados.
