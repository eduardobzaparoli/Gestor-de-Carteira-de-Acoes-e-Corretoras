## Why

Hoje a aplicação exige que frontend, backend e PostgreSQL sejam preparados e iniciados separadamente, o que aumenta a dificuldade de configuração e produz diferenças entre ambientes. Uma composição Docker reproduzível permitirá executar toda a solução com um único fluxo, preservando segurança, persistência e os perfis já existentes.

## What Changes

- Criar imagens independentes e enxutas para o backend Spring Boot e o frontend React servido por Nginx, usando builds em múltiplas etapas.
- Criar uma composição Docker com frontend, backend e PostgreSQL em rede interna, inicialização coordenada e verificações de saúde.
- Persistir os dados PostgreSQL em volume nomeado e manter o H2 apenas como banco embarcado para testes ou execução explícita.
- Externalizar senhas, segredo JWT e chaves de integrações por variáveis de ambiente, sem incorporá-los às imagens ou ao bundle público.
- Configurar o frontend para encaminhar as chamadas da API ao backend dentro da composição, evitando dependência de endereços internos no navegador.
- Adicionar exclusões de contexto de build e documentação para construir, iniciar, parar, reconstruir, inspecionar logs e remover o ambiente com segurança.
- Manter disponíveis os fluxos atuais de execução sem Docker.

## Capabilities

### New Capabilities

- `containerized-application-runtime`: Execução reproduzível da aplicação completa em contêineres, incluindo rede, persistência, configuração, saúde e operação local.

### Modified Capabilities

Nenhuma. Os contratos funcionais existentes da API e da interface permanecem inalterados.

## Impact

- Novos arquivos de imagem e composição Docker no projeto e no diretório do frontend.
- Configuração do Nginx para servir a aplicação de página única e encaminhar chamadas da API.
- Modelos de ambiente e documentação operacional atualizados sem inclusão de credenciais reais.
- Builds Maven e Node executados durante a criação das imagens; nenhuma nova dependência de execução é adicionada ao código da aplicação.
- Desenvolvedores poderão usar Docker Desktop com Docker Compose, mas a execução direta com Maven, Vite, H2 ou PostgreSQL continuará suportada.
