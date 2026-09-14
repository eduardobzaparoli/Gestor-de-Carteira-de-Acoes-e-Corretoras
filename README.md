# Bom Investidor

Aplicação completa para gestão privada de corretoras, carteiras, lançamentos, posições, valorização, evolução patrimonial e proventos. O backend usa Java 17 e Spring Boot; a interface web usa React, TypeScript e Vite.

## Início rápido com Docker

Com o Docker Desktop instalado e aberto, prepare o `.env` a partir do modelo, substitua os valores fictícios de `DB_PASSWORD` e `JWT_SECRET` e execute:

```powershell
docker compose up --build --detach
docker compose ps
```

A interface estará em `http://localhost:5173` e a API em `http://localhost:8080`. O banco permanece em um volume local quando a composição é parada normalmente.

Se o comando `docker` ainda não estiver disponível ou se esta for sua primeira experiência com contêineres, siga o [guia completo de Docker](docs/docker.md), que inclui instalação, conceitos, logs, diagnóstico e o alerta para remoção de dados.

## Execução sem Docker

O perfil padrão `dev` usa PostgreSQL. Crie seu arquivo local de referência a partir do modelo versionado:

```powershell
Copy-Item .env.example .env
```

Substitua em `.env` os valores fictícios de `DB_PASSWORD` e `JWT_SECRET`. O Spring Boot não lê esse arquivo automaticamente: carregue-o pela configuração de execução do VS Code, informe os mesmos pares no IntelliJ IDEA ou exporte-os no terminal, conforme o [guia de configuração](docs/configuration.md).

Com as variáveis disponíveis no processo, execute no Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Para uma execução local explícita com H2, defina `SPRING_PROFILES_ACTIVE=h2` e um `JWT_SECRET` com pelo menos 32 bytes. A API atende em `http://localhost:8080`.

- OpenAPI: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

Em outro terminal, instale e inicie o frontend (Node.js 22 recomendado):

```powershell
Set-Location frontend
npm ci
npm run dev
```

A interface estará em `http://localhost:5173`. Mantenha `CORS_ALLOWED_ORIGINS=http://localhost:5173` entre as variáveis do backend. Se a API estiver em outro endereço, copie `frontend/.env.example` para `frontend/.env.local` e ajuste somente `VITE_API_BASE_URL`. Variáveis `VITE_*` ficam públicas no bundle e nunca devem conter senhas, tokens ou chaves.

Consulte [Docker](docs/docker.md), [configuração](docs/configuration.md), [versionamento do banco](docs/database-migrations.md), [guia da API](docs/api-guide.md) e [operação](docs/operations.md).

## Validação de release

O gate H2 não usa serviços externos:

```powershell
./mvnw.cmd -Prelease-h2 test
```

O gate PostgreSQL exige um banco de teste vazio e as variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/bom_investidor_test"
$env:DB_USERNAME = "bom_investidor_test"
$env:DB_PASSWORD = "senha-de-teste"
./mvnw.cmd -Prelease-postgres test
```

Valide também a interface:

```powershell
Set-Location frontend
npm run format:check
npm run lint
npm test
npm run build
```

O GitHub Actions executa os gates H2, PostgreSQL e frontend em pull requests e em pushes para `dev`. Os gates usam dublês determinísticos para integrações externas; smoke tests reais são opcionais e não fazem parte do pipeline obrigatório.

## Graphify (opcional)

O projeto pode ser analisado localmente com o [Graphify](https://graphify.com/docs), que gera um grafo das relações entre arquivos, classes e métodos. A ferramenta é apenas um apoio ao desenvolvimento e não é uma dependência da aplicação.

### Configuração local

Com o `uv` instalado:

```powershell
uv tool install graphifyy
```

Para impedir que os artefatos locais apareçam no Git sem alterar o `.gitignore` do projeto, adicione `graphify-out/` ao arquivo local `.git/info/exclude`:

```powershell
if (-not (Select-String -Path .git/info/exclude -SimpleMatch "graphify-out/" -Quiet)) {
    Add-Content -Path .git/info/exclude -Value "graphify-out/"
}
```

Gere o grafo somente a partir do código, sem chave de API:

```powershell
graphify . --code-only
graphify cluster-only "."
```

Consulte o grafo em linguagem natural:

```powershell
graphify query "como a autenticação se conecta à persistência de usuários?"
```

Depois de mudanças no código, atualize o grafo local:

```powershell
graphify update .
```

Os arquivos gerados em `graphify-out/`, configurações locais e chaves de API não devem ser versionados.
