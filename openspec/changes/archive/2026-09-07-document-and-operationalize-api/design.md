## Context

Veja `proposal.md` para a motivação e `specs/api-operations/spec.md` para o contrato. A segurança atual centraliza rotas no Spring Security e a configuração já é separada por perfis. Não há contrato OpenAPI, Actuator, política CORS explícita ou guia operacional completo.

## Goals / Non-Goals

**Goals:**

- Gerar documentação HTTP a partir da aplicação e validá-la por testes.
- Oferecer uma sonda de saúde mínima adequada a implantação.
- Centralizar CORS em propriedades com padrão fechado.
- Documentar uma operação reproduzível sem armazenar segredos.

**Non-Goals:**

- Criar dashboards de métricas, tracing distribuído ou integração com plataforma de observabilidade.
- Alterar regras de negócio, formatos existentes ou autorização dos endpoints funcionais.
- Definir a origem definitiva de um frontend ainda inexistente.

## Decisions

### 1. Springdoc para OpenAPI

Adicionar o starter WebMVC UI do Springdoc e uma configuração de metadados e segurança Bearer. Controllers e DTOs receberão apenas as anotações necessárias para completar respostas e erros que a inferência não descreve adequadamente.

Alternativa considerada: manter um YAML manual. Ele tende a divergir dos controllers e exigiria duplicação ampla dos modelos.

### 2. Actuator limitado ao health agregado

Adicionar o Actuator, expor somente `health` pela web e configurar detalhes como `never`. A segurança liberará `/actuator/health`, o documento OpenAPI e a interface de documentação; nenhum outro endpoint de gerenciamento será público.

Alternativa considerada: criar controller próprio. O Actuator possui semântica de sonda e integração com o ciclo de vida mais padronizadas.

### 3. CORS tipado e fechado por padrão

Uma propriedade de lista receberá origens exatas. A configuração CORS só será ativada quando a lista não estiver vazia, permitindo `GET`, `POST`, `PUT`, `DELETE` e `OPTIONS`, além de `Authorization` e `Content-Type`, sem credenciais de cookie.

Alternativa considerada: liberar `*`. Isso amplia desnecessariamente a superfície para navegadores e dificulta uma futura política segura.

### 4. Observabilidade mínima com recursos nativos

Usar configuração de níveis de log por perfil e sanitização já existente no tratamento de erros, evitando nova plataforma ou biblioteca. A documentação definirá o que é seguro registrar e como aumentar temporariamente o nível em diagnóstico.

### 5. Documentação operacional separada por finalidade

O README oferecerá início rápido e apontará para guias em `docs/`: configuração, referência de API/OpenAPI e operação (migração, implantação, rollback, recuperação e smoke tests). Exemplos usarão valores fictícios e nomes de variáveis, nunca segredos reais.

## Risks / Trade-offs

- [Contrato gerado omitir erros específicos] → Cobrir os endpoints e componentes OpenAPI essenciais com teste e metadados explícitos.
- [Health público revelar infraestrutura] → Expor somente estado agregado e testar ausência de detalhes.
- [CORS configurado incorretamente em produção] → Padrão vazio e documentação com lista explícita de origens completas.
- [Documentação envelhecer] → Vincular os comandos aos gates existentes e testar a disponibilidade do contrato e do health.

## Migration Plan

1. Adicionar dependências e configurações com padrões compatíveis com os perfis atuais.
2. Publicar OpenAPI, health e CORS, preservando a segurança das rotas funcionais.
3. Executar gates H2 e PostgreSQL e validar os novos endpoints.
4. Implantar sem origens CORS; configurar a lista somente quando houver frontend conhecido.
5. Em rollback, remover as dependências e configurações operacionais sem migração de banco ou alteração de dados.
