# Execução com Docker

Este guia inicia o Bom Investidor completo sem instalar Java, Node.js ou PostgreSQL diretamente no Windows. É necessário instalar somente o Docker Desktop.

## O que o Docker fará

- **Imagem:** pacote reproduzível com o programa e o ambiente necessário para executá-lo.
- **Contêiner:** uma execução isolada de uma imagem.
- **Serviço:** uma parte da aplicação descrita em `compose.yaml`.
- **Rede:** canal privado usado pelos serviços para se comunicarem.
- **Volume:** armazenamento persistente que mantém os dados do PostgreSQL mesmo após os contêineres serem recriados.

O projeto inicia três serviços:

```text
Navegador -> frontend -> backend -> database
              :5173       :8080     rede interna
```

O `frontend` serve a interface React e encaminha chamadas `/api` ao `backend`. O `backend` executa a API Spring Boot. O `database` executa PostgreSQL e não publica sua porta no Windows por padrão.

## 1. Instalar no Windows

1. Instale o [Docker Desktop para Windows](https://docs.docker.com/desktop/setup/install/windows-install/).
2. Durante a instalação, mantenha habilitado o mecanismo baseado em WSL 2 quando essa opção for apresentada.
3. Reinicie o Windows se o instalador solicitar.
4. Abra o Docker Desktop e aguarde a indicação de que o mecanismo está em execução.
5. Abra um novo PowerShell e confirme:

```powershell
docker --version
docker compose version
```

Se ambos mostrarem uma versão, o ambiente está pronto. Não é necessário instalar o comando antigo `docker-compose` separado.

## 2. Preparar as configurações

Na raiz do projeto, crie `.env` somente se ele ainda não existir:

```powershell
if (-not (Test-Path .env)) {
    Copy-Item .env.example .env
}
```

Abra `.env` e substitua ao menos estes valores fictícios:

```text
DB_USERNAME=postgres
DB_PASSWORD=escolha-uma-senha-local-forte
JWT_SECRET=escolha-um-segredo-local-com-pelo-menos-32-bytes
```

As chaves `BRAPI_TOKEN`, `TWELVE_DATA_API_KEY` e `ALPHA_VANTAGE_API_KEY` são necessárias somente para os recursos atendidos por cada provedor. Nunca coloque credenciais reais em `frontend/.env*` ou em variáveis `VITE_*`, pois elas podem se tornar públicas no navegador.

O Docker Compose lê o `.env` para substituir e entregar explicitamente as variáveis aos serviços. O Spring Boot continua sem ler esse arquivo por conta própria quando executado fora do Docker.

## 3. Validar e iniciar

Com o Docker Desktop aberto, execute na raiz do projeto:

```powershell
docker compose config --quiet
docker compose up --build --detach
docker compose ps
```

O primeiro build pode demorar porque baixa as imagens e dependências. Os próximos normalmente reutilizam o cache.

Quando os três serviços aparecerem como saudáveis, acesse:

- Interface: `http://localhost:5173`
- Saúde da API: `http://localhost:8080/actuator/health`
- Swagger: `http://localhost:8080/swagger-ui.html`

As portas podem ser alteradas no `.env` com `FRONTEND_PORT` e `BACKEND_PORT` se já estiverem ocupadas.

## Operações comuns

Consultar estado:

```powershell
docker compose ps
```

Consultar os logs de todos os serviços:

```powershell
docker compose logs --follow
```

Consultar somente um serviço:

```powershell
docker compose logs --follow backend
docker compose logs --follow frontend
docker compose logs --follow database
```

Parar a aplicação preservando os dados:

```powershell
docker compose down
```

Reconstruir depois de alterar o código:

```powershell
docker compose up --build --detach
```

Reiniciar sem reconstruir:

```powershell
docker compose restart
```

## Apagar todo o ambiente local

> **Atenção:** o comando abaixo remove também o volume do PostgreSQL. Usuários, carteiras, ativos, lançamentos e todos os demais dados locais serão apagados. Essa operação não possui desfazer pelo Docker.

Use somente quando quiser começar com um banco vazio:

```powershell
docker compose down --volumes --remove-orphans
```

Uma parada normal com `docker compose down` não remove o volume.

## Diagnóstico

### O comando `docker` não existe

Instale o Docker Desktop, conclua qualquer reinicialização solicitada e abra um terminal novo. Se ele já estiver instalado, abra o aplicativo e aguarde o mecanismo iniciar.

### Uma porta já está sendo usada

Altere no `.env`:

```text
FRONTEND_PORT=5174
BACKEND_PORT=8081
```

Depois execute novamente `docker compose up --build --detach` e use as novas portas no navegador.

### Um serviço não fica saudável

Execute `docker compose ps` para identificar o serviço e consulte seu log, por exemplo:

```powershell
docker compose logs backend
```

Falhas do `database` normalmente indicam credenciais inválidas ou um volume criado anteriormente com outras credenciais. Falhas do `backend` podem indicar configuração ausente ou erro de migration. Não publique logs que contenham informações privadas.

### Alterei uma variável, mas nada mudou

Variáveis do backend exigem recriação do serviço. A URL pública usada pelo frontend é definida durante o build. Para garantir a atualização:

```powershell
docker compose up --build --detach --force-recreate
```

## Execução sem Docker

O Docker é uma opção adicional. Os fluxos existentes com Maven, Vite, PostgreSQL local e perfil H2 continuam disponíveis no [README](../README.md) e no [guia de configuração](configuration.md).
