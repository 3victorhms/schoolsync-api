# SchoolSync API

API REST do projeto SchoolSync, desenvolvida com Spring Boot.  
A aplicação gerencia as entidades de usuários, salas, grupos, membros de salas, membros de grupos, atividades, tarefas e cadernos de atividades e tarefas.

## Tecnologias

- Java 17
- Spring Boot 3.5.15
- Spring Web
- Spring Data JPA
- Bean Validation
- MySQL
- Lombok
- Swagger / Springdoc OpenAPI

## Requisitos

- Java 17 ou superior
- MySQL
- Maven ou Maven Wrapper

## Swagger (apenas local por enquanto)
```yaml
http://localhost:8080/swagger-ui/index.html
```

## Configuração do Banco

Por padrão, a API tenta conectar em:

```yaml
jdbc:mysql://localhost:3306/schoolsync
```

