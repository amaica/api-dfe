# api-dfe

Microsserviço **Spring Boot 3** para **distribuição DFe** (consulta de NF-e de entrada na SEFAZ) via biblioteca **java-nfe** (SW Consultoria).

Cadastra emitentes com certificado A1, consulta notas periodicamente e expõe REST para listar XML/chaves.

**Autenticação:** HTTP Basic (mesmo padrão do [nfe-service](../nfeService)).

---

## Credenciais (VPS produção — 100.71.54.35)

| Uso | Usuário | Senha |
|-----|---------|--------|
| **API REST** (HTTP Basic) | `dfeapi` | `dfeapi` |
| **MySQL** (banco `dfe-service`) | `root` | `@lface#81` |

Arquivo no servidor: `/opt/apidfe/application.properties`

```bash
curl -u dfeapi:dfeapi http://100.71.54.35:9090/api/v1/empresa
```

---

## Stack

| Item | Versão / tecnologia |
|------|---------------------|
| Java | 17 (build); Spring Boot 3.2.x |
| Banco | MySQL (`dfe-service`) |
| Migração | Flyway |
| Fiscal | `java-nfe` 4.00.40 |
| Agendamento | `@Scheduled` (consulta a cada 1h, 1ª após 10 min) |

---

## Execução local

### Pré-requisitos

- JDK **17**
- Maven 3.8+
- MySQL

### Build e run

```bash
cd api-dfePsk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64   # ajuste se precisar
export MYSQL_PASSWORD=sua_senha
export DFE_API_USER=dfeapi
export DFE_API_PASSWORD=dfeapi

mvn -DskipTests package
java -jar target/api-dfe-11.jar
```

Porta padrão: **9090** (`DFE_SERVER_PORT` ou `server.port`).

### Variáveis principais

| Variável | Descrição |
|----------|-----------|
| `DFE_SERVER_PORT` | Porta HTTP (padrão `9090`) |
| `DFE_API_USER` / `DFE_API_PASSWORD` | HTTP Basic da API |
| `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USERNAME`, `MYSQL_PASSWORD` | MySQL |
| `JPA_DDL_AUTO` | Padrão `validate`; em produção com Flyway use `none` |

Exemplo de config: [`src/main/resources/application.properties.example`](src/main/resources/application.properties.example)

---

## API

- **Base URL (produção atual):** `http://100.71.54.35:9090`
- **Usuário / senha:** `dfeapi` / `dfeapi`
- **Autenticação:** `Authorization: Basic` em **todas** as rotas (sem exceção de swagger — este projeto não expõe OpenAPI ainda).

Guia completo: **[docs/API-DFE.md](docs/API-DFE.md)**

### Fluxo rápido

1. `POST /api/v1/empresa/upload` — cadastrar CNPJ + certificado `.pfx`
2. `GET /api/v1/notaEntrada/consulta` — forçar consulta na SEFAZ (ou aguardar o job)
3. `GET /api/v1/notaEntrada` — listar notas
4. `GET /api/v1/notaEntrada/xml/{id}` — XML

```bash
curl -u dfeapi:dfeapi http://localhost:9090/api/v1/empresa
```

---

## Deploy VPS

Script na pasta **servers** (repo separado):

```bash
/home/aurelio/FONTES/servers/deploy-apidfe-vps.sh
```

Detalhes da implantação em **100.71.54.35**: **[deploy/README-DEPLOY-VPS.md](deploy/README-DEPLOY-VPS.md)**

---

## Estrutura

```
src/main/java/br/com/synki/apidfe/
  config/           # SecurityConfig (HTTP Basic)
  controller/       # Empresa, NotaEntrada
  service/          # Distribuição SEFAZ, persistência
  quartz/           # AgendadorConsulta (@Scheduled)
src/main/resources/db/migration/   # Flyway
```

---

## Licença / uso

Uso interno Synki / projetos associados. Senhas de **certificado A1** (`.pfx`) não devem ir para o Git — só usuário/senha da API e MySQL documentados acima para a equipe.
