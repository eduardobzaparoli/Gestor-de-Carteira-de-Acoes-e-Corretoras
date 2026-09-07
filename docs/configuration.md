# Configuração

A aplicação usa Java 17 e perfis Spring. `dev` é o perfil padrão com H2; `postgres` serve à validação local; `prod` usa PostgreSQL com esquema validado pelo Flyway.

## Variáveis essenciais

| Variável | Perfis | Obrigatória | Padrão/finalidade |
| --- | --- | --- | --- |
| `JWT_SECRET` | dev, postgres, prod | Sim ao executar a aplicação | Segredo com ao menos 32 bytes |
| `JWT_EXPIRATION` | todos | Não | `PT1H` |
| `DB_URL` | postgres, prod | Sim | URL JDBC PostgreSQL |
| `DB_USERNAME` | postgres, prod | Sim | Usuário do banco |
| `DB_PASSWORD` | postgres, prod | Sim | Senha do banco |
| `CORS_ALLOWED_ORIGINS` | todos | Não | Lista separada por vírgulas; vazia bloqueia CORS |
| `ADMIN_BOOTSTRAP_NAME` | todos | Não | Nome do administrador inicial |
| `ADMIN_BOOTSTRAP_EMAIL` | todos | Não | E-mail do administrador inicial |
| `ADMIN_BOOTSTRAP_PASSWORD` | todos | Não | Senha do administrador inicial |

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

Nunca versionar valores reais de senhas, tokens ou chaves.
