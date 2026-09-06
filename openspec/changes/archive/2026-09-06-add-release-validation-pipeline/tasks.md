## 1. Gates Maven determinísticos

- [x] 1.1 Inspecionar a seleção atual de perfis e testes para definir comandos explícitos de validação H2 e PostgreSQL.
- [x] 1.2 Configurar o build para que os testes PostgreSQL recebam URL, usuário, senha e segredo JWT somente por propriedades ou variáveis de ambiente explícitas.
- [x] 1.3 Garantir que a execução H2 obrigatória não exija PostgreSQL, credenciais de provedores ou rede externa.
- [x] 1.4 Garantir que a execução PostgreSQL valide migração em banco vazio e os fluxos de integração existentes, sem depender de chamadas reais a provedores.

## 2. Automação de entrega

- [x] 2.1 Criar workflow GitHub Actions acionado em pull requests e pushes para `dev`, configurando Java 17 e cache de dependências Maven.
- [x] 2.2 Implementar o job H2 usando o comando Maven documentado.
- [x] 2.3 Implementar o job PostgreSQL com serviço efêmero, variáveis restritas ao job e o comando Maven documentado.
- [x] 2.4 Manter smoke tests com provedores reais fora dos jobs obrigatórios e registrar sua ativação manual segura.

## 3. Higiene e documentação operacional

- [x] 3.1 Verificar o uso efetivo de OpenFeign e remover a dependência somente se não houver consumo no código, configuração ou testes.
- [x] 3.2 Documentar pré-requisitos, variáveis de ambiente sem valores sensíveis e os comandos locais equivalentes aos dois gates.

## 4. Verificação

- [x] 4.1 Executar localmente o gate H2 e o gate PostgreSQL com banco de teste limpo.
- [x] 4.2 Validar a sintaxe do workflow e revisar que nenhum segredo ou chamada externa obrigatória foi introduzido.
