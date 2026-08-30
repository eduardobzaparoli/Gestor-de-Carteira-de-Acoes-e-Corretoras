## 1. Modelo de domínio e persistência

- [x] 1.1 Criar os tipos de domínio para lançamento, operação de compra/venda e status do ciclo de vida.
- [x] 1.2 Implementar os estados e transições permitidas para pendência, efetivação e cancelamento.
- [x] 1.3 Criar entidade, relacionamento obrigatório com carteira e mapeamento persistente dos dados do ativo, valores monetários, quantidade, data e status.
- [x] 1.4 Criar repositório de lançamentos com consultas para histórico ordenado, reconciliação de pendências, posição por ativo e verificação de existência por carteira.
- [x] 1.5 Criar mapper entre domínio, entidade e respostas públicas, sem expor dados internos de proprietário.

## 2. Serviços e regras de negócio

- [x] 2.1 Implementar serviço de lançamentos que obtenha a carteira pelo investidor autenticado e bloqueie acessos administrativos.
- [x] 2.2 Implementar validação e normalização do comando de criação, incluindo retrato do ativo, quantidade, preço e custos.
- [x] 2.3 Implementar a reconciliação transacional de lançamentos futuros baseada em relógio injetável antes de cada consulta ou operação.
- [x] 2.4 Implementar cálculo transacional de posição efetiva e reserva de vendas pendentes, rejeitando venda acima da quantidade disponível.
- [x] 2.5 Implementar cancelamento lógico somente de lançamentos pendentes e preservação imutável dos demais registros.
- [x] 2.6 Alterar o serviço de carteira para impedir exclusão quando houver qualquer lançamento associado.

## 3. API e tratamento de erros

- [x] 3.1 Criar DTOs de criação e resposta de lançamento com validações de entrada e serialização JSON.
- [x] 3.2 Expor as rotas autenticadas de criação, histórico e cancelamento no contexto da carteira.
- [x] 3.3 Adicionar erros públicos padronizados para lançamento inexistente, cancelamento inválido, saldo insuficiente e carteira com histórico.
- [x] 3.4 Confirmar que rotas sem token, por administradores ou para carteira de terceiro mantêm as respostas `401`, `403` e `404` esperadas.

## 4. Testes automatizados

- [x] 4.1 Cobrir unitariamente transições de estado, validações de valores, registro de compra e imutabilidade.
- [x] 4.2 Cobrir posição, venda acima do saldo, reservas de múltiplas vendas futuras e liberação de reserva por cancelamento.
- [x] 4.3 Cobrir reconciliação de pendências com relógio controlado e histórico em ordem decrescente.
- [x] 4.4 Cobrir integração web das rotas, propriedade da carteira, respostas de erro e bloqueio de exclusão com histórico.
- [x] 4.5 Estender a verificação opt-in do PostgreSQL para confirmar o ciclo de lançamentos e a proteção de exclusão sem depender de provedores externos.

## 5. Verificação final

- [x] 5.1 Executar a suíte Maven no perfil H2 e corrigir falhas relacionadas à mudança.
- [x] 5.2 Executar a verificação opt-in do PostgreSQL com banco local configurado e registrar o resultado.
- [x] 5.3 Validar manualmente o contrato REST de compra, venda, lançamento futuro, cancelamento e consulta de histórico usando um token de investidor.
