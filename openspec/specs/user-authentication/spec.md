# user-authentication Specification

## Purpose

Estabelecer uma identidade segura para investidores e administradores, permitindo cadastro, autenticação stateless e acesso protegido às capacidades da API.

## Requirements

### Requirement: Cadastro público de investidor
O sistema SHALL disponibilizar `POST /api/auth/register` para cadastrar um usuário com nome, e-mail e senha, atribuindo sempre o papel `INVESTOR` quando o cadastro for realizado pela rota pública.

#### Scenario: Cadastro realizado com sucesso
- **WHEN** uma pessoa informa nome, e-mail e senha válidos ainda não cadastrados
- **THEN** o sistema persiste o usuário com papel `INVESTOR` e responde com status `201` e os campos públicos `id`, `name`, `email` e `role`

#### Scenario: Tentativa de escolher papel privilegiado
- **WHEN** uma pessoa envia campos adicionais tentando definir o papel `ADMIN`
- **THEN** o sistema ignora qualquer papel fornecido pelo cliente e cadastra o usuário como `INVESTOR`

### Requirement: Normalização e validação dos dados cadastrais
O sistema MUST remover espaços das extremidades do nome e do e-mail, normalizar o e-mail para letras minúsculas e validar os dados após a normalização. O nome SHALL ser obrigatório e ter no máximo 100 caracteres, o e-mail SHALL ser obrigatório, válido e ter no máximo 254 caracteres, e a senha SHALL ter entre 8 e 72 caracteres.

#### Scenario: Dados válidos com espaços nas extremidades
- **WHEN** nome e e-mail válidos são enviados com espaços nas extremidades
- **THEN** o sistema remove esses espaços antes de validar e persistir os dados

#### Scenario: Nome inválido após normalização
- **WHEN** o nome fica vazio ou ultrapassa 100 caracteres após a remoção dos espaços das extremidades
- **THEN** o sistema rejeita o cadastro com status `400` e identifica o campo `name` na resposta JSON

#### Scenario: E-mail inválido
- **WHEN** o e-mail está vazio, possui formato inválido ou ultrapassa 254 caracteres após a normalização
- **THEN** o sistema rejeita o cadastro com status `400` e identifica o campo `email` na resposta JSON

#### Scenario: Senha fora dos limites
- **WHEN** a senha possui menos de 8 ou mais de 72 caracteres
- **THEN** o sistema rejeita o cadastro com status `400` e identifica o campo `password` na resposta JSON

### Requirement: Unicidade de e-mail sem distinção de caixa
O sistema MUST impedir que dois usuários possuam o mesmo e-mail, desconsiderando diferenças entre letras maiúsculas e minúsculas e espaços nas extremidades.

#### Scenario: E-mail duplicado com caixa diferente
- **WHEN** já existe `investidor@example.com` e um novo cadastro informa ` INVESTIDOR@example.com `
- **THEN** o sistema rejeita o cadastro com status `409` e código de erro `EMAIL_ALREADY_REGISTERED`

#### Scenario: Cadastros concorrentes com o mesmo e-mail
- **WHEN** duas requisições concorrentes tentam cadastrar o mesmo e-mail normalizado
- **THEN** somente um usuário é persistido e a outra requisição recebe status `409`

### Requirement: Proteção da senha
O sistema MUST armazenar somente uma representação criptográfica unidirecional da senha e MUST NOT incluir senha ou hash em qualquer resposta da API.

#### Scenario: Persistência segura da senha
- **WHEN** um usuário é cadastrado com sucesso
- **THEN** o valor persistido é um hash diferente da senha informada

#### Scenario: Respostas públicas do usuário
- **WHEN** dados de um usuário são retornados por cadastro, login ou consulta de identidade
- **THEN** a resposta contém somente `id`, `name`, `email` e `role`, sem senha ou hash

### Requirement: Autenticação por credenciais
O sistema SHALL disponibilizar `POST /api/auth/login` para autenticar um usuário ativo por e-mail e senha e, em caso de sucesso, emitir um token JWT do tipo Bearer com expiração configurável.

#### Scenario: Login realizado com sucesso
- **WHEN** um usuário ativo informa e-mail normalizado para uma conta existente e a senha correta
- **THEN** o sistema responde com status `200`, token JWT, tipo `Bearer`, instante de expiração e dados públicos do usuário

#### Scenario: E-mail inexistente
- **WHEN** uma pessoa tenta entrar com um e-mail não cadastrado
- **THEN** o sistema responde com status `401` e código `INVALID_CREDENTIALS`

#### Scenario: Senha incorreta
- **WHEN** uma pessoa tenta entrar com a senha incorreta de uma conta existente
- **THEN** o sistema responde com o mesmo status `401`, código e mensagem usados para e-mail inexistente

#### Scenario: Conta inativa tenta entrar
- **WHEN** uma pessoa informa as credenciais corretas de uma conta inativa
- **THEN** o sistema responde com status `401` e código `ACCOUNT_INACTIVE`, sem emitir token

### Requirement: Autenticação stateless por JWT
O sistema MUST validar assinatura, expiração, identidade e estado ativo da conta de tokens JWT em requisições protegidas, sem criar sessão no servidor.

#### Scenario: Token válido
- **WHEN** uma requisição protegida apresenta um token Bearer válido e não expirado de uma conta ativa
- **THEN** o sistema autentica a identidade e disponibiliza seu identificador e papel durante a requisição

#### Scenario: Token ausente
- **WHEN** uma requisição protegida não apresenta token Bearer
- **THEN** o sistema rejeita o acesso com status `401` e resposta JSON

#### Scenario: Token inválido ou expirado
- **WHEN** uma requisição protegida apresenta token adulterado, malformado ou expirado
- **THEN** o sistema rejeita o acesso com status `401` e resposta JSON sem revelar detalhes criptográficos

#### Scenario: Token emitido antes da desativação
- **WHEN** uma requisição protegida apresenta token válido de uma conta que foi desativada após a emissão
- **THEN** o sistema rejeita o acesso com status `401` e código `ACCOUNT_INACTIVE`

### Requirement: Política padrão de acesso
O sistema SHALL manter públicas somente as rotas de cadastro, login e infraestrutura explicitamente autorizadas; os demais endpoints SHALL exigir autenticação. Quando um endpoint exigir um papel que o usuário não possui, o sistema SHALL responder com status `403`.

#### Scenario: Acesso a rota pública
- **WHEN** uma pessoa não autenticada acessa cadastro ou login
- **THEN** o sistema permite que a requisição seja processada

#### Scenario: Usuário autenticado sem papel suficiente
- **WHEN** um investidor autenticado acessa uma rota reservada a administradores
- **THEN** o sistema rejeita o acesso com status `403` e resposta JSON

### Requirement: Consulta da identidade autenticada
O sistema SHALL disponibilizar `GET /api/auth/me` para retornar os dados públicos do usuário correspondente ao token válido apresentado.

#### Scenario: Consulta autenticada
- **WHEN** um usuário autenticado chama `GET /api/auth/me`
- **THEN** o sistema responde com status `200` e os campos `id`, `name`, `email` e `role` da identidade autenticada

#### Scenario: Usuário do token não está mais disponível
- **WHEN** o token é válido, mas o usuário correspondente não existe mais
- **THEN** o sistema rejeita a requisição com status `401` e resposta JSON

### Requirement: Erros padronizados em JSON
O sistema MUST retornar erros de validação, autenticação, autorização e conflito exclusivamente em JSON, contendo ao menos `timestamp`, `status`, `code`, `message` e `path`; erros de campos SHALL incluir uma coleção `fieldErrors`.

#### Scenario: Falha de validação
- **WHEN** uma requisição de cadastro ou login possui campos inválidos
- **THEN** o sistema responde com status `400`, estrutura de erro padronizada e os campos inválidos em `fieldErrors`

#### Scenario: Corpo JSON inválido
- **WHEN** uma requisição contém JSON malformado ou tipos incompatíveis
- **THEN** o sistema responde com status `400` usando a mesma estrutura JSON padronizada

### Requirement: Persistência compatível com os bancos suportados
O sistema MUST persistir usuários e aplicar a restrição de unicidade do e-mail tanto no H2 quanto no PostgreSQL, mantendo o mesmo comportamento funcional nos dois bancos. Nos ambientes H2 de desenvolvimento e teste, o esquema SHALL ser gerenciado automaticamente pelo provedor JPA.

#### Scenario: Inicialização de banco vazio
- **WHEN** a aplicação inicia sobre um banco H2 vazio com um perfil de desenvolvimento ou teste
- **THEN** a estrutura necessária para usuários é criada automaticamente a partir do mapeamento persistente e a aplicação fica pronta para cadastro e login

### Requirement: Persistência compatível do estado de conta
O sistema SHALL manter o estado de conta persistido de forma compatível com H2 e PostgreSQL. Em produção, a evolução de `users.status` MUST ser aplicada por uma migração Flyway versionada, que preserve os usuários existentes como ativos e seja registrada no histórico do banco.

#### Scenario: Banco PostgreSQL existente recebe a migração
- **WHEN** uma versão da aplicação com esta mudança inicia em produção ou no perfil de integração PostgreSQL contra um banco existente que ainda não possui histórico Flyway
- **THEN** o sistema estabelece o baseline sem recriar dados e aplica uma única vez a migração que inclui `users.status` com valor `ACTIVE` às contas existentes

#### Scenario: Ambiente H2 de desenvolvimento ou teste
- **WHEN** a aplicação inicia com perfil de desenvolvimento ou teste baseado em H2
- **THEN** o Flyway não executa a migração de produção e o Hibernate continua responsável pelo ciclo de vida do esquema desse ambiente
