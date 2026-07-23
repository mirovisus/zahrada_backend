# Zahrada - backend

REST API pro aplikaci na správu zahrad a poptávek zahradnických prací.
Spring Boot 4.0.3, Java 17, H2, JWT.

## Požadavky

- JDK 17
- Maven (nebo přiložený wrapper `./mvnw`)

Ověření verze:

```bash
java -version
```

## Spuštění

```bash
./mvnw spring-boot:run
```

Aplikace běží na `http://localhost:8080`.

Alternativně sestavení a spuštění jaru:

```bash
./mvnw clean package
java -jar target/zahrada_backend-0.0.1-SNAPSHOT.jar
```

## Databáze

Používá se vestavěná H2 v souborovém režimu - není potřeba nic instalovat.
Data se ukládají do složky `data/` v kořeni projektu.

Webová konzole: `http://localhost:8080/h2-console`

| Pole     | Hodnota                  |
|----------|--------------------------|
| JDBC URL | `jdbc:h2:file:./data/garden` |
| User     | `sa`                     |
| Password | (prázdné)                |

Přesné hodnoty jsou v `src/main/resources/application.properties`.

Schéma se generuje automaticky (`ddl-auto=update`).
Reset databáze - smazat složku a restartovat aplikaci:

```bash
rm -rf data/
```

## Dokumentace API

Swagger UI: `http://localhost:8080/swagger-ui.html`
OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Autentizace

JWT. Token se získá registrací nebo přihlášením a posílá se v hlavičce
`Authorization: Bearer <token>`.

Veřejné endpointy: `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`,
`/h2-console/**`, `/actuator/health`. Vše ostatní vyžaduje platný token.

Registrace:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Eva","lastName":"Dvorakova","role":"WORKER","email":"eva@example.com","password":"heslo1234"}'
```

Přihlášení:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"eva@example.com","password":"heslo1234"}'
```

Role: `OWNER` (majitel zahrady), `WORKER` (pracovník).

## Konfigurace

Klíčové vlastnosti v `application.properties`:

| Vlastnost              | Popis                          |
|------------------------|--------------------------------|
| `app.jwt.secret`       | Base64 klíč pro podpis tokenů  |
| `app.jwt.expiration`   | Platnost tokenu v ms           |
| `spring.datasource.url`| Umístění H2 databáze           |

## Struktura projektu

```
upce/fei/garden/
  config/      - CORS, OpenAPI
  controller/  - REST endpointy
  dto/         - přenosové objekty
  exception/   - vlastní výjimky, globální handler
  model/       - JPA entity
  repository/  - přístup k datům
  security/    - JWT, filtry, konfigurace přístupu
  service/     - business logika
  validation/  - vlastní validační pravidla
```