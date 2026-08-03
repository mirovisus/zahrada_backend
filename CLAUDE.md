# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

REST API for a garden-management / gardening-work-request app. Spring Boot 4.0.3, Java 17, H2 (file-mode), JWT auth. Package root: `upce.fei.garden`. The README (`README.md`, in Czech) is the canonical spec — it documents the access matrix, security model, validation rules, and data model in detail; consult it before making changes to auth, authorization, or entity relationships rather than re-deriving them from code.

## Commands

```bash
./mvnw spring-boot:run        # run the app (http://localhost:8080)
./mvnw clean package          # build jar
java -jar target/zahrada_backend-0.0.1-SNAPSHOT.jar
./mvnw test                   # run all tests
./mvnw test -Dtest=DemandServiceTest                     # single test class
./mvnw test -Dtest=DemandServiceTest#someMethodName      # single test method
```

Test classes **must** end in `Test`/`Tests`, not `IT` — Surefire (bound to the `test` phase, which this project uses) skips `IT`-suffixed classes since that suffix is the Failsafe/`integration-test` convention.

Tests run against an in-memory H2 database via the `test` profile (`src/test/resources/application-test.properties`) and never touch the file-based dev database in `data/`.

- Unit tests: `src/test/java/.../service/*Test.java` — JUnit 5 + Mockito, no Spring context, repositories and `CurrentUserService` mocked.
- Integration tests: `src/test/java/.../controller/*IntegrationTest.java` — `@SpringBootTest` + `MockMvc` against a real context; auth is not bypassed — tests register users via `/api/auth/register` and send the real returned JWT.

Dev database is file-based H2 at `data/garden.mv.db`; reset by deleting the `data/` folder and restarting (schema regenerates via `ddl-auto=update`). H2 console at `/h2-console` (JDBC URL `jdbc:h2:file:./data/garden`, user `sa`, empty password). Swagger UI at `/swagger-ui.html`, OpenAPI JSON at `/v3/api-docs`.

## Architecture

Strict layered architecture — each layer only calls the layer directly below it:

```
Controller (@RestController, @PreAuthorize)  →  Service (@Service, @Transactional)  →  Repository (Spring Data JPA)  →  Entity (model/)
```

- **`dto/`** — API boundary objects. Controllers never accept or return entities directly. Entity↔DTO conversion is done by package-private `*Mapper` classes living inside `service/` (e.g. `DemandMapper`), keeping the `*Service` class itself limited to business logic.
- **`security/`** — `JwtAuthenticationFilter` (validates JWT from the `Authorization` header, populates `SecurityContext`) runs before `UsernamePasswordAuthenticationFilter`; an invalid/missing token does *not* stop the request — rejection is decided later by `SecurityConfig` authorization rules (401 via `JwtAuthenticationEntryPoint`). `JwtService` handles token sign/validate. `CurrentUserService` is the uniform way for any service class to get the logged-in user.
- **`exception/`** — `NotFoundException`, `ConflictException`, `ForbiddenException`, `ValidationException`, all mapped by `GlobalExceptionHandler` to a single `ApiError` response shape.
- **`validation/`** — custom Bean Validation rules beyond the standard annotations, split into `validation/rules` (annotations) and `validation/validator` (`ConstraintValidator` impls): `@FutureOrToday` (date not in the past) and `@ValidCzechPhone` (`+420` + 9 digits, optional field).
- **`config/`** — cross-cutting, domain-independent config: CORS (frontend origin `http://localhost:5173` only, see `CorsConfig`), OpenAPI/Swagger, `RequestLoggingFilter` (logs every `/api/**` request at INFO — method, path, status, duration; never logs headers/body, so no secrets leak into logs), `ServiceTypeDataInitializer` (seeds the service-type lookup table at startup), `H2ConsoleConfig` (registers the H2 web console servlet, disabled under the `test` profile).

### Ownership → 404, not 403

If an owner accesses another owner's garden/demand, or a worker accesses a demand outside status `NOVA`, the service layer throws `NotFoundException` (404) — never `ForbiddenException` (403). This is a deliberate convention across `GardenService`, `DemandService`, and `ProposalService` to avoid confirming a foreign record's existence. Pure role mismatches (e.g. a `WORKER` hitting an `OWNER`-only endpoint) are instead blocked earlier by `@PreAuthorize` and correctly return 403. When adding new ownership-scoped endpoints, follow this same split.

### Domain model

`Owner` and `Worker` both extend an abstract `User` entity via `@Inheritance(strategy = JOINED)` — three joined tables (`users`, `owner`, `worker`) behind one class hierarchy in code.

Demand lifecycle (`Demand.status`): `NOVA → SCHVALENA → CEKA_NA_PLATBU → ZAPLACENA → PRACE_DOKONCENY → PRACE_SCHVALENY` (or `ZRUSENA` at any point). Reaching `SCHVALENA` happens by accepting a proposal (`ProposalService#accept`), which simultaneously rejects (`ZAMITNUT`) every other proposal on that demand.

`WorkReport`, `Review`, `ProposalComment`, and `GardenPhoto` entities (and their DTOs under `dto/workreport`, `dto/review`) exist in the data model for a future stage but currently have **no REST endpoint** — don't assume API access to them exists.

Two roles only: `OWNER` and `WORKER`. See the README's access matrix for the full endpoint-by-role table before adding or changing an endpoint's authorization.

Business validation that depends on related-record state (not expressible as a field annotation) lives directly in services, e.g. `DemandService#ensureNoProposals` (can't edit/delete a demand once it has a proposal → 409) and `ProposalService#create` (demand must be `NOVA`; a worker can't submit twice).

## Uncommitted / pending changes

At present the working tree has pending changes not yet committed: `pom.xml` and `application.properties` modifications, and a new untracked file `src/main/java/upce/fei/garden/config/H2ConsoleConfig.java` (registers `/h2-console/*` outside the `test` profile). Be aware of this when reasoning about "current" vs. "committed" state.
