## 1. Componente compartilhado

- [x] 1.1 Evoluir o componente `Field` para renderizar, em campos `type="password"`, um botão de alternância com ícones de olho, estado independente e tipo efetivo entre `password` e `text`.
- [x] 1.2 Preservar valor, eventos, foco, validação, atributos de preenchimento automático e descrições de ajuda ou erro durante a alternância.
- [x] 1.3 Implementar rótulos dinâmicos em português, semântica de estado e operação por teclado no botão `type="button"`.
- [x] 1.4 Adicionar um contrato de reinicialização para restaurar o estado oculto sem interferir nos campos comuns.
- [x] 1.5 Estilizar o contêiner, espaço interno, ícone, foco e estados de interação nos modos claro, escuro e responsivo.

## 2. Integração dos formulários

- [x] 2.1 Confirmar o controle e a reinicialização segura nos campos de senha das telas de login e cadastro público.
- [x] 2.2 Integrar o controle ao campo de senha do modal de criação e edição administrativa, restaurando o estado oculto ao cancelar, concluir ou trocar de usuário.
- [x] 2.3 Integrar controles independentes aos campos de senha atual, nova senha e confirmação do perfil, ocultando-os novamente após limpeza, erro sensível ou sucesso.
- [x] 2.4 Revisar todos os usos de `type="password"` no frontend para garantir cobertura integral e ausência de implementação paralela fora do padrão compartilhado.

## 3. Verificação automatizada e visual

- [x] 3.1 Criar testes do componente para estado inicial oculto, mostrar, ocultar, preservação do valor e foco, independência entre campos, acessibilidade e reinicialização.
- [x] 3.2 Atualizar os testes de autenticação para cobrir o controle no login e no cadastro sem alterar os dados enviados.
- [x] 3.3 Atualizar os testes administrativos para cobrir criação, edição, fechamento e reabertura do modal com senha ocultada por padrão.
- [x] 3.4 Atualizar os testes do perfil para cobrir os três controles independentes e o retorno ao estado oculto após limpeza, falha ou sucesso.
- [x] 3.5 Validar visualmente alinhamento, contraste, mensagens e navegação por teclado em ambos os temas e em largura reduzida.
- [x] 3.6 Executar formatação, análise estática, suíte completa de testes e build de produção do frontend.
