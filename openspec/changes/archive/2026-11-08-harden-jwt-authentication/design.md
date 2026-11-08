## Context

O backend já emite JWT HS256 com `sub`, papel, emissão e expiração, valida o segredo mínimo, usa Resource Server stateless e consulta a conta em cada requisição para aplicar imediatamente estado e papel atuais. Veja `proposal.md` para a motivação. A mudança atravessa segurança, configuração, Docker, testes e documentação, mas não altera endpoints nem o modelo persistente.

## Goals / Non-Goals

**Goals:**

- Vincular tokens emitidos e aceitos a uma identidade explícita da aplicação.
- Falhar cedo quando o emissor não estiver configurado.
- Manter a validação de tempo já fornecida pelo Spring Security ao adicionar a validação do emissor.
- Tornar adulteração, emissor incorreto e códigos `401`/`403` verificáveis e documentados.

**Non-Goals:**

- Substituir HS256, adicionar refresh token, revogação, cookies ou autenticação federada.
- Reescrever o login com `AuthenticationManager` ou `UserDetailsService`, pois o fluxo atual já consulta o banco e compara BCrypt com segurança.
- Trocar Flyway por Liquibase ou alterar regras de autorização específicas do produto.
- Alterar o armazenamento da sessão no frontend ou o contrato de resposta do login.

## Decisions

### 1. Estender a configuração JWT existente

`JWT_ISSUER` será mapeado pela propriedade tipada já existente e deverá ser não vazio. Essa abordagem reutiliza a validação de inicialização atual e evita uma nova classe de configuração.

Alternativa considerada: usar um emissor fixo no código. Foi rejeitada porque dificultaria separar ambientes e contrariaria a externalização indicada para a aplicação.

### 2. Emitir e validar o claim padrão `iss`

O serviço atual de tokens incluirá o emissor configurado nos claims. O decoder Nimbus continuará restrito a HS256 e receberá o validador padrão do Spring Security com issuer, preservando a validação temporal e acrescentando a conferência de `iss`.

Alternativa considerada: validar `iss` em filtro próprio. Foi rejeitada por duplicar uma responsabilidade já suportada pelo Resource Server e aumentar o risco de inconsistência.

### 3. Preservar a autorização baseada no estado atual do banco

O filtro de conta ativa continuará substituindo as autoridades do token pelo papel persistido. A validação de emissor ocorrerá antes desse filtro, portanto tokens externos não provocarão consulta de identidade nem alcançarão regras de negócio.

### 4. Cobrir a segurança com testes focados

Os testes de integração existentes serão ampliados com um token válido quanto a formato e tempo, mas emitido por outro issuer, e com um token cuja assinatura tenha sido alterada. Configurações de teste e PostgreSQL receberão issuer próprio e previsível. Não será criada uma suíte paralela.

### 5. Usar documentação equivalente à coleção Postman

O guia existente receberá uma seção curta de testes manuais com login, Bearer token, token ausente, adulterado, expirado e acesso sem papel. A documentação também registrará que o payload contém apenas identificador, papel e claims técnicos, nunca senha, hash ou dados financeiros.

## Risks / Trade-offs

- [Ambientes existentes deixam de iniciar sem `JWT_ISSUER`] → Atualizar `.env.example`, Docker Compose e documentação antes da implantação e destacar a variável como obrigatória.
- [Tokens emitidos antes da atualização não possuem `iss`] → Exigir novo login após a implantação; os tokens antigos serão rejeitados intencionalmente.
- [Mesmo segredo compartilhado entre ambientes] → Manter segredos distintos por ambiente; a validação de issuer é defesa adicional, não substitui a separação de chaves.
- [Teste de adulteração pode ficar acoplado ao formato textual do JWT] → Alterar somente um caractere da assinatura e verificar o comportamento público `401`, sem depender da mensagem interna do decoder.

## Migration Plan

1. Definir `JWT_ISSUER` em todos os ambientes, incluindo testes e Docker, antes de subir a nova versão.
2. Implantar o backend atualizado; clientes com tokens antigos deverão autenticar novamente.
3. Validar login, `/api/auth/me`, `401` e `403` pelos testes automatizados e pelo roteiro manual.
4. Em rollback, restaurar a versão anterior do backend; nenhuma reversão de banco é necessária.
