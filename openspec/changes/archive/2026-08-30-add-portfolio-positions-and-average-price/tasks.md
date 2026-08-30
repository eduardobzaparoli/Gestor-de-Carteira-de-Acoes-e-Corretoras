## 1. Domínio e acesso aos lançamentos

- [x] 1.1 Criar o modelo de domínio de posição com ticker, retrato do ativo, quantidade, custo em custódia e preço médio.
- [x] 1.2 Criar o identificador de agrupamento por ticker normalizado e mercado.
- [x] 1.3 Adicionar consulta de lançamentos por carteira em ordem ascendente de data da transação e data de criação.
- [x] 1.4 Extrair ou disponibilizar a reconciliação de pendências para reutilização segura pelo serviço de posições.

## 2. Cálculo de posição e consistência cronológica

- [x] 2.1 Implementar calculador puro que processe somente lançamentos efetivados e consolide posições por ticker e mercado.
- [x] 2.2 Implementar compras com custo `quantidade × preço + custos` e média ponderada de alta precisão.
- [x] 2.3 Implementar vendas com redução proporcional do custo, preservação do preço médio e zeragem na liquidação total.
- [x] 2.4 Implementar reabertura de posição com novo ciclo de preço médio após liquidação.
- [x] 2.5 Implementar simulação cronológica de venda efetiva candidata e rejeitar qualquer saldo intermediário negativo.
- [x] 2.6 Preservar a reserva e a validação existentes para vendas futuras pendentes.

## 3. Serviço e API privada

- [x] 3.1 Criar serviço de posições que valide propriedade, reconcilie pendências e projete os lançamentos da carteira.
- [x] 3.2 Criar DTO e mapper públicos com arredondamento do preço médio em até oito casas usando `HALF_UP`.
- [x] 3.3 Expor `GET /api/portfolios/{portfolioId}/positions` para investidores autenticados.
- [x] 3.4 Ordenar a resposta por mercado e ticker e omitir posições com quantidade zero.
- [x] 3.5 Confirmar respostas `401`, `403` e `404` e ausência de dados internos na representação pública.

## 4. Testes automatizados

- [x] 4.1 Cobrir primeira compra, múltiplas compras com custos e média ponderada recorrente.
- [x] 4.2 Cobrir venda parcial por preços diferentes sem alteração do preço médio.
- [x] 4.3 Cobrir liquidação total, omissão da posição e reabertura com média reiniciada.
- [x] 4.4 Cobrir agrupamento por ticker e mercado, metadados mais recentes e ordenação da resposta.
- [x] 4.5 Cobrir exclusão de lançamentos pendentes e cancelados e reconciliação de pendência vencida.
- [x] 4.6 Cobrir rejeição atômica de venda retroativa que produziria saldo negativo e aceitação de sequência válida.
- [x] 4.7 Cobrir integração web da rota e isolamento entre investidores.
- [x] 4.8 Estender a verificação opt-in PostgreSQL para confirmar os mesmos resultados sem nova tabela de posições.

## 5. Verificação final

- [x] 5.1 Executar a suíte Maven completa no perfil H2 e corrigir falhas relacionadas à mudança.
- [x] 5.2 Executar a verificação opt-in PostgreSQL com banco local configurado.
- [x] 5.3 Validar manualmente o contrato REST com compras, venda parcial, liquidação e reabertura de posição.
