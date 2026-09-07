## ADDED Requirements

### Requirement: Histórico verificável e migrations imutáveis
O sistema SHALL registrar versão, descrição, ordem, resultado e checksum das migrations aplicadas ao PostgreSQL e MUST rejeitar divergências entre esse histórico e os arquivos versionados.

#### Scenario: Migration aplicada permanece inalterada
- **WHEN** a aplicação inicia e os arquivos correspondem aos checksums registrados
- **THEN** a validação do histórico é aprovada e somente migrations pendentes podem ser executadas

#### Scenario: Migration aplicada foi modificada
- **WHEN** o conteúdo de uma migration já registrada é alterado
- **THEN** a validação falha por divergência de checksum sem reescrever silenciosamente o histórico

#### Scenario: Correção posterior do esquema
- **WHEN** uma estrutura já implantada precisa ser corrigida ou ampliada
- **THEN** a mudança é entregue por uma nova migration de versão superior, preservando os arquivos aplicados

### Requirement: Execução concorrente protegida
O sistema MUST serializar tentativas concorrentes de alteração do mesmo esquema para impedir que duas instâncias apliquem migrations simultaneamente.

#### Scenario: Duas instâncias iniciam juntas
- **WHEN** duas instâncias tentam migrar o mesmo banco PostgreSQL ao mesmo tempo
- **THEN** apenas uma aplica cada migration enquanto a outra aguarda ou reconhece o histórico já atualizado
