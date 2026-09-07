# Bom Investidor

API REST em Java 17 e Spring Boot para gestão privada de corretoras, carteiras, lançamentos, posições, valorização, evolução patrimonial e proventos.

## Início rápido

Defina um `JWT_SECRET` com pelo menos 32 bytes e execute `./mvnw.cmd spring-boot:run` no Windows. O perfil padrão usa H2 e atende em `http://localhost:8080`.

- OpenAPI: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

Consulte [configuração](docs/configuration.md), [guia da API](docs/api-guide.md) e [operação](docs/operations.md).

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

O GitHub Actions executa ambos em pull requests e em pushes para `dev`. Os gates usam dublês determinísticos para integrações externas; smoke tests reais são opcionais e não fazem parte do pipeline obrigatório.

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
