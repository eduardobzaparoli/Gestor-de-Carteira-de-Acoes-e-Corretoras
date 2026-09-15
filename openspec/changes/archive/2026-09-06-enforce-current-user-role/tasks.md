## 1. Autoridades da requisição autenticada

- [x] 1.1 Ajustar o processamento de conta ativa para compor as autoridades da requisição a partir do papel persistido atual, preservando o JWT como identidade autenticada.
- [x] 1.2 Preservar os tratamentos `401` existentes para token inválido, conta inexistente e conta inativa, bem como o claim `role` na emissão de tokens.

## 2. Cobertura de integração

- [x] 2.1 Adicionar um teste no qual um administrador é rebaixado e o token emitido antes da alteração recebe `403` ao acessar rota administrativa.
- [x] 2.2 Adicionar um teste no qual um investidor é promovido e o token emitido antes da alteração consegue acessar rota administrativa.
- [x] 2.3 Manter e executar os cenários já cobertos de autenticação, conta inativa e proteção do último administrador.

## 3. Verificação

- [x] 3.1 Executar a suíte Maven completa.
- [x] 3.2 Validar esta change em modo estrito com OpenSpec.
