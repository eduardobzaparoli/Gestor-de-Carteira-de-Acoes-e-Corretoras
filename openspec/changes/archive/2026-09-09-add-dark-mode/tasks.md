## 1. Infraestrutura de tema

- [x] 1.1 Criar o resolvedor e o estado global de tema com suporte a `light` e `dark`, preferência do sistema, fallback claro e persistência segura no navegador.
- [x] 1.2 Aplicar o tema e `color-scheme` no elemento raiz antes da montagem do React para evitar a primeira pintura com o modo incorreto.
- [x] 1.3 Criar um controle reutilizável de alternância com ícones, rótulo acessível, foco visível e integração ao estado global.

## 2. Aparência e integração

- [x] 2.1 Preservar os valores atuais como tema claro e ampliar os tokens semânticos para superfícies, textos, bordas, sombras, campos e estados de interação.
- [x] 2.2 Implementar a paleta escura com preto e cinzas escuros, acentos laranja e variantes legíveis de erro, perda, aviso, sucesso e informação.
- [x] 2.3 Inserir o controle de tema nas telas de autenticação e na estrutura autenticada de desktop e mobile, cobrindo investidores e administradores.
- [x] 2.4 Adaptar gráficos, tooltips, eixos, grades, legendas, modais, tabelas e estados vazios aos tokens dos dois temas sem alterar logotipos externos.

## 3. Testes e validação

- [x] 3.1 Testar resolução inicial, fallback, preferência do sistema, persistência, alternância e tolerância à indisponibilidade das APIs do navegador.
- [x] 3.2 Atualizar testes de componentes e de tema para verificar o controle acessível, a manutenção do modo claro e a cobertura da paleta escura.
- [x] 3.3 Executar formatação, lint, testes automatizados, build de produção e testes de navegador do frontend.
- [x] 3.4 Inspecionar os modos claro e escuro nas telas públicas, do investidor e administrativas em desktop e mobile, corrigindo contraste, flash de tema e resíduos claros inadequados.
- [x] 3.5 Validar a change OpenSpec em modo estrito, executar varredura final de cores e atualizar o mapa Graphify.
