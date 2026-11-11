# Backend — API de Avaliações

API REST de exemplo para cadastro e consulta de avaliações. O projeto usa Java 21,
Spring Boot, Spring Security Resource Server, Auth0, PostgreSQL, Flyway e Maven.

Este README descreve o estado atual do repositório, como executá-lo e como
substituir o modelo de autenticação/autorização quando a API passar a atender
outro domínio de negócio. O frontend está no repositório
[`simulado-frontend`](../simulado-frontend/README.md).

> **Atenção — configuração temporária de demonstração:** a EC2 deste exercício
> foi alterada para escutar em `0.0.0.0:8080`, e a porta TCP 8080 está acessível
> publicamente para permitir que a rewrite da Vercel chegue à API. O tráfego da
> Vercel até a EC2 usa HTTP, não HTTPS. Tokens e dados atravessam esse trecho sem
> criptografia. Não use esta configuração para dados reais. O serviço originalmente
> foi instalado para escutar em `127.0.0.1`; consulte [Exposição temporária da
> EC2](#exposição-temporária-da-ec2-e-como-reverter) para restaurar esse estado.

## 1. O que a API faz hoje

| Método | Caminho | Regra de acesso |
|---|---|---|
| `GET` | `/avaliacoes` | Token válido com role `USER` ou `ADMIN`; também aceita a permissão `avaliacoes:read`. |
| `POST` | `/avaliacoes` | Token válido com role `ADMIN`; também aceita a permissão `avaliacoes:write`. |
| `OPTIONS` | `/avaliacoes` | Preflight CORS para métodos e cabeçalhos configurados. |

Todas as outras combinações de método e caminho são negadas. Não há endpoint
público de health check neste momento.

### Criar avaliação

`POST /avaliacoes` recebe:

```json
{
  "emailAvaliado": "pessoa@empresa.com",
  "comentariosAvaliacao": "Colaborou bem com a equipe.",
  "nota": 9.5
}
```

Regras de validação atuais:

- `emailAvaliado`: obrigatório, formato de e-mail e no máximo 320 caracteres.
- `comentariosAvaliacao`: obrigatório, no máximo 5.000 caracteres.
- `nota`: obrigatória, de 0 a 10, com no máximo uma casa decimal.
- `emailAvaliador`: não é aceito no corpo; é retirado da claim configurada no
  access token.
- `id` e `dataAvaliacao`: gerados pelo servidor.
- Resposta de criação: HTTP `201` com a avaliação criada.

### Consultar avaliações

`GET /avaliacoes` retorna a lista de avaliações. A resposta contém `id`,
`emailAvaliador`, `emailAvaliado`, `comentariosAvaliacao`, `nota` e
`dataAvaliacao`.

### Códigos de resposta comuns

- `200`: consulta concluída.
- `201`: registro criado.
- `400`: corpo inválido ou campos não passaram na validação.
- `401`: token ausente, inválido, expirado, issuer incorreto ou audience ausente.
- `403`: token válido, mas sem a role/permissão necessária; ou sem a claim de
  e-mail necessária ao criar uma avaliação.
- `5xx`: falha do servidor, banco de dados ou dependência.

## 2. Componentes e configuração

| Caminho | Responsabilidade |
|---|---|
| `src/main/java/br/com/simulado/avaliacoes/avaliacao/` | Controller, serviço, entidade, DTOs e regras de validação do domínio de avaliações. |
| `src/main/java/br/com/simulado/avaliacoes/config/SecurityConfig.java` | Regras HTTP, validação do JWT, conversão de roles/permissões e CORS. |
| `src/main/java/br/com/simulado/avaliacoes/config/AudienceValidator.java` | Confere se o access token contém a audience da API. |
| `src/main/java/br/com/simulado/avaliacoes/config/Auth0RolesConverter.java` | Traduz claims de roles e permissions do JWT em authorities Spring. |
| `src/main/resources/application.yml` | Configuração Spring e nomes das variáveis de ambiente. |
| `src/main/resources/db/migration/` | Migrações versionadas do banco, aplicadas pelo Flyway. |
| `src/test/` | Testes unitários e de integração. |
| `docker-compose.yml` | PostgreSQL para desenvolvimento local. |
| `.github/workflows/deploy.yml` | Build/teste e deploy SSH para a EC2 quando há push em `main`. |
| `.env.example` | Exemplo de configuração local; não contém credenciais válidas de produção. |

Versões principais declaradas em `pom.xml`: Spring Boot 3.4.4 e Java 21.

## 3. Como o Auth0 participa de uma chamada

O backend é um **resource server**: não recebe senha, não inicia login e não cria
sessão de navegador. O frontend direciona o usuário ao Auth0, recebe um access
token e envia `Authorization: Bearer <access-token>` à API. O backend valida a
assinatura e as claims do token antes de executar a rota.

Fluxo resumido:

1. A SPA autentica o usuário no tenant Auth0.
2. A SPA solicita um access token para a API indicada por `AUTH0_AUDIENCE`.
3. Uma Post Login Action acrescenta as claims customizadas de roles e e-mail.
4. A SPA envia o access token à API.
5. Spring Security valida assinatura, issuer e audience.
6. `Auth0RolesConverter` converte roles/permissões para authorities Spring.
7. As regras da rota autorizam ou negam a operação.

Não confunda:

- **Domain/Issuer:** identifica o tenant que emitiu o token. `AUTH0_ISSUER_URI`
  deve corresponder exatamente ao issuer (`iss`) do token, normalmente com
  `https://` e `/` final.
- **Client ID:** identifica uma aplicação cliente Auth0, como a SPA. Não é a
  audience e não é segredo quando usado por uma SPA.
- **Audience / API Identifier:** identifica o resource server. Precisa coincidir
  com `AUTH0_AUDIENCE` no backend e `VITE_AUTH0_AUDIENCE` no frontend.
- **Role:** papel atribuído ao usuário no Auth0, como `ADMIN`. O backend ainda
  precisa mapear e exigir esse valor; criar a role no Dashboard, sozinho, não
  muda as regras do código.
- **Claim:** propriedade dentro do JWT. Os nomes de claims customizadas precisam
  coincidir exatamente entre Action, configuração do backend e leitura no front.

### Configuração atual do tenant

Valores específicos deste exercício, que devem ser substituídos ao criar outra
aplicação:

| Item | Valor atual |
|---|---|
| Tenant domain | `dev-cfz5e0arc3y56xsi.us.auth0.com` |
| Issuer | `https://dev-cfz5e0arc3y56xsi.us.auth0.com/` |
| API Identifier / audience | `https://simulado-backend` |
| Claim de roles | `https://simulado-backend.example.com/roles` |
| Claim de e-mail | `https://simulado-backend.example.com/email` |
| Roles de domínio reconhecidas | `ADMIN`, `USER` |
| Permissions reconhecidas | `avaliacoes:read`, `avaliacoes:write` |

O Identifier `https://simulado-backend` é um identificador lógico; não precisa
ser uma URL que abre um site. Mantenha-o igual em todos os participantes do fluxo.

### Preparar Auth0 para este backend

1. Em **Applications > APIs**, crie ou selecione uma API. Configure um Identifier
   (audience) único, por exemplo `https://simulado-backend`, e use RS256.
2. Em **Applications > Applications**, crie ou selecione uma aplicação do tipo
   **Single Page Application** para o frontend.
3. Na API, abra **Application Access** e permita acesso delegado à SPA. Neste
   tenant o controle é **Per-app authorization**; a SPA deste exercício foi
   autorizada individualmente. Ao trocar de SPA, conceda acesso à nova aplicação
   à API. Não autorize aplicações que não precisam dela.
4. Em **User Management > Roles**, crie as roles que o sistema realmente usa e
   atribua-as a usuários. Atualmente `ADMIN` e `USER` são os papéis aceitos.
5. Em **Actions > Library**, crie/edite uma Action do trigger **Login / Post
   Login** e conecte-a ao flow **Actions > Flows > Login** (o nome exato do menu
   pode variar com a interface do Dashboard).
6. Use o namespace configurado no backend. Exemplo atual:

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

7. Faça **Deploy** da Action e confira que ela está posicionada no fluxo de login,
   antes de **Complete**. Uma Action apenas criada ou implantada, mas não
   conectada ao flow, não altera os tokens.
8. Configure callback, logout e web origins da SPA com as origens reais:
   `http://localhost:5173` para Vite local e `https://simulado-frontend-seven.vercel.app`
   para produção. Para URLs de callback/origin, não acrescente caminhos; use a
   origem exata.
9. Faça login e inspecione o **access token** emitido para a API. Verifique `iss`,
   `aud`, a claim de roles e, se necessário, a claim de e-mail. Não publique o
   token em screenshots, logs ou tickets.

#### Roles ou permissions

Este backend aceita os dois formatos:

- **Roles:** a Action emite a lista na claim `AUTH0_ROLES_CLAIM`; valores `ADMIN`
  e `USER` são mapeados a `ROLE_ADMIN` e `ROLE_USER`. A configuração de roles
  precisa ser enviada ao access token, não apenas ao ID token.
- **Permissions:** o backend reconhece `avaliacoes:read` como `ROLE_USER` e
  `avaliacoes:write` como `ROLE_ADMIN`, se as strings vierem no array padrão
  `permissions` do access token.

Para permissions, configure permissões na API do Auth0, habilite RBAC e
**Add Permissions in the Access Token**, e conceda as permissões à aplicação/
usuário conforme a política do tenant. A API continuará sendo a autoridade final:
esconder um botão no frontend não protege endpoint algum.

Não é obrigatório usar roles e permissions ao mesmo tempo. Escolha um modelo,
documente-o e mantenha Action, frontend e backend consistentes.

## 4. Adaptar para uma aplicação diferente

Use esta sequência como checklist. Evite reaproveitar nomes de roles ou claims
sem confirmar que ainda representam as regras do novo domínio.

### 4.1 Escolha a estratégia de identidade

- Para isolar aplicações ou ambientes, crie um novo **API Identifier** e use-o
  como audience. Isso ajuda a evitar que um token emitido para uma API seja aceito
  por outra.
- Decida se o usuário será identificado por `sub` (identificador estável do
  Auth0) ou por e-mail. E-mail pode mudar e pode não estar verificado; para
  relações duradouras, normalmente é melhor persistir `sub` e tratar e-mail como
  dado de perfil.
- Defina uma convenção de namespace URI para claims customizadas que você
  controla, por exemplo `https://sua-organizacao.example/claims/roles`. Evite
  nomes curtos e nomes pertencentes a protocolos reservados.
- Decida se autorização será por roles amplas (RBAC), permissions granulares ou
  uma combinação deliberada. Não faça o código aceitar qualquer texto vindo de
  um claim como autoridade sem validação.

### 4.2 Atualize o Auth0

- Crie/configure o resource server (API) com o novo Identifier.
- Configure a política de acesso delegado e autorize apenas os clientes
  necessários.
- Crie as roles e/ou permissions do novo domínio e atribua-as aos usuários.
- Atualize a Post Login Action para emitir os claims que o backend realmente
  espera. Para roles, `event.authorization?.roles` contém roles atribuídas ao
  usuário; para permissions, configure a API para incluir `permissions`.
- Faça deploy da Action e confira sua posição no flow.
- Configure URLs permitidas para cada ambiente da SPA.
- Teste access tokens da API, não apenas ID tokens. Confira `iss`, `aud`,
  permissions e claims customizadas.
- Mantenha configuração separada entre desenvolvimento e produção quando os
  tenants, APIs, usuários ou políticas diferirem.

### 4.3 Atualize este backend

1. `src/main/resources/application.yml`: verifique a variável de issuer, audience,
   claim de roles, claim de e-mail e origens CORS.
2. `src/main/java/br/com/simulado/avaliacoes/config/Auth0RolesConverter.java`:
   ajuste quais roles/permissions são reconhecidas e como elas se mapeiam às
   authorities Spring.
3. `SecurityConfig.java`: troque as regras `requestMatchers` de cada método/rota.
   Prefira negar por padrão e autorizar explicitamente.
4. Controller/serviço: troque a forma de extrair identidade. Atualmente `POST`
   precisa da claim de e-mail configurada; uma nova aplicação pode precisar de
   `sub`, organization ID ou outra claim.
5. Testes: atualize testes do converter, do validator de audience e das regras
   de endpoint para incluir casos autorizados e negados.
6. `.env.example` e configuração no servidor: atualize nomes/valores para o novo
   ambiente, sem adicionar segredos ao Git.
7. Se as entidades mudarem, crie novas migrações Flyway; não altere migrações já
   aplicadas em banco compartilhado.

`AUTH0_ROLES_CLAIM` e `AUTH0_EMAIL_CLAIM` são configuráveis por variável de
ambiente. A lista de roles reconhecidas e o mapeamento de permissions, por outro
lado, são código no `Auth0RolesConverter`; mudar só a variável não cria uma nova
role suportada.

## 5. Executar localmente

### Requisitos

- JDK 21.
- Maven 3.9+ (ou Maven Wrapper, se adicionado ao projeto).
- Docker Desktop / Docker Compose para PostgreSQL local.
- Tenant e API Auth0 de desenvolvimento para testar endpoints autenticados.

### Banco local

Na raiz do repositório:

```powershell
Copy-Item .env.example .env
```

Edite `.env` e defina ao menos `DB_PASSWORD`. O Spring Boot **não carrega um
arquivo `.env` automaticamente**. Exporte as variáveis no processo, use uma
ferramenta local de dotenv ou configure o ambiente de execução da IDE. Exemplo
no PowerShell:

```powershell
$env:DB_PASSWORD = "senha-local"
$env:AUTH0_ISSUER_URI = "https://seu-tenant.us.auth0.com/"
$env:AUTH0_AUDIENCE = "identifier-da-api"
$env:AUTH0_ROLES_CLAIM = "https://sua-organizacao.example/claims/roles"
$env:AUTH0_EMAIL_CLAIM = "https://sua-organizacao.example/claims/email"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173"
```

Inicie o PostgreSQL:

```powershell
docker compose up -d
```

Depois rode a API:

```powershell
mvn spring-boot:run
```

O Flyway executa as migrações ao iniciar. Para verificar/buildar:

```powershell
mvn verify
```

O PostgreSQL local publicado pelo Compose usa a porta 5432 e volume persistente
`postgres-data`. Não remova esse volume se quiser manter os dados de
desenvolvimento.

### Variáveis do backend

| Variável | Obrigatória? | Uso |
|---|---:|---|
| `PORT` | Não | Porta do Spring; padrão `8080`. |
| `DATABASE_URL` | Não | JDBC URL; padrão `jdbc:postgresql://localhost:5432/avaliacoes`. |
| `DB_USERNAME` | Não | Usuário do banco; padrão do ambiente local `avaliacoes`. |
| `DB_PASSWORD` | Sim | Senha do banco; fornecida fora do repositório. |
| `AUTH0_ISSUER_URI` | Sim | Issuer do tenant Auth0, normalmente com barra final. |
| `AUTH0_AUDIENCE` | Sim | Identifier exato da API Auth0 que o token precisa conter. |
| `AUTH0_ROLES_CLAIM` | Não | Claim de roles; padrão `https://simulado-backend.example.com/roles`. |
| `AUTH0_EMAIL_CLAIM` | Não | Claim usada para identificar e-mail do avaliador; padrão `https://simulado-backend.example.com/email`. |
| `CORS_ALLOWED_ORIGINS` | Não | Lista separada por vírgula de origens; padrão local inclui portas 3000 e 5173. |

Não coloque senha do banco, chave SSH, client secret ou token de usuário em
`.env.example`, no frontend, no GitHub ou em logs. A SPA não precisa de client
secret.

## 6. Deploy atual na AWS EC2

### Como o GitHub Actions faz deploy

`.github/workflows/deploy.yml`:

1. Roda em push para `main` ou `workflow_dispatch`.
2. Configura Java 21.
3. Executa `mvn --batch-mode verify`.
4. Configura SSH usando secrets do repositório.
5. Copia `target/avaliacoes-api.jar` para `/tmp/avaliacoes-api.jar`.
6. Executa `/usr/local/sbin/deploy-avaliacoes-api` por SSH com `sudo`.

Esse fluxo **não exige AWS access key** nem permissão de gerenciamento do
Security Group. Ele exige rede SSH até a EC2 e credenciais SSH adequadas. O
script de deploy substitui o JAR e reinicia `avaliacoes-api`; configurações do
systemd em drop-ins permanecem no host entre deploys.

Secrets esperados em **GitHub > Repository > Settings > Secrets and variables >
Actions**:

| Secret | Valor |
|---|---|
| `EC2_HOST` | IP público ou DNS estável da EC2; Elastic IP evita mudança de IP. |
| `EC2_USER` | Usuário SSH com permissão restrita para executar o deploy. |
| `EC2_SSH_PRIVATE_KEY` | Chave privada dedicada ao workflow. Nunca publicar ou reutilizar a chave administrativa. |
| `EC2_KNOWN_HOSTS` | Chave pública de host verificada para a instância. Não desabilitar `StrictHostKeyChecking`. |

O arquivo `/etc/avaliacoes-api.env` da EC2 contém configuração do banco e do
Auth0; o workflow não o cria nem sincroniza. Proteja-o no servidor com
`root:root` e modo `600`.

### Estado atual conhecido da EC2

- Host/IP usado pela configuração atual da Vercel: `3.221.155.116`.
- Serviço systemd: `avaliacoes-api`.
- JAR: `/opt/avaliacoes-api/app.jar`.
- Porta da API: TCP `8080`.
- PostgreSQL local: porta `5432`, não deve ser pública.
- A aplicação responde `401` sem token em `/avaliacoes`, comportamento normal
  que também é usado como verificação simples de saúde pelo script de deploy.
- A aplicação está atualmente configurada por um drop-in
  `/etc/systemd/system/avaliacoes-api.service.d/override.conf` para bind
  `0.0.0.0` durante o exercício.
- `CORS_ALLOWED_ORIGINS` inclui
  `https://simulado-frontend-seven.vercel.app`.
- A porta 8080 está temporariamente acessível publicamente para a rewrite.

O Security Group é uma configuração da AWS fora do repositório. O acesso SSH à
máquina não dá, por si só, permissão para alterar o Security Group.

### Exposição temporária da EC2 e como reverter

O bind `0.0.0.0:8080` torna a aplicação acessível em todas as interfaces de rede.
Com a entrada TCP 8080 permitida no Security Group, qualquer IP pode tentar
conectar-se à API. A autenticação JWT continua exigida para GET/POST, mas isso
não cifra o tráfego e não elimina riscos de exposição.

Quando terminar a demonstração:

1. No Console AWS, remova a regra de entrada TCP 8080 para `0.0.0.0/0` (ou remova
   a regra TCP 8080 temporária). Este passo requer alguém com permissão AWS.
2. Na EC2, remova o drop-in público e volte ao unit file original que usa
   `-Dserver.address=127.0.0.1`:

   ```bash
   sudo rm /etc/systemd/system/avaliacoes-api.service.d/override.conf
   sudo systemctl daemon-reload
   sudo systemctl restart avaliacoes-api
   sudo systemctl is-active avaliacoes-api
   sudo ss -ltnp | grep 8080
   ```

3. Confirme que o listener voltou a `127.0.0.1:8080`.
4. Revise as regras da Vercel e remova o rewrite para HTTP se ele não for mais
   necessário. O frontend deixará de chamar a API até existir outro endpoint.
5. Verifique se o endereço público, logs ou tokens foram compartilhados durante
   o teste; revogue/rotacione credenciais se necessário.

Para uma publicação duradoura, use domínio com TLS em proxy reverso ou load
balancer, mantenha a aplicação em localhost, restrinja as regras de rede e evite
encaminhar access tokens por HTTP.

## 7. Configuração Vercel/API e CORS

O frontend deste exercício usa uma rewrite same-origin:

```text
Browser --HTTPS--> simulado-frontend-seven.vercel.app/api/avaliacoes
Vercel  --HTTP----> 3.221.155.116:8080/avaliacoes
```

Isso mantém HTTPS apenas entre o navegador e a Vercel; o segundo trecho é HTTP.
O browser vê chamadas same-origin, então CORS não costuma ser necessário nessa
rota específica. O backend, no entanto, mantém CORS configurável para uso direto,
local e outras origens.

CORS **não é autenticação** e não bloqueia clientes que não sejam navegadores.
`CORS_ALLOWED_ORIGINS` deve conter a origem, sem caminho nem barra extra,
separada por vírgulas. Se o frontend chamar a API diretamente, o browser pode
fazer preflight por causa de `Authorization` e `Content-Type`; o backend permite
`GET`, `POST`, `OPTIONS` e esses cabeçalhos.

## 8. Testes e diagnóstico

```powershell
mvn test
mvn verify
```

Checklist de falhas comuns:

| Sintoma | O que conferir |
|---|---|
| `401 Unauthorized` | Header Bearer enviado? Token expirado? `AUTH0_ISSUER_URI` e `AUTH0_AUDIENCE` correspondem exatamente às claims `iss` e `aud`? |
| `403 Forbidden` em GET | Role `USER`/`ADMIN` ou permission `avaliacoes:read` está no **access token**? Claim name coincide com `AUTH0_ROLES_CLAIM`? |
| `403 Forbidden` em POST | Usuário tem `ADMIN` ou `avaliacoes:write`? A Action emite roles? O JWT contém claim de e-mail em `AUTH0_EMAIL_CLAIM`? |
| Erro de issuer | Use o issuer completo do tenant, normalmente incluindo `/` final. Não use Client ID como issuer. |
| Erro de audience | Use o API Identifier, não domínio, Client ID ou Management API audience. |
| Action não altera token | Action está implantada e conectada ao Login Flow? O pedido é por access token para a API? |
| `Failed to connect`/502 | Serviço ativo? Listener na interface adequada? Porta liberada no Security Group? O destino na rewrite está correto? |
| CORS no navegador | Origem do frontend exatamente igual à configurada? A chamada usa origem direta ou rewrite same-origin? Preflight está chegando? |
| Erro de banco | PostgreSQL ativo? JDBC URL, usuário e senha corretos? Porta 5432 não está sendo confundida com API? Flyway/migrações concluídas? |

No servidor:

```bash
sudo systemctl status avaliacoes-api --no-pager
sudo journalctl -u avaliacoes-api -n 100 --no-pager
sudo ss -ltnp
curl -i http://127.0.0.1:8080/avaliacoes
```

Um `401` no último comando confirma que o processo respondeu e a rota está
protegida; não confirma que um usuário autenticado conseguirá ler dados.

## 9. Cuidados antes de reaproveitar

- Troque audience, claims e regras de autorização para cada API/domínio.
- Não use a Management API do Auth0 como audience de uma API de negócio.
- Não confie no frontend para impor permissões.
- Não guarde credenciais ou tokens no código, bundle Vite, issues ou logs.
- Use identificador estável do usuário (`sub`) se o domínio depender de identidade
  duradoura; e-mail é um atributo que pode mudar.
- Defina políticas de acesso por endpoint, não apenas roles genéricas.
- Revise CORS, TLS, Security Groups, banco, backups e retenção.
- Separe dados e credenciais de desenvolvimento e produção.
- Adicione migrations e testes ao alterar o modelo.
- Depois da demonstração, remova a exposição pública temporária da porta 8080.
