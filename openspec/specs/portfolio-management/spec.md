# Portfolio Management Specification

## Purpose

Permitir que investidores organizem seus investimentos em carteiras privadas, cada uma vinculada obrigatoriamente a uma corretora já cadastrada e validada.

## Requirements

### Requirement: Acesso protegido e propriedade da carteira
O sistema SHALL restringir todas as operações de carteira a investidores autenticados e MUST determinar o proprietário exclusivamente pelo token. O cliente MUST NOT informar ou controlar o proprietário de uma carteira, e administradores MUST NOT acessar as carteiras privadas de investidores.

#### Scenario: Investidor autenticado gerencia suas carteiras
- **WHEN** um investidor apresenta um token Bearer válido em uma operação de carteira
- **THEN** o sistema executa a operação usando o UUID do token como proprietário

#### Scenario: Requisição sem autenticação
- **WHEN** uma pessoa chama uma operação de carteira sem token válido
- **THEN** o sistema responde com status `401` e erro padronizado em JSON

#### Scenario: Administrador tenta acessar carteiras privadas
- **WHEN** um administrador autenticado chama uma operação de carteira
- **THEN** o sistema responde com status `403` e não revela dados de carteiras

### Requirement: Criação de carteira vinculada a corretora
O sistema SHALL criar uma carteira somente quando o investidor informar `name` e `brokerageId`. A carteira MUST ser vinculada a exatamente uma corretora já cadastrada e pertencente ao mesmo investidor, e uma corretora MAY ser reutilizada por várias carteiras do seu proprietário.

#### Scenario: Carteira criada com sucesso
- **WHEN** o investidor informa nome válido e o UUID de uma de suas corretoras cadastradas
- **THEN** o sistema cria uma carteira privada vinculada àquela corretora e responde com status `201`

#### Scenario: Corretora de outro investidor é informada
- **WHEN** o investidor tenta criar uma carteira com o UUID de uma corretora que pertence a outra pessoa
- **THEN** o sistema responde com status `404` e código `BROKERAGE_NOT_FOUND`, sem criar a carteira

#### Scenario: Corretora inexistente é informada
- **WHEN** o investidor informa um UUID que não corresponde a uma corretora cadastrada por ele
- **THEN** o sistema responde com status `404` e código `BROKERAGE_NOT_FOUND`, sem criar a carteira

#### Scenario: Corretora previamente cadastrada é reutilizada
- **WHEN** o investidor cria uma carteira usando uma corretora já cadastrada, mesmo que ela já esteja vinculada a outra carteira
- **THEN** o sistema reutiliza o cadastro existente sem repetir consultas à Brasil API, ViaCEP ou CVM

### Requirement: Dados e unicidade do nome da carteira
O sistema SHALL receber o nome da carteira como texto obrigatório. O sistema MUST remover espaços nas extremidades, rejeitar nome vazio e limitar o valor normalizado a 100 caracteres. Um investidor MUST NOT possuir duas carteiras com o mesmo nome normalizado sem distinção entre maiúsculas e minúsculas; investidores diferentes MAY usar o mesmo nome.

#### Scenario: Nome válido é normalizado
- **WHEN** o investidor cria uma carteira com espaços nas extremidades do nome
- **THEN** o sistema remove esses espaços antes de validar e persistir o nome

#### Scenario: Nome obrigatório ausente após normalização
- **WHEN** o nome fica vazio depois da remoção dos espaços das extremidades
- **THEN** o sistema responde com status `400` e identifica o campo `name` no erro JSON

#### Scenario: Nome excede o limite
- **WHEN** o nome normalizado ultrapassa 100 caracteres
- **THEN** o sistema responde com status `400` e identifica o campo `name` no erro JSON

#### Scenario: Nome repetido pelo mesmo investidor
- **WHEN** o investidor tenta criar uma carteira com nome equivalente a outro já usado por ele, desconsiderando caixa e espaços nas extremidades
- **THEN** o sistema responde com status `409` e código `PORTFOLIO_NAME_ALREADY_REGISTERED`

#### Scenario: Mesmo nome para investidores diferentes
- **WHEN** dois investidores usam o mesmo nome normalizado para suas carteiras
- **THEN** o sistema permite a criação privada de uma carteira para cada investidor

### Requirement: Consulta e listagem privada de carteiras
O sistema SHALL disponibilizar consulta individual e listagem das carteiras do investidor autenticado. A listagem MUST ser ordenada da carteira mais antiga para a mais recente e cada resposta pública SHALL conter `id`, `name`, resumo da corretora vinculada, `createdAt` e `updatedAt`.

#### Scenario: Investidor lista suas carteiras em ordem cronológica
- **WHEN** o investidor consulta suas carteiras
- **THEN** o sistema responde com status `200` e somente as suas carteiras, ordenadas da mais antiga para a mais recente

#### Scenario: Investidor ainda não possui carteiras
- **WHEN** o investidor consulta suas carteiras sem possuir registros
- **THEN** o sistema responde com status `200` e uma coleção vazia

#### Scenario: Investidor consulta uma carteira própria
- **WHEN** o investidor consulta o UUID de uma de suas carteiras
- **THEN** o sistema responde com status `200` e os dados públicos da carteira

#### Scenario: Investidor consulta carteira de outra pessoa ou inexistente
- **WHEN** o investidor consulta o UUID de uma carteira que não pertence a ele ou não existe
- **THEN** o sistema responde com status `404` e código `PORTFOLIO_NOT_FOUND`, sem revelar se o registro existe

### Requirement: Exclusão de carteira privada
O sistema SHALL permitir que o investidor exclua uma de suas próprias carteiras. Nesta mudança, a exclusão SHALL remover a carteira definitivamente, pois lançamentos e histórico ainda não fazem parte da capacidade.

#### Scenario: Investidor exclui carteira própria
- **WHEN** o investidor exclui o UUID de uma de suas carteiras
- **THEN** o sistema remove a carteira e responde com status `204`

#### Scenario: Investidor tenta excluir carteira de outra pessoa ou inexistente
- **WHEN** o investidor exclui o UUID de uma carteira que não pertence a ele ou não existe
- **THEN** o sistema responde com status `404` e código `PORTFOLIO_NOT_FOUND`, sem revelar se o registro existe

### Requirement: Compatibilidade de persistência e respostas públicas
O sistema MUST persistir carteiras e suas restrições de unicidade com o mesmo comportamento funcional no H2 e no PostgreSQL. As respostas e erros MUST ser JSON e MUST NOT expor entidades persistentes, proprietário interno ou detalhes de corretoras de outros investidores.

#### Scenario: Banco local vazio
- **WHEN** a aplicação inicia sobre um banco H2 ou PostgreSQL local vazio com um perfil local suportado
- **THEN** a estrutura de carteiras fica pronta para criação, consulta, listagem e exclusão

#### Scenario: Cliente tenta controlar dados internos
- **WHEN** o cliente envia campos adicionais para definir proprietário ou dados internos da corretora
- **THEN** o sistema ignora esses campos e usa somente a identidade autenticada e a corretora identificada por `brokerageId`
