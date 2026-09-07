## Why

A API está funcional e testada, mas ainda depende de orientação manual para configuração, consumo e operação. É necessário tornar seus contratos e requisitos de execução verificáveis, além de oferecer um sinal de saúde seguro para implantação e monitoramento.

## What Changes

- Publicar um contrato OpenAPI navegável para os endpoints, autenticação Bearer, corpos, respostas e códigos públicos de erro.
- Disponibilizar um health check mínimo, sem autenticação e sem exposição de detalhes internos, mantendo os demais endpoints operacionais protegidos ou não expostos.
- Adicionar observabilidade mínima por logs estruturados e configuração de nível por ambiente, sem registrar tokens, senhas ou chaves.
- Definir uma política CORS desabilitada por padrão e habilitável por lista explícita de origens quando um frontend for conectado.
- Organizar a documentação de perfis, variáveis de ambiente, execução, migrações, implantação, recuperação e smoke tests.

## Capabilities

### New Capabilities

- `api-operations`: contrato OpenAPI, saúde operacional segura, política CORS configurável e requisitos mínimos de observabilidade.

### Modified Capabilities

Nenhuma. Os comportamentos funcionais das capacidades existentes permanecem inalterados.

## Impact

- Configuração do Spring Security, MVC e perfis de ambiente.
- Dependências de OpenAPI e Actuator no `pom.xml`.
- Anotações ou metadados dos controllers e DTOs públicos.
- Novos testes de contrato, segurança operacional e CORS.
- `README.md` e documentação operacional em `docs/`.
