## 1. Validação cronológica de vendas efetivas

- [x] 1.1 Restringir a reserva por disponibilidade agregada às vendas `PENDING` futuras.
- [x] 1.2 Validar vendas `EFFECTIVE` pela sequência cronológica de transações efetivas, incluindo a compra e a venda na mesma data.
- [x] 1.3 Preservar o erro `INSUFFICIENT_ASSET_QUANTITY` quando uma venda retroativa produzir saldo negativo.

## 2. Estabilização dos testes temporais

- [x] 2.1 Atualizar o cenário de lançamentos retroativos para representar uma sequência cronológica determinística.
- [x] 2.2 Substituir datas absolutas dos testes de reserva e efetivação futura por datas relativas ao relógio controlado.

## 3. Verificação

- [x] 3.1 Executar os testes de integração de lançamentos e posições.
- [x] 3.2 Executar a suíte Maven completa e confirmar que não há falhas.
