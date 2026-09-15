## Why

O sistema precisa identificar o investidor antes de permitir o acesso às futuras carteiras e operações, além de distinguir usuários comuns de administradores. Esta primeira capacidade estabelece uma base segura e reutilizável de cadastro e autenticação para as próximas funcionalidades.

## What Changes

- Adicionar cadastro de usuário com nome, e-mail e senha, atribuindo o papel de investidor por padrão.
- Garantir unicidade de e-mail sem diferenciar maiúsculas e minúsculas e armazenar senhas somente de forma criptografada.
- Adicionar login por e-mail e senha com emissão de token JWT para autenticação stateless.
- Disponibilizar a consulta dos dados públicos do usuário autenticado.
- Proteger os demais endpoints da API por padrão, mantendo públicas somente as rotas necessárias de autenticação e infraestrutura autorizada.
- Padronizar validações e erros de autenticação em respostas JSON.
- Persistir usuários de forma compatível com H2 em desenvolvimento/testes e PostgreSQL no ambiente de produção.
- Usar provisoriamente o gerenciamento de esquema do Hibernate nos ambientes locais, deixando a adoção de migrações versionadas para uma mudança posterior antes da primeira implantação em produção.
- Estruturar a implementação nas camadas Domain, Mapper, Controller, Service, Repository, Entity e DTO, com componentes de segurança e tratamento de erros isolados.
- Alinhar as orientações internas do projeto à arquitetura em camadas aprovada no `product-spec.md`.

## Capabilities

### New Capabilities

- `user-authentication`: Cadastro de investidores, autenticação por credenciais, emissão e validação de JWT e consulta da identidade autenticada.

### Modified Capabilities

- Nenhuma.

## Impact

- Novos endpoints REST de cadastro, login e consulta do usuário autenticado.
- Nova persistência de usuários e papéis, incluindo restrições de integridade no mapeamento JPA e no banco.
- Inclusão e configuração dos módulos Spring Security e suporte a JWT, sem adicionar Flyway nesta mudança.
- Novas camadas e pacotes para autenticação, domínio de usuário, persistência, mapeamento, DTOs e tratamento centralizado de erros.
- Novos perfis de configuração para H2 e PostgreSQL, sem credenciais versionadas.
- Testes unitários e de integração para regras de cadastro, autenticação, autorização e persistência.
