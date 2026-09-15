## Context

Conforme descrito em `proposal.md`, a aplicação possui seis campos de senha distribuídos entre autenticação, administração de usuários e perfil do investidor. Todos utilizam o componente compartilhado `Field`, mas hoje ele renderiza apenas um `input` simples e não oferece uma ação interna. A mudança é exclusivamente de frontend e deve preservar as integrações existentes com formulários controlados, React Hook Form, validações e preenchimento automático.

## Goals / Non-Goals

**Goals:**

- Centralizar o comportamento de mostrar e ocultar senha para evitar implementações divergentes entre telas.
- Manter estado de visibilidade independente por campo e oculto por padrão.
- Preservar foco, valor, eventos, atributos de acessibilidade, validação e `autoComplete` já fornecidos pelos formulários.
- Integrar o controle aos temas claro e escuro e aos layouts responsivos existentes.

**Non-Goals:**

- Alterar políticas de senha, autenticação, contratos da API ou persistência.
- Criar medidor de força, geração automática ou recuperação de senha.
- Manter a senha visível entre navegações, reaberturas ou reinicializações de formulário.

## Decisions

### 1. Incorporar o comportamento ao componente compartilhado de campo

O `Field` detectará quando recebeu `type="password"` e renderizará o `input` dentro de um contêiner com um botão de alternância. Assim, os campos existentes e futuros que usam o padrão da aplicação recebem o recurso de forma uniforme, sem duplicar estado e marcação em cada tela.

Alternativa considerada: criar botões e estados em cada formulário. Foi descartada por aumentar repetição, risco de diferenças de acessibilidade e custo de manutenção.

### 2. Manter o estado local e independente

Cada instância do campo manterá seu próprio booleano de visibilidade. O tipo efetivamente renderizado alternará entre `password` e `text`, enquanto as demais propriedades e o mesmo elemento lógico do formulário serão preservados. Reabertura por remontagem e um mecanismo explícito de reinicialização ocultarão novamente o conteúdo quando o formulário persistir montado após uma limpeza.

Alternativa considerada: um estado único por formulário. Foi descartada porque mostrar uma senha não deve revelar automaticamente outras credenciais, especialmente no perfil do investidor.

### 3. Usar botão sem efeito de submissão e semântica acessível

O controle será um `button` com `type="button"`, ícones de olho aberto/fechado e rótulo dinâmico em português, como “Mostrar senha” e “Ocultar senha”. O estado será exposto por semântica apropriada, e o botão participará da ordem normal de teclado com foco visível.

Alternativa considerada: tornar o ícone clicável sem elemento de botão. Foi descartada porque não oferece, por si só, semântica nem operação por teclado adequadas.

### 4. Preservar integração e geometria do campo

O contêiner reservará espaço à direita para o botão, mantendo o texto digitado, mensagens de ajuda e erro fora da área do ícone. Os estilos usarão os tokens de cor existentes para funcionar nos dois temas e evitarão dimensões fixas que prejudiquem telas estreitas.

Alternativa considerada: posicionar o botão fora do campo. Foi descartada por fugir do padrão visual solicitado e aumentar o espaço ocupado pelos formulários.

## Risks / Trade-offs

- [Alternar o tipo do `input` pode afetar seleção ou foco em alguns navegadores] → manter a mesma instância do elemento, validar foco e cursor nos testes de interação e não recriar o campo durante a alternância.
- [O botão pode encobrir textos longos ou indicadores nativos] → reservar preenchimento interno específico e validar os temas e larguras suportadas.
- [Um formulário persistente pode conservar visibilidade após limpar valores] → oferecer reinicialização explícita do estado e acioná-la junto às rotinas existentes de limpeza, cancelamento e sucesso.
- [Novos campos podem ignorar o padrão compartilhado] → cobrir o contrato do componente em testes e registrar a expectativa de que campos de senha utilizem `Field`.

## Migration Plan

1. Evoluir o componente e seus estilos sem mudar sua interface para campos que não sejam de senha.
2. Integrar a reinicialização segura nos formulários persistentes e confirmar os seis usos atuais.
3. Executar testes de componente e dos fluxos afetados, seguidos das verificações completas do frontend.
4. Em caso de regressão, reverter apenas o comportamento visual; nenhuma migração de dados ou rollback de API será necessário.
