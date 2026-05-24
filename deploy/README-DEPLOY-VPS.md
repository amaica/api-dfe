# Deploy api-dfe na VPS (100.71.54.35)

Implantação feita em **mai/2026** — serviço **api-dfe** (distribuição DFe / notas de entrada).

---

## O que existe na VPS

| Caminho | Função |
|---------|--------|
| `/opt/apidfe/api-dfe-11.jar` | Spring Boot (Java 17) |
| `/opt/apidfe/application.properties` | MySQL, HTTP Basic, Flyway (`ddl-auto=none`) |
| `/opt/apidfe/logs/apidfe.log` | Log da aplicação (rotação automática) |
| Banco MySQL `dfe-service` | Tabelas `empresa_dfe`, `nota_entrada` |
| `systemd` **`apidfe.service`** | Serviço habilitado no boot |

**URL:** `http://100.71.54.35:9090`

---

## Credenciais

| Uso | Usuário | Senha | Onde |
|-----|---------|--------|------|
| **API REST** (HTTP Basic) | `dfeapi` | `dfeapi` | `/opt/apidfe/application.properties` → `spring.security.user.name` / `password` |
| **MySQL** | `root` | `@lface#81` | mesmo arquivo → `spring.datasource.username` / `password` |
| **SSH VPS** (deploy) | `root` | `Ifibiruba@10000` | `root@100.71.54.35` |

Exemplo em `/opt/apidfe/application.properties`:

```properties
spring.security.user.name=dfeapi
spring.security.user.password=dfeapi
spring.datasource.username=root
spring.datasource.password=@lface#81
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/dfe-service?createDatabaseIfNotExist=true&serverTimezone=UTC

# Log em arquivo
logging.file.name=/opt/apidfe/logs/apidfe.log
logging.logback.rollingpolicy.max-file-size=50MB
logging.logback.rollingpolicy.max-history=14
```

---

## Logs

### Arquivo (recomendado — `tail`)

```bash
tail -f /opt/apidfe/logs/apidfe.log
tail -100 /opt/apidfe/logs/apidfe.log
```

Rotação: até **50 MB** por arquivo, **14** dias, **500 MB** total.

### Systemd (alternativa)

```bash
journalctl -u apidfe -f
```

---

## Systemd

Unit: `/etc/systemd/system/apidfe.service`

```ini
[Service]
WorkingDirectory=/opt/apidfe
Environment=JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ExecStart=/usr/lib/jvm/java-17-openjdk-amd64/bin/java \
  --add-opens java.base/java.nio.charset=ALL-UNNAMED \
  -jar /opt/apidfe/api-dfe-11.jar
Restart=on-failure
```

Comandos:

```bash
systemctl status apidfe
systemctl restart apidfe
tail -f /opt/apidfe/logs/apidfe.log
```

---

## Deploy a partir da máquina de dev

Script (repo **servers**):

```bash
/home/aurelio/FONTES/servers/deploy-apidfe-vps.sh
```

Ou manual:

```bash
cd /home/aurelio/FONTES/SPRING/api-dfePsk
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 mvn -DskipTests package
scp target/api-dfe-11.jar root@100.71.54.35:/opt/apidfe/
ssh root@100.71.54.35 systemctl restart apidfe
```

---

## Validação pós-deploy

```bash
# deve retornar 401
curl -s -o /dev/null -w "%{http_code}\n" http://100.71.54.35:9090/api/v1/empresa

# deve retornar 200 e []
curl -u dfeapi:dfeapi http://100.71.54.35:9090/api/v1/empresa
```

---

## Requisitos no servidor

- **OpenJDK 17** (`openjdk-17-jre-headless`)
- **MySQL 8** ativo
- Porta **9090** liberada (UFW estava inativo na implantação)

---

## Flyway

- Migrações em `src/main/resources/db/migration/`
- Produção: `spring.flyway.validate-on-migrate=false` (evita falha se o SQL histórico mudar após já aplicado)
- `spring.jpa.hibernate.ddl-auto=none` no servidor

Se precisar reparar checksum: `flyway repair` no banco ou alinhar o arquivo `V1` com o que foi aplicado.

---

## SEFAZ / SSL (obrigatório na VPS)

Distribuição DFe usa `https://www1.nfe.fazenda.gov.br/NFeDistribuicaoDFe/NFeDistribuicaoDFe.asmx`.

Instalar cadeia SERPRO/ICP-Brasil no SO (já aplicado em mai/2026):

```bash
curl -fsSL -o /usr/local/share/ca-certificates/icpbrasilv10.crt https://repositorio.serpro.gov.br/docs/icpbrasilv10.crt
curl -fsSL -o /usr/local/share/ca-certificates/serprossl.crt https://repositorio.serpro.gov.br/cadeias/serprossl.crt
update-ca-certificates
curl -sS -o /dev/null -w "%{http_code}\n" https://nfe.svrs.rs.gov.br/ws/NfeDistribuicaoDFe/NfeDistribuicaoDFe.asmx
# Esperado: 403 (sem certificado cliente) — não deve falhar SSL
```

Paths da aplicação:

| Item | Caminho |
|------|---------|
| Schemas XSD | `/opt/apidfe/schemas` |
| Cacert java-nfe (opcional) | `/opt/apidfe/cacert` |
| Log | `/opt/apidfe/logs/apidfe.log` |

**Importante:** o java-nfe só envia o certificado A1 na chamada HTTPS com `Multithreading=true` (já configurado no código).

Se o log mostrar `Connection or outbound has closed` com certificado válido, verifique: senha do `.pfx` no cadastro, certificado de produção, e se o IP da VPS não está bloqueado pela SEFAZ.

---

## Segurança recomendada

1. Restringir porta 9090 no firewall (só IPs dos backends).
2. Colocar **nginx** com HTTPS na frente (opcional).
3. Preferir chave SSH em vez de senha no `root`.
4. Rotacionar senhas da API/MySQL/SSH periodicamente.
