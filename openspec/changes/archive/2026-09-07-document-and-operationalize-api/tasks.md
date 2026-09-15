## 1. Contrato OpenAPI

- [x] 1.1 Adicionar e justificar a dependência Springdoc compatível com Spring Boot, sem alterar dependências de domínio.
- [x] 1.2 Configurar metadados da API, esquema Bearer JWT e rotas públicas da documentação.
- [x] 1.3 Completar a descrição dos endpoints, corpos, respostas e erros públicos que não forem inferidos corretamente.
- [x] 1.4 Testar que o documento OpenAPI e a interface navegável são acessíveis, não contêm dados sensíveis e identificam operações protegidas.

## 2. Saúde e observabilidade

- [x] 2.1 Adicionar Actuator e expor somente o health agregado sem detalhes de componentes.
- [x] 2.2 Ajustar a segurança para liberar apenas health e documentação entre as novas rotas públicas, mantendo os demais recursos protegidos.
- [x] 2.3 Configurar níveis de log apropriados por ambiente e revisar os fluxos de erro para impedir registro de credenciais e tokens.
- [x] 2.4 Testar o health público mínimo e a indisponibilidade de outros endpoints operacionais.

## 3. Política CORS

- [x] 3.1 Criar propriedades tipadas para uma lista de origens permitidas, vazia por padrão.
- [x] 3.2 Integrar a política CORS à segurança com origens exatas, métodos e cabeçalhos mínimos e sem credenciais de cookie.
- [x] 3.3 Testar ausência de autorização CORS por padrão, origem permitida e origem rejeitada.

## 4. Documentação operacional

- [x] 4.1 Reorganizar o README com início rápido, perfis, comandos de testes e links para a documentação detalhada.
- [x] 4.2 Documentar todas as variáveis de ambiente por finalidade, obrigatoriedade, padrão e perfil, sem valores sensíveis reais.
- [x] 4.3 Documentar o uso da API e do OpenAPI, incluindo autenticação, fluxos principais e estrutura de erros.
- [x] 4.4 Documentar migração, implantação, health check, rollback, backup/recuperação e smoke tests opcionais.

## 5. Verificação final

- [x] 5.1 Executar os testes direcionados de OpenAPI, segurança operacional e CORS.
- [x] 5.2 Executar os gates completos H2 e PostgreSQL e validar a change OpenSpec estritamente.
- [x] 5.3 Atualizar o mapa local do Graphify e revisar que as novas dependências preservam a arquitetura e não expõem segredos.
