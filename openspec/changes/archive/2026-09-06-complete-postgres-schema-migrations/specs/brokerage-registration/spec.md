## MODIFIED Requirements

### Requirement: Compatibilidade de persistência
O sistema MUST persistir corretoras, endereços e restrições de unicidade com o mesmo comportamento funcional no H2 e no PostgreSQL. Nos perfis H2 locais de desenvolvimento e teste, o gerenciamento automático do esquema SHALL permanecer habilitado. No PostgreSQL, as tabelas e restrições SHALL ser disponibilizadas por migrações versionadas.

#### Scenario: Banco local vazio
- **WHEN** a aplicação inicia com um perfil local suportado sobre um banco vazio
- **THEN** a estrutura de corretoras é criada a partir do mapeamento persistente e fica pronta para cadastro e listagem

#### Scenario: Banco PostgreSQL vazio
- **WHEN** a aplicação inicia com o perfil PostgreSQL sobre um banco vazio
- **THEN** as tabelas de corretoras e seus relacionamentos são criados por migrações versionadas e ficam prontos para cadastro e listagem
