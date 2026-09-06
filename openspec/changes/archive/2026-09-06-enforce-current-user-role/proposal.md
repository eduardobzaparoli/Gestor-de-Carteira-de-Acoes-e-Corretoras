## Why

O papel usado para autorizar uma requisição hoje é extraído do JWT emitido no login. Se uma conta é promovida ou rebaixada, tokens ainda válidos mantêm as permissões antigas até expirarem, o que pode prolongar acesso administrativo indevido.

## What Changes

- Usar o papel persistido atualmente para compor as permissões de cada requisição autenticada.
- Manter o JWT como identidade assinada e preservar o tratamento já existente para conta inexistente ou inativa.
- Fazer com que promoções e rebaixamentos alterem imediatamente o acesso às rotas administrativas, sem exigir novo login.
- Cobrir os dois sentidos da alteração de papel com testes de integração usando o mesmo token emitido antes da atualização.

## Capabilities

### New Capabilities

- Nenhuma.

### Modified Capabilities

- `user-authentication`: permissões de um token válido passam a refletir o papel atual da conta.
- `admin-user-management`: alterações administrativas de papel passam a ter efeito imediato sobre tokens já emitidos.

## Impact

- Afeta o filtro de segurança, a composição das autoridades Spring Security e os testes de autenticação/administração.
- Não altera endpoints, estrutura dos tokens, expiração configurada, dados persistidos ou dependências.
