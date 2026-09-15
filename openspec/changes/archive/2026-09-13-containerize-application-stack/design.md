## Context

Consulte `proposal.md` para a motivação. O projeto possui um backend Spring Boot 4 com Java 17, frontend React/Vite, perfil `dev` baseado em PostgreSQL, perfil explícito H2 e endpoint público de saúde em `/actuator/health`. O frontend usa caminhos `/api/...` combinados com `VITE_API_BASE_URL`; as variáveis sensíveis já são externalizadas e os bancos PostgreSQL usam Flyway.

A solução precisa ser simples para uma pessoa iniciante, funcionar no Docker Desktop em Windows e não substituir os fluxos atuais de Maven e Vite.

## Goals / Non-Goals

**Goals:**

- Produzir uma composição local, reproduzível e próxima da execução de produção.
- Isolar frontend, backend e PostgreSQL, com rede, persistência e saúde explícitas.
- Manter as imagens finais menores que os ambientes de compilação e evitar execução privilegiada quando viável.
- Oferecer um único endereço da interface, com encaminhamento de `/api` para o backend.
- Documentar operações comuns e a diferença entre parar contêineres e apagar volumes.

**Non-Goals:**

- Orquestração em Kubernetes, publicação em nuvem ou criação de pipeline para registro de imagens.
- Alta disponibilidade, múltiplas réplicas, TLS público ou gestão centralizada de segredos.
- Colocar H2 em serviço separado; ele permanece embarcado na JVM.
- Alterar endpoints, regras financeiras, modelo de dados ou integrações de mercado.
- Remover a execução direta do projeto fora do Docker.

## Decisions

### 1. Três serviços em uma única composição

A composição terá `frontend`, `backend` e `database`. O PostgreSQL ficará acessível apenas pela rede interna; o backend poderá manter a porta `8080` publicada para Swagger e diagnóstico local, e o frontend será publicado na porta familiar `5173`.

Alternativas consideradas:

- Um único contêiner para toda a aplicação reduziria o número de serviços, mas misturaria processos com ciclos de vida diferentes e dificultaria atualizações e diagnóstico.
- H2 como banco padrão simplificaria o primeiro início, mas não exercitaria a infraestrutura adotada no perfil `dev` e não ofereceria persistência equivalente ao PostgreSQL.

### 2. Builds em múltiplas etapas

O backend será construído em uma etapa Maven baseada em Java 17 e executado em uma imagem JRE 17. O frontend será construído em Node.js 22 e servido por uma imagem Nginx não privilegiada. Versões principais compatíveis serão declaradas de forma explícita.

Alternativas consideradas:

- Montar o código e executar servidores de desenvolvimento facilitaria hot reload, porém produziria um ambiente menos previsível e carregaria ferramentas de build em execução. Uma composição de desenvolvimento poderá ser adicionada futuramente.
- Servir os arquivos React pelo Spring Boot acoplaria os builds e exigiria reorganização do empacotamento atual.

### 3. Mesmo domínio para interface e API

O build da interface receberá `VITE_API_BASE_URL=/`. A normalização já existente transforma esse valor em base vazia, fazendo as chamadas `/api/...` permanecerem no mesmo domínio. O Nginx encaminhará `/api` para `backend:8080` e aplicará fallback para `index.html` nas demais rotas da SPA.

Isso evita expor o hostname interno `backend` ao navegador e reduz a dependência de CORS no uso normal da composição. O acesso direto à API em `localhost:8080` continuará disponível para documentação e diagnóstico.

Alternativa considerada: compilar a URL `http://localhost:8080` no frontend. Ela funcionaria apenas quando o navegador estivesse na mesma máquina e obrigaria CORS mesmo dentro do fluxo integrado.

### 4. Saúde e ordem de inicialização

O banco usará `pg_isready`; o backend consultará `/actuator/health`; o frontend validará sua própria resposta HTTP. Dependências da composição usarão condições de saúde: backend após banco saudável e frontend após backend saudável. As políticas de reinício serão adequadas à execução local, sem esconder falhas permanentes de configuração.

Alternativa considerada: depender apenas da ordem de criação. A ordem não garante que banco e aplicação estejam prontos para aceitar conexões.

### 5. PostgreSQL persistente e não publicado

Um volume nomeado armazenará `/var/lib/postgresql/data`. `docker compose down` preservará o volume; somente o comando explícito com remoção de volumes apagará dados. A porta `5432` não será publicada por padrão para reduzir conflitos com instalações locais e exposição desnecessária.

### 6. Perfil e variáveis do backend

O backend em contêiner usará o perfil `prod`, com URL JDBC apontando para o nome interno `database`. Usuário, senha, segredo JWT e chaves opcionais serão mapeados explicitamente a partir do ambiente local. A composição não criará valores padrão para segredos obrigatórios. O modelo `.env.example` será ampliado apenas com valores fictícios necessários ao Docker.

O arquivo `.env` será interpretado pelo Docker Compose para substituição e injeção explícita; a aplicação Spring continuará recebendo variáveis do processo e não ganhará biblioteca dotenv. Nenhuma variável sensível será prefixada com `VITE_`.

### 7. Contextos de build controlados

O backend usará o repositório como contexto, com `.dockerignore` excluindo `.git`, builds, IDEs, Graphify, frontend e arquivos locais de ambiente. O frontend terá contexto próprio e exclusões para `node_modules`, `dist`, testes e arquivos locais. O cache de dependências será aproveitado copiando descritores antes do código.

### 8. Validação proporcional

A implementação será verificada por validação sintática da composição, testes existentes de backend e frontend, construção das duas imagens e um smoke test local: serviços saudáveis, frontend acessível, health da API e persistência após reinício. Credenciais fictícias e dublês continuarão sendo usados nos testes automatizados; provedores externos reais não serão acionados.

## Risks / Trade-offs

- [Docker Desktop ou virtualização indisponível] → Documentar pré-requisitos e comandos de verificação antes do build.
- [Download inicial demorado] → Usar etapas cacheáveis e contextos reduzidos; explicar que builds seguintes reutilizam camadas.
- [Perda acidental de dados com remoção de volume] → Destacar o caráter destrutivo do comando e separá-lo do procedimento normal de parada.
- [Segredo real incluído no contexto] → Excluir `.env` e variantes locais, mapear somente variáveis declaradas e verificar o conteúdo das imagens e do bundle.
- [Backend saudável demora por migrações] → Definir intervalos, tentativas e período inicial compatíveis com o Flyway.
- [Proxy mascara parcialmente erros do backend] → Preservar status e disponibilizar logs separados e acesso local ao endpoint de saúde.
- [Imagens base mudam ao longo do tempo] → Fixar versões principais e revisar periodicamente correções de segurança; hashes imutáveis ficam fora do escopo inicial.
- [Portas 5173 ou 8080 ocupadas] → Documentar diagnóstico e permitir sobrescrita local das portas sem alterar imagens.

## Migration Plan

1. Adicionar arquivos de imagem, proxy, composição e exclusão de contexto sem remover os fluxos atuais.
2. Atualizar o modelo de ambiente e a documentação com preparação e operação Docker.
3. Validar testes existentes e a configuração estática da composição.
4. Construir as imagens e iniciar a composição com um volume novo.
5. Confirmar saúde, interface, API, migrações e persistência após reinício.
6. Em caso de falha, parar a composição preservando o volume e continuar usando a execução direta anterior; nenhum dado ou migration precisa ser revertido.
