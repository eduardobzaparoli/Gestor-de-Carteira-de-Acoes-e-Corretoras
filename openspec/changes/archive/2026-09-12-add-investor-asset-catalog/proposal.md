## Why

O fluxo atual permite pesquisar ativos diretamente dentro de cada carteira e usa uma referência temporária para criar lançamentos, o que repete pesquisas externas e não oferece ao investidor um catálogo reutilizável. Um cadastro prévio de ativos reduz consultas desnecessárias, preserva a cotação conhecida no momento do cadastro e torna a seleção consistente entre todas as carteiras do mesmo usuário.

## What Changes

- Adicionar um catálogo persistente e privado de ações e ETFs brasileiros e americanos para cada investidor.
- Permitir pesquisar um ativo nos provedores existentes, cadastrá-lo com sua cotação corrente e registrar a data e hora da consulta.
- Listar os ativos cadastrados com filtros por todos, brasileiros e americanos, incluindo cotação armazenada e instante da última atualização.
- Permitir atualizar individualmente a cotação armazenada, preservando o último valor válido caso o provedor falhe.
- Permitir excluir um ativo ou todo o catálogo, bloqueando a operação de forma atômica quando algum ativo possuir saldo positivo em qualquer carteira do investidor.
- Adicionar uma tela protegida de ativos ao frontend, seguindo os componentes, estados visuais, responsividade, temas e identidade existentes na gestão de corretoras.
- Exibir cotações americanas em reais, tornar a marca do menu um retorno à visão geral, manter logos de ativos nos seletores e remover atribuições visuais de fornecedores de logos.
- Restringir o formulário de lançamento aos ativos previamente cadastrados pelo investidor, disponibilizando o mesmo catálogo em qualquer uma de suas carteiras.
- Consultar novamente o provedor quando um ativo cadastrado for selecionado para um lançamento e preencher o preço unitário com a cotação mais recente, sem substituir a cotação histórica armazenada no catálogo até uma atualização explícita do cadastro.
- Preservar o retrato do ativo em cada lançamento existente e novo para que alterações futuras no catálogo não modifiquem o histórico.
- **BREAKING**: substituir, na criação de lançamentos, a referência temporária emitida pela pesquisa vinculada à carteira por uma referência ao ativo persistido no catálogo do investidor.

## Capabilities

### New Capabilities

- `investor-asset-catalog`: cadastro, unicidade, listagem, filtros, atualização de cotação, isolamento por investidor e persistência compatível com H2 e PostgreSQL.

### Modified Capabilities

- `asset-search`: mover a pesquisa externa usada para seleção de ativos do contexto de uma carteira para o contexto privado do catálogo do investidor.
- `portfolio-transactions`: exigir um ativo previamente cadastrado, validar sua propriedade e obter uma cotação nova ao selecioná-lo para um lançamento.
- `react-web-interface`: adicionar a navegação e a tela de ativos e substituir a pesquisa livre do formulário de lançamento pela seleção do catálogo do investidor.

## Impact

- Nova entidade, domínio, DTOs, mapper, repository, service e controller para ativos cadastrados.
- Nova migration Flyway e validações de esquema para H2 e PostgreSQL, incluindo vínculo com usuário e restrição única por investidor, mercado e ticker normalizado.
- Reuso das estratégias Brapi e Twelve Data e dos mecanismos de tradução de falhas, com operações transacionais para não persistir cadastros ou atualizações parciais.
- Alteração do contrato de pesquisa de ativos e da criação de lançamentos, mantendo respostas JSON e autorização por papel e proprietário.
- Novas operações de exclusão segura do catálogo, com validação das posições consolidadas do investidor antes de remover registros.
- Nova rota, item de navegação, página, diálogos, filtros, estados de carregamento/erro/vazio e testes no frontend React.
- Atualização da documentação da API, configuração e especificação global do produto para refletir o cadastro prévio obrigatório.
