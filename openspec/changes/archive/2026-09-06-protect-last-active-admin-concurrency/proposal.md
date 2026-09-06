## Why

A proteção atual contra remoção do último administrador ativo verifica uma contagem sem coordenar transações concorrentes. Duas atualizações ou desativações simultâneas podem aprovar a remoção de administradores diferentes e deixar o sistema sem nenhuma conta administrativa ativa.

## What Changes

- Tornar atômica a decisão de rebaixar ou desativar uma conta `ADMIN` ativa.
- Garantir que operações concorrentes preservem ao menos um administrador ativo ao final.
- Manter o contrato atual de conflito `409 LAST_ACTIVE_ADMIN` quando uma operação não puder remover o último administrador.
- Adicionar testes de integração concorrentes para rebaixamento e desativação.

## Capabilities

### New Capabilities

- Nenhuma.

### Modified Capabilities

- `admin-user-management`: a continuidade de ao menos um administrador ativo passa a ser garantida também sob operações concorrentes.

## Impact

- Afeta o serviço administrativo, o repositório de usuários e testes de integração.
- Não altera endpoints, estruturas JSON, modelo persistido, migrações ou dependências.
