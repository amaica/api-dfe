# API DFe — documentação

Contrato de integração para backends que consomem o **api-dfe** (distribuição de documentos fiscais eletrônicos de **entrada**).

---

## Visão geral

| Item | Valor |
|------|--------|
| **Autenticação** | HTTP Basic em **todas** as rotas |
| **Usuário API** | `dfeapi` |
| **Senha API** | `dfeapi` |
| **MySQL (VPS)** | usuário `root`, senha `@lface#81`, banco `dfe-service` |
| **Porta padrão** | `9090` |
| **Base URL (VPS)** | `http://100.71.54.35:9090` |
| **Content-Type** | `application/json` (exceto upload multipart) |

Sem credenciais → **401 Unauthorized**.

---

## Autenticação

| Campo | Valor |
|-------|--------|
| Usuário | `dfeapi` |
| Senha | `dfeapi` |

```http
Authorization: Basic base64(dfeapi:dfeapi)
```

```bash
curl -u dfeapi:dfeapi "http://100.71.54.35:9090/api/v1/empresa"
```

Em ambiente local, sobrescreva com `DFE_API_USER` e `DFE_API_PASSWORD` se precisar.

---

## Empresa — `/api/v1/empresa`

Cadastro do emitente que possui certificado A1 para consultar a SEFAZ.

### Listar

```http
GET /api/v1/empresa
```

Resposta: array JSON de empresas (`certificado` e `senhaCertificado` **não** retornam na leitura).

### Buscar por ID

```http
GET /api/v1/empresa/{id}
```

### Cadastrar (JSON)

```http
POST /api/v1/empresa
Content-Type: application/json
```

| Campo | Obrigatório | Descrição |
|-------|-------------|-----------|
| `cpfCnpj` | sim | CNPJ/CPF |
| `certificado` | sim | Bytes do `.pfx` (no JSON) |
| `razaoSocial` | não | Razão social |
| `uf` | não | UF (ex.: `SP`) |
| `ambiente` | não | `HOMOLOGACAO` ou `PRODUCAO` |
| `senhaCertificado` | não | Senha do arquivo PFX |
| `tipoPessoa` | não | Ex.: `J` / `F` |

### Cadastrar com upload (recomendado)

```http
POST /api/v1/empresa/upload
Content-Type: multipart/form-data
```

| Campo form | Obrigatório | Descrição |
|------------|-------------|-----------|
| `cpfCnpj` | sim | CNPJ/CPF |
| `certificado` | sim | Arquivo `.pfx` / `.p12` |
| `razaoSocial` | não | |
| `uf` | não | |
| `ambiente` | não | `HOMOLOGACAO` ou `PRODUCAO` |
| `senhaCertificado` | não | Senha do certificado |
| `tipoPessoa` | não | |

Exemplo:

```bash
curl -u dfeapi:dfeapi -X POST "http://100.71.54.35:9090/api/v1/empresa/upload" \
  -F "cpfCnpj=12345678000199" \
  -F "razaoSocial=Empresa Exemplo LTDA" \
  -F "uf=SP" \
  -F "ambiente=HOMOLOGACAO" \
  -F "senhaCertificado=senha_do_pfx" \
  -F "tipoPessoa=J" \
  -F "certificado=@/caminho/certificado.pfx"
```

### Excluir

```http
DELETE /api/v1/empresa/{id}
```

Resposta OK: texto `"Empresa deletada com sucesso."`

---

## Nota de entrada — `/api/v1/notaEntrada`

Notas obtidas via **distribuição DFe** (NSU) para as empresas cadastradas.

### Listar todas

```http
GET /api/v1/notaEntrada
```

### Buscar por ID

```http
GET /api/v1/notaEntrada/{id}
```

### XML da nota

```http
GET /api/v1/notaEntrada/xml/{id}
```

### Buscar pela chave (44 dígitos)

```http
GET /api/v1/notaEntrada/chave/{chave}
```

### Disparar consulta na SEFAZ

```http
GET /api/v1/notaEntrada/consulta
```

Consulta **todas** as empresas cadastradas, persiste novas notas e devolve a lista atualizada (mesmo formato do `GET` sem sufixo).

```bash
curl -u dfeapi:dfeapi "http://100.71.54.35:9090/api/v1/notaEntrada/consulta"
```

---

## Job automático

Classe `AgendadorConsulta`:

- **Primeira execução:** 10 minutos após subir o serviço
- **Depois:** a cada **1 hora**

Equivalente ao `GET /api/v1/notaEntrada/consulta`, sem passar pelo HTTP.

---

## Respostas e erros

| HTTP | Significado |
|------|-------------|
| 200 | Sucesso (JSON ou texto) |
| 400 | Erro de negócio/validação — corpo com mensagem |
| 401 | Sem autenticação ou credencial inválida |

---

## Modelo de dados (resumo)

**empresa_dfe:** emitente + certificado A1 + NSU + ambiente (homologação/produção).

**nota_entrada:** chave, emitente, valor, CFOP, XML, vínculo com `empresa_id`, flags de importação.

Schema: Flyway `V1__init_schema.sql`.

---

## Integração com outros serviços

1. Configure `DFE_API_USER` / `DFE_API_PASSWORD` no cliente HTTP.
2. Cadastre cada CNPJ uma vez (`/empresa/upload`).
3. Consuma `/notaEntrada` ou reaja ao job horário.
4. Para homologação, use `ambiente=HOMOLOGACAO` e certificado de teste.

Não há headers extras de contexto (diferente do nfe-service com `X-Empresa-Cnpj`); o certificado fica **por empresa** no banco.
