# Deploy api-dfe na VPS (100.71.54.35)

Implantação feita em **mai/2026** — serviço **api-dfe** (distribuição DFe / notas de entrada).

---

## O que existe na VPS

| Caminho | Função |
|---------|--------|
| `/opt/apidfe/api-dfe-11.jar` | Spring Boot (Java 17) |
| `/opt/apidfe/application.properties` | MySQL, HTTP Basic, Flyway (`ddl-auto=none`) |
| Banco MySQL `dfe-service` | Tabelas `empresa_dfe`, `nota_entrada` |
| `systemd` **`apidfe.service`** | Serviço habilitado no boot |

**URL:** `http://100.71.54.35:9090`

---

## Credenciais (trocar em produção)

| Uso | Variável / arquivo | Padrão atual |
|-----|-------------------|--------------|
| API HTTP Basic | `spring.security.user.*` em `/opt/apidfe/application.properties` | `dfeapi` / `dfeapi` |
| MySQL | `spring.datasource.*` no mesmo arquivo | `root` + senha local |

**Não** commitar senhas reais no Git — só no servidor.

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
journalctl -u apidfe -f
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

## Segurança recomendada

1. Trocar `dfeapi`/`dfeapi` por senha forte.
2. Restringir porta 9090 no firewall (só IPs dos backends).
3. Colocar **nginx** com HTTPS na frente (opcional).
4. Usar chave SSH em vez de senha no `root`.
