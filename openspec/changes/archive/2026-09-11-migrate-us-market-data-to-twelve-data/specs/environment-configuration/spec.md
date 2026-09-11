## MODIFIED Requirements

### Requirement: Modelo de ambiente seguro e versionável
O projeto SHALL fornecer um arquivo de exemplo versionado que identifique o perfil de desenvolvimento, a conexão PostgreSQL, a chave da Twelve Data usada em pesquisa, cotação e histórico `US`, a chave da Alpha Vantage usada exclusivamente em dividendos `US` e os demais valores obrigatórios para iniciar a aplicação. Esse modelo MUST conter somente valores públicos ou inequivocamente fictícios e MUST NOT funcionar como repositório de segredos reais.

#### Scenario: Desenvolvedor prepara o ambiente local
- **WHEN** um desenvolvedor consulta o modelo versionado
- **THEN** encontra `SPRING_PROFILES_ACTIVE`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `TWELVE_DATA_API_KEY` e `ALPHA_VANTAGE_API_KEY` documentados sem credenciais reais

#### Scenario: Modelo é enviado ao repositório
- **WHEN** o arquivo de exemplo é incluído em um commit
- **THEN** nenhum segredo local, token de provedor ou senha verdadeira é publicado

#### Scenario: Credencial de dados de mercado ausente
- **WHEN** um fluxo de pesquisa, cotação ou histórico `US` é solicitado sem `TWELVE_DATA_API_KEY` válida
- **THEN** somente esse fluxo responde com indisponibilidade pública da Twelve Data, sem impedir a inicialização da aplicação nem consultar a Alpha Vantage como fallback

#### Scenario: Credencial de dividendos ausente
- **WHEN** candidatos de dividendos `US` são solicitados sem `ALPHA_VANTAGE_API_KEY` válida
- **THEN** somente esse fluxo responde com indisponibilidade pública da Alpha Vantage, sem impedir pesquisa, cotação ou histórico pela Twelve Data

