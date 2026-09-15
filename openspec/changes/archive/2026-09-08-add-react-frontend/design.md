## Context

O repositório contém apenas a API Spring Boot. Os endpoints já cobrem autenticação, corretoras, carteiras, ativos, transações, posições, valorização, evolução, proventos e usuários administrativos; o levantamento foi confirmado pelas especificações consolidadas, pelos controllers e pelo mapa `graphify-out`. A interface deve consumir esses contratos sem alterar o backend ou duplicar regras financeiras no navegador.

## Goals / Non-Goals

**Goals:**

- Entregar uma aplicação web React independente, executável e testável dentro do mesmo repositório.
- Criar uma experiência financeira coesa, responsiva, acessível e com densidade informacional de dashboard BI.
- Manter tipos, requisições, erros e invalidações de cache centralizados para reduzir divergência em relação à API.
- Isolar falhas de provedores externos por painel para que dados ainda disponíveis continuem úteis.
- Cobrir todos os fluxos atuais dos papéis `INVESTOR` e `ADMIN`.

**Non-Goals:**

- Alterar cálculos financeiros, permitir reescrita de lançamentos efetivados ou remover corretoras ainda vinculadas a carteiras.
- Implementar atualização em tempo real por WebSocket, refresh token ou autenticação por cookie HttpOnly, pois não há suporte correspondente na API atual.
- Criar uma aplicação móvel nativa, suporte offline ou internacionalização completa nesta mudança.
- Calcular no frontend valores financeiros que já sejam responsabilidade do backend; cálculos locais ficam limitados à apresentação, como valor bruto de uma linha e filtros.

## Decisions

### 1. Aplicação React, TypeScript e Vite em `frontend/`

O frontend será um projeto isolado em `frontend/`, construído com React, TypeScript e Vite. Essa separação preserva o ciclo Maven do backend, permite desenvolvimento independente e mantém explícitos os limites entre cliente e API.

Alternativas consideradas: incluir recursos compilados diretamente em `src/main/resources/static` acoplaria builds Node e Maven desde o primeiro momento; Next.js adicionaria renderização de servidor sem benefício para uma aplicação autenticada e dependente da API local.

### 2. Arquitetura orientada a recursos

O código será dividido em `app` (providers e rotas), `components` (design system e composição), `features` (auth, brokerages, portfolios, transactions, income e admin), `lib` (cliente HTTP e formatação) e `types` (contratos compartilhados). Cada feature concentrará páginas, componentes, hooks e validações do seu domínio.

Rotas públicas: `/login` e `/cadastro`. Rotas do investidor: `/app/carteiras`, `/app/corretoras` e `/app/carteiras/:portfolioId`, com áreas internas de visão geral, lançamentos e proventos. Rota administrativa: `/admin/usuarios`.

Alternativa considerada: organizar somente por tipo técnico gera pastas globais extensas e maior acoplamento entre fluxos.

### 3. Estado remoto com TanStack Query e estado local próximo ao uso

TanStack Query controlará consultas, cache, tentativas e invalidações. Formulários, filtros, abas e modais usarão estado local; não haverá store global genérica. Após mutações, somente as chaves afetadas serão invalidadas, por exemplo transações, posições, valorização e evolução após um lançamento.

Alternativas consideradas: Redux seria excessivo para o estado atual; chamadas manuais em efeitos repetiriam controle de concorrência, cache e erros.

### 4. Cliente HTTP tipado e tratamento por códigos públicos

Um cliente HTTP central aplicará a URL-base, cabeçalho Bearer, desserialização e conversão de falhas para um tipo `ApiError`. O interceptor de `401` encerrará a sessão. Componentes tratarão códigos públicos conhecidos e `fieldErrors`; nenhum fluxo dependerá de comparar mensagens textuais.

Os contratos TypeScript refletirão os DTOs atuais. O uso de `number` ficará restrito a gráficos; valores monetários recebidos serão mantidos como representação compatível e formatados na borda da interface, evitando novos cálculos de domínio.

Alternativa considerada: gerar automaticamente o cliente pelo OpenAPI pode ser adotado depois, mas introduziria uma etapa de geração e ajustes de tipos maior que o benefício inicial para o contrato atual.

### 5. Sessão em `sessionStorage` validada por `/api/auth/me`

O token será mantido em memória e espelhado em `sessionStorage` para sobreviver a recargas dentro da mesma sessão do navegador, sem persistir após seu encerramento. Na inicialização, a identidade e o papel serão revalidados pela API. Logout, expiração e respostas `401` limparão todo o estado autenticado e o cache de consultas.

Alternativas consideradas: `localStorage` prolongaria desnecessariamente a exposição do token; cookie HttpOnly exigiria mudança de contrato no backend.

### 6. Formulários com React Hook Form e Zod

React Hook Form reduzirá renderizações e padronizará estados de envio; Zod declarará validações equivalentes às restrições públicas da API. Erros locais e `fieldErrors` remotos serão apresentados nos mesmos componentes de campo. Valores decimais serão capturados como texto normalizado antes do envio para preservar precisão textual.

Alternativa considerada: validação artesanal em cada tela aumentaria inconsistências entre fluxos semelhantes.

### 7. Sistema visual próprio e dashboard com Recharts

A interface usará CSS com tokens semânticos e componentes reutilizáveis, evitando dependência de um kit visual completo. A direção visual será financeira contemporânea: base clara de baixo contraste, navegação em azul-marinho profundo, destaque esmeralda, tipografia limpa, cartões com hierarquia forte e cores semânticas para ganhos, perdas, pendências e alertas.

Recharts será usado para donut de alocação e linhas de evolução. Tooltips, legendas e tabelas/resumos equivalentes garantirão leitura precisa e acessível. Lucide fornecerá ícones consistentes. Tabelas terão visualização compacta em desktop e adaptação para cartões ou rolagem interna controlada em telas menores.

Alternativas consideradas: uma biblioteca de dashboard pronta limitaria a identidade visual; gráficos implementados manualmente aumentariam custo de acessibilidade e responsividade.

### 8. Dashboard composto por consultas independentes

A página da carteira carregará em paralelo detalhes, posições, valorização, evolução, transações e resumo de proventos. Cada painel terá skeleton, vazio, erro e nova tentativa próprios. O topo agregará KPIs do `consolidatedSummary` quando disponível e recorrerá aos resumos por moeda sem inventar consolidação quando câmbio estiver indisponível.

O gráfico de composição usará `allocationPercentage` retornado pela valorização. O gráfico de evolução usará exclusivamente os pontos do endpoint histórico. A interface nunca reconstruirá custo médio, ganho ou rentabilidade.

### 9. Fluxos mutáveis em painéis e diálogos acessíveis

Criação de carteira e corretora, novo lançamento, confirmação/cadastro de provento e manutenção de usuário serão formulários focados, abertos como páginas compactas ou diálogos conforme a complexidade. Exclusão, cancelamento e desativação sempre exigirão confirmação identificando o registro. Enquanto a mutação estiver ativa, controles serão bloqueados contra duplo envio.

### 10. Testes em camadas e validação contínua

Vitest, React Testing Library e MSW cobrirão componentes e jornadas integradas com respostas HTTP determinísticas: sessão, criação da primeira carteira, dashboard, compra/venda, proventos, autorização administrativa e erros públicos. O build TypeScript e ESLint formarão o gate estático. Playwright cobrirá ao menos os caminhos essenciais em navegador com API simulada, sem consumir provedores externos.

O pipeline existente ganhará um job de frontend com instalação reproduzível, lint, testes e build, separado dos jobs Java para diagnóstico claro.

### 11. Configuração local e integração CORS

`VITE_API_BASE_URL` configurará a API e terá exemplo versionado no frontend. No desenvolvimento, a origem padrão será documentada para inclusão em `CORS_ALLOWED_ORIGINS`; segredos nunca serão colocados em variáveis `VITE_*`, pois elas são incorporadas ao bundle público.

## Risks / Trade-offs

- [A API pode falhar por cota ou indisponibilidade de provedores] → Isolar erros por widget, manter dados independentes e oferecer nova tentativa sem fabricar resultados.
- [Um frontend único é uma mudança extensa] → Implementar por fundação e fatias verticais, mantendo build e testes verdes ao final de cada grupo de tarefas.
- [Token acessível ao JavaScript permanece sensível a XSS] → Usar `sessionStorage`, evitar HTML não confiável, não registrar token e manter dependências auditáveis; autenticação por cookie fica como evolução de backend.
- [DTOs podem evoluir separadamente dos tipos TypeScript] → Centralizar contratos, adicionar testes de integração simulada e documentar a possibilidade futura de geração OpenAPI.
- [Tabelas financeiras são densas em telas pequenas] → Priorizar campos essenciais, usar cartões responsivos e disponibilizar detalhes sem esconder ações.
- [Gráficos podem transmitir precisão enganosa em estados parciais] → Renderizar somente dados retornados pela API, informar indisponibilidade e sempre fornecer valores textuais equivalentes.

## Migration Plan

1. Adicionar o projeto `frontend/` sem alterar a inicialização do backend.
2. Entregar fundação visual, cliente HTTP, sessão e rotas protegidas.
3. Adicionar as jornadas do investidor em fatias testáveis e depois o painel administrativo.
4. Incluir configuração, documentação e job de validação do frontend.
5. Validar build de produção, testes do frontend e gates H2/PostgreSQL do backend antes do merge.

Rollback: remover o diretório do frontend e seu job de CI restaura o repositório anterior; nenhuma migração de banco ou mudança incompatível de API será introduzida.

### 12. Refinamentos validados em uso real

O cadastro de corretora consultará o CNPJ antes do envio final e exibirá a razão social retornada como dado somente leitura. Erros públicos conhecidos serão traduzidos por código para português, sem depender da mensagem técnica do provedor. A exclusão será exposta somente com confirmação e o backend a recusará quando houver carteira vinculada.

Seletores de corretora usarão o apelido definido pelo investidor como identificação principal. Os estados vazios do dashboard manterão espaçamento vertical uniforme em relação aos painéis adjacentes.

O formulário de lançamento será reinicializado a cada abertura. Datas serão digitadas e exibidas como `dd/mm/aaaa`, sendo convertidas para ISO apenas na integração com a API. Preço e custos aceitarão entrada decimal e serão formatados conforme a moeda ao perder o foco, mantendo valor canônico separado da apresentação.

A edição será limitada a lançamentos `PENDING`. O endpoint de atualização revalidará data, quantidade, preço, custos e saldo de venda, preservando ativo e metadados selecionados originalmente. Lançamentos `EFFECTIVE` e `CANCELLED` são históricos imutáveis.

### 13. Entrada brasileira e apresentação monetária consolidada

Os formulários desativarão a validação textual nativa do navegador e apresentarão mensagens próprias em português para campos ausentes ou inválidos. Campos de data compartilharão um controle que aceita `dd/mm/aaaa`, conclui entradas `dd/mm` com o ano corrente ao perder o foco e disponibiliza o seletor de calendário nativo sem expor o formato ISO.

O provento manual selecionará o ticker entre as posições atualmente custodiadas na carteira. Datas de pagamento e elegibilidade usarão o mesmo controle brasileiro, enquanto valor recebido e valor unitário usarão máscara monetária em reais.

Todos os valores monetários exibidos ao investidor serão apresentados em BRL. Para ativos em moeda estrangeira, o frontend consultará uma taxa referente à data da operação, converterá somente a representação visual e fará a conversão inversa antes de enviar preço, custos ou proventos ao backend. Assim, os contratos e cálculos de domínio continuam armazenando valores na moeda nativa do ativo, enquanto a interface mantém uma unidade de leitura consistente. A taxa e sua data de referência serão informadas ao usuário quando houver conversão.

Uma operação autenticada e vinculada à carteira exporá a taxa entre a moeda do ativo e BRL para uma data solicitada. A autorização reutilizará a verificação de propriedade da carteira e a integração cambial já existente; indisponibilidade continuará usando o erro público `EXCHANGE_RATE_UNAVAILABLE`.

### 14. Logotipos de ativos com fallback local

Um resolvedor visual central derivará a URL do logotipo a partir do mercado e do ticker normalizado. Ativos `BR` usarão o serviço de ícones da brapi, já relacionado ao provedor brasileiro adotado pelo backend. Ativos `US` usarão a Logo API pública da Parqet por símbolo ticker, acompanhada da atribuição visível e do link exigidos pelo provedor.

As imagens serão carregadas com `loading="lazy"`, política de referência restritiva, texto alternativo e dimensões fixas para evitar deslocamento de layout. Nenhum token, identificador de usuário ou dado de carteira será incluído na URL. Falha de rede, ticker desconhecido ou resposta inválida ocultará a imagem e revelará um marcador local derivado do ticker/mercado, preservando a leitura e sem exibir ícone quebrado.

O componente será compartilhado por posições, histórico e edição de lançamentos e histórico de proventos. A aplicação não persistirá URLs de terceiros nem alterará o contrato ou o esquema do backend, pois os logotipos são apenas enriquecimento visual dispensável.
