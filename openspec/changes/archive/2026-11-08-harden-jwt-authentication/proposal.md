## Why

A autenticação JWT existente já protege a API, mas não identifica nem valida o emissor do token. A mudança completa o endurecimento esperado para tokens HS256 e registra cenários verificáveis de adulteração, expiração e emissor incorreto, preservando as regras atuais do produto.

## What Changes

- **BREAKING (configuração):** adicionar um emissor JWT obrigatório e externalizado por configuração; ambientes existentes precisarão definir `JWT_ISSUER` antes da atualização.
- Incluir o claim `iss` nos tokens emitidos e rejeitar tokens cujo emissor não corresponda ao configurado.
- Ampliar os testes de integração para cobrir assinatura adulterada e emissor incorreto, mantendo os cenários existentes de token ausente, inválido, expirado e papel insuficiente.
- Atualizar a configuração e a documentação dos ambientes locais, Docker e testes com `JWT_ISSUER`.
- Documentar os cenários manuais de autenticação, a diferença entre `401` e `403` e a decisão de não incluir dados sensíveis no JWT.
- Preservar o cadastro público, as autorizações específicas do produto, o Flyway e o fluxo atual de login por e-mail e BCrypt.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `user-authentication`: exigir emissor configurado na emissão e na validação dos tokens JWT e cobrir explicitamente tokens adulterados ou emitidos por outro emissor.
- `environment-configuration`: declarar e propagar `JWT_ISSUER` nos ambientes suportados sem versionar segredos.

## Impact

- Configuração de segurança JWT, emissão e decodificação de tokens.
- Propriedades da aplicação, Docker Compose, `.env.example` e documentação operacional.
- Testes de autenticação em H2 e, quando habilitado, PostgreSQL.
- Não há alteração de endpoints, formato de login, dependências, esquema do banco ou comportamento do frontend.
