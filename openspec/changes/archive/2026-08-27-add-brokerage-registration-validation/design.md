## Context

O sistema já possui autenticação JWT stateless, identidade do usuário por UUID, papéis `INVESTOR` e `ADMIN`, erros JSON padronizados e separação entre Domain, Entity, Mapper, Repository, Service, DTO e Controller. Ainda não existem corretoras nem clientes para integrações externas. Consulte `proposal.md` para a motivação e `specs/brokerage-registration/spec.md` para o contrato comportamental.

A confirmação do cadastro depende de três fontes com características diferentes: Brasil API e ViaCEP expõem consultas HTTP por identificador, enquanto a CVM publica periodicamente um conjunto de dados de participantes intermediários. Chamadas de rede não podem deixar uma transação de banco aberta nem permitir persistência parcial. O projeto continua sem Flyway nesta etapa e sem Docker; o Hibernate permanece limitado à inicialização de esquemas locais.

## Goals / Non-Goals

**Goals:**

- Preservar a arquitetura em camadas e manter a coordenação das regras no Service.
- Isolar cada fonte externa por uma interface substituível e testável.
- Repetir no backend todas as validações definitivas, sem confiar na consulta assistida feita pela interface.
- Evitar transações de banco durante chamadas de rede e garantir unicidade concorrente na persistência.
- Produzir comportamento equivalente no H2 e no PostgreSQL.

**Non-Goals:**

- Implementar a interface gráfica ou o botão de confirmação.
- Editar ou excluir corretoras após o cadastro.
- Vincular corretoras a carteiras nesta mudança.
- Permitir que administradores gerenciem ou consultem corretoras de investidores.
- Validar selos B3, processos sancionadores, dados bancários ou qualidade comercial da instituição.
- Restringir o cadastro a categorias específicas da CVM além do requisito aprovado de participação ativa.
- Introduzir Flyway ou preparar implantação em produção antes das migrações versionadas.

## Decisions

### 1. Modelo privado de corretora por investidor

O Domain terá uma corretora com UUID próprio, UUID do proprietário, apelido, CNPJ, dados oficiais da instituição, categoria/situação da CVM, endereço e timestamps. O endereço será um objeto de valor do Domain. A Entity representará a tabela de corretoras e se relacionará ao usuário persistido, sem expor esse relacionamento nos DTOs públicos.

As restrições únicas serão compostas por proprietário + CNPJ normalizado e proprietário + apelido normalizado. O apelido original normalizado para exibição será armazenado junto de uma chave em minúsculas para comparação determinística nos dois bancos.

Alternativa considerada: manter uma instituição global por CNPJ e criar uma associação separada para cada usuário. Foi rejeitada nesta primeira entrega porque amplia o modelo e o ciclo de atualização sem benefício necessário para o cadastro privado; a duplicação controlada dos dados oficiais entre investidores mantém o isolamento simples.

### 2. Portas de integração e Strategy

O Service dependerá de três contratos: consulta cadastral de CNPJ, consulta de endereço por CEP e consulta de participação ativa na CVM. Implementações HTTP/dataset ficarão fora do Domain e serão injetadas pelas interfaces, permitindo testes determinísticos e futuras substituições de fornecedor.

Os clientes usarão o mecanismo HTTP já fornecido pelo Spring, com URL base e timeouts configuráveis por propriedades. Não será adicionada uma biblioteca HTTP paralela. Respostas externas serão convertidas imediatamente para modelos internos mínimos; DTOs dos fornecedores não atravessarão o limite do componente de integração.

Alternativa considerada: chamar as APIs diretamente no Controller ou no Service por URLs fixas. Foi rejeitada por acoplar regras de negócio ao transporte, dificultar testes e contrariar o Strategy aprovado para integrações externas.

### 3. Consulta assistida separada da validação definitiva

Uma operação protegida de consulta de CEP retornará os campos disponíveis na ViaCEP para auxiliar a interface. Ela não criará estado e não produzirá uma autorização reutilizável. O cadastro repetirá a consulta de CEP e executará as validações de CNPJ e CVM, impedindo que dados antigos ou manipulados no cliente contornem as regras.

Rua, bairro, número e complemento virão do pedido final e serão normalizados. Cidade e estado virão exclusivamente da ViaCEP no momento da confirmação. Campos extras enviados pelo cliente não controlarão dados oficiais.

Alternativa considerada: emitir um token de validação após a primeira consulta e reutilizá-lo no cadastro. Foi rejeitada porque adiciona expiração e armazenamento de estado sem eliminar a necessidade de conferir CNPJ e CVM na confirmação.

### 4. Pipeline de validação antes da transação

O fluxo de cadastro será coordenado nesta ordem lógica:

```text
entrada e identidade
        ↓
normalização e validação local
        ↓
checagem preliminar de duplicidade
        ↓
Brasil API ── fornece dados oficiais e CEP cadastral
        ↓
CVM ───────── confirma participação ativa por CNPJ
        ↓
comparação do CEP informado com o CEP cadastral
        ↓
ViaCEP ────── confirma existência e fornece cidade/estado
        ↓
transação curta: nova checagem + persistência
```

As chamadas externas ocorrerão antes da transação de escrita. Dentro da transação curta, o sistema verificará novamente as duplicidades e persistirá. Restrições únicas no banco serão a garantia final contra concorrência, e suas violações serão traduzidas para o conflito específico de CNPJ ou apelido.

Alternativa considerada: envolver todo o fluxo em um único `@Transactional`. Foi rejeitada porque manteria conexão e locks de banco durante chamadas de rede imprevisíveis.

### 5. Leitura e vigência do cadastro da CVM

A integração da CVM tratará o arquivo oficial de participantes como um snapshot e construirá um índice por CNPJ normalizado. Uma cópia carregada com idade máxima configurável, inicialmente 24 horas, poderá atender validações sem baixar o mesmo arquivo a cada cadastro. Quando não houver cópia vigente, a primeira validação fará a atualização de forma sincronizada; falha na atualização resultará em indisponibilidade e impedirá o cadastro.

O índice conservará apenas os campos necessários para identificar o participante, sua categoria e situação. Registros ausentes ou não ativos serão rejeitados. O download ZIP usará a biblioteca padrão; para interpretar CSV com delimitadores, aspas e codificação de forma segura, será adicionada uma biblioteca pequena e específica de CSV caso a inspeção do recurso oficial confirme essa necessidade.

Alternativa considerada: baixar e percorrer o conjunto completo em toda confirmação. Foi rejeitada por aumentar latência e carga sobre a fonte oficial. Persistir todo o cadastro da CVM no banco também foi adiado por introduzir sincronização e retenção que não são necessárias nesta entrega.

### 6. Erros e observabilidade das integrações

Cada integração traduzirá respostas de ausência cadastral para resultados de negócio e timeouts, erros HTTP inesperados, conteúdo inválido ou falhas de leitura para exceções técnicas próprias. O tratamento centralizado mapeará essas exceções para os códigos públicos definidos na especificação.

Logs poderão registrar provedor, tipo da falha e identificadores mascarados, mas não corpos brutos potencialmente extensos, URLs com parâmetros sensíveis ou stack traces nas respostas. Não haverá retentativas automáticas nesta primeira versão; timeouts curtos e erro explícito evitam prolongar excessivamente a confirmação.

Alternativa considerada: tratar toda falha externa como cadastro inexistente. Foi rejeitada porque produziria falsos negativos e impediria o usuário de distinguir dados inválidos de indisponibilidade temporária.

### 7. Testes sem dependência permanente da internet

Testes do Service usarão implementações controladas das três interfaces. Testes dos adaptadores usarão servidor HTTP simulado e arquivos pequenos representativos do ZIP/CSV da CVM. Testes integrados da API cobrirão autenticação, isolamento, normalização, erros e persistência no H2.

Uma verificação opt-in poderá executar consultas reais e o fluxo de persistência no PostgreSQL local, sem fazer parte da suíte padrão e sem Docker. Ela não usará CNPJ ou CEP inventados para afirmar comportamento das fontes oficiais; os dados de verificação serão configuráveis e nenhum segredo será versionado.

Alternativa considerada: usar somente as APIs públicas reais na suíte Maven. Foi rejeitada por tornar os testes lentos, não determinísticos e dependentes de disponibilidade e limites externos.

## Risks / Trade-offs

- [A base da CVM pode alterar estrutura, codificação ou nomes de colunas] → Isolar parsing, validar cabeçalhos esperados e cobrir fixtures representativas com falha segura.
- [Brasil API, ViaCEP e CVM podem divergir temporariamente] → Exigir todas as condições aprovadas, retornar código específico e não persistir até que as fontes voltem a concordar.
- [O snapshot em memória é local a cada instância] → Aceitar duplicação de cache nesta fase e configurar vigência; considerar armazenamento compartilhado somente se houver múltiplas instâncias e carga relevante.
- [Usuário pode ajustar rua e bairro para valores que não coincidem literalmente com a ViaCEP] → Preservar a decisão de permitir endereço manual, mantendo CEP, cidade e estado validados e auditáveis.
- [Checagem preliminar não evita corrida] → Aplicar restrições únicas compostas no banco e traduzir violações concorrentes.
- [Novas tabelas ainda não têm migrações versionadas] → Restringir criação automática aos ambientes locais e manter Flyway obrigatório antes da primeira implantação em produção.

## Migration Plan

1. Adicionar propriedades locais para URLs, timeouts e vigência do snapshot da CVM.
2. Criar o modelo persistente de corretoras e suas restrições no esquema gerenciado localmente pelo Hibernate.
3. Implementar e validar as integrações com respostas simuladas antes da verificação opt-in com fontes reais.
4. Validar cadastro e listagem no H2 e no PostgreSQL local sem Docker.
5. Em rollback local, remover apenas os registros/tabelas de corretoras dos bancos de desenvolvimento ou teste afetados; usuários existentes permanecem intactos.
6. Antes de produção, incluir a tabela de corretoras na futura mudança de migrações Flyway e manter `ddl-auto=validate` no perfil produtivo.

