# Guia da API

Com a aplicação em `http://localhost:8080`, o contrato fica em `/v3/api-docs` e a interface navegável em `/swagger-ui.html`.

1. Cadastre uma conta em `POST /api/auth/register` ou autentique em `POST /api/auth/login`.
2. Envie o token nas rotas protegidas como `Authorization: Bearer <token>`.
3. Cadastre uma corretora, crie uma carteira, pesquise o ativo e use o `selectionId` retornado para registrar o lançamento.
4. Consulte posições, valorização, evolução e proventos pelos endpoints da carteira.

Erros usam JSON com `timestamp`, `status`, `code`, `message`, `path` e `fieldErrors`. O código público é a referência estável para tratamento pelo cliente; mensagens não devem ser usadas como identificadores.

O health check público está em `GET /actuator/health` e expõe somente o estado agregado.
