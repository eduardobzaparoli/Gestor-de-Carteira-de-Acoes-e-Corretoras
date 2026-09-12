# investor-asset-catalog Specification

## Purpose

Permitir que cada investidor mantenha um catálogo privado e reutilizável de ações e ETFs validados, com a última cotação conhecida e sua data de consulta.

## Requirements

### Requirement: Catálogo privado e global por investidor
O sistema SHALL disponibilizar o catálogo de ativos somente ao investidor autenticado. Cada ativo cadastrado MUST pertencer ao investidor determinado pelo token e SHALL ficar disponível para todas as carteiras desse mesmo investidor. Administradores MUST NOT acessar ou alterar catálogos privados.

#### Scenario: Investidor acessa o próprio catálogo
- **WHEN** um investidor autenticado lista, cadastra ou atualiza um ativo
- **THEN** o sistema executa a operação exclusivamente no catálogo pertencente ao usuário do token

#### Scenario: Catálogos de investidores diferentes
- **WHEN** dois investidores cadastram ou consultam ativos
- **THEN** cada um visualiza somente seus próprios registros, mesmo que os tickers sejam iguais

#### Scenario: Acesso sem autenticação ou por administrador
- **WHEN** uma pessoa sem token válido ou um administrador tenta acessar o catálogo
- **THEN** o sistema responde respectivamente com `401` ou `403` em JSON e não revela ativos privados

### Requirement: Cadastro a partir de ativo validado e cotado
O sistema SHALL cadastrar somente uma ação ou ETF previamente selecionado em uma pesquisa externa válida do próprio investidor. No cadastro, o sistema MUST consultar novamente a cotação corrente no provedor do mercado, persistir ticker normalizado, nome, mercado, tipo, moeda, cotação positiva e o instante da consulta definido pelo servidor. O cadastro MUST ser atômico e MUST NOT aceitar do cliente identidade, cotação ou data de consulta livres.

#### Scenario: Ativo brasileiro cadastrado
- **WHEN** o investidor confirma um resultado válido do mercado `BR`
- **THEN** o sistema consulta a Brapi, persiste o ativo com cotação em `BRL` e responde com status `201`

#### Scenario: Ativo americano cadastrado
- **WHEN** o investidor confirma um resultado válido do mercado `US`
- **THEN** o sistema consulta a Twelve Data, persiste o ativo com cotação em `USD` e responde com status `201`

#### Scenario: Seleção inválida ou expirada
- **WHEN** a referência pesquisada não existe, expirou ou pertence a outro investidor
- **THEN** o sistema responde com `409` e código público de seleção expirada, sem cadastrar o ativo

#### Scenario: Cotação indisponível durante o cadastro
- **WHEN** o provedor não retorna uma cotação válida ou está indisponível
- **THEN** o sistema responde com `503` e código público correspondente e não persiste registro parcial

### Requirement: Unicidade do ativo no catálogo
O sistema MUST permitir no máximo um cadastro para a mesma combinação de investidor, mercado e ticker normalizado, inclusive sob requisições concorrentes. O mesmo ticker MAY existir em catálogos de investidores diferentes.

#### Scenario: Cadastro duplicado
- **WHEN** o investidor tenta cadastrar novamente um ticker já existente no mesmo mercado
- **THEN** o sistema responde com `409` e código `ASSET_ALREADY_REGISTERED`, preservando o registro original

#### Scenario: Cadastros concorrentes do mesmo ticker
- **WHEN** duas requisições concorrentes tentam cadastrar o mesmo ticker e mercado para o mesmo investidor
- **THEN** somente um registro é criado e a outra requisição recebe `409`

### Requirement: Listagem e filtros do catálogo
O sistema SHALL listar os ativos do investidor em ordem determinística e permitir filtrar por mercado `BR` ou `US`, mantendo uma consulta sem filtro para todos. Cada item MUST expor identificador público, ticker, nome, mercado, tipo, moeda, última cotação e data e hora da última consulta.

#### Scenario: Listagem completa
- **WHEN** o investidor consulta o catálogo sem filtro de mercado
- **THEN** o sistema responde com status `200` e todos os ativos cadastrados por ele

#### Scenario: Filtro por mercado
- **WHEN** o investidor consulta o catálogo com filtro `BR` ou `US`
- **THEN** o sistema retorna somente os ativos próprios do mercado solicitado

#### Scenario: Catálogo vazio
- **WHEN** o investidor ainda não cadastrou ativos
- **THEN** o sistema responde com status `200` e uma coleção vazia

### Requirement: Atualização explícita da cotação armazenada
O sistema SHALL permitir que o investidor atualize individualmente a cotação de um ativo do próprio catálogo. Uma atualização bem-sucedida MUST substituir atomicamente a cotação e o instante da consulta; uma falha MUST preservar integralmente o último valor válido e sua data.

#### Scenario: Cotação atualizada
- **WHEN** o investidor solicita a atualização e o provedor retorna uma cotação positiva
- **THEN** o sistema persiste o novo valor e o novo instante e devolve o ativo atualizado

#### Scenario: Atualização de ativo inexistente ou alheio
- **WHEN** o identificador não existe ou pertence a outro investidor
- **THEN** o sistema responde com `404` e código `REGISTERED_ASSET_NOT_FOUND` sem consultar o provedor

#### Scenario: Falha na atualização
- **WHEN** o provedor falha ou devolve uma cotação inválida durante a atualização
- **THEN** o sistema responde com `503` e mantém a cotação e a data anteriormente armazenadas

### Requirement: Cotação corrente para preparação de lançamento
O sistema SHALL permitir consultar uma cotação corrente de um ativo cadastrado para preencher um novo lançamento. Essa consulta MUST validar a propriedade do ativo, consultar o provedor correspondente e retornar a cotação e seu instante sem alterar a cotação histórica armazenada no catálogo.

#### Scenario: Ativo selecionado para lançamento
- **WHEN** o investidor seleciona um ativo próprio ao preparar um lançamento
- **THEN** o sistema consulta o provedor e retorna uma cotação corrente positiva na moeda nativa do ativo

#### Scenario: Consulta corrente não atualiza o catálogo
- **WHEN** uma cotação é obtida somente para preparar um lançamento
- **THEN** a última cotação e a data exibidas no catálogo permanecem as registradas no cadastro ou na última atualização explícita

#### Scenario: Cotação corrente indisponível
- **WHEN** o provedor não fornece uma cotação válida para o ativo selecionado
- **THEN** o sistema responde com `503` e não fornece preço parcial para o lançamento

### Requirement: Persistência compatível
O sistema MUST persistir o catálogo com comportamento equivalente no H2 e no PostgreSQL, usando identificadores e precisão decimal compatíveis.

#### Scenario: Banco suportado vazio
- **WHEN** a aplicação inicia sobre H2 ou PostgreSQL vazio com um perfil suportado
- **THEN** a estrutura do catálogo e suas restrições ficam disponíveis por migrations versionadas

### Requirement: Exclusão protegida por saldo em carteira
O sistema SHALL permitir excluir individualmente um ativo cadastrado ou todos os ativos do investidor. Antes da remoção, o sistema MUST verificar o saldo atual por mercado e ticker em todas as carteiras do proprietário. Ativos com saldo positivo MUST NOT ser excluídos. A exclusão total MUST ser atômica e MUST NOT remover nenhum registro quando qualquer ativo estiver protegido. Os lançamentos históricos MUST permanecer preservados.

#### Scenario: Exclusão individual sem saldo
- **WHEN** o investidor exclui um ativo próprio que não possui saldo positivo em nenhuma de suas carteiras
- **THEN** o sistema remove o cadastro, preserva os lançamentos históricos e responde com `204`

#### Scenario: Exclusão individual com saldo
- **WHEN** o investidor tenta excluir um ativo com saldo positivo em ao menos uma carteira
- **THEN** o sistema responde com `409 REGISTERED_ASSET_HAS_POSITION` e mantém o cadastro

#### Scenario: Exclusão total permitida
- **WHEN** nenhum ativo do catálogo possui saldo positivo nas carteiras do investidor
- **THEN** o sistema remove todos os cadastros e responde com `204`

#### Scenario: Exclusão total bloqueada
- **WHEN** ao menos um ativo do catálogo possui saldo positivo em alguma carteira do investidor
- **THEN** o sistema responde com `409 REGISTERED_ASSET_HAS_POSITION` e não exclui nenhum cadastro
