<div align="center">

# Zahrada - backend

**Spring Boot REST API for a two-sided marketplace connecting garden owners with independent gardeners.**

[Live demo](https://zahrada-frontend.vercel.app) · [Frontend](https://github.com/mirovisus/zahrada_frontend)

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.3-6DB33F?logo=springboot&logoColor=white)
![H2](https://img.shields.io/badge/H2-file--mode-4169E1)
![JWT](https://img.shields.io/badge/JWT-auth-FB015B?logo=jsonwebtokens&logoColor=white)
![Swagger UI](https://img.shields.io/badge/Swagger_UI-OpenAPI-85EA2D?logo=swagger&logoColor=white)

</div>

## What it does

Garden owners post requests for gardening work (lawn mowing, hedge trimming, planting, tree pruning). Gardeners browse a public catalog of open requests and submit bids with a price and short description. The owner reviews the bids, accepts one, and once the accepted gardener finishes the work, they mark it as completed for the owner's approval.

This repository is the API that powers the marketplace: user accounts and JWT authentication, gardens and requests, bids and their lifecycle, and the authorization rules behind every one of those operations. The frontend, [zahrada_frontend](https://github.com/mirovisus/zahrada_frontend), is a separate React application that talks to this API exclusively over REST. Its live demo runs on mocked APIs (MSW) rather than this backend, so it's reachable and predictable without this service running.

## Tech stack

Spring Boot 4.0.3 on Java 17, JPA/Hibernate over an embedded H2 file database, Spring Security with stateless JWT authentication, Bean Validation on request DTOs, a custom exception hierarchy mapped by a global `@RestControllerAdvice`, OpenAPI/Swagger UI for interactive API docs, and JUnit 5 + MockMvc for testing.

## Key backend features

- **Stateless JWT authentication.** `JwtAuthenticationFilter` validates the token from the `Authorization` header and populates the security context before Spring's own `UsernamePasswordAuthenticationFilter` runs. A missing or invalid token doesn't fail the request on the spot - `SecurityConfig`'s authorization rules decide that later, and an unauthenticated request to a protected route gets a 401 from `JwtAuthenticationEntryPoint`. There's no server-side session or token store, so any instance of the API can validate a token on its own.
- **Layered authorization.** Role checks happen at the route level via `@PreAuthorize` (`hasRole('OWNER')` / `hasRole('WORKER')`); ownership of a specific record - is this my garden, my request, my bid - is checked again in the service layer on every access. A foreign record returns 404, not 403, so a user can't even tell whether someone else's id exists; see `docs/TECHNICKA_DOKUMENTACE.md` (in Czech), Security section, for the full reasoning.
- **Explicit request lifecycle.** A request moves through `NOVA → SCHVALENA → PRACE_DOKONCENY → PRACE_SCHVALENY` (or `ZRUSENA` at any point), and every transition is validated in the service layer rather than just implied by the UI. The status enum also reserves `CEKA_NA_PLATBU` and `ZAPLACENA` for a future payment step that isn't wired up yet. Once at least one bid exists on a request, editing or deleting it is blocked with HTTP 409.
- **Cascading bid acceptance.** Accepting a bid is one `@Transactional` service call: the chosen `Proposal` moves to `SCHVALEN`, every other bid on the same request is rejected (`ZAMITNUT`), and the request itself moves to `SCHVALENA` - all atomically, so there's no window where a request ends up with two accepted bids or an inconsistent state if something fails halfway.
- **Custom exception hierarchy with `@RestControllerAdvice`.** Domain errors (`NotFoundException`, `ConflictException`, `ForbiddenException`, `ValidationException`) are thrown directly from services and mapped by a single global handler into one consistent JSON error shape, instead of leaking stack traces or improvising a response per endpoint.
- **Embedded H2 in file mode.** No external database to install or configure - the schema is generated on startup (`ddl-auto=update`) and data persists to a local file between restarts, while integration tests run against a separate in-memory H2 instance so they never touch dev data.

## Getting started

Requires JDK 17 (verify with `java -version`) and Maven, or the bundled wrapper `./mvnw`.

```bash
cp .env.example .env
```

Replace the placeholder values inside with real secrets, e.g.:

```bash
openssl rand -base64 32
```

```bash
./mvnw spring-boot:run
```

The app runs at `http://localhost:8080`.

Or build and run the jar:

```bash
./mvnw clean package
java -jar target/zahrada_backend-0.0.1-SNAPSHOT.jar
```

## Configuration

Key properties in `application.properties`:

| Property | Description |
|----------|--------------|
| `app.jwt.secret` | Base64 signing key for tokens |
| `app.jwt.expiration` | Token validity in ms |
| `spring.datasource.url` | H2 database location |
| `app.upload.dir` | Directory for uploaded garden photos |
| `app.upload.max-size` | Maximum uploaded file size |
| `app.upload.allowed-types` | Allowed `Content-Type` values for uploads |

`app.jwt.secret` (and its `test`-profile counterpart) is read from the `APP_JWT_SECRET` / `APP_JWT_TEST_SECRET` environment variables - the default value baked into `application.properties` is intentionally just a readable placeholder that must never be used outside local development. The variable template lives in `.env.example`; copy it to `.env` and replace the values with your own (e.g. via `openssl rand -base64 32`).

## Database

Uses embedded H2 in file mode - nothing to install. Data is stored in the `data/` folder at the project root.

Web console: `http://localhost:8080/h2-console`

| Field | Value |
|-------|-------|
| JDBC URL | `jdbc:h2:file:./data/garden` |
| User | `sa` |
| Password | (empty) |

Exact values live in `src/main/resources/application.properties`.

Schema is generated automatically (`ddl-auto=update`).
Reset the database - delete the folder and restart the app:

```bash
rm -rf data/
```

## API documentation

Swagger UI: `http://localhost:8080/swagger-ui/index.html`
OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Authentication

JWT. Obtain a token by registering or logging in, then send it in the `Authorization: Bearer <token>` header.

Public endpoints: `/api/auth/**`, `/api/demands/catalog`, `/api/demands/urgencies`, `/api/service-types`, `/uploads/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/h2-console/**`, `/actuator/health`. Everything else requires a valid token (see [Access control matrix](#access-control-matrix) for the full breakdown; exactly how the token is verified and authorization enforced is described in `docs/TECHNICKA_DOKUMENTACE.md` (in Czech), Security section).

Register:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Eva","lastName":"Dvorakova","role":"WORKER","email":"eva@example.com","password":"heslo1234"}'
```

Login:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"eva@example.com","password":"heslo1234"}'
```

Roles: `OWNER` (garden owner), `WORKER` (gardener).

## Access control matrix

The app has two roles: **`OWNER`** (garden owner - posts requests, picks a gardener) and **`WORKER`** (gardener - browses the public catalog of requests and submits bids on them).

| Endpoint | Method | Unauthorized | OWNER | WORKER |
|----------|--------|:---:|:---:|:---:|
| `/api/auth/register`, `/api/auth/login` | POST | ✅ | ✅ | ✅ |
| `/api/service-types` | GET | ✅ | ✅ | ✅ |
| `/api/demands/catalog` | GET | ✅ | ✅ | ✅ |
| `/api/demands/urgencies` | GET | ✅ | ✅ | ✅ |
| `/uploads/**` | GET | ✅ | ✅ | ✅ |
| `/api/profile` | GET | ❌ | ✅ | ✅ |
| `/api/profile/owner` | PUT | ❌ | ✅ | ❌ |
| `/api/profile/worker` | PUT | ❌ | ❌ | ✅ |
| `/api/profile` | DELETE | ❌ | ✅ | ✅ |
| `/api/gardens` (list/detail/create/update/delete) | * | ❌ | ✅ | ❌ |
| `/api/gardens/{id}/photo` | POST, DELETE | ❌ | ✅ | ❌ |
| `/api/demands`, `/api/demands/statistics` | GET | ❌ | ✅ | ❌ |
| `/api/gardens/{gardenId}/demands` | GET, POST | ❌ | ✅ | ❌ |
| `/api/demands/{id}` | GET | ✅ (only in status `NOVA`) | ✅ (own only) | ✅ (only in status `NOVA` or own bid) |
| `/api/demands/{id}` | PUT, DELETE | ❌ | ✅ | ❌ |
| `/api/demands/{demandId}/proposals` | POST | ❌ | ❌ | ✅ |
| `/api/demands/{demandId}/proposals` | GET | ❌ | ✅ (own request only) | ❌ |
| `/api/proposals/my` | GET | ❌ | ❌ | ✅ |
| `/api/proposals/{id}/accept`, `/reject` | POST | ❌ | ✅ | ❌ |
| `/api/proposals/{id}/request-changes` | POST | ❌ | ✅ (own bid only, in status `NOVY`) | ❌ |
| `/api/proposals/{id}` | DELETE | ❌ | ❌ | ✅ (own only, in status `NOVY` or `UPRAVY_POZADOVANY`) |
| `/api/proposals/{id}` | PUT | ❌ | ❌ | ✅ (own only, in status `UPRAVY_POZADOVANY`) |
| `/api/worker/jobs` | GET | ❌ | ❌ | ✅ |
| `/api/demands/{id}/work-report` | POST | ❌ | ❌ | ✅ (only with own accepted bid, in status `SCHVALENA`) |
| `/api/demands/{id}/accept-work` | POST | ❌ | ✅ (own request only, in status `PRACE_DOKONCENY`) | ❌ |
| `/actuator/health` | GET | ✅ | ✅ | ✅ |
| `/actuator/info` and other actuator endpoints | GET | ❌ | ✅ | ✅ |
| `/swagger-ui/**`, `/v3/api-docs/**`, `/h2-console/**` | * | ✅ | ✅ | ✅ |

"✅ (own only)" in the table means ownership of the record is additionally verified in the service layer (`docs/TECHNICKA_DOKUMENTACE.md` (in Czech), Security section, explains why a foreign record returns 404 instead of 403).

## Testing

Tests run against an in-memory H2 database (`test` profile), so they never touch the file-based database in `data/`. Split into unit tests (`src/test/java/.../service/*Test.java`) and integration tests (`src/test/java/.../controller/*IntegrationTest.java`) - a detailed description of both layers, and why they're split, is in `docs/TECHNICKA_DOKUMENTACE.md` (in Czech), Testing strategy section.

Run the whole suite:

```bash
./mvnw test
```

Run a single class or method:

```bash
./mvnw test -Dtest=DemandServiceTest
./mvnw test -Dtest=DemandServiceTest#someMethodName
```

> Test classes must end in `Test`/`Tests`, not `IT` - Maven Surefire (bound to the `test` phase) otherwise skips them.

## Project structure

```
upce/fei/garden/
  config/      - CORS, OpenAPI, request-logging filter, service-type lookup seed, static /uploads/**
  controller/  - REST endpoints
  dto/         - data transfer objects
  exception/   - custom exceptions, global handler
  model/       - JPA entities
  repository/  - data access
  security/    - JWT, filters, access configuration
  service/     - business logic
  validation/  - custom validation rules
```

## About the project

Built as a semester project for KIT/BRPW2 (Ročníkový projekt II) at the Faculty of Electrical Engineering and Informatics, University of Pardubice, supervised by Ing. Lukáš Čegan, Ph.D.
