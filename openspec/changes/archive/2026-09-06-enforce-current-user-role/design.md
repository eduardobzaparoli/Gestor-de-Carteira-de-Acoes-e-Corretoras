## Context

O processamento de um Bearer JWT já consulta a conta persistida para negar acesso quando ela não existe ou está inativa. Porém, as autoridades usadas pelas regras de rota são montadas a partir do campo `role` que foi assinado no momento do login. Veja a motivação em [proposal.md](proposal.md) e os comportamentos esperados nas especificações desta change.

## Goals / Non-Goals

**Goals:**

- Tornar o papel persistido da conta a fonte de verdade das autoridades da requisição autenticada.
- Fazer promoções e rebaixamentos terem efeito na próxima requisição com um token válido existente.
- Preservar a identidade JWT para os controladores e manter os erros atuais de token, conta ausente e conta inativa.

**Non-Goals:**

- Revogar tokens após troca de senha, criar blacklist de JWT ou versionar tokens por usuário.
- Alterar o formato, a expiração ou o endpoint de emissão de tokens.
- Alterar regras de acesso de recursos financeiros, endpoints administrativos ou o modelo persistido.

## Decisions

### Substituir as autoridades pelo papel atual depois da validação da conta

Após a validação criptográfica normal do JWT, o filtro que já carrega a conta verificará seu estado e reconstruirá as autoridades Spring Security usando o papel persistido atual. A credencial principal continuará sendo o JWT, preservando o identificador usado pelos controladores e serviços.

Essa escolha concentra a decisão de autorização no mesmo ponto em que a conta já é consultada, sem adicionar uma segunda leitura do banco por requisição. Também permite que as regras existentes de rota por papel continuem inalteradas.

Alternativas consideradas:

- **Confiar apenas no claim `role` do JWT:** descartada porque mantém privilégios antigos até o token expirar.
- **Consultar o banco no conversor inicial do JWT:** descartada porque mistura a validação/decodificação do token com acesso persistente e não reutiliza a verificação de conta já existente.
- **Invalidar todos os tokens ao mudar o papel:** descartada porque exigiria versão de token ou lista de revogação e contrariaria a decisão aprovada de manter o token como identidade válida.

### Manter o claim de papel no token por compatibilidade

O token continuará sendo emitido com `role`, mas esse dado não será a fonte usada para autorizar rotas. Isso evita uma mudança de contrato desnecessária para consumidores que eventualmente leiam o claim, sem comprometer a segurança da autorização no servidor.

### Preservar semântica de autenticação e erros existentes

Conta inativa ou inexistente, token inválido e token expirado continuam sendo falhas de autenticação (`401`). Conta ativa com papel insuficiente continua sendo falha de autorização (`403`). A mudança somente troca a origem da autoridade que decide esse `403`.

## Risks / Trade-offs

- **Leitura da conta a cada requisição protegida** → essa leitura já é feita para validar o estado ativo; a implementação reutilizará seu resultado e não criará consulta adicional.
- **Alteração concorrente de papel durante uma requisição** → a autorização vale para o estado observado ao início da requisição; a próxima requisição reflete o novo estado persistido.
- **Claim de papel antigo pode confundir consumidores externos** → o contrato deixará explícito que o servidor usa o papel atual persistido; respostas de identidade continuam refletindo a conta no banco.

## Migration Plan

1. Publicar a alteração de segurança sem migração de dados ou banco.
2. Tokens válidos já emitidos passam a usar automaticamente o papel atual no primeiro acesso após a publicação.
3. Em caso de rollback, reverter apenas o código; não há dados ou esquema a reverter. O comportamento anterior de confiar no papel histórico do token voltará a vigorar.
