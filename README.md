# SchoolSync API

API REST do projeto SchoolSync, desenvolvida com Spring Boot.
Gerencia usuários, salas (com matérias e períodos letivos), membros, atividades, caderno de atividades, grupos, tarefas, comentários, boletim de notas e notificações.

## Tecnologias

- Java 17
- Spring Boot 3.5.15
- Spring Web
- Spring Data JPA
- Bean Validation
- Spring Security: login com `AuthenticationManager` + `UserDetailsService`, token JWT com a biblioteca jjwt e senhas com BCrypt
- PostgreSQL (hospedado no Neon)
- Lombok
- Swagger / Springdoc OpenAPI
- Firebase Admin SDK (notificações push)
- Cloudinary (fotos de perfil)
- JUnit 5, Mockito e H2 (testes)

## Requisitos

- Java 17 ou superior
- Maven ou Maven Wrapper (`./mvnw`)
- Um banco PostgreSQL

## Variáveis de ambiente

As configurações ficam em `schoolsync-api/src/main/resources/application.yaml` e são lidas de variáveis de ambiente. Nenhuma senha fica no código.

| Variável | Obrigatória | Para que serve |
| --- | --- | --- |
| `DATABASE_URL` | Não (tem padrão) | URL JDBC do PostgreSQL |
| `DATABASE_USER` | Sim | Usuário do banco |
| `DATABASE_PASSWORD` | Sim | Senha do banco |
| `JWT_SECRET` | Sim | Chave que assina os tokens JWT (mínimo de 32 caracteres, exigência do jjwt) |
| `JWT_EXPIRATION_MS` | Não (padrão 24 h) | Validade do token em milissegundos |
| `SHOW_SQL` | Não (padrão `false`) | Mostra no log o SQL gerado pelo Hibernate (útil só localmente) |
| `FIREBASE_ENABLED` | Não (padrão `false`) | Liga o envio de notificações push |
| `FIREBASE_PROJECT_ID` | Só com Firebase | Id do projeto no Firebase |
| `FIREBASE_CREDENTIALS_BASE64` | Só com Firebase | JSON da conta de serviço em Base64 |
| `CLOUDINARY_CLOUD_NAME` | Só para fotos | Nome da conta no Cloudinary |
| `CLOUDINARY_API_KEY` | Só para fotos | Chave da API do Cloudinary |
| `CLOUDINARY_API_SECRET` | Só para fotos | Segredo da API do Cloudinary |
| `BREVO_API_KEY` | Só para recuperar senha | Chave da API da Brevo, que envia o e-mail com o código |
| `EMAIL_REMETENTE` | Só para recuperar senha | E-mail verificado como remetente na Brevo |
| `EMAIL_REMETENTE_NOME` | Não (padrão `SchoolSync`) | Nome que aparece como remetente |
| `RECUPERACAO_SENHA_VALIDADE_MINUTOS` | Não (padrão 15) | Validade do código de recuperação |
| `GOOGLE_CLIENT_IDS` | Só para login com Google | Client ID "Aplicativo da Web" do Google Cloud (mais de um separado por vírgula) |

Se `BREVO_API_KEY` e `EMAIL_REMETENTE` não estiverem configuradas, só a recuperação de senha fica fora do ar (responde 503). O e-mail sai pela API da Brevo porque o plano grátis do Render bloqueia SMTP. Do mesmo jeito, sem `GOOGLE_CLIENT_IDS` só o login com Google responde 503.

As tabelas são criadas e atualizadas pelo Hibernate (`ddl-auto: update`).

## Como executar

Os comandos abaixo rodam dentro da pasta `schoolsync-api/`, onde ficam o `pom.xml` e o `Dockerfile`.

```bash
./mvnw spring-boot:run
```

Testes (usam H2 em memória, sem acessar o banco real):

```bash
./mvnw test
```

Os mesmos testes rodam no GitHub Actions a cada push (`.github/workflows/testes.yml`).

## Docker

O `Dockerfile` compila o projeto e gera uma imagem só com o `.jar` e o Java, usada no deploy no Render:

```bash
docker build -t schoolsync-api .
docker run -p 8080:8080 -e DATABASE_USER=... -e DATABASE_PASSWORD=... -e JWT_SECRET=... schoolsync-api
```

## Autenticação

1. `POST /usuarios` cadastra o usuário.
2. `POST /usuarios/autenticar` com e-mail e senha devolve o token.
3. As demais rotas exigem o cabeçalho `Authorization: Bearer <token>`.

O usuário que faz cada ação vem sempre do token, nunca de um parâmetro da requisição. Rotas em `/admin/**` exigem o perfil `ADMIN`.

Depois de 5 senhas erradas no mesmo e-mail (ou 20 no mesmo IP) em 15 minutos, o login fica bloqueado por 15 minutos e a API responde `429`.

### Recuperar senha

1. `POST /usuarios/senha/esqueci` com `{"email": "..."}` manda um código de 6 dígitos pro e-mail. Responde `204` mesmo se o e-mail não tiver conta, pra não dar pra descobrir quem está cadastrado.
2. `POST /usuarios/senha/redefinir` com `{"email", "codigo", "novaSenha"}` troca a senha. O código vale 15 minutos e aceita 5 tentativas, e só o hash dele fica no banco. Quando a senha muda, as sessões abertas em outros aparelhos caem.

### Login com Google

`POST /usuarios/google` com `{"idToken": "..."}` (o token que o Google devolve pro app) responde igual ao `/usuarios/autenticar`. A API confere o token no `https://oauth2.googleapis.com/tokeninfo`: ele tem que ter sido emitido pra um dos `GOOGLE_CLIENT_IDS`, estar dentro da validade e ter o e-mail verificado.

- Se já existe conta com aquele e-mail, ela é ligada ao Google e a pessoa continua podendo entrar com a senha.
- Se não existe, a conta é criada com o nome e a foto do Google e uma senha aleatória. Pra ter senha também, é só usar o "esqueci minha senha".
- Conta desativada responde `403`, e um e-mail que já está ligado a outra conta Google responde `409`.

### Modo manutenção

Um ADMIN liga e desliga por `PUT /admin/manutencao` com `{"ativa": true, "mensagem": "...", "previsao": "hoje às 22h"}` (`GET /admin/manutencao` mostra o estado e quem mexeu por último). O estado fica na tabela `tb_configuracao_sistema`, então sobrevive a um restart do Render.

Enquanto está ligado, quem não é ADMIN recebe `503` com `{"manutencao": true, "mensagem", "previsao"}` em qualquer rota, e o app mostra a tela de manutenção. Continuam liberados o login, o `GET /sistema/status` (público, o app usa pra saber quando voltou) e o Swagger. O filtro guarda o estado em memória por 20 segundos pra não ir no banco a cada requisição.

## Paginação

Notificações e comentários vêm de 20 em 20 (dá pra pedir até 50), da mais nova pra mais antiga. Pra buscar os próximos, manda em `antesDe` a data do item mais antigo que já veio.

| Rota | Resposta |
| --- | --- |
| `GET /notificacoes?tamanho=20&antesDe=...` | `{ itens, temMais, naoLidas }` (`naoLidas` conta todas as não lidas) |
| `GET /atividades/{id}/comentarios?tamanho=20&antesDe=...` | `{ itens, temMais }` (itens em ordem cronológica) |

## Dados de demonstração

O script `scripts/popular-dados-demo.mjs` cria, pela própria API, uma sala DS3 completa: líder, colegas, matérias, bimestres, atividades, caderno, grupos com tarefas, comentários e boletim. Precisa só do Node 18 ou mais novo.

```bash
node scripts/popular-dados-demo.mjs --senha SENHA_DO_LIDER
```

Por padrão ele usa a API publicada; para a API local, acrescente `--api http://localhost:8080`. Se a sala DS3 já existir, o script para sem duplicar nada.

Para ter uma conta de cada perfil, rode `scripts/contas-admin-e-usuario.sql` no editor SQL do Neon: ele cria `vhadmin@gmail.com` (ADMIN) e `vhusuario@gmail.com` (USUARIO). O primeiro admin só pode ser criado pelo banco, de propósito. Depois disso, um ADMIN promove ou rebaixa outras contas por `PUT /admin/usuarios/{id}/perfil` com `{"perfil": "ADMIN"}` ou `{"perfil": "USUARIO"}` (ou pelo botão no painel do administrador do app). Ninguém pode mudar o próprio perfil.

## Swagger

```
http://localhost:8080/swagger-ui/index.html
```

## Estrutura

```
schoolsync-api/src/main/java/br/cefetmg/schoolsync_api
├── config       configurações (Firebase, Cloudinary, recuperação de senha, Google)
├── controller   rotas REST
├── dto          objetos de entrada e saída
├── entity       entidades JPA
├── exception    tratamento global de erros
├── repository   acesso ao banco (Spring Data JPA)
├── security     JWT, filtro de autenticação, modo manutenção, regras de acesso e limite de login
└── service      regras de negócio
```
