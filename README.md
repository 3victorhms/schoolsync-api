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
├── config       configurações (Firebase, Cloudinary)
├── controller   rotas REST
├── dto          objetos de entrada e saída
├── entity       entidades JPA
├── exception    tratamento global de erros
├── repository   acesso ao banco (Spring Data JPA)
├── security     JWT, filtro de autenticação e regras de acesso
└── service      regras de negócio
```
