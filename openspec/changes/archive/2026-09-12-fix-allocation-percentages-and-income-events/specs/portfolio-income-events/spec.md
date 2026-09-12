## MODIFIED Requirements

### Requirement: Consulta explícita e normalizada de candidatos
O sistema SHALL consultar proventos somente mediante solicitação do investidor, usando a Brapi para ativos `BR` e a Alpha Vantage para ativos `US` que possuam histórico efetivado relevante na carteira. A resposta MUST ser um objeto JSON com `candidates`, instante de atualização, indicação de reutilização de dados anteriores e avisos públicos por ticker. Cada candidato MUST normalizar ticker, retrato do ativo, mercado, moeda, tipo, valor por unidade, data de elegibilidade, data de pagamento, origem e identidade do evento quando disponíveis. Somente eventos monetários `DIVIDEND`, `INTEREST_ON_EQUITY` e `DISTRIBUTION` SHALL ser considerados; eventos que alterem quantidade MUST ser ignorados. O sistema MUST evitar consultas externas equivalentes repetidas ou concorrentes, reutilizar temporariamente respostas válidas inclusive quando vazias e MUST NOT persistir um provento antes da confirmação.

#### Scenario: Candidatos brasileiros consultados
- **WHEN** o investidor solicita candidatos `BR` para uma carteira que possui ativos brasileiros no histórico efetivado relevante
- **THEN** o sistema consulta a Brapi, normaliza dividendos, JCP e distribuições monetárias e retorna os candidatos elegíveis sem persistir registros

#### Scenario: Candidatos americanos consultados
- **WHEN** o investidor solicita candidatos `US` para uma carteira que possui ativos americanos no histórico efetivado relevante
- **THEN** o sistema consulta a Alpha Vantage, normaliza distribuições de dividendos e retorna os candidatos elegíveis sem persistir registros

#### Scenario: Evento não monetário retornado pelo provedor
- **WHEN** o provedor retorna bonificação, desdobramento, grupamento, subscrição ou outro evento que altere quantidade
- **THEN** o sistema ignora o evento e não o apresenta como candidato de provento

#### Scenario: Carteira sem ticker relevante no mercado
- **WHEN** a carteira não possui ativo que precise ser consultado no mercado solicitado
- **THEN** o sistema responde com coleção de candidatos vazia, sem avisos e sem chamar o provedor

#### Scenario: Resposta válida reutilizada
- **WHEN** uma consulta equivalente para o mesmo mercado e ticker ocorre dentro da janela configurada
- **THEN** o sistema reutiliza a resposta temporária, inclusive quando vazia, sem consumir nova requisição externa

#### Scenario: Consultas concorrentes equivalentes
- **WHEN** duas solicitações concorrentes precisam dos mesmos dados de provedor
- **THEN** o sistema executa no máximo uma chamada externa por chave equivalente e compartilha o resultado entre as solicitações

#### Scenario: Provedor falha e existe último resultado utilizável
- **WHEN** uma atualização falha por indisponibilidade ou limite e existe resposta válida ainda dentro da janela de contingência
- **THEN** o sistema reutiliza o último resultado, marca a resposta como não atualizada e inclui aviso público para o ticker afetado

#### Scenario: Falha parcial por ticker
- **WHEN** ao menos um ticker produz resultado confiável e outro falha sem possuir dado reutilizável
- **THEN** o sistema responde com status `200`, entrega os candidatos confiáveis e inclui aviso público com ticker e código da falha, sem apresentar o conjunto como totalmente atualizado

#### Scenario: Provedor indisponível, limitado ou sem acesso contratado
- **WHEN** todos os tickers necessários falham tecnicamente, excedem limite ou não possuem acesso contratado e nenhum resultado reutilizável existe
- **THEN** o sistema responde com `503` e código público correspondente, sem expor credenciais, conteúdo bruto nem dados não confiáveis

#### Scenario: Nova tentativa após expiração
- **WHEN** a janela de reutilização normal termina e o provedor está disponível
- **THEN** a próxima solicitação realiza uma nova consulta, atualiza o instante público e substitui o resultado temporário anterior

## ADDED Requirements

### Requirement: Compatibilidade controlada do contrato de candidatos
O sistema MUST migrar a aplicação web e a documentação para o objeto de resposta enriquecido da consulta de candidatos. Referências opacas de candidatos MUST continuar vinculadas ao investidor e à carteira, possuir expiração e permanecer confirmáveis durante sua validade mesmo quando o evento foi obtido de cache seguro.

#### Scenario: Candidato proveniente de resposta reutilizada
- **WHEN** um candidato é reconstruído a partir de resultado temporário válido ou reutilizável
- **THEN** o sistema emite uma nova referência opaca vinculada ao investidor e à carteira e permite a confirmação pelas mesmas regras de elegibilidade

#### Scenario: Cliente usa o contrato antigo
- **WHEN** um cliente espera uma coleção JSON na raiz após a implantação da mudança
- **THEN** ele precisa ser migrado para ler `candidates` e os metadados do novo objeto de resposta
