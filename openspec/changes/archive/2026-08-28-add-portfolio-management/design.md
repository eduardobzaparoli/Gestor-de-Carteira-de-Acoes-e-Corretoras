## Context

O sistema já possui identidade JWT com papéis de investidor e administrador, corretoras privadas vinculadas ao usuário e persistência local compatível com H2 e PostgreSQL. A nova carteira é o próximo agregado de negócio: ela pertence a um investidor e referencia uma corretora previamente aprovada. Consulte `proposal.md` para a motivação e `specs/portfolio-management/spec.md` para o contrato público.

## Goals / Non-Goals

**Goals:**

- Manter a arquitetura em camadas e concentrar regras de propriedade, normalização e unicidade no Service.
- Garantir que a corretora escolhida pertença ao investidor sem repetir integrações externas já concluídas no seu cadastro.
- Disponibilizar um contrato simples para a futura tela de listagem e abertura de carteira.
- Garantir comportamento equivalente no H2 e PostgreSQL.

**Non-Goals:**

- Implementar dashboard, ativos, cotações, lançamentos, preço médio ou indicadores financeiros.
- Permitir alterar o nome ou a corretora depois da criação.
- Revalidar corretoras na Brasil API, ViaCEP ou CVM durante a criação da carteira.
- Implementar arquivamento ou retenção de histórico de carteiras nesta etapa.

## Decisions

### 1. Carteira privada com uma corretora obrigatória

O Domain representará uma carteira com UUID próprio, UUID do proprietário, nome exibido, chave normalizada do nome, UUID da corretora e timestamps. A Entity manterá referências obrigatórias ao usuário e à corretora persistidos. Uma corretora poderá atender várias carteiras do mesmo investidor; cada carteira terá somente uma corretora.

Alternativa considerada: criar uma entidade global de carteira ou permitir várias corretoras por carteira. Foi rejeitada porque quebraria o isolamento já aplicado a usuários e corretoras e anteciparia uma relação que o produto ainda não exige.

### 2. Corretora cadastrada como limite de validação

O Service verificará a existência da corretora por UUID e proprietário autenticado. Não fará chamadas às fontes externas, pois o fluxo de cadastro da corretora já é responsável pela validação do CNPJ, CEP e participação na CVM. Assim, a criação de carteira permanece rápida e não depende de disponibilidade de fornecedores externos.

Alternativa considerada: repetir a validação externa a cada criação. Foi rejeitada por duplicar regras, aumentar latência e impedir o uso de uma corretora já aprovada quando um fornecedor estiver indisponível.

### 3. Unicidade composta e ordenação determinística

O nome será armazenado após remoção de espaços nas extremidades e acompanhado de uma chave em minúsculas. A persistência aplicará restrição única para proprietário + chave do nome, e o Service repetirá a verificação antes da escrita para retornar o conflito público adequado. A listagem ordenará por data de criação crescente e UUID crescente como desempate determinístico.

Alternativa considerada: exigir nome globalmente único. Foi rejeitada porque carteiras são privadas e nomes como "Principal" podem existir para investidores diferentes.

### 4. Consulta, exclusão e ocultação de recursos alheios

As consultas e a exclusão buscarão a carteira pelo UUID e proprietário no mesmo acesso ao repositório. Ausência ou propriedade de outra pessoa produzirão a mesma resposta `404`, evitando vazamento de existência. A exclusão será física nesta mudança, pois ainda não existem lançamentos que precisem ser preservados.

Alternativa considerada: soft delete imediato. Foi rejeitada por introduzir estado, filtros e restauração antes de haver exigência de retenção de dados.

### 5. Contratos públicos enxutos

Os DTOs de entrada aceitarão somente nome e UUID da corretora. As respostas trarão um resumo imutável da corretora já vinculada, evitando que a interface necessite buscar novamente seus dados para exibir a carteira. Entities e identificadores do proprietário não serão expostos.

## Risks / Trade-offs

- [Uma corretora pode tornar-se irregular depois do cadastro] → A mudança mantém o cadastro como ponto de validação; uma política periódica de revalidação poderá ser adicionada separadamente.
- [A exclusão física pode conflitar com o futuro histórico de lançamentos] → A futura capacidade de lançamentos deverá revisar esta regra e bloquear exclusão ou introduzir arquivamento antes de persistir históricos.
- [Duas requisições simultâneas podem usar o mesmo nome] → A restrição única no banco será a garantia final e a violação será traduzida para conflito de nome.

## Migration Plan

1. Adicionar a Entity e as restrições de carteira ao esquema gerenciado somente nos perfis locais de desenvolvimento e teste.
2. Validar criação, consulta, listagem e exclusão no H2 e no PostgreSQL local.
3. Em rollback local, remover somente os registros ou a tabela de carteiras dos bancos afetados; usuários e corretoras permanecem intactos.
4. Antes de produção, incluir a tabela e as restrições de carteiras na futura mudança de Flyway e manter validação do esquema no perfil produtivo.
