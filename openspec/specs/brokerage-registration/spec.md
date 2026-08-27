# brokerage-registration Specification

## Purpose

Permitir que investidores cadastrem e consultem suas próprias corretoras somente após validações cadastrais confiáveis de CNPJ, CEP e participação ativa na CVM.

## Requirements

### Requirement: Acesso protegido e propriedade da corretora
O sistema SHALL restringir as operações de corretoras a investidores autenticados e MUST vincular cada corretora ao investidor identificado pelo token, sem aceitar um proprietário fornecido pelo cliente. Administradores MUST NOT acessar corretoras privadas de investidores.

#### Scenario: Investidor autenticado acessa a capacidade
- **WHEN** um investidor apresenta um token Bearer válido em uma operação de corretora
- **THEN** o sistema processa a operação usando o UUID do token como proprietário

#### Scenario: Requisição sem autenticação
- **WHEN** uma pessoa chama uma operação de corretora sem token válido
- **THEN** o sistema responde com status `401` e erro padronizado em JSON

#### Scenario: Administrador tenta acessar corretoras
- **WHEN** um administrador autenticado chama uma operação reservada aos investidores
- **THEN** o sistema responde com status `403` e não revela dados de corretoras

### Requirement: Consulta assistida de endereço por CEP
O sistema SHALL disponibilizar uma operação protegida para consultar um CEP na ViaCEP e retornar `cep`, `street`, `neighborhood`, `city` e `state`. Os dados retornados servem para preencher ou atualizar rua e bairro na interface, sem fornecer ou substituir número e complemento.

#### Scenario: CEP localizado
- **WHEN** o investidor consulta um CEP válido existente
- **THEN** o sistema responde com status `200` e os dados de endereço disponíveis na ViaCEP

#### Scenario: ViaCEP não informa rua ou bairro
- **WHEN** a ViaCEP reconhece o CEP, mas retorna rua ou bairro vazio
- **THEN** o sistema responde com os demais dados encontrados e permite que os campos ausentes sejam preenchidos manualmente

#### Scenario: CEP com formato inválido
- **WHEN** o investidor consulta um valor que não resulta em oito dígitos de CEP
- **THEN** o sistema responde com status `400` e identifica o CEP inválido

#### Scenario: CEP inexistente
- **WHEN** a ViaCEP informa que o CEP consultado não existe
- **THEN** o sistema responde com status `404` e código `CEP_NOT_FOUND`

#### Scenario: ViaCEP indisponível
- **WHEN** a ViaCEP excede o tempo limite ou retorna uma falha técnica
- **THEN** o sistema responde com status `503` e código `ADDRESS_PROVIDER_UNAVAILABLE`

### Requirement: Dados e normalização do cadastro
O sistema SHALL receber no cadastro `nickname`, `cnpj`, `cep`, `street`, `neighborhood`, `number` e `complement`. Apelido, CNPJ, CEP, rua, bairro e número SHALL ser obrigatórios; complemento SHALL ser opcional. O sistema MUST remover espaços das extremidades dos textos, aceitar CNPJ e CEP com ou sem formatação e persistir ambos somente com dígitos.

#### Scenario: Cadastro com campos válidos
- **WHEN** o investidor confirma apelido, CNPJ, CEP e endereço válidos
- **THEN** o sistema normaliza os campos antes de realizar as validações externas

#### Scenario: Campo obrigatório ausente após normalização
- **WHEN** apelido, rua, bairro ou número fica vazio após a remoção dos espaços das extremidades
- **THEN** o sistema responde com status `400` e identifica cada campo inválido em `fieldErrors`

#### Scenario: Limites dos campos textuais
- **WHEN** o apelido ultrapassa 100 caracteres, rua ultrapassa 150, bairro ultrapassa 100, número ultrapassa 20 ou complemento ultrapassa 100 caracteres após a normalização
- **THEN** o sistema responde com status `400` e identifica os campos que excederam seus limites

#### Scenario: CNPJ ou CEP estruturalmente inválido
- **WHEN** o CNPJ não possui 14 dígitos válidos ou o CEP não possui oito dígitos após a remoção da formatação
- **THEN** o sistema responde com status `400` sem chamar as fontes externas para o valor inválido

### Requirement: Validação definitiva do CNPJ, da CVM e do CEP
Ao confirmar o cadastro, o sistema MUST validar novamente os dados no backend, independentemente de uma consulta de CEP feita anteriormente. O CNPJ MUST existir na Brasil API, constar como participante ativo no cadastro da CVM e fornecer um CEP cadastral igual ao CEP informado pelo investidor após a normalização. O CEP informado MUST também existir na ViaCEP.

#### Scenario: Todas as validações são aprovadas
- **WHEN** a Brasil API reconhece o CNPJ, a CVM contém o mesmo CNPJ como participante ativo, o CEP cadastral coincide com o CEP informado e a ViaCEP reconhece esse CEP
- **THEN** o sistema considera a corretora apta para persistência

#### Scenario: CNPJ não localizado
- **WHEN** a Brasil API não encontra o CNPJ informado
- **THEN** o sistema responde com status `422` e código `CNPJ_NOT_FOUND`

#### Scenario: CNPJ não possui participação ativa na CVM
- **WHEN** o CNPJ não consta no cadastro da CVM ou consta sem situação ativa
- **THEN** o sistema responde com status `422` e código `CNPJ_NOT_ACTIVE_AT_CVM`

#### Scenario: CEP não pertence ao CNPJ
- **WHEN** o CEP cadastral retornado para o CNPJ é diferente do CEP informado pelo investidor
- **THEN** o sistema responde com status `422` e código `CEP_CNPJ_MISMATCH`

#### Scenario: CEP do cadastro não existe na ViaCEP
- **WHEN** o CEP coincide com o cadastro do CNPJ, mas não é reconhecido pela ViaCEP
- **THEN** o sistema responde com status `422` e código `CEP_NOT_FOUND`

### Requirement: Endereço manual com enriquecimento oficial
O sistema MUST persistir rua, bairro, número e complemento normalizados conforme confirmados pelo investidor, permitindo ajustes manuais mesmo após a consulta de CEP. Cidade e estado MUST ser obtidos da ViaCEP durante a validação definitiva e não SHALL ser aceitos como entrada controlável pelo cliente.

#### Scenario: Investidor ajusta rua ou bairro sugerido
- **WHEN** o investidor altera manualmente rua ou bairro após a consulta assistida e as validações de CNPJ e CEP são aprovadas
- **THEN** o sistema persiste os valores manuais confirmados para rua e bairro

#### Scenario: Cidade e estado divergentes enviados como campos adicionais
- **WHEN** o cliente tenta fornecer cidade ou estado diferentes dos retornados pela ViaCEP
- **THEN** o sistema ignora esses campos e persiste cidade e estado obtidos na validação oficial

### Requirement: Persistência atômica e resposta pública
O sistema SHALL criar a corretora somente depois de todas as validações obrigatórias e responder com status `201`. A resposta SHALL conter `id`, `nickname`, `cnpj`, `legalName`, `tradeName`, `registrationStatus`, `cvmParticipantCategory`, endereço, `createdAt` e `updatedAt`, sem expor Entity ou dados internos das integrações.

#### Scenario: Corretora criada com sucesso
- **WHEN** todas as validações e restrições de unicidade são aprovadas
- **THEN** o sistema persiste uma única corretora vinculada ao investidor e retorna seus dados públicos com status `201`

#### Scenario: Falha ocorre antes da persistência
- **WHEN** qualquer validação cadastral ou integração obrigatória falha
- **THEN** nenhuma corretora ou dado parcial da tentativa é persistido

### Requirement: Unicidade por investidor
O sistema MUST impedir que o mesmo investidor possua corretoras com o mesmo CNPJ normalizado ou com o mesmo apelido normalizado sem distinção entre maiúsculas e minúsculas. Investidores diferentes SHALL poder cadastrar a mesma instituição e usar o mesmo apelido.

#### Scenario: CNPJ repetido para o mesmo investidor
- **WHEN** o investidor tenta cadastrar novamente um CNPJ que já pertence a uma de suas corretoras
- **THEN** o sistema responde com status `409` e código `BROKERAGE_CNPJ_ALREADY_REGISTERED`

#### Scenario: Apelido repetido para o mesmo investidor
- **WHEN** o investidor tenta cadastrar um apelido já usado por ele, desconsiderando caixa e espaços nas extremidades
- **THEN** o sistema responde com status `409` e código `BROKERAGE_NICKNAME_ALREADY_REGISTERED`

#### Scenario: Cadastros concorrentes duplicados
- **WHEN** duas requisições concorrentes tentam cadastrar o mesmo CNPJ ou apelido normalizado para o mesmo investidor
- **THEN** somente uma corretora é persistida e a outra requisição recebe o conflito correspondente

#### Scenario: Mesma instituição para investidores diferentes
- **WHEN** dois investidores distintos cadastram o mesmo CNPJ com dados válidos
- **THEN** o sistema cria uma corretora privada para cada investidor

### Requirement: Listagem isolada de corretoras
O sistema SHALL disponibilizar uma operação para listar as corretoras pertencentes ao investidor autenticado e MUST NOT retornar registros de outros investidores.

#### Scenario: Investidor possui corretoras
- **WHEN** o investidor consulta suas corretoras
- **THEN** o sistema responde com status `200` e somente os registros vinculados ao seu UUID

#### Scenario: Investidor ainda não possui corretoras
- **WHEN** o investidor consulta suas corretoras sem possuir registros
- **THEN** o sistema responde com status `200` e uma coleção vazia

### Requirement: Falhas externas padronizadas e sem persistência
O sistema MUST distinguir rejeições cadastrais de indisponibilidade técnica da Brasil API, ViaCEP ou fonte da CVM. Falhas técnicas SHALL responder com status `503`, código específico do provedor e estrutura JSON padronizada, sem revelar URLs internas, stack traces ou conteúdo bruto das fontes.

#### Scenario: Brasil API indisponível
- **WHEN** a consulta obrigatória de CNPJ excede o tempo limite ou falha tecnicamente
- **THEN** o sistema responde com `503`, código `CNPJ_PROVIDER_UNAVAILABLE` e não persiste a tentativa

#### Scenario: Fonte da CVM indisponível
- **WHEN** não existe uma cópia vigente disponível e a atualização obrigatória do cadastro da CVM falha
- **THEN** o sistema responde com `503`, código `CVM_PROVIDER_UNAVAILABLE` e não persiste a tentativa

#### Scenario: Erro externo retorna conteúdo sensível ou inesperado
- **WHEN** uma fonte externa devolve detalhes técnicos ou um corpo não reconhecido
- **THEN** o sistema registra internamente o necessário para diagnóstico e retorna somente o erro público padronizado

### Requirement: Compatibilidade de persistência
O sistema MUST persistir corretoras, endereços e restrições de unicidade com o mesmo comportamento funcional no H2 e no PostgreSQL, mantendo o gerenciamento automático do esquema restrito aos perfis locais de desenvolvimento e teste enquanto as migrações versionadas permanecem pendentes.

#### Scenario: Banco local vazio
- **WHEN** a aplicação inicia com um perfil local suportado sobre um banco vazio
- **THEN** a estrutura de corretoras é criada a partir do mapeamento persistente e fica pronta para cadastro e listagem
