## 1. Coordenação transacional de administradores

- [x] 1.1 Adicionar ao repositório uma consulta com bloqueio pessimista e ordem estável para as contas `ADMIN` ativas.
- [x] 1.2 Ajustar rebaixamento e desativação para adquirir o bloqueio antes de reler a conta-alvo e decidir se a continuidade administrativa é preservada.
- [x] 1.3 Manter os contratos atuais de sucesso e o conflito `409 LAST_ACTIVE_ADMIN` em operações não concorrentes.

## 2. Testes concorrentes

- [x] 2.1 Criar teste de integração que coordene dois rebaixamentos concorrentes e comprove que ao menos um administrador permanece ativo.
- [x] 2.2 Criar teste de integração que coordene duas desativações concorrentes e comprove que ao menos um administrador permanece ativo.
- [x] 2.3 Manter os testes existentes de tentativa de auto-desativação e de último administrador.

## 3. Verificação

- [x] 3.1 Executar a suíte Maven completa.
- [x] 3.2 Validar a change em modo estrito com OpenSpec.
