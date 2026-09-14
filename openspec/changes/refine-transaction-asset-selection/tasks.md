## 1. Lista de ativos do lançamento

- [x] 1.1 Remover da opção de ativo a cotação armazenada ou convertida, preservando ticker, nome, tipo e estado selecionado.
- [x] 1.2 Manter a consulta de cotação corrente somente após o clique e confirmar que ela ainda preenche e valida o preço unitário.

## 2. Identificação por logo

- [x] 2.1 Validar o uso do componente compartilhado de logo nas opções brasileiras e americanas e corrigir a resolução das fontes necessária para o carregamento.
- [x] 2.2 Preservar texto alternativo, tentativas alternativas, marcador local e ausência de imagem quebrada quando a fonte externa falhar.
- [x] 2.3 Conferir o alinhamento das opções nos temas claro e escuro e em largura reduzida após a retirada da coluna monetária.

## 3. Testes automatizados

- [x] 3.1 Atualizar os testes do diálogo para comprovar que a lista não apresenta a cotação e que renderiza a logo junto à identidade do ativo.
- [x] 3.2 Cobrir sucesso e falha do carregamento das logos BR e US sem depender de chamadas externas.
- [x] 3.3 Confirmar por teste que selecionar o ativo ainda solicita a cotação fresca e habilita o lançamento somente com preço válido.

## 4. Verificação

- [x] 4.1 Executar formatação, lint, testes e build do frontend.
- [x] 4.2 Atualizar ou consultar o grafo do Graphify para confirmar que o impacto permanece restrito à interface e registrar eventual limitação da ferramenta.
- [x] 4.3 Validar a change OpenSpec em modo estrito e registrar os resultados da implementação.
