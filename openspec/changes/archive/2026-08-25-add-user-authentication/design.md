## Context

O projeto contém apenas o esqueleto Spring Boot, dependências básicas de Web MVC, validação, JPA, H2 e PostgreSQL, sem modelo de usuário ou segurança. A capacidade deve atender à especificação em `specs/user-authentication/spec.md`, funcionar sem Docker e adotar a arquitetura aprovada no `product-spec.md`.

As orientações em `AGENTS.md` e `openspec/config.yaml` ainda omitem `Domain` e `Mapper`; elas precisam ser alinhadas para que futuras mudanças não retomem a estrutura anterior.

## Goals / Non-Goals

**Goals:**

- Tornar visíveis e separadas as responsabilidades de Domain, Mapper, Controller, Service, Repository, Entity e DTO.
- Manter regras de cadastro e autenticação no Service, sem expor Entity na API.
- Usar os mecanismos nativos do Spring Security para autenticação, autorização e validação do JWT.
- Manter configurações sensíveis fora do repositório e validar H2 e PostgreSQL local sem exigir Docker.
- Criar testes que expressem os cenários do contrato OpenSpec.

**Non-Goals:**

- Implementar interface gráfica de login/cadastro.
- Implementar recuperação de senha, verificação de e-mail, refresh token, logout com revogação ou autenticação social.
- Criar endpoints administrativos de gestão de usuários nesta mudança.
- Cadastrar administradores pela rota pública ou criar automaticamente uma conta administrativa.
- Implementar carteiras, corretoras ou isolamento dos dados dessas capacidades.
- Introduzir Flyway ou outro mecanismo de migração versionada nesta primeira mudança.
- Preparar esta versão para implantação em produção antes da adoção de migrações versionadas.

## Decisions

### 1. Separação explícita das camadas

A implementação usará pacotes de camada sob `com.bominvestidor.spring`, com subpacotes por contexto quando necessário:

```text
controller/     endpoints e tradução HTTP
dto/            contratos de entrada, saída e erro
service/        casos de uso e regras de negócio
domain/         modelo de negócio puro, sem anotações JPA
entity/         representação persistente JPA
mapper/         conversões DTO ↔ Domain e Domain ↔ Entity
repository/     acesso aos dados persistidos
security/       configuração, emissão e validação de JWT
exception/      exceções de negócio e tratamento centralizado
```

O fluxo principal será:

```text
HTTP → Controller → DTO → Mapper → Domain → Service
                                      ↕
                              Mapper ↔ Entity ↔ Repository
                                      ↓
                                DTO de resposta
```

O Controller não acessará Repository ou Entity. DTOs não serão usados como modelo persistente. O Domain não terá dependência de JPA, HTTP ou Spring Security. O Service coordenará normalização, unicidade, hash de senha e emissão de token.

Alternativa considerada: usar somente uma classe JPA `User` em todas as camadas. Foi rejeitada porque mistura contrato HTTP, regra de negócio e persistência, exatamente o acoplamento que a arquitetura solicitada pretende evitar.

### 2. Modelo de usuário e mapeamento

O Domain de usuário terá identificador UUID, nome, e-mail normalizado, hash da senha, papel e datas de criação/atualização. A Entity espelhará os dados persistidos na tabela `users`, com papel armazenado como texto e restrição única para o e-mail normalizado.

DTOs serão records imutáveis e específicos por operação: cadastro, login, resposta de autenticação, token e usuário público. O cliente não fornecerá papel no DTO de cadastro. O Mapper centralizará as conversões e nunca mapeará o hash para respostas.

Alternativa considerada: expor a Entity diretamente nos endpoints. Foi rejeitada para evitar vazamento de senha, acoplamento do contrato da API ao banco e alterações acidentais de campos controlados pelo servidor.

### 3. Spring Security e JWT com componentes oficiais

Serão adicionados `spring-boot-starter-security`, suporte OAuth2 Resource Server/Jose para JWT e `spring-security-test`. A aplicação usará o filtro Bearer nativo do Spring Security, `PasswordEncoder` com BCrypt e codificador/decodificador JWT baseado em Nimbus, evitando um filtro JWT artesanal e uma biblioteca paralela.

O JWT usará assinatura HMAC SHA-256, `sub` com o UUID do usuário, claim de papel, instante de emissão e expiração. O segredo terá no mínimo 256 bits e virá de `JWT_SECRET`; a duração virá de configuração. Produção não terá segredo padrão versionado, e testes fornecerão sua própria chave.

Alternativa considerada: implementar manualmente parsing e filtro de token. Foi rejeitada por ampliar a superfície de segurança e duplicar comportamento já mantido pelo Spring Security.

### 4. Autenticação e política de autorização

O cadastro normalizará nome e e-mail antes das regras de tamanho e unicidade. O e-mail será persistido em minúsculas. A senha será validada antes de BCrypt. O papel público será sempre `INVESTOR`.

O login produzirá a mesma resposta para e-mail inexistente e senha incorreta, reduzindo enumeração de contas. A cadeia de segurança será stateless, com CSRF desabilitado para a API Bearer, cadastro/login públicos e demais rotas autenticadas por padrão. Falhas de autenticação e autorização usarão handlers JSON.

### 5. Gerenciamento temporário do esquema e perfis sem Docker

O Flyway será deliberadamente adiado. Nesta primeira mudança, o Hibernate gerenciará o esquema a partir das Entities: testes H2 usarão criação e descarte do esquema, enquanto os perfis locais de desenvolvimento poderão usar atualização automática. A mesma estratégia poderá inicializar o PostgreSQL local usado para verificação.

Configurações serão separadas em propriedades comuns e perfis locais para H2 e PostgreSQL, sempre com credenciais e segredo JWT fornecidos fora do repositório. Nenhum perfil destinado a uma implantação real deverá usar atualização automática do esquema.

Antes da primeira implantação em produção, uma mudança OpenSpec específica deverá introduzir Flyway, criar uma migração de referência compatível com o esquema existente e trocar o Hibernate para somente validar o banco.

Alternativa considerada: introduzir Flyway agora. Foi adiada para reduzir o escopo inicial e concentrar a primeira entrega em cadastro, autenticação e arquitetura em camadas. O custo aceito é que esta versão fica limitada a desenvolvimento e testes até a adoção das migrações.

### 6. Concorrência e unicidade

O Service verificará previamente o e-mail para produzir uma resposta clara, mas a restrição única do banco será a garantia definitiva contra cadastros concorrentes. Violações dessa restrição serão traduzidas para `EMAIL_ALREADY_REGISTERED`, sem classificar toda falha de integridade como duplicidade de e-mail.

### 7. Contrato uniforme de erros

Um `@ControllerAdvice` produzirá o DTO de erro comum. Erros originados antes do Controller, como token ausente/inválido e acesso negado, usarão entry point e access denied handler compatíveis com o mesmo formato. Mensagens internas, hashes, stack traces e detalhes criptográficos não serão retornados.

### 8. Estratégia de testes

- Testes unitários do Service validarão normalização, papel padrão, duplicidade, hash e credenciais inválidas.
- Testes do Mapper garantirão separação entre Domain, Entity e DTO e ausência do hash nas respostas.
- Testes MVC/integração cobrirão status, JSON, JWT, `GET /api/auth/me`, `401`, `403` e validação de campos.
- Testes de persistência validarão a criação automática do esquema e a restrição única no H2.
- Uma execução de verificação com perfil PostgreSQL local validará a inicialização do esquema, cadastro e login sem Docker.

## Risks / Trade-offs

- [Duplicação controlada entre Domain e Entity] → Manter o Mapper como único ponto de conversão e cobri-lo com testes.
- [Segredo JWT fraco ou ausente] → Validar a configuração no início da aplicação e exigir ao menos 256 bits.
- [Diferenças no DDL gerado para H2 e PostgreSQL] → Usar mapeamentos JPA portáveis, UUID gerado pela aplicação e executar verificação explícita nos dois bancos.
- [Atualização automática não oferece histórico ou implantação previsível] → Restringir `ddl-auto` a ambientes locais e exigir uma mudança Flyway antes da primeira implantação em produção.
- [Condição de corrida no cadastro] → Combinar consulta prévia com restrição única no banco e tradução específica da violação.
- [Tokens continuam válidos até expirar] → Usar expiração curta e aceitar essa característica nesta primeira versão; revogação e refresh token permanecem fora do escopo.

## Migration Plan

1. Adicionar dependências e configurações comuns de segurança e persistência.
2. Configurar o Hibernate para inicializar o esquema em H2 e no PostgreSQL local de teste.
3. Implementar as camadas de usuário, autenticação, segurança e erros.
4. Executar testes automatizados no perfil de teste H2.
5. Executar verificação da inicialização e do fluxo principal no PostgreSQL local.
6. Antes da primeira implantação em produção, criar uma nova mudança OpenSpec para introduzir Flyway e converter o esquema inicial em migração versionada.
7. Em caso de rollback nesta fase local, descartar somente os bancos de desenvolvimento/teste afetados e recriá-los a partir do mapeamento JPA.
