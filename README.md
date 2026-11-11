# API de avaliações

Backend REST em Java 21, Spring Boot, PostgreSQL e Auth0. O front-end não faz parte deste repositório.

## Endpoints

Todas as rotas exigem um access token JWT válido do Auth0, com issuer e audience correspondentes à configuração da API.

| Método | Rota | Acesso |
|---|---|---|
| `POST` | `/avaliacoes` | Role `ADMIN` |
| `GET` | `/avaliacoes` | Role `ADMIN` ou `USER` |

O `POST` recebe:

```json
{
  "emailAvaliado": "pessoa@empresa.com",
  "comentariosAvaliacao": "Colaborou bem com a equipe.",
  "nota": 9.5
}
```

O e-mail do avaliador não é aceito no corpo: é obtido da claim `email` do token autenticado. O servidor gera `id` e `dataAvaliacao`. A nota aceita valores de `0` a `10`, com até uma casa decimal.

## Auth0

1. No Auth0 Dashboard, abra **Applications > APIs > Create API**. Defina um nome e um **Identifier** (esse valor será `AUTH0_AUDIENCE`) e escolha RS256.
2. Em **User Management > Roles**, crie as roles `ADMIN` e `USER` e atribua-as aos usuários.
3. Crie uma **Post Login Action** em **Actions > Library** e adicione ao flow de login. Exemplo para emitir roles e e-mail no access token:

   ```javascript
   exports.onExecutePostLogin = async (event, api) => {
     const namespace = "https://simulado-backend.example.com/";
     const roles = event.authorization?.roles ?? [];

     api.accessToken.setCustomClaim(`${namespace}roles`, roles);
     if (event.user.email) {
       api.accessToken.setCustomClaim(`${namespace}email`, event.user.email);
     }
   };
   ```

4. No ambiente da aplicação, configure:
   - `AUTH0_ISSUER_URI`: domínio do tenant exibido nas configurações do Auth0 (por exemplo, `https://seu-tenant.us.auth0.com/`) com a barra final.
   - `AUTH0_AUDIENCE`: o **Identifier** da API criada no passo 1.
   - `AUTH0_ROLES_CLAIM`: `https://simulado-backend.example.com/roles` se usar o exemplo acima.
   - `AUTH0_EMAIL_CLAIM`: `https://simulado-backend.example.com/email` se usar o exemplo acima.

A API também reconhece as permissões `avaliacoes:read` e `avaliacoes:write` no claim `permissions`, mapeadas para `USER` e `ADMIN`, respectivamente. Caso use permissões em vez de roles, habilite RBAC e **Add Permissions in the Access Token** nas configurações da API do Auth0. Se trocar o namespace do exemplo, atualize as claims de roles e e-mail no Action e configure `AUTH0_ROLES_CLAIM` e `AUTH0_EMAIL_CLAIM` com os nomes exatos.

## Executar localmente

Requisitos: JDK 21, Maven 3.9+ e Docker Compose.

1. Copie `.env.example` para `.env`, defina `DB_PASSWORD` e substitua os valores de exemplo pelas configurações do Auth0. O Spring não carrega `.env` automaticamente; exporte as variáveis no terminal ou configure-as na IDE. No PowerShell, por exemplo:

   ```powershell
   $env:DB_PASSWORD = "senha-local"
   $env:AUTH0_ISSUER_URI = "https://seu-tenant.us.auth0.com/"
   $env:AUTH0_AUDIENCE = "identifier-da-api"
   $env:AUTH0_ROLES_CLAIM = "https://simulado-backend.example.com/roles"
   $env:AUTH0_EMAIL_CLAIM = "https://simulado-backend.example.com/email"
   ```

2. Inicie o PostgreSQL: `docker compose up -d`.
3. Exporte as variáveis do `.env` no terminal e rode `mvn spring-boot:run`; alternativamente, configure-as na sua IDE. O Flyway cria a tabela automaticamente.
4. Execute os testes e gere o JAR com `mvn verify`.

Variáveis principais: `DATABASE_URL` (opcional; padrão `jdbc:postgresql://localhost:5432/avaliacoes`), `DB_USERNAME`, `DB_PASSWORD`, `AUTH0_ISSUER_URI`, `AUTH0_AUDIENCE`, `AUTH0_ROLES_CLAIM`, `AUTH0_EMAIL_CLAIM`, `CORS_ALLOWED_ORIGINS` e `PORT`.

## Deploy na AWS EC2 com GitHub Actions

O workflow `.github/workflows/deploy.yml` executa `mvn verify` em pushes para `main` e em disparos manuais. Em seguida, copia o JAR por SSH e reinicia um serviço `systemd`. Ele não precisa de chaves de acesso AWS: usa uma chave SSH dedicada ao deploy.

### Preparar a EC2

Os comandos abaixo assumem Ubuntu 24.04. Restrinja a porta 22 no Security Group ao seu IP e aos IPs de saída do runner que você configurar. Para produção, use HTTPS por meio de um domínio e proxy reverso/Load Balancer. A aplicação deve ficar vinculada a `127.0.0.1` e ser publicada pelo proxy HTTPS; não exponha diretamente a porta 8080.

Conecte-se à instância e instale Java e PostgreSQL:

```bash
sudo apt update
sudo apt install -y openjdk-21-jre-headless postgresql
sudo systemctl enable --now postgresql
sudo useradd --system --home /opt/avaliacoes-api --shell /usr/sbin/nologin avaliacoes
sudo install -d -o avaliacoes -g avaliacoes -m 0750 /opt/avaliacoes-api
```

Crie um banco e usuário PostgreSQL isolados para a API:

```bash
sudo -u postgres psql
```

No prompt do PostgreSQL:

```sql
CREATE ROLE avaliacoes LOGIN;
\password avaliacoes
CREATE DATABASE avaliacoes OWNER avaliacoes;
\q
```

Defina uma senha forte quando `\password` solicitar e guarde-a apenas no servidor. Crie `/etc/avaliacoes-api.env` com os valores do banco e do Auth0 (não os coloque no repositório):

```ini
PORT=8080
DATABASE_URL=jdbc:postgresql://HOST_DO_POSTGRES:5432/avaliacoes
DB_USERNAME=USUARIO_DO_BANCO
DB_PASSWORD=SENHA_DO_BANCO
AUTH0_ISSUER_URI=https://SEU_TENANT_AUTH0/
AUTH0_AUDIENCE=IDENTIFIER_DA_API
AUTH0_ROLES_CLAIM=https://simulado-backend.example.com/roles
AUTH0_EMAIL_CLAIM=https://simulado-backend.example.com/email
CORS_ALLOWED_ORIGINS=https://SEU_FRONT.vercel.app
```

Na configuração deste servidor, o PostgreSQL roda localmente e escuta apenas em `127.0.0.1`. Se optar por RDS, permita conexões à porta 5432 somente a partir do Security Group da EC2. Proteja o arquivo de ambiente:

```bash
sudo chown root:root /etc/avaliacoes-api.env
sudo chmod 600 /etc/avaliacoes-api.env
```

Crie `/etc/systemd/system/avaliacoes-api.service`:

```ini
[Unit]
Description=API de avaliações
After=network.target postgresql.service
Requires=postgresql.service

[Service]
User=avaliacoes
Group=avaliacoes
WorkingDirectory=/opt/avaliacoes-api
EnvironmentFile=/etc/avaliacoes-api.env
ExecStart=/usr/bin/java -Dserver.address=127.0.0.1 -Xms64m -Xmx384m -XX:MaxMetaspaceSize=128m -jar /opt/avaliacoes-api/app.jar
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Crie `/usr/local/sbin/deploy-avaliacoes-api` para instalar o artefato e reiniciar o serviço:

```bash
#!/usr/bin/env bash
set -euo pipefail
test -s /tmp/avaliacoes-api.jar
install -o avaliacoes -g avaliacoes -m 0644 /tmp/avaliacoes-api.jar /opt/avaliacoes-api/app.jar
rm -f /tmp/avaliacoes-api.jar
systemctl restart avaliacoes-api
for attempt in $(seq 1 30); do
  status=$(curl --silent --output /dev/null --write-out '%{http_code}' http://127.0.0.1:8080/avaliacoes || true)
  if [[ "$status" == "401" ]]; then
    exit 0
  fi
  sleep 2
done
systemctl --no-pager --full status avaliacoes-api
exit 1
```

Configure o serviço e um usuário de deploy dedicado. Gere um par de chaves SSH próprio para o GitHub Actions no seu computador; não reutilize a chave administrativa `.pem` da EC2:

```bash
sudo chmod 750 /usr/local/sbin/deploy-avaliacoes-api
sudo systemctl daemon-reload
sudo systemctl enable avaliacoes-api
sudo useradd --create-home --shell /bin/bash deploy
sudo install -d -o deploy -g deploy -m 0700 /home/deploy/.ssh
```

No Windows, gere a chave com `ssh-keygen -t ed25519 -C github-actions-deploy -f "$env:USERPROFILE\Downloads\simulado-backend-deploy"` e deixe a passphrase vazia para uso não interativo. Instale o conteúdo do arquivo `.pub` em `/home/deploy/.ssh/authorized_keys`, proteja-o com `sudo chown deploy:deploy /home/deploy/.ssh/authorized_keys` e `sudo chmod 600 /home/deploy/.ssh/authorized_keys`. Autorize somente o script de deploy:

```bash
echo 'deploy ALL=(root) NOPASSWD: /usr/local/sbin/deploy-avaliacoes-api' | sudo tee /etc/sudoers.d/avaliacoes-deploy
sudo chmod 440 /etc/sudoers.d/avaliacoes-deploy
sudo visudo -cf /etc/sudoers.d/avaliacoes-deploy
```

### Configurar GitHub Actions

No GitHub, abra o repositório e vá a **Settings > Secrets and variables > Actions > New repository secret**. Cadastre:

| Secret | Valor / onde obter |
|---|---|
| `EC2_HOST` | IP público ou DNS da EC2; use um Elastic IP para não mudar após reinicializações. |
| `EC2_USER` | `deploy`, usuário SSH criado acima. |
| `EC2_SSH_PRIVATE_KEY` | Conteúdo completo da chave privada dedicada ao GitHub Actions correspondente à chave pública em `authorized_keys`. Cole somente no secret, nunca no repositório ou no chat. |
| `EC2_KNOWN_HOSTS` | Linha de chave pública do host para `EC2_HOST`, obtida de uma conexão SSH previamente verificada. Não desative `StrictHostKeyChecking`. |

No PowerShell, a chave de host já conhecida pode ser consultada com:

```powershell
ssh-keygen -F 3.221.155.116 -f "$env:USERPROFILE\.ssh\known_hosts" | Select-String "ssh-ed25519"
```

Copie a linha `ssh-ed25519` para `EC2_KNOWN_HOSTS`. Os valores de PostgreSQL/Auth0 são configurados no arquivo de ambiente da EC2, não nos secrets usados para o deploy. Depois de salvar os secrets, faça push para `main` ou execute **Actions > Build and deploy backend > Run workflow**. Consulte o log da execução em **Actions** e, se necessário, na EC2 use `sudo journalctl -u avaliacoes-api -f`.

O serviço deste projeto escuta somente em `127.0.0.1:8080`. Antes de chamar a API pelo front-end, configure um domínio e o proxy HTTPS para encaminhar `/avaliacoes` à aplicação; ajuste `CORS_ALLOWED_ORIGINS` no arquivo de ambiente da EC2 para a origem do front-end.
