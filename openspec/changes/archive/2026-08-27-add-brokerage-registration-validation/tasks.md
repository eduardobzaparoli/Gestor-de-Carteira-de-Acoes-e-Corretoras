## 1. Configuração e contratos externos

- [x] 1.1 Configurar URLs base, timeouts e vigência do snapshot da CVM por propriedades, com valores locais seguros e substituíveis em testes.
- [x] 1.2 Inspecionar o ZIP/CSV oficial de participantes da CVM, documentar cabeçalhos, delimitador, codificação e valores de situação usados na validação.
- [x] 1.3 Adicionar e justificar uma dependência específica de parsing CSV somente se o formato oficial não puder ser tratado com segurança pela biblioteca padrão, sem incluir outro cliente HTTP.
- [x] 1.4 Criar modelos internos mínimos para os resultados de CNPJ, CEP e participação CVM, sem expor DTOs dos fornecedores às demais camadas.
- [x] 1.5 Criar interfaces Strategy para consulta cadastral de CNPJ, endereço por CEP e participação ativa na CVM.

## 2. Domain, persistência e mapeamento

- [x] 2.1 Criar os modelos Domain puros de corretora e endereço, incluindo proprietário, apelido, dados oficiais, categoria/situação CVM e timestamps.
- [x] 2.2 Criar a Entity de corretora vinculada à Entity de usuário, com colunas portáveis e restrições únicas para proprietário + CNPJ e proprietário + chave normalizada do apelido.
- [x] 2.3 Criar o Repository com consultas por proprietário, CNPJ e apelido normalizados, além de listagem isolada por proprietário.
- [x] 2.4 Criar DTOs imutáveis para cadastro, consulta de CEP, corretora pública e listagem, sem aceitar proprietário, cidade, estado ou dados oficiais controlados pelo cliente.
- [x] 2.5 Criar o Mapper Domain ↔ Entity e Domain/integrações ↔ DTOs públicos, sem expor Entity ou metadados internos.
- [x] 2.6 Testar mapeamentos, relacionamento com usuário e restrições de unicidade no H2, incluindo o mesmo CNPJ permitido para proprietários diferentes.

## 3. Integrações Brasil API, ViaCEP e CVM

- [x] 3.1 Implementar o adaptador da Brasil API para CNPJ, normalizando a resposta e distinguindo ausência cadastral de falha técnica.
- [x] 3.2 Implementar o adaptador da ViaCEP, incluindo resposta com campos ausentes, CEP inexistente, conteúdo inválido, timeout e erro técnico.
- [x] 3.3 Implementar download e leitura segura do snapshot da CVM, validando estrutura e construindo índice por CNPJ com categoria e situação.
- [x] 3.4 Implementar cache concorrente do snapshot da CVM com vigência configurável, atualização sincronizada e falha segura quando não houver cópia vigente.
- [x] 3.5 Criar testes determinísticos dos adaptadores HTTP com servidor simulado e do parser/cache CVM com fixtures ZIP/CSV pequenas.
- [x] 3.6 Garantir que logs das integrações mascarem identificadores quando necessário e não exponham corpos brutos, URLs internas ou detalhes técnicos nas respostas.

## 4. Regras de negócio e transação curta

- [x] 4.1 Implementar normalização e validação local de apelido, CNPJ, CEP e endereço, incluindo dígitos verificadores do CNPJ e limites pós-normalização.
- [x] 4.2 Implementar consulta assistida de CEP sem persistência, retornando somente os campos previstos e preservando rua/bairro ausentes como preenchíveis manualmente.
- [x] 4.3 Implementar a orquestração de cadastro fora da transação: identidade, duplicidade preliminar, Brasil API, CVM ativa, comparação dos CEPs e ViaCEP.
- [x] 4.4 Implementar persistência em transação curta com nova checagem de duplicidade e tradução específica das violações concorrentes de CNPJ e apelido.
- [x] 4.5 Persistir rua, bairro, número e complemento confirmados pelo investidor e obter cidade e estado exclusivamente do resultado definitivo da ViaCEP.
- [x] 4.6 Implementar listagem por UUID do investidor sem acesso cruzado e sem permitir que o cliente escolha o proprietário.
- [x] 4.7 Criar testes unitários do Service para cada etapa aprovada, falhas externas, ausência de persistência parcial, ajustes manuais e isolamento entre investidores.

## 5. API, segurança e erros públicos

- [x] 5.1 Criar endpoint protegido de consulta assistida de CEP para investidores, com respostas `200`, `400`, `404` e `503` padronizadas.
- [x] 5.2 Criar `POST /api/brokerages` para confirmação do cadastro, retornando `201` e somente o DTO público da corretora.
- [x] 5.3 Criar `GET /api/brokerages` para listar somente as corretoras do investidor autenticado.
- [x] 5.4 Restringir todas as operações de corretora ao papel `INVESTOR`, preservando `401` sem token e retornando `403` para administradores.
- [x] 5.5 Criar exceções de negócio e mapear no tratamento centralizado os códigos de validação, conflito, divergência cadastral e indisponibilidade definidos na especificação.
- [x] 5.6 Garantir que JSON malformado, campos adicionais e erros externos não permitam controlar proprietário, cidade, estado, dados oficiais ou detalhes internos.

## 6. Testes integrados e compatibilidade

- [x] 6.1 Testar consulta de CEP válida, incompleta, inválida, inexistente e indisponível com integrações simuladas.
- [x] 6.2 Testar cadastro aprovado e cada rejeição de Brasil API, CVM, divergência de CEP e ViaCEP, confirmando que nenhuma falha persiste dados parciais.
- [x] 6.3 Testar campos obrigatórios, limites, normalização, endereço manual, cidade/estado oficiais e resposta pública sem Entity ou conteúdo interno.
- [x] 6.4 Testar duplicidade de CNPJ e apelido por investidor, concorrência, repetição permitida entre investidores e listagem sem vazamento de dados.
- [x] 6.5 Testar autenticação e autorização das três operações, incluindo `401` sem token e `403` para administrador.
- [x] 6.6 Executar a suíte Maven completa no perfil H2 e corrigir falhas ou alertas relacionados à mudança.
- [x] 6.7 Executar verificação opt-in das fontes reais com CNPJ/CEP configuráveis, sem tornar a suíte padrão dependente da internet.
- [x] 6.8 Inicializar e validar o fluxo de corretoras no PostgreSQL local sem Docker, usando somente variáveis de ambiente e dados de teste controlados.

## 7. Revisão final da mudança

- [x] 7.1 Executar `openspec validate add-brokerage-registration-validation --strict` e reconciliar código, testes, spec, design e tarefas.
- [x] 7.2 Revisar `git diff` para confirmar arquitetura em camadas, isolamento das estratégias externas, ausência de credenciais e preservação de alterações não relacionadas do usuário.
- [x] 7.3 Confirmar que criação automática do esquema continua restrita a desenvolvimento/testes e que a futura migração Flyway inclui a tabela de corretoras antes da produção.
