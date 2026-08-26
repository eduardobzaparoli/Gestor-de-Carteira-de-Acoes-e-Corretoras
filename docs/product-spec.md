# Especificação Técnica (PRD): Gestor de Carteiras de Ações e Corretoras

## 1. Visão Geral do Produto
O sistema é uma API RESTful em Java com Spring Boot para gestão de investimentos. A aplicação simula um software de gestão de ações com uma interface orientada a Business Intelligence (BI). Permite o cadastro de carteiras, corretoras e ativos (brasileiros e americanos), integrando-se a APIs públicas para validação e obtenção de dados em tempo real. O sistema possui um histórico de lançamentos (log de transações) e indicadores financeiros calculados dinamicamente.

## 2. Atores do Sistema
- **Investidor (Usuário Comum):** Acessa a aplicação via login, gerencia suas próprias corretoras, cria carteiras, realiza lançamentos (compra/venda) e visualiza o dashboard de BI. Seus dados são isolados.
- **Administrador:** Possui acesso a um painel exclusivo para gerenciar (CRUD) usuários do sistema. Não tem acesso às carteiras e transações privadas dos investidores.

## 3. Jornada do Usuário e Requisitos Funcionais
- **RF01 (Autenticação):** A primeira tela do sistema é o login/cadastro de usuário.
- **RF02 (Gestão de Carteiras):** Após o login, o usuário visualiza suas carteiras e pode criar novas.
- **RF03 (Vínculo com Corretora):** A criação de uma carteira exige a vinculação a uma única corretora.
- **RF04 (Cadastro de Corretora):** Realizado a partir do CNPJ. O sistema consulta a **Brasil API** para dados cadastrais, a **ViaCEP** para o endereço e valida a instituição no **Portal de Dados Abertos (CVM)**.
- **RF05 (Dashboard de BI):** Ao abrir uma carteira, o sistema exibe indicadores financeiros atualizados (consultando as APIs de cotação naquele momento) e gráficos de análise.
- **RF06 (Pesquisa de Ativos):** A tela da carteira possui uma barra de pesquisa com filtro de mercado (BR ou EUA) para localizar ativos via **Brapi** ou **AlphaVantage**.
- **RF07 (Lançamentos / Log):** Ao selecionar um ativo, o usuário registra um Lançamento de Compra ou Venda, informando data, quantidade, preço e custos.
- **RF08 (Histórico):** Todos os lançamentos formam um log histórico consultável.

## 4. Regras de Negócio e Cálculos
- **RN01 (Bloqueio de Venda a Descoberto):** O sistema impede a venda de um ativo em quantidade superior ao saldo atual na carteira.
- **RN02 (Validação Estrita):** Proibido preencher manualmente dados de corretoras sem validação via CNPJ/CEP e Portal de Dados Abertos.
- **RN03 (Lançamentos Futuros):** Um lançamento com data posterior à atual fica com status "Pendente" e pode ser cancelado até a sua efetivação.
- **RN04 (Preço Médio):** Calculado como média ponderada entre o custo total para determinado ticker e a quantidade de ações adquiridas. Vendas não alteram o preço médio, apenas reduzem a quantidade.
- **RN05 (Isenção de Impostos):** Nenhum cálculo do sistema incide impostos.
- **RN06 (Fórmulas do Dashboard):**
  - **Valor Investido:** Soma do custo das Compras subtraindo o custo das Vendas (baseado no Preço Médio).
  - **Patrimônio Total:** (Quantidade Atual de todos os ativos) * (Preço Atual da API).
  - **Lucro Total (Ganho de Capital):** Patrimônio Total - Valor Investido.
  - **Rentabilidade (%):** (Lucro Total / Valor Investido) * 100.

## 5. Visualização de Dados (Gráficos)
- **Gráfico de Composição (Rosca/Donut):** Exibe o percentual de cada ativo em relação ao total da carteira.
- **Gráfico de Evolução Patrimonial (Linha):** Compara o Valor Investido com o Patrimônio Total ao longo do tempo.

## 6. Arquitetura e Requisitos Não-Funcionais
- **Stack Tecnológica:** Java 17+ com Spring Boot 3+.
- **Banco de Dados:** H2 (memória/desenvolvimento) e PostgreSQL (produção).
- **Integrações Externas:** 
  - CNPJ e CNAE: `Brasil API`
  - CEP: `ViaCEP`
  - CVM: `Portal de Dados Abertos`
  - Ações Brasileiras: `Brapi`
  - Ações Americanas: `AlphaVantage`
- **Padrões de Projeto:**
  - **Strategy:** Isolar integrações de APIs externas (ex: `StockQuoteStrategy`), permitindo alternar ou escalar serviços de cotação.
  - **State:** Gerenciar o ciclo de vida dos Lançamentos (`PENDENTE` -> `EFETIVADO` -> `CANCELADO`).
- **Resiliência e Padrões de Código:**
  - Tratamento centralizado de erros (`@ControllerAdvice`).
  - Tratamento de falhas de rede (APIs fora do ar, limites de requisição excedidos, ticker/CNPJ inexistente).
  - Arquitetura em camadas (Domain, Mapper, Controller, Service, Repository, Entity, DTO) aplicando princípios SOLID.
  - Retornos da API exclusivamente em formato JSON.
