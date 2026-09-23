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

Swagger UI: `http://localhost:8080/swagger-ui/index.html`
OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Autentizace

JWT. Token se získá registrací nebo přihlášením a posílá se v hlavičce
`Authorization: Bearer <token>`.

Veřejné endpointy: `/api/auth/**`, `/api/demands/catalog`, `/api/demands/urgencies`,
`/api/service-types`, `/uploads/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/h2-console/**`,
`/actuator/health`. Vše ostatní vyžaduje platný token (podrobná matice viz
[Role a přístupová matice](#role-a-přístupová-matice); jak přesně se token ověřuje a jak se
vynucuje autorizace popisuje `docs/TECHNICKA_DOKUMENTACE.md`, sekce Bezpečnost).

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

| Vlastnost                     | Popis                                          |
|--------------------------------|-------------------------------------------------|
| `app.jwt.secret`               | Base64 klíč pro podpis tokenů                   |
| `app.jwt.expiration`           | Platnost tokenu v ms                            |
| `spring.datasource.url`        | Umístění H2 databáze                            |
| `app.upload.dir`               | Adresář pro nahrané fotografie zahrad           |
| `app.upload.max-size`          | Maximální velikost nahrávaného souboru          |
| `app.upload.allowed-types`     | Povolené `Content-Type` pro nahrávaný soubor     |

Klíč `app.jwt.secret` (a jeho testovací protějšek pro profil `test`) se od teď načítá z proměnných
prostředí `APP_JWT_SECRET` / `APP_JWT_TEST_SECRET` - výchozí hodnota v `application.properties` je
záměrně jen čitelný placeholder, který se nesmí použít mimo lokální vývoj. Vzor proměnných je v
`.env.example`; zkopírujte jej do `.env` a hodnoty nahraďte vlastními (např. `openssl rand -base64 32`).

## Struktura projektu

```
upce/fei/garden/
  config/      - CORS, OpenAPI, request-logging filtr, seed číselníku služeb, statické /uploads/**
  controller/  - REST endpointy
  dto/         - přenosové objekty
  exception/   - vlastní výjimky, globální handler
  model/       - JPA entity
  repository/  - přístup k datům
  security/    - JWT, filtry, konfigurace přístupu
  service/     - business logika
  validation/  - vlastní validační pravidla
```

## Role a přístupová matice

Aplikace má dvě role: **`OWNER`** (vlastník zahrady - zadává poptávky, vybírá zahradníka) a
**`WORKER`** (zahradník - prohlíží veřejný katalog poptávek a podává na ně návrhy).

| Endpoint                                  | Metoda | Neautorizovaný | OWNER | WORKER |
|--------------------------------------------|--------|:---:|:---:|:---:|
| `/api/auth/register`, `/api/auth/login`    | POST   | ✅ | ✅ | ✅ |
| `/api/service-types`                       | GET    | ✅ | ✅ | ✅ |
| `/api/demands/catalog`                     | GET    | ✅ | ✅ | ✅ |
| `/api/demands/urgencies`                   | GET    | ✅ | ✅ | ✅ |
| `/uploads/**`                              | GET    | ✅ | ✅ | ✅ |
| `/api/profile`                             | GET    | ❌ | ✅ | ✅ |
| `/api/profile/owner`                       | PUT    | ❌ | ✅ | ❌ |
| `/api/profile/worker`                      | PUT    | ❌ | ❌ | ✅ |
| `/api/profile`                             | DELETE | ❌ | ✅ | ✅ |
| `/api/gardens` (list/detail/create/update/delete) | *   | ❌ | ✅ | ❌ |
| `/api/gardens/{id}/photo`                  | POST, DELETE | ❌ | ✅ | ❌ |
| `/api/demands`, `/api/demands/statistics`  | GET    | ❌ | ✅ | ❌ |
| `/api/gardens/{gardenId}/demands`          | GET, POST | ❌ | ✅ | ❌ |
| `/api/demands/{id}`                        | GET    | ✅ (jen stav `NOVA`) | ✅ (jen svá) | ✅ (jen stav `NOVA` nebo vlastní návrh) |
| `/api/demands/{id}`                        | PUT, DELETE | ❌ | ✅ | ❌ |
| `/api/demands/{demandId}/proposals`        | POST   | ❌ | ❌ | ✅ |
| `/api/demands/{demandId}/proposals`        | GET    | ❌ | ✅ (jen svá poptávka) | ❌ |
| `/api/proposals/my`                        | GET    | ❌ | ❌ | ✅ |
| `/api/proposals/{id}/accept`, `/reject`    | POST   | ❌ | ✅ | ❌ |
| `/api/proposals/{id}/request-changes`      | POST   | ❌ | ✅ (jen svůj návrh, ve stavu `NOVY`) | ❌ |
| `/api/proposals/{id}`                      | DELETE | ❌ | ❌ | ✅ (jen svůj, ve stavech `NOVY` nebo `UPRAVY_POZADOVANY`) |
| `/api/proposals/{id}`                      | PUT    | ❌ | ❌ | ✅ (jen svůj, ve stavu `UPRAVY_POZADOVANY`) |
| `/api/worker/jobs`                         | GET    | ❌ | ❌ | ✅ |
| `/api/demands/{id}/work-report`            | POST   | ❌ | ❌ | ✅ (jen s vlastním přijatým návrhem, ve stavu `SCHVALENA`) |
| `/api/demands/{id}/accept-work`            | POST   | ❌ | ✅ (jen svá poptávka, ve stavu `PRACE_DOKONCENY`) | ❌ |
| `/actuator/health`                         | GET    | ✅ | ✅ | ✅ |
| `/actuator/info` a ostatní actuator        | GET    | ❌ | ✅ | ✅ |
| `/swagger-ui/**`, `/v3/api-docs/**`, `/h2-console/**` | *  | ✅ | ✅ | ✅ |

"✅ (jen svá/svůj)" v tabulce znamená, že vlastnictví záznamu se navíc ověřuje v servisní vrstvě
(proč cizí záznam vrací 404 místo 403 vysvětluje `docs/TECHNICKA_DOKUMENTACE.md`, sekce Bezpečnost).

## Testování

Testy běží proti in-memory H2 databázi (profil `test`), takže se nikdy nedotknou souborové
databáze ve `data/`. Rozdělené jsou na unit testy (`src/test/java/.../service/*Test.java`) a
integrační testy (`src/test/java/.../controller/*IntegrationTest.java`) - podrobný popis obou
vrstev a proč jsou rozdělené je v `docs/TECHNICKA_DOKUMENTACE.md`, sekce Testovací strategie.

Spuštění celé sady:

```bash
./mvnw test
```

Spuštění jedné třídy nebo metody:

```bash
./mvnw test -Dtest=DemandServiceTest
./mvnw test -Dtest=DemandServiceTest#someMethodName
```

> Třídy testů musí končit na `Test`/`Tests`, ne na `IT` - Maven Surefire (spouštěný fází `test`)
> jinak takové testy přeskočí.

## TODO / budoucí úpravy

- Zkontrolovat, zda je vlastní validátor `@FutureOrToday` a jeho implementace mrtvý kód. Pokud
  není nikde použit, odstranit jej a všechny odkazy na něj v projektu.

## Technická dokumentace

Podrobný popis architektury, bezpečnostního modelu, validace, zpracování chyb a testovací
strategie je v [`docs/TECHNICKA_DOKUMENTACE.md`](docs/TECHNICKA_DOKUMENTACE.md).
