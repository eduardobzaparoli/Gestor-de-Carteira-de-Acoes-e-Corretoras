# Formato do cadastro de participantes intermediários da CVM

Inspeção realizada em 26/08/2026 a partir do recurso oficial [Participantes Intermediários: Informação Cadastral](https://dados.cvm.gov.br/dataset/intermed-cad).

- URL do snapshot: `https://dados.cvm.gov.br/dados/INTERMED/CAD/DADOS/cad_intermed.zip`.
- O ZIP contém `cad_intermed.csv` (cadastro principal) e `cad_intermed_resp.csv` (responsáveis). O sistema usa somente o primeiro arquivo.
- O CSV usa delimitador ponto e vírgula (`;`) e codificação Windows-1252.
- O cabeçalho observado é: `TP_PARTIC;CNPJ;DENOM_SOCIAL;DENOM_COMERC;DT_REG;DT_CANCEL;MOTIVO_CANCEL;SIT;DT_INI_SIT;CD_CVM;SETOR_ATIV;CONTROLE_ACIONARIO;VL_PATRIM_LIQ;DT_PATRIM_LIQ;TP_ENDER;LOGRADOURO;COMPL;BAIRRO;MUN;UF;PAIS;CEP;DDD_TEL;TEL;DDD_FAX;FAX;EMAIL;SITE_WEB`.
- Para esta validação, o CNPJ é indexado pela coluna `CNPJ`, a categoria pela coluna `TP_PARTIC` e a situação pela coluna `SIT`.
- O status ativo observado é `EM FUNCIONAMENTO NORMAL`. Registros com `CANCELADA` ou qualquer outro status não são considerados ativos.

O parser da aplicação trata aspas e campos com quebra de linha usando apenas a biblioteca padrão do Java. Portanto, não foi adicionada uma dependência específica de CSV.
