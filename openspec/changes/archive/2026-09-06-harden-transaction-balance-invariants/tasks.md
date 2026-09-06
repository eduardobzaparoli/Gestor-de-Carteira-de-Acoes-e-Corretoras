## 1. Projeção e acesso concorrente ao saldo

- [x] 1.1 Criar o acesso bloqueante à carteira para operações de lançamentos na mesma transação.
- [x] 1.2 Extrair uma projeção única de saldo por ticker e mercado que trate lançamentos efetivos, reservas de vendas pendentes e ordenação cronológica.
- [x] 1.3 Aplicar a projeção à criação de vendas presentes, retroativas e futuras, preservando o conflito `INSUFFICIENT_ASSET_QUANTITY`.

## 2. Efetivação segura de pendências

- [x] 2.1 Processar lançamentos pendentes vencidos em ordem cronológica sob o bloqueio da carteira.
- [x] 2.2 Validar cada venda pendente antes de efetivá-la e manter o estado inalterado quando a efetivação violar o saldo.
- [x] 2.3 Garantir que consulta, criação e cancelamento usem o mesmo caminho transacional de reconciliação.

## 3. Verificação

- [x] 3.1 Ampliar os testes de integração para vendas efetivas que disputam reservas futuras e para a efetivação de pendência inválida.
- [x] 3.2 Adicionar teste de concorrência que prove que duas vendas não consomem o mesmo saldo.
- [x] 3.3 Executar os testes Maven aplicáveis, incluindo o perfil PostgreSQL quando o ambiente estiver configurado.
