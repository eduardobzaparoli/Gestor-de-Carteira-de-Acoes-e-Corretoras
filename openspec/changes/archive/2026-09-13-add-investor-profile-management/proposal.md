## Why

O investidor atualmente visualiza sua identidade no rodapé da navegação, mas não possui um fluxo privado para consultar e manter os próprios dados. A mudança transforma esse ponto da interface em acesso ao perfil e permite corrigir nome, e-mail e senha sem intervenção administrativa.

## What Changes

- Tornar o bloco com nome e e-mail do investidor no canto inferior esquerdo uma ação acessível que abre a tela de perfil.
- Disponibilizar uma operação autenticada para consultar e atualizar somente o perfil do próprio investidor.
- Permitir alterar nome e e-mail e, opcionalmente, definir uma nova senha mediante confirmação da senha atual.
- Aplicar ao perfil as regras já existentes de normalização, formato, tamanho e proteção criptográfica dos dados de autenticação.
- Impedir a adoção de um e-mail já pertencente a outra conta, inclusive sob atualizações concorrentes, preservando o e-mail atual do próprio investidor.
- Atualizar imediatamente a identidade apresentada na interface após uma edição bem-sucedida, mantendo os padrões visuais, responsivos, acessíveis e de tema atuais.
- Apresentar validações e falhas em português sem limpar os valores corrigíveis do formulário.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `user-authentication`: adiciona consulta e atualização autenticada do perfil do próprio investidor, incluindo troca segura de senha e unicidade de e-mail.
- `react-web-interface`: adiciona navegação pelo bloco do usuário e uma tela de visualização e edição do perfil integrada ao design atual.

## Impact

- API de autenticação/identidade, DTOs, serviço de usuário, persistência e tratamento centralizado de erros.
- Contexto de autenticação e cliente HTTP da aplicação React, navegação lateral, formulário de perfil e mensagens localizadas.
- Testes de serviço, integração e interface para autorização, validação, concorrência, atualização da identidade e responsividade.
- Não requer nova dependência nem alteração do papel do usuário, e não expõe senha ou hash em respostas.
