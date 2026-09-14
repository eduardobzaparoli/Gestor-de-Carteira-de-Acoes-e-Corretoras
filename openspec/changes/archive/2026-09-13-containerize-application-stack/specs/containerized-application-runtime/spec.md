## Purpose

Permitir que a aplicação completa seja construída e executada de forma reproduzível e segura em um ambiente local de contêineres, sem exigir a instalação direta de Java, Node.js ou PostgreSQL.

## ADDED Requirements

### Requirement: Inicialização coordenada da aplicação completa
O projeto SHALL fornecer um fluxo único para construir e iniciar a interface web, a API e o PostgreSQL em serviços isolados e conectados. A API MUST aguardar a disponibilidade do banco, e a interface MUST ser considerada pronta somente depois que a API estiver saudável.

#### Scenario: Primeira inicialização
- **WHEN** uma pessoa fornece as variáveis obrigatórias e inicia a composição em uma máquina com Docker disponível
- **THEN** banco, API e interface são construídos e iniciados na ordem necessária sem exigir Java, Maven, Node.js ou PostgreSQL instalados diretamente na máquina

#### Scenario: Dependência ainda indisponível
- **WHEN** o banco ou a API ainda não está saudável durante a inicialização
- **THEN** os serviços dependentes não são considerados prontos e a composição permite identificar qual verificação falhou

#### Scenario: Aplicação pronta
- **WHEN** todos os serviços concluem suas verificações de saúde
- **THEN** a interface fica acessível em endereço local documentado e consegue consumir a API

### Requirement: Roteamento entre interface e API
A execução em contêineres MUST permitir que o navegador acesse os endpoints da API por meio do endereço público da interface, sem expor ao navegador nomes internos da rede de contêineres. Rotas da aplicação de página única MUST continuar abrindo diretamente e após atualização do navegador.

#### Scenario: Chamada da interface para a API
- **WHEN** a interface executada em contêiner solicita um caminho iniciado por `/api`
- **THEN** a requisição é encaminhada à API na rede interna e a resposta preserva seu status, corpo JSON e cabeçalhos necessários

#### Scenario: Atualização em rota interna
- **WHEN** a pessoa atualiza o navegador em uma rota válida da interface diferente da raiz
- **THEN** a interface é carregada normalmente em vez de responder que o arquivo não existe

### Requirement: Persistência segura do PostgreSQL
O serviço PostgreSQL MUST armazenar seus dados em volume persistente e não MUST publicar sua porta para a máquina hospedeira por padrão. A parada ou recriação normal dos contêineres MUST preservar os dados, enquanto a remoção do volume MUST exigir uma ação explícita e documentada.

#### Scenario: Reinício normal do ambiente
- **WHEN** a composição é parada e iniciada novamente sem remoção de volumes
- **THEN** usuários, carteiras, ativos, lançamentos e demais dados persistidos continuam disponíveis

#### Scenario: Remoção completa solicitada
- **WHEN** a pessoa executa explicitamente o procedimento documentado para remover também os volumes
- **THEN** a documentação alerta previamente que os dados locais do PostgreSQL serão apagados

### Requirement: Configuração e segredos externalizados
As imagens e os arquivos versionados MUST NOT conter senhas reais, segredo JWT ou chaves de provedores externos. A composição SHALL receber valores sensíveis por variáveis de ambiente, MUST falhar de forma observável quando uma variável obrigatória estiver ausente e MUST evitar enviar segredos do backend ao bundle ou ao processo público da interface.

#### Scenario: Preparação segura do ambiente
- **WHEN** uma pessoa cria sua configuração local a partir do modelo versionado
- **THEN** encontra somente valores fictícios e instruções para fornecer banco, JWT e integrações sem versionar credenciais reais

#### Scenario: Segredo obrigatório ausente
- **WHEN** a composição é iniciada sem uma senha de banco ou segredo JWT obrigatório
- **THEN** o serviço afetado não inicia com uma credencial conhecida por padrão e o diagnóstico indica a configuração ausente sem revelar outros segredos

#### Scenario: Construção da interface
- **WHEN** a imagem da interface é construída
- **THEN** senhas, segredo JWT e chaves de provedores não são incorporados aos arquivos entregues ao navegador

### Requirement: Imagens reproduzíveis e enxutas
As imagens da API e da interface SHALL separar dependências de construção do conteúdo necessário em execução, MUST excluir artefatos locais desnecessários do contexto e SHOULD executar processos da aplicação sem privilégios administrativos sempre que a imagem utilizada permitir.

#### Scenario: Construção limpa
- **WHEN** as imagens são criadas a partir de uma cópia limpa do repositório
- **THEN** o backend é empacotado, o frontend é compilado e somente os artefatos necessários são incluídos nas imagens finais

#### Scenario: Artefatos locais presentes
- **WHEN** a máquina contém dependências, resultados de testes, builds anteriores ou metadados locais
- **THEN** esses itens não são enviados desnecessariamente ao contexto nem copiados para as imagens finais

### Requirement: Operação e diagnóstico documentados
O projeto MUST documentar os pré-requisitos e os comandos para configurar, construir, iniciar, parar, reconstruir e consultar o estado e os logs dos serviços. O fluxo existente sem Docker MUST continuar documentado e funcional.

#### Scenario: Primeiro uso por pessoa iniciante
- **WHEN** uma pessoa sem conhecimento prévio de Docker segue o guia a partir de uma máquina preparada
- **THEN** ela consegue iniciar a aplicação, identificar os endereços disponíveis e verificar a saúde dos serviços

#### Scenario: Diagnóstico de falha
- **WHEN** um serviço não inicia ou deixa de ficar saudável
- **THEN** o guia orienta como consultar estado e logs sem expor credenciais

#### Scenario: Execução sem Docker
- **WHEN** uma pessoa opta pelo fluxo direto já suportado
- **THEN** ela ainda pode executar backend, frontend e testes usando as instruções e perfis existentes
