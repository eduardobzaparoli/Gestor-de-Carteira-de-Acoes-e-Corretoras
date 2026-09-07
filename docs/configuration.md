# Configuração

A aplicação usa Java 17 e perfis Spring. `dev` é o perfil padrão com PostgreSQL, `h2` oferece execução local em memória, `test` isola a suíte automatizada, `postgres` valida a integração real e `prod` exige toda a conexão PostgreSQL externalizada.

## Perfis

| Perfil | Banco | Finalidade |
| --- | --- | --- |
| `dev` | PostgreSQL | Desenvolvimento integrado; URL e usuário possuem padrões locais, mas a senha é obrigatória |
| `h2` | H2 em memória | Execução local explícita sem PostgreSQL |
| `test` | H2 em memória | Testes automatizados que não dependem de infraestrutura externa |
| `postgres` | PostgreSQL | Testes de integração e validação de migrações |
| `prod` | PostgreSQL | Produção, sem padrões para a conexão |

Todos os perfis habilitam o Flyway, usam as migrations compartilhadas em `classpath:db/migration` e configuram o Hibernate somente para validar o esquema. Apenas os perfis PostgreSQL aceitam a baseline automática necessária para bancos legados. Veja o [guia de versionamento do banco](database-migrations.md).

## Variáveis essenciais

| Variável | Perfis | Obrigatória | Padrão/finalidade |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | todos | Não | `dev`; seleciona explicitamente o perfil quando informado |
| `JWT_SECRET` | dev, h2, postgres, prod | Sim ao executar a aplicação | Segredo com ao menos 32 bytes |
| `JWT_EXPIRATION` | todos | Não | `PT1H` |
| `DB_URL` | dev, postgres, prod | Em `prod` | Em `dev` há o padrão `jdbc:postgresql://localhost:5432/bominvestidor` |
| `DB_USERNAME` | dev, postgres, prod | Em `postgres` e `prod` | Em `dev` há o padrão `postgres` |
| `DB_PASSWORD` | dev, postgres, prod | Sim | Senha do banco, sem valor padrão |
| `CORS_ALLOWED_ORIGINS` | todos | Não | Lista separada por vírgulas; vazia bloqueia CORS |
| `ADMIN_BOOTSTRAP_NAME` | todos | Não | Nome do administrador inicial |
| `ADMIN_BOOTSTRAP_EMAIL` | todos | Não | E-mail do administrador inicial |
| `ADMIN_BOOTSTRAP_PASSWORD` | todos | Não | Senha do administrador inicial |

## Arquivo `.env` local

O repositório fornece `.env.example` somente como modelo. Crie sua cópia local:

```powershell
Copy-Item .env.example .env
```

No Linux ou macOS:

```bash
cp .env.example .env
```

Edite `.env` e substitua os valores iniciados por `troque-`. Não use espaços em torno de `=` e não reutilize senhas pessoais ou institucionais.

O Spring Boot **não carrega `.env` automaticamente** neste projeto. O arquivo guarda os valores locais como referência; o sistema operacional, a IDE ou o processo que inicia a aplicação precisa fornecê-los como variáveis de ambiente. Nenhuma biblioteca dotenv é utilizada.

### IntelliJ IDEA

1. Acesse **Run → Edit Configurations**.
2. Selecione a configuração Spring Boot da classe `Application`.
3. Em **Modify options**, habilite **Environment variables** se necessário.
4. Abra o editor do campo e cadastre os mesmos pares presentes no seu `.env`.
5. Mantenha habilitada a inclusão das variáveis do sistema, aplique e execute.

As configurações pessoais do IntelliJ ficam normalmente em `.idea`, que é ignorada pelo Git.

### Visual Studio Code

Instale o **Extension Pack for Java** e crie localmente o arquivo `.vscode/launch.json`:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Bom Investidor (dev)",
      "request": "launch",
      "mainClass": "com.bominvestidor.spring.Application",
      "envFile": "${workspaceFolder}/.env",
      "console": "integratedTerminal"
    }
  ]
}
```

Em **Executar e Depurar**, selecione **Bom Investidor (dev)** e pressione `F5`. O depurador Java do VS Code lê o arquivo indicado por `envFile` e entrega os valores como variáveis de ambiente ao processo Java; o Spring Boot continua sem carregar `.env` diretamente.

O diretório `.vscode` é ignorado pelo Git neste projeto, portanto essa configuração permanece local. Reinicie a aplicação depois de alterar o `.env` para que os novos valores sejam carregados.

### PowerShell

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:DB_URL = "jdbc:postgresql://localhost:5432/bominvestidor"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "sua-senha-local"
$env:JWT_SECRET = "seu-segredo-local-com-pelo-menos-32-bytes"
.\mvnw.cmd spring-boot:run
```

Esses valores valem somente para a sessão atual do PowerShell.

### Linux ou macOS

```bash
export SPRING_PROFILES_ACTIVE=dev
export DB_URL=jdbc:postgresql://localhost:5432/bominvestidor
export DB_USERNAME=postgres
export DB_PASSWORD='sua-senha-local'
export JWT_SECRET='seu-segredo-local-com-pelo-menos-32-bytes'
./mvnw spring-boot:run
```

Use aspas para valores que contenham caracteres interpretados pelo shell.

### Execução com H2

O perfil H2 não precisa de variáveis do PostgreSQL:

```powershell
$env:SPRING_PROFILES_ACTIVE = "h2"
$env:JWT_SECRET = "seu-segredo-local-com-pelo-menos-32-bytes"
.\mvnw.cmd spring-boot:run
```

## Integrações

As URLs possuem padrões públicos configurados em `application.properties`. As variáveis disponíveis são:

| Variável | Obrigatória | Padrão/finalidade |
| --- | --- | --- |
| `BRASIL_API_BASE_URL` | Não | `https://brasilapi.com.br` |
| `VIA_CEP_BASE_URL` | Não | `https://viacep.com.br` |
| `CVM_SNAPSHOT_URL` | Não | Snapshot oficial de intermediários da CVM |
| `BRAPI_BASE_URL` | Não | `https://brapi.dev` |
| `BRAPI_TOKEN` | Não | Token para capacidades contratadas da Brapi |
| `ALPHA_VANTAGE_BASE_URL` | Não | `https://www.alphavantage.co` |
| `ALPHA_VANTAGE_API_KEY` | Não | Chave da Alpha Vantage |
| `INTEGRATIONS_CONNECT_TIMEOUT` | Não | `PT3S` |
| `INTEGRATIONS_READ_TIMEOUT` | Não | `PT5S` |
| `CVM_CACHE_TTL` | Não | `PT24H` |
| `CVM_ACTIVE_STATUS` | Não | Estado cadastral considerado ativo |
| `CVM_MAX_SNAPSHOT_BYTES` | Não | `10000000` |
| `ASSET_SEARCH_CACHE_TTL` | Não | `PT5M` |
| `ASSET_QUOTE_CACHE_TTL` | Não | `PT1M` |
| `EXCHANGE_RATE_CACHE_TTL` | Não | `PT1H` |
| `INCOME_CANDIDATE_CACHE_TTL` | Não | `PT10M` |
| `HISTORICAL_PRICE_WINDOW_DAYS` | Não | `90` |
| `HISTORICAL_PRICE_CACHE_TTL` | Não | `PT15M` |

`BRAPI_TOKEN` e `ALPHA_VANTAGE_API_KEY` são opcionais para iniciar, mas capacidades dependentes do respectivo provedor podem responder com indisponibilidade.

## Segurança e verificação

Nunca versione valores reais de senhas, tokens ou chaves. Confirme as regras antes de criar um commit:

```powershell
git check-ignore -v .env
git status
```

O `.env` não deve aparecer entre os arquivos do commit, enquanto `.env.example` deve permanecer versionável. O `.gitignore` não remove um arquivo previamente rastreado nem apaga segredos do histórico. Se uma credencial real for publicada, considere-a comprometida, troque-a imediatamente e trate a limpeza do histórico separadamente.
