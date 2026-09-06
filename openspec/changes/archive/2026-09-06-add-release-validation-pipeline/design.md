## Context

Veja `proposal.md` para a motivação. A suíte atual usa H2 pelo perfil `test` e possui testes de integração PostgreSQL condicionados às variáveis de conexão fornecidas pelo ambiente. O repositório está hospedado no GitHub, ainda não possui workflow em `.github/workflows/` e usa Maven com Java 17.

## Goals / Non-Goals

**Goals:**

- Tornar a validação de H2 e PostgreSQL repetível localmente e no GitHub Actions.
- Executar testes sem acesso a provedores reais, credenciais ou limites de APIs externas.
- Falhar cedo quando variáveis necessárias para os testes PostgreSQL estiverem ausentes ou inválidas.
- Manter um smoke test externo separado e opcional ao gate obrigatório de entrega.

**Non-Goals:**

- Publicar artefatos, implantar a aplicação ou alterar a política de branches do repositório.
- Executar chamadas reais para Brapi, Alpha Vantage, Brasil API, ViaCEP, CVM ou Brasil API de câmbio no CI obrigatório.
- Substituir os testes de integração existentes por Testcontainers ou introduzir uma dependência de contêiner na aplicação.

## Decisions

### 1. GitHub Actions com jobs separados por banco

O workflow terá um job H2 e outro PostgreSQL, ambos em Java 17 e Maven. O job PostgreSQL usará um serviço PostgreSQL efêmero do GitHub Actions e transmitirá a URL, usuário e senha exclusivamente por variáveis de ambiente de execução.

Alternativa considerada: um único job sequencial. Ela reduz duplicação, mas torna a identificação do banco causador mais lenta e permite que uma falha em H2 esconda a validação PostgreSQL.

### 2. Um comando Maven explícito para cada gate

O `pom.xml` concentrará perfis ou propriedades de teste que permitam executar H2 sem infraestrutura externa e PostgreSQL com parâmetros obrigatórios. Os comandos documentados serão os mesmos usados pelo workflow, inclusive com um repositório Maven local opcional.

Alternativa considerada: scripts exclusivos de shell. Perfis Maven preservam portabilidade entre PowerShell, GitHub Actions e outros terminais, sem duplicar a lógica de seleção de testes.

### 3. Provedores externos somente por dublês no gate

Os testes determinísticos continuarão usando estratégias dublês injetadas pelo contexto de teste. Smoke tests que chamem provedores reais, se mantidos, serão ativados somente por perfil explícito e segredo configurado, nunca em pull requests ou no push comum para `dev`.

Alternativa considerada: executar smoke tests em todo pipeline. Isso produziria falhas instáveis por limite de cota, indisponibilidade ou ausência de plano comercial.

### 4. Análise de dependências antes de remover OpenFeign

Será feita uma busca de declarações e uso efetivo de OpenFeign. A dependência só será removida se não houver cliente, configuração ou teste que a exija; caso contrário, a remoção ficará explicitamente fora desta change.

Alternativa considerada: removê-la por suspeita. Isso pode quebrar inicialização transitiva ou trabalho ainda não indexado.

## Risks / Trade-offs

- [Serviço PostgreSQL do CI divergir de uma instalação local] → Fixar uma versão suportada e usar as mesmas propriedades do perfil PostgreSQL.
- [Credenciais locais expostas na documentação] → Documentar somente nomes de variáveis e exemplos fictícios; não persistir segredos.
- [Testes externos se tornarem obsoletos por serem opcionais] → Identificá-los claramente como smoke tests e registrar como executá-los sob demanda.
- [Execução duplicada aumentar o tempo do CI] → Reutilizar o cache de dependências Maven e manter os jobs independentes para diagnóstico rápido.

## Migration Plan

1. Adicionar os perfis/comandos e executar localmente contra H2 e PostgreSQL.
2. Adicionar o workflow GitHub Actions com serviço PostgreSQL.
3. Confirmar que o workflow não requer segredos nem chamadas externas.
4. Reverter removendo o arquivo de workflow e as configurações de teste adicionadas, sem afetar dados persistidos ou contratos da API.
