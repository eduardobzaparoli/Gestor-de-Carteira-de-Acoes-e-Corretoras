## 1. Domain, persistência e mapeamento

- [x] 1.1 Criar o modelo Domain puro de carteira com proprietário, corretora, nome exibido, chave normalizada e timestamps.
- [x] 1.2 Criar a Entity de carteira vinculada às Entities de usuário e corretora, com colunas portáveis e restrição única para proprietário + chave do nome.
- [x] 1.3 Criar o Repository com consultas por UUID e proprietário e listagem do proprietário ordenada por criação crescente e UUID crescente.
- [x] 1.4 Criar DTOs imutáveis para criação, resumo público de corretora e resposta pública de carteira, sem aceitar proprietário ou dados internos de corretora.
- [x] 1.5 Criar o Mapper Domain ↔ Entity e Domain ↔ DTOs públicos, sem expor Entity.
- [x] 1.6 Testar mapeamentos, relacionamento obrigatório com corretora e restrição de unicidade no H2.

## 2. Regras de negócio e segurança

- [x] 2.1 Implementar normalização e validação do nome, incluindo remoção de espaços nas extremidades, limite de 100 caracteres e chave sem distinção de caixa.
- [x] 2.2 Implementar criação usando somente o UUID do investidor autenticado e uma corretora pertencente a ele, sem chamadas às integrações externas.
- [x] 2.3 Implementar tratamento de corretora inexistente ou de outro investidor como `BROKERAGE_NOT_FOUND` sem persistência parcial.
- [x] 2.4 Implementar verificação de nome duplicado e tradução da violação concorrente para `PORTFOLIO_NAME_ALREADY_REGISTERED`.
- [x] 2.5 Implementar consulta individual, listagem cronológica crescente e exclusão física restritas ao proprietário, retornando `PORTFOLIO_NOT_FOUND` para recursos inexistentes ou alheios.
- [x] 2.6 Criar testes unitários do Service para criação, reutilização de corretora, normalização, conflitos, ordenação, exclusão e isolamento entre investidores.

## 3. API e tratamento de erros

- [x] 3.1 Criar endpoints protegidos `POST /api/portfolios`, `GET /api/portfolios`, `GET /api/portfolios/{id}` e `DELETE /api/portfolios/{id}` com os status públicos definidos na especificação.
- [x] 3.2 Restringir as operações ao papel `INVESTOR`, preservando `401` sem token e retornando `403` para administradores.
- [x] 3.3 Adicionar exceções e mapeamentos centralizados para conflito de nome, corretora não encontrada e carteira não encontrada em JSON padronizado.
- [x] 3.4 Garantir que JSON malformado ou campos adicionais não controlem proprietário, dados internos de corretora ou dados de outras pessoas.

## 4. Testes integrados e compatibilidade

- [x] 4.1 Testar criação válida, campos obrigatórios, limite, normalização e conflito de nome pela API.
- [x] 4.2 Testar corretora inexistente, corretora de outro investidor e reutilização de uma corretora já cadastrada sem dependência de fontes externas.
- [x] 4.3 Testar consulta individual, listagem da mais antiga para a mais recente, coleção vazia, exclusão e ausência de vazamento entre investidores.
- [x] 4.4 Testar autenticação e autorização das quatro operações, incluindo `401` sem token e `403` para administrador.
- [x] 4.5 Executar a suíte Maven completa no perfil H2 e corrigir falhas relacionadas à mudança.
- [x] 4.6 Inicializar e validar criação, listagem, consulta e exclusão no PostgreSQL local sem Docker, usando somente variáveis de ambiente e dados de teste controlados.

## 5. Revisão final da mudança

- [x] 5.1 Executar `openspec validate add-portfolio-management --strict` e reconciliar código, testes, especificação, design e tarefas.
- [x] 5.2 Revisar o diff para confirmar arquitetura em camadas, ausência de credenciais e preservação de alterações não relacionadas.
- [x] 5.3 Confirmar que criação automática do esquema continua restrita a desenvolvimento e teste e que a futura migração Flyway inclui a tabela de carteiras antes da produção.
