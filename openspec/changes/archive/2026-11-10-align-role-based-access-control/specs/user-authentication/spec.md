## MODIFIED Requirements

### Requirement: Política padrão de acesso
O sistema SHALL manter públicas somente as rotas de cadastro, login e infraestrutura explicitamente autorizadas; os demais endpoints SHALL exigir autenticação. As capacidades financeiras SHALL exigir o papel `INVESTOR` e continuar limitadas aos recursos pertencentes ao investidor autenticado. A gestão de usuários SHALL exigir o papel `ADMIN`, e administradores MUST NOT obter acesso às capacidades financeiras privadas. Quando uma identidade válida não possuir o papel exigido, o sistema SHALL responder `403` em JSON; quando não houver identidade válida, SHALL responder `401` em JSON.

#### Scenario: Acesso a rota pública
- **WHEN** uma pessoa não autenticada acessa cadastro ou login
- **THEN** o sistema permite que a requisição seja processada

#### Scenario: Requisição protegida sem identidade válida
- **WHEN** uma pessoa acessa uma rota protegida sem um token Bearer válido
- **THEN** o sistema responde `401` em JSON

#### Scenario: Investidor acessa capacidade financeira
- **WHEN** um investidor autenticado acessa uma capacidade financeira sobre recursos que lhe pertencem
- **THEN** o sistema permite que a operação seja processada conforme as regras do domínio

#### Scenario: Usuário autenticado sem papel suficiente
- **WHEN** um investidor autenticado acessa uma rota reservada a administradores
- **THEN** o sistema responde `403` em JSON

#### Scenario: Administrador gerencia usuários
- **WHEN** um administrador ativo acessa uma rota administrativa de usuários
- **THEN** o sistema permite que a operação seja processada conforme as regras de gestão de contas

#### Scenario: Administrador tenta acessar dados financeiros
- **WHEN** um administrador autenticado acessa uma rota financeira reservada a investidores
- **THEN** o sistema responde `403` em JSON sem consultar nem expor recursos financeiros

#### Scenario: Investidor tenta acessar recurso financeiro de terceiro
- **WHEN** um investidor autenticado referencia um recurso financeiro pertencente a outro investidor
- **THEN** o sistema rejeita a operação conforme a política de propriedade do recurso e não expõe seus dados
