## 1. Preparação e segurança

- [x] 1.1 Verificar a disponibilidade do Docker Engine e do Docker Compose e registrar qualquer limitação do ambiente antes dos testes integrados.
- [x] 1.2 Revisar os arquivos ignorados e garantir que `.env`, credenciais, builds locais, dependências instaladas e artefatos do Graphify não entrem nos contextos das imagens.
- [x] 1.3 Definir no modelo de ambiente somente valores fictícios e as variáveis necessárias à composição, sem alterar ou expor o `.env` local.

## 2. Imagem do backend

- [x] 2.1 Criar Dockerfile do backend com etapa Maven/Java 17 para empacotamento e etapa JRE 17 contendo somente o artefato executável e o recurso necessário à verificação de saúde.
- [x] 2.2 Configurar usuário não privilegiado, porta, comando de inicialização e verificação de `/actuator/health` na imagem final do backend.
- [x] 2.3 Criar `.dockerignore` do contexto raiz com exclusões seguras e confirmar que o frontend, `.git`, builds e arquivos locais não são enviados ao build do backend.
- [x] 2.4 Construir a imagem do backend isoladamente e confirmar que ela inicia com o perfil e as variáveis PostgreSQL fornecidos externamente.

## 3. Imagem e proxy do frontend

- [x] 3.1 Criar Dockerfile do frontend com etapa Node.js 22 para `npm ci` e build e etapa final Nginx não privilegiada contendo somente os arquivos compilados.
- [x] 3.2 Configurar o build com base pública `/` para que os caminhos `/api` permaneçam no mesmo domínio da interface.
- [x] 3.3 Criar configuração Nginx que encaminhe `/api` para o backend, preserve as respostas e aplique fallback para `index.html` nas rotas da SPA.
- [x] 3.4 Criar `.dockerignore` do frontend e confirmar a exclusão de `node_modules`, `dist`, resultados de testes e arquivos locais de ambiente.
- [x] 3.5 Construir a imagem do frontend isoladamente e validar seu processo não privilegiado e sua verificação HTTP de saúde.

## 4. Composição dos serviços

- [x] 4.1 Criar `compose.yaml` com os serviços `database`, `backend` e `frontend` em rede interna e nomes consistentes para imagens e volume.
- [x] 4.2 Configurar PostgreSQL com volume nomeado, credenciais obrigatórias externalizadas, verificação `pg_isready` e sem publicação padrão da porta `5432`.
- [x] 4.3 Configurar o backend com perfil `prod`, URL JDBC interna, variáveis explicitamente mapeadas, porta local de diagnóstico e dependência do banco saudável.
- [x] 4.4 Configurar o frontend na porta local documentada e fazê-lo depender da saúde do backend.
- [x] 4.5 Validar sintaticamente a composição e confirmar que a ausência de senha do banco ou segredo JWT obrigatório produz erro claro antes de uma inicialização insegura.

## 5. Documentação operacional

- [x] 5.1 Adicionar um guia introdutório com os conceitos de imagem, contêiner, serviço, rede e volume aplicados ao projeto.
- [x] 5.2 Documentar preparação do `.env`, construção, inicialização, endereços, estado, logs, parada e reconstrução dos serviços.
- [x] 5.3 Documentar separadamente a remoção destrutiva do volume, com alerta explícito de perda dos dados locais.
- [x] 5.4 Atualizar README, configuração e operação com o fluxo Docker sem remover as instruções atuais de Maven, Vite, H2 e PostgreSQL direto.

## 6. Validação integrada

- [x] 6.1 Executar os gates existentes do backend e do frontend e corrigir somente regressões relacionadas à conteinerização.
- [x] 6.2 Construir e iniciar a composição completa e aguardar os três serviços alcançarem estado saudável.
- [x] 6.3 Confirmar acesso à interface, atualização direta de rota SPA, encaminhamento de uma chamada `/api`, Swagger e endpoint de saúde.
- [x] 6.4 Criar um dado descartável, reiniciar a composição sem remover volumes e confirmar que o dado permanece disponível.
- [x] 6.5 Inspecionar configuração, imagens e bundle público para confirmar ausência de segredos e ausência de publicação da porta do banco.
- [x] 6.6 Atualizar o grafo local com Graphify quando a ferramenta estiver disponível e confirmar que seus artefatos continuam fora do versionamento.
- [x] 6.7 Executar a validação estrita da change e revisar o estado final do Git sem criar commit ou enviar alterações remotas.
