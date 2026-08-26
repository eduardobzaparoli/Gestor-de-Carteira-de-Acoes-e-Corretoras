## 1. Alinhamento e configuração da mudança

- [x] 1.1 Atualizar `AGENTS.md` e `openspec/config.yaml` para incluir Domain e Mapper na arquitetura em camadas já aprovada no `product-spec.md`.
- [x] 1.2 Adicionar e justificar no `pom.xml` as dependências de Spring Security, OAuth2 Resource Server/Jose e testes de segurança.
- [x] 1.3 Criar propriedades comuns e perfis locais H2 e PostgreSQL, usando variáveis de ambiente para credenciais e `JWT_SECRET` e restringindo a criação automática do esquema a desenvolvimento/testes.
- [x] 1.4 Configurar testes para fornecer chave JWT própria e banco H2 isolado, sem depender de segredos ou serviços externos.

## 2. Persistência, Domain, Entity, Repository e Mapper

- [x] 2.1 Configurar temporariamente o Hibernate para criar/descartar o esquema nos testes e atualizá-lo somente nos perfis locais de desenvolvimento, sem habilitar essa estratégia para produção.
- [x] 2.2 Criar o modelo Domain de usuário e o enum de papéis sem anotações de JPA, HTTP ou segurança.
- [x] 2.3 Criar a Entity JPA de usuário separada do Domain, incluindo UUID, nome, e-mail normalizado único, hash da senha, papel, timestamps e restrições coerentes com o contrato.
- [x] 2.4 Criar o Repository com consultas de e-mail normalizado e identificador necessárias aos casos de uso.
- [x] 2.5 Criar o Mapper para Domain ↔ Entity e Domain ↔ DTO público, garantindo que o hash nunca seja exposto.
- [x] 2.6 Criar testes do Mapper cobrindo todas as conversões e a ausência de dados sensíveis nas respostas.

## 3. DTOs e validação de entrada

- [x] 3.1 Criar DTOs imutáveis para cadastro, login, usuário público e resposta de autenticação.
- [x] 3.2 Aplicar validações de formato e limites nos DTOs sem permitir que o cliente defina o papel do cadastro público.
- [x] 3.3 Criar DTOs padronizados de erro e erro de campo com `timestamp`, `status`, `code`, `message`, `path` e `fieldErrors`.

## 4. Regras de cadastro e autenticação no Service

- [x] 4.1 Implementar normalização reutilizável de nome e e-mail antes das validações finais e da persistência.
- [x] 4.2 Implementar cadastro com papel `INVESTOR`, verificação prévia de e-mail, BCrypt e transação.
- [x] 4.3 Traduzir especificamente a violação concorrente da restrição única de e-mail para `EMAIL_ALREADY_REGISTERED`.
- [x] 4.4 Implementar autenticação por e-mail e senha com resposta indistinguível para usuário inexistente e senha incorreta.
- [x] 4.5 Implementar consulta da identidade autenticada por UUID, rejeitando tokens cujo usuário não esteja mais disponível.
- [x] 4.6 Criar testes unitários do Service para normalização, limites pós-normalização, papel padrão, duplicidade, hash, login e usuário ausente.

## 5. Segurança stateless e JWT

- [x] 5.1 Configurar BCrypt, cadeia stateless do Spring Security, CSRF adequado à API Bearer e política pública/protegida dos endpoints.
- [x] 5.2 Implementar emissão de JWT HS256 com `sub` UUID, papel, emissão e expiração configurável, validando a força de `JWT_SECRET`.
- [x] 5.3 Configurar validação JWT pelo Resource Server e conversão do claim de papel para autoridades Spring Security.
- [x] 5.4 Implementar respostas JSON padronizadas para token ausente/inválido e acesso negado.
- [x] 5.5 Criar testes de segurança para token válido, ausente, adulterado, expirado e papel insuficiente.

## 6. Controller e tratamento de erros

- [x] 6.1 Criar `AuthController` com `POST /api/auth/register`, retornando `201` e somente DTO público.
- [x] 6.2 Implementar `POST /api/auth/login`, retornando token Bearer, expiração e usuário público.
- [x] 6.3 Implementar `GET /api/auth/me` protegido, retornando a identidade correspondente ao token.
- [x] 6.4 Criar exceções de negócio e `@ControllerAdvice` para validação, JSON malformado, conflito de e-mail e credenciais inválidas.
- [x] 6.5 Garantir que Controller e respostas HTTP não exponham Entity, Domain, hash, stack trace ou detalhes criptográficos.

## 7. Testes integrados com H2

- [x] 7.1 Testar cadastro válido, normalização, validações de campos e tentativa de fornecer papel administrativo.
- [x] 7.2 Testar duplicidade de e-mail sem distinção de caixa e a garantia única no banco.
- [x] 7.3 Testar login bem-sucedido e respostas idênticas para e-mail inexistente e senha incorreta.
- [x] 7.4 Testar `GET /api/auth/me`, rotas protegidas e formato JSON dos erros `400`, `401`, `403` e `409`.
- [x] 7.5 Executar a suíte Maven completa no perfil de teste H2 e corrigir falhas ou alertas relacionados à mudança.

## 8. Verificação com PostgreSQL local e revisão final

- [x] 8.1 Inicializar um banco PostgreSQL local vazio pelo mapeamento JPA temporário, sem Docker, usando somente variáveis de ambiente locais.
- [x] 8.2 Validar no PostgreSQL o fluxo de cadastro, unicidade de e-mail, login e consulta autenticada.
- [x] 8.3 Executar `openspec validate add-user-authentication --strict` e reconciliar qualquer divergência entre código, testes, spec, design e tarefas.
- [x] 8.4 Revisar `git diff` para confirmar a arquitetura por camadas, ausência de credenciais e preservação do template de Pull Request adicionado pelo usuário.
- [x] 8.5 Registrar como trabalho futuro obrigatório a adoção do Flyway antes da primeira implantação em produção.
