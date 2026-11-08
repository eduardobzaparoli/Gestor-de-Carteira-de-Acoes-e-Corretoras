## 1. Configuração do emissor

- [x] 1.1 Adicionar o emissor obrigatório às propriedades JWT tipadas e à configuração comum da aplicação, com validação de valor não vazio.
- [x] 1.2 Propagar `JWT_ISSUER` para a configuração de testes, `.env.example` e serviço backend do Docker Compose.

## 2. Emissão e validação JWT

- [x] 2.1 Incluir o claim padrão `iss` nos tokens emitidos pelo serviço JWT existente.
- [x] 2.2 Configurar o decoder Nimbus para validar o emissor esperado sem remover as validações padrão de tempo e a restrição HS256.
- [x] 2.3 Preservar o filtro de conta ativa e a autorização pelo papel atual persistido, sem alterar endpoints ou contrato do login.

## 3. Verificação automatizada

- [x] 3.1 Ajustar fixtures e testes existentes para fornecer um emissor previsível e verificar o claim emitido.
- [x] 3.2 Adicionar testes de integração que rejeitem com `401` um token com assinatura adulterada e um token corretamente assinado por emissor diferente.
- [x] 3.3 Executar os testes focados de autenticação e a suíte H2; executar o gate PostgreSQL quando o banco descartável estiver disponível.

## 4. Documentação e validação final

- [x] 4.1 Documentar `JWT_ISSUER`, a necessidade de novo login após a implantação e a configuração local/Docker nos guias existentes.
- [x] 4.2 Documentar o roteiro manual equivalente à coleção Postman, a diferença entre `401` e `403` e a ausência de dados sensíveis no payload JWT.
- [x] 4.3 Atualizar o Graphify após a implementação e validar a change com OpenSpec em modo estrito, confirmando que nenhum segredo real foi versionado.
