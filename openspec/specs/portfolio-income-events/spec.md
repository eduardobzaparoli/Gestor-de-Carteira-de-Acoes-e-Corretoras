# portfolio-income-events Specification

## Purpose

Permitir que o investidor localize, confirme, registre e acompanhe proventos monetários de ativos de sua carteira com elegibilidade histórica, rastreabilidade e consolidação cambial.

## Requirements

### Requirement: Acesso privado aos proventos da carteira
O sistema SHALL disponibilizar candidatos, registros, histórico e resumo de proventos somente no contexto de uma carteira pertencente ao investidor autenticado. O proprietário MUST ser determinado exclusivamente pelo token, e administradores MUST NOT acessar nem alterar proventos privados.

#### Scenario: Investidor acessa proventos de carteira própria
- **WHEN** um investidor autenticado consulta, confirma, registra, cancela ou resume proventos de uma de suas carteiras
- **THEN** o sistema executa a operação somente no contexto daquela carteira e retorna JSON

#### Scenario: Carteira inexistente ou de outro investidor
- **WHEN** o investidor informa uma carteira inexistente ou pertencente a outra pessoa
- **THEN** o sistema responde com `404` e código `PORTFOLIO_NOT_FOUND`, sem consultar provedores nem revelar a existência da carteira

#### Scenario: Requisição sem autenticação ou por administrador
- **WHEN** uma pessoa sem token válido ou um administrador chama uma operação privada de proventos
- **THEN** o sistema responde respectivamente com `401` ou `403` em JSON e não revela nem altera dados

### Requirement: Consulta explícita e normalizada de candidatos
O sistema SHALL consultar proventos somente mediante solicitação do investidor, usando a Brapi para ativos `BR` e a Alpha Vantage para ativos `US` que possuam ao menos uma compra efetivada no histórico da carteira. A resposta MUST normalizar ticker, retrato do ativo, mercado, moeda, tipo, valor por unidade, data de elegibilidade, data de pagamento, origem e identidade do evento quando disponíveis. Somente eventos monetários `DIVIDEND`, `INTEREST_ON_EQUITY` e `DISTRIBUTION` SHALL ser considerados; eventos que alterem quantidade MUST ser ignorados. A consulta de um mercado MUST ser atômica e MUST NOT persistir um provento antes da confirmação.

#### Scenario: Candidatos brasileiros consultados
- **WHEN** o investidor solicita candidatos `BR` para uma carteira que possui ativos brasileiros no histórico efetivado
- **THEN** o sistema consulta a Brapi, normaliza dividendos, JCP e distribuições monetárias e retorna os candidatos elegíveis sem persistir registros

#### Scenario: Candidatos americanos consultados
- **WHEN** o investidor solicita candidatos `US` para uma carteira que possui ativos americanos no histórico efetivado
- **THEN** o sistema consulta a Alpha Vantage, normaliza distribuições de dividendos e retorna os candidatos elegíveis sem persistir registros

#### Scenario: Evento não monetário retornado pelo provedor
- **WHEN** o provedor retorna bonificação, desdobramento, grupamento, subscrição ou outro evento que altere quantidade
- **THEN** o sistema ignora o evento e não o apresenta como candidato de provento

#### Scenario: Provedor indisponível, limitado ou sem acesso contratado
- **WHEN** a consulta necessária falha tecnicamente, excede limite ou o plano configurado não permite obter proventos
- **THEN** o sistema responde com `503` e código público correspondente, sem expor credenciais, conteúdo bruto nem candidatos parciais daquele mercado

### Requirement: Elegibilidade derivada do histórico efetivado
O sistema SHALL calcular a quantidade elegível reproduzindo, em ordem cronológica, somente compras e vendas `EFFECTIVE` do mesmo ticker e mercado. Para evento `BR`, a quantidade MUST corresponder à posição ao final da data-com informada pelo provedor. Para evento `US`, a quantidade MUST corresponder à posição imediatamente anterior à data ex-dividendo. Um candidato MUST ser elegível somente quando essa quantidade for positiva. Enquanto a data de elegibilidade for futura, o evento MAY ser exibido como informativo, mas MUST NOT ser confirmável nem apresentar quantidade definitiva.

#### Scenario: Direito brasileiro preservado após venda posterior
- **WHEN** o investidor possuía quantidade positiva ao final da data-com e vendeu o ativo depois dela
- **THEN** o sistema calcula o candidato usando a quantidade existente na data-com, ainda que a posição esteja encerrada no pagamento

#### Scenario: Compra americana na data ex não concede direito
- **WHEN** uma compra `US` foi efetivada na própria data ex-dividendo
- **THEN** essa compra não compõe a quantidade elegível do candidato

#### Scenario: Lançamentos não efetivados
- **WHEN** existem lançamentos `PENDING` ou `CANCELLED` para o ativo
- **THEN** esses lançamentos não alteram a quantidade elegível

#### Scenario: Quantidade elegível inexistente
- **WHEN** a reprodução histórica resulta em quantidade igual a zero na data aplicável
- **THEN** o sistema não oferece aquele evento para confirmação

#### Scenario: Evento anunciado antes da data de elegibilidade
- **WHEN** o provedor retorna um evento cuja data-com ou data ex-dividendo ainda não chegou
- **THEN** o sistema o identifica como informativo e impede sua confirmação até que a elegibilidade possa ser determinada

### Requirement: Cálculo previsto e confirmação assistida
O sistema SHALL calcular o valor bruto previsto como `quantidade elegível × valor por unidade`, mantendo precisão decimal sem arredondamento monetário prematuro. Um candidato com elegibilidade concluída e data de pagamento conhecida MUST poder ser confirmado por referência opaca emitida pelo sistema. O investidor MUST informar o valor positivo efetivamente recebido; esse valor MAY divergir do previsto, mas uma justificativa não vazia MUST ser informada nesse caso. O sistema MUST preservar o retrato confirmado, a quantidade elegível, o valor unitário, o valor previsto, o valor recebido, a origem e as datas do evento, sem calcular impostos.

#### Scenario: Confirmação com valor previsto
- **WHEN** o investidor confirma um candidato válido usando como valor recebido o valor bruto previsto
- **THEN** o sistema cria um único provento com o retrato calculado e a origem do provedor

#### Scenario: Confirmação com valor ajustado
- **WHEN** o investidor confirma valor recebido diferente do previsto e fornece uma justificativa válida
- **THEN** o sistema preserva os dois valores e a justificativa no registro

#### Scenario: Divergência sem justificativa
- **WHEN** o valor recebido difere do previsto e nenhuma justificativa válida é informada
- **THEN** o sistema responde com `400`, identifica o campo inválido e não cria o provento

#### Scenario: Candidato expirado, incompleto ou não confirmável
- **WHEN** o investidor tenta confirmar referência expirada ou candidato sem elegibilidade concluída ou data de pagamento
- **THEN** o sistema responde com erro público correspondente e não persiste o provento

### Requirement: Registro manual de contingência
O sistema SHALL permitir registro manual de provento para ativo com ao menos uma compra efetivada na carteira. O registro MUST conter ticker e tipo monetário suportado, data de pagamento e valor recebido positivo; data de elegibilidade, quantidade elegível, valor unitário e observação MAY ser informados. O sistema MUST recuperar nome, mercado, tipo e moeda do retrato da compra efetiva da própria carteira, em vez de aceitá-los do cliente. Valores monetários e quantitativos informados MUST comportar no máximo 11 dígitos inteiros e 8 casas decimais. O registro manual MUST ser identificado com origem `MANUAL` e MUST NOT exigir consulta externa nem cálculo de impostos.

#### Scenario: Provento manual válido
- **WHEN** o investidor informa um provento manual válido para ativo presente no histórico efetivado da carteira
- **THEN** o sistema cria o registro com origem `MANUAL` e preserva os valores informados e o retrato histórico do ativo

#### Scenario: Ativo sem compra efetivada na carteira
- **WHEN** o investidor tenta registrar manualmente provento de ativo que nunca possuiu compra efetivada naquela carteira
- **THEN** o sistema responde com `409` e código `ASSET_NOT_ACQUIRED`, não cria o registro e não revela dados de outras carteiras

#### Scenario: Dados manuais inválidos
- **WHEN** tipo, pagamento, valor recebido, quantidade elegível ou valor unitário não atendem às regras, inclusive precisão ou escala compatíveis com o armazenamento
- **THEN** o sistema responde com `400`, identifica os campos inválidos e não cria o provento

### Requirement: Ciclo de vida e imutabilidade dos proventos
O sistema SHALL criar como `EFFECTIVE` o provento cuja data de pagamento seja igual ou anterior à data atual e como `PENDING` aquele cujo pagamento seja futuro. Antes de consultar ou operar registros e resumo, o sistema MUST efetivar os pendentes cuja data chegou. Um provento `PENDING` MAY ser cancelado; registros `EFFECTIVE` e `CANCELLED` MUST permanecer imutáveis e não poderão ser removidos fisicamente.

#### Scenario: Pagamento ocorrido
- **WHEN** um candidato ou registro manual confirmado possui data de pagamento igual ou anterior à data atual
- **THEN** o sistema o cria como `EFFECTIVE`

#### Scenario: Pagamento futuro
- **WHEN** um candidato ou registro manual confirmado possui data de pagamento futura
- **THEN** o sistema o cria como `PENDING` e não o inclui nos indicadores recebidos

#### Scenario: Efetivação na próxima interação
- **WHEN** a data de pagamento de um provento `PENDING` já chegou e o investidor interage com os proventos da carteira
- **THEN** o sistema muda o status para `EFFECTIVE` antes de concluir a operação solicitada

#### Scenario: Cancelamento antes do pagamento
- **WHEN** o investidor cancela um provento `PENDING` antes da data de pagamento
- **THEN** o sistema o mantém no histórico como `CANCELLED` e responde com `204`

#### Scenario: Tentativa de cancelar provento imutável
- **WHEN** o investidor tenta cancelar um provento `EFFECTIVE` ou `CANCELLED`
- **THEN** o sistema responde com `409` e não altera o histórico

### Requirement: Prevenção de duplicidade e preservação do retrato
O sistema MUST impedir que a mesma carteira confirme duas vezes o mesmo evento de provedor, usando sua identidade externa ou uma impressão determinística dos dados normalizados. Um registro manual com a mesma identidade de negócio de outro registro manual MUST ser rejeitado. Nova sincronização MUST identificar eventos já gravados, e alterações posteriores do provedor MUST NOT sobrescrever automaticamente o retrato confirmado.

#### Scenario: Candidato já confirmado reaparece
- **WHEN** uma sincronização retorna evento que já foi confirmado na carteira
- **THEN** o sistema o identifica como já registrado e não permite nova confirmação

#### Scenario: Confirmações concorrentes
- **WHEN** duas solicitações tentam confirmar simultaneamente o mesmo candidato
- **THEN** apenas um registro é criado e a outra solicitação recebe `409`

#### Scenario: Provedor altera evento confirmado
- **WHEN** a API passa a retornar dados diferentes para evento já confirmado
- **THEN** o sistema preserva o retrato histórico existente e não o atualiza silenciosamente

### Requirement: Histórico privado e ordenado
O sistema SHALL disponibilizar todos os proventos da carteira em ordem da data de pagamento mais recente para a mais antiga, usando a data de criação como desempate. Cada item MUST expor status, retrato do ativo, tipo, moeda, elegibilidade disponível, valor unitário disponível, valor previsto disponível, valor recebido, justificativa disponível, origem, datas do evento e datas públicas. Entidades persistentes e detalhes internos MUST NOT ser expostos.

#### Scenario: Histórico com diferentes estados e origens
- **WHEN** a carteira possui proventos efetivos, pendentes e cancelados, manuais e oriundos de provedores
- **THEN** o sistema retorna todos em ordem de pagamento decrescente com seus respectivos estados e origens

#### Scenario: Histórico vazio
- **WHEN** a carteira não possui proventos registrados
- **THEN** o sistema responde com `200` e coleção vazia

### Requirement: Resumo de proventos recebidos
O sistema SHALL disponibilizar um resumo próprio para o dashboard considerando somente proventos `EFFECTIVE`. O resumo MUST informar o total recebido em cada moeda nativa e um total consolidado em `BRL`. Valores em `BRL` MUST ser somados diretamente; valores em `USD` MUST ser convertidos pela PTAX histórica de compra aplicável à data de pagamento, usando o último fechamento anterior quando necessário e expondo as taxas efetivamente utilizadas. O resumo MUST incluir igualmente registros manuais e confirmados por provedor e MUST NOT alterar posições, preço médio, custo em custódia, valor investido, patrimônio ou ganho/perda de mercado.

#### Scenario: Proventos efetivos em BRL e USD
- **WHEN** a carteira possui proventos efetivados nas duas moedas
- **THEN** o resumo contém totais nativos separados e o total consolidado em BRL com rastreabilidade das conversões históricas

#### Scenario: Provento pendente ou cancelado
- **WHEN** a carteira possui provento `PENDING` ou `CANCELLED`
- **THEN** esse registro permanece no histórico, mas não participa do resumo recebido

#### Scenario: Carteira sem proventos efetivados
- **WHEN** não existe provento `EFFECTIVE` na carteira
- **THEN** o sistema retorna resumo vazio ou zerado sem consultar o provedor cambial

#### Scenario: Taxa histórica indisponível
- **WHEN** um provento efetivo em USD exige taxa histórica que não pode ser obtida
- **THEN** o sistema responde com `503` e código `EXCHANGE_RATE_UNAVAILABLE`, sem devolver resumo parcial

### Requirement: Compatibilidade de persistência e respostas
O sistema MUST manter o mesmo comportamento funcional de proventos no H2 e no PostgreSQL. Todas as respostas e falhas MUST ser JSON e MUST usar o tratamento centralizado de erros, sem expor credenciais, URLs internas, respostas brutas, entidades ou detalhes de outros investidores.

#### Scenario: Banco suportado vazio
- **WHEN** a aplicação inicia com H2 ou PostgreSQL vazio em perfil suportado
- **THEN** a estrutura de proventos fica pronta para consulta, confirmação, registro, histórico, cancelamento e resumo
