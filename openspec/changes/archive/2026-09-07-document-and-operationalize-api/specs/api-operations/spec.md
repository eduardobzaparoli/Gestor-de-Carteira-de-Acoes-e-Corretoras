## Purpose

Tornar a API autoexplicativa e operável com segurança, oferecendo contrato HTTP verificável, sinal mínimo de saúde e configuração explícita para acesso por frontend.

## ADDED Requirements

### Requirement: Contrato OpenAPI acessível e fiel
O sistema SHALL publicar um documento OpenAPI que descreva os endpoints públicos e protegidos, autenticação Bearer JWT, parâmetros, corpos, respostas de sucesso e estrutura comum de erros. A documentação MUST NOT conter credenciais, dados reais de investidores nem URLs internas sensíveis.

#### Scenario: Consumidor consulta o contrato
- **WHEN** um consumidor solicita o documento OpenAPI ou sua interface navegável
- **THEN** o sistema responde sem exigir autenticação e apresenta os contratos atuais da API

#### Scenario: Operação protegida documentada
- **WHEN** o contrato descreve uma operação que exige autenticação
- **THEN** a operação referencia o esquema Bearer JWT e documenta os principais resultados de autorização e validação

### Requirement: Saúde pública mínima
O sistema SHALL expor um endpoint público de saúde que informe somente o estado agregado necessário para sondas de execução. O sistema MUST NOT expor nesse endpoint componentes, propriedades, variáveis de ambiente, credenciais, detalhes do banco ou integrações externas.

#### Scenario: Aplicação saudável
- **WHEN** uma sonda consulta o endpoint de saúde de uma instância disponível
- **THEN** o sistema responde com status HTTP de sucesso e estado agregado `UP`

#### Scenario: Tentativa de acessar informações operacionais adicionais
- **WHEN** um cliente não autenticado solicita outro endpoint de gerenciamento ou detalhes de componentes
- **THEN** o sistema não expõe essas informações

### Requirement: CORS por lista explícita
O sistema MUST rejeitar acesso cross-origin por padrão. Quando uma lista de origens permitidas for configurada, o sistema SHALL aceitar somente origens exatas dessa lista para os métodos e cabeçalhos necessários à API, sem transformar padrões amplos em autorização implícita.

#### Scenario: Nenhuma origem configurada
- **WHEN** a aplicação inicia sem lista de origens permitidas
- **THEN** respostas cross-origin não recebem autorização CORS

#### Scenario: Origem permitida
- **WHEN** uma requisição preflight parte de uma origem presente na lista configurada
- **THEN** o sistema autoriza os métodos e cabeçalhos previstos para consumo da API

#### Scenario: Origem não permitida
- **WHEN** uma requisição cross-origin parte de origem ausente da lista
- **THEN** o sistema não concede autorização CORS

### Requirement: Diagnóstico sem dados sensíveis
O sistema SHALL registrar eventos suficientes para diagnosticar inicialização e falhas de requisição, com níveis configuráveis por ambiente. Logs MUST NOT registrar senhas, tokens JWT, chaves de provedores ou corpos contendo esses valores.

#### Scenario: Falha tratada da API
- **WHEN** uma requisição produz uma falha pública tratada
- **THEN** o sistema registra contexto técnico seguro sem incluir credenciais ou dados de autenticação

#### Scenario: Configuração de produção
- **WHEN** a aplicação executa no perfil de produção
- **THEN** os níveis de log evitam diagnóstico detalhado excessivo e preservam os erros necessários à operação
