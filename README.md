# Gestor-de-A-es
Repositório criado para solução do Trabalho da matéria de Laboratório de Programação VI do curso de Sistemas de Informação da UNIFEF

## Graphify (opcional)

O projeto pode ser analisado localmente com o [Graphify](https://graphify.com/docs), que gera um grafo das relações entre arquivos, classes e métodos. A ferramenta é apenas um apoio ao desenvolvimento e não é uma dependência da aplicação.

### Configuração local

Com o `uv` instalado:

```powershell
uv tool install graphifyy
```

Para impedir que os artefatos locais apareçam no Git sem alterar o `.gitignore` do projeto, adicione `graphify-out/` ao arquivo local `.git/info/exclude`:

```powershell
if (-not (Select-String -Path .git/info/exclude -SimpleMatch "graphify-out/" -Quiet)) {
    Add-Content -Path .git/info/exclude -Value "graphify-out/"
}
```

Gere o grafo somente a partir do código, sem chave de API:

```powershell
graphify . --code-only
graphify cluster-only "."
```

Consulte o grafo em linguagem natural:

```powershell
graphify query "como a autenticação se conecta à persistência de usuários?"
```

Depois de mudanças no código, atualize o grafo local:

```powershell
graphify update .
```

Os arquivos gerados em `graphify-out/`, configurações locais e chaves de API não devem ser versionados.
