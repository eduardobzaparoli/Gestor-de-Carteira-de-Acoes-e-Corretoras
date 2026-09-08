## Why

O produto já possui uma API funcional, mas ainda não oferece a interface web prevista para que investidores e administradores executem seus fluxos de forma visual. Esta mudança entrega uma aplicação React completa, responsiva e orientada a BI sobre o contrato atual do backend.

## What Changes

- Adicionar uma aplicação web React com TypeScript, navegação por rotas, layout responsivo e identidade visual consistente.
- Implementar login, cadastro, persistência segura da sessão no navegador, encerramento de sessão e proteção de rotas por autenticação e papel atual do usuário.
- Implementar a área do investidor com listagem e criação de carteiras, cadastro e consulta de corretoras e tratamento dos estados vazios necessários ao primeiro uso.
- Implementar o dashboard da carteira com indicadores consolidados e por moeda, posições, composição patrimonial, evolução histórica, câmbio utilizado e atualização dos dados sob demanda.
- Implementar pesquisa de ativos e cadastro de compras e vendas, além do histórico de lançamentos e cancelamento de lançamentos elegíveis.
- Permitir editar lançamentos pendentes e excluir corretoras que ainda não estejam vinculadas a carteiras, preservando a integridade do histórico financeiro.
- Implementar a gestão de proventos com resumo, candidatos fornecidos pelas integrações, confirmação, cadastro manual, histórico e cancelamento.
- Implementar a área administrativa para consultar, criar, editar, desativar e reativar usuários sem expor dados financeiros dos investidores.
- Padronizar carregamento, confirmação, sucesso, erro, ausência de dados e indisponibilidade das integrações externas, usando os códigos públicos retornados pela API.
- Padronizar validações de formulário em português, oferecer entrada de datas brasileiras com calendário e conclusão do ano, restringir proventos manuais aos ativos da carteira e apresentar valores financeiros em reais, convertendo moedas estrangeiras somente na borda da interface.
- Identificar visualmente os ativos com logotipos obtidos de fontes públicas por mercado e manter um fallback local quando a imagem não estiver disponível.
- Adicionar testes automatizados do frontend para os fluxos críticos e documentação para instalação, configuração e execução local.

## Capabilities

### New Capabilities

- `react-web-interface`: Interface web completa para os fluxos de investidores e administradores, incluindo autenticação, gestão de corretoras e carteiras, dashboard BI, lançamentos, proventos e estados operacionais.

### Modified Capabilities

- A gestão de corretoras passa a oferecer consulta cadastral prévia e exclusão segura.
- O ciclo de lançamentos passa a permitir a correção de dados enquanto o lançamento estiver pendente.

## Impact

- Novo diretório de frontend no repositório, com manifesto, configuração de build, código React/TypeScript, estilos, testes e documentação próprios.
- Consumo dos endpoints REST existentes sob `/api`, com URL-base configurável por variável de ambiente e integração local compatível com a política CORS já disponível.
- Novas dependências restritas ao frontend para roteamento, cache de requisições, formulários, validação, gráficos, ícones e testes.
- O backend recebe operações REST pontuais para consulta cadastral, exclusão segura de corretora, edição de lançamento pendente e consulta cambial autenticada, sem alteração de esquema.
- A interface passa a carregar imagens públicas da brapi para ativos brasileiros e da Parqet para ativos americanos, incluindo a atribuição exigida pelo provedor e sem transmitir credenciais da aplicação.
