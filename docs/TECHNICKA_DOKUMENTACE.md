# Technická dokumentace - Zahrada (backend)

## 1. Účel aplikace

Zahrada je REST API pro webovou/mobilní aplikaci, která propojuje **vlastníky zahrad** s
**zahradníky**. Vlastník zahrady zadá poptávku (jaké práce potřebuje a s jakou naléhavostí),
zahradníci na ni mohou podat návrh (cenová nabídka a popis), vlastník si vybere jeden z návrhů a
schválí ho - ostatní návrhy se automaticky zamítnou. Aplikace dále eviduje profily obou rolí,
zahrady vlastníka (včetně fotografie) a číselník typů zahradnických služeb.

Cílem backendu je poskytnout bezpečné, dobře zdokumentované a otestované REST API, které
frontend (samostatný projekt) volá přes JSON přes HTTP.

## 2. Použité technologie

| Vrstva | Technologie | Proč zvoleno |
|---|---|---|
| Jazyk | Java 17 | moderní LTS verze vhodná pro Spring Boot 4 |
| Backend | Spring Boot 4.0.3 | rychlé vytváření REST API a správná podpora pro Java 17 |
| API a bezpečnost | Spring MVC, Spring Security, JWT | standardní řešení pro REST endpointy, autentizaci a autorizaci |
| Data | Spring Data JPA + Hibernate, H2 | jednoduchá práce s databází bez nutnosti samostatné instalace |
| Validace | Bean Validation + vlastní pravidla | snadná validace vstupů na úrovni DTO |
| Dokumentace | OpenAPI / Swagger | automatická dokumentace API z anotací |
| Testování | JUnit 5, Mockito, Spring Boot Test | běžný a efektivní stack pro unit i integrační testy |
| Build | Maven (`./mvnw`) | jednoduché spuštění buildu bez lokální instalace Mavenu |

## 3. Architektura

Aplikace je postavena jako klasická vrstvená monolitická architektura:

```
HTTP request
    |
    v
Controller (REST, @RestController)      - přijme/vrátí DTO, autorizace @PreAuthorize
    |
    v
Service (@Service, @Transactional)      - business logika, vlastnictví záznamů, validace
    |
    v
Repository (Spring Data JPA)            - dotazy nad databází
    |
    v
Entity (@Entity)                        - JPA mapování na tabulky
```

Každá vrstva zná jen tu bezprostředně pod sebou - controller nikdy nevolá repository přímo a
nikdy nepracuje s entitou.

Balíčky (`upce.fei.garden.*`):

- **`controller`** - REST endpointy, mapování HTTP metod na service volání, autorizace přes
  `@PreAuthorize`, Swagger anotace (`@Tag`, `@Operation`, `@Parameter`).
- **`service`** - business logika. Každá doménová oblast (Auth, Garden, Demand, Proposal,
  Profile, ServiceType) má vlastní service třídu. Vedle toho `FileStorageService` řeší ukládání
  nahraných souborů na disk (viz [Bezpečnost](#5-bezpečnost)) a je service vrstvě k dispozici
  jako běžná závislost.
- **`dto`** - přenosové objekty pro request/response; entity se nikdy nevrací přímo z
  controlleru ven z aplikace.
- **`model`** - JPA entity a jejich enum stavy (`DemandStatus`, `ProposalStatus`, `DemandUrgency`,
  `UserRole`).
- **`repository`** - rozhraní `JpaRepository`/`JpaSpecificationExecutor` s odvozenými i
  vlastními (`@Query`) dotazy.
- **`security`** - JWT filtr a služba, `CurrentUserService` (jednotný přístup k přihlášenému
  uživateli), `SecurityConfig` (pravidla přístupu k jednotlivým cestám).
- **`exception`** - vlastní výjimky a `GlobalExceptionHandler`, který je centrálně převádí na
  jednotný formát chybové odpovědi a loguje.
- **`validation`** - vlastní Bean Validation pravidla (anotace + `ConstraintValidator`).
- **`config`** - průřezová konfigurace nezávislá na doméně: CORS (`CorsConfig`), OpenAPI
  (`OpenApiConfig`), logovací filtr (`RequestLoggingFilter`), naplnění číselníku služeb při
  startu (`ServiceTypeDataInitializer`), registrace H2 konzole mimo testovací profil
  (`H2ConsoleConfig`) a statický resource handler pro nahrané fotografie (`WebMvcConfig`).

Hlavní doménové oblasti a jejich endpointy: autentizace (`/api/auth`), profil
(`/api/profile`), zahrady včetně jejich fotografie (`/api/gardens`), poptávky (`/api/demands`,
veřejný katalog `/api/demands/catalog`, číselník naléhavosti `/api/demands/urgencies`) a návrhy
zahradníků (`/api/demands/{id}/proposals`, `/api/proposals/**`). Entity `WorkReport`, `Review`,
`ProposalComment` a `GardenPhoto` jsou v datovém modelu připravené pro navazující etapy vývoje,
ale zatím nemají REST endpoint (viz [Známá omezení](#11-známá-omezení)).

## 4. Datový model

Klíčové entity a vztahy:

```mermaid
erDiagram
    OWNER ||--o{ GARDEN : vlastní
    GARDEN ||--o{ DEMAND : obsahuje
    GARDEN ||--o{ GARDEN_PHOTO : má
    DEMAND }o--o{ SERVICE_TYPE : vyžaduje
    DEMAND ||--o{ PROPOSAL : přijímá
    WORKER ||--o{ PROPOSAL : podává
    PROPOSAL ||--o{ PROPOSAL_COMMENT : má
    DEMAND ||--o| WORK_REPORT : má
    WORKER ||--o{ WORK_REPORT : vytváří
    DEMAND ||--o| REVIEW : má
    OWNER ||--o{ REVIEW : píše
    WORKER ||--o{ REVIEW : dostává
```

`Owner` a `Worker` dědí společné údaje (e-mail, heslo, jméno, telefon, adresa URL avataru, datum
registrace) z abstraktní entity `User` přes `@Inheritance(strategy = JOINED)` - v databázi tak
vzniknou tři propojené tabulky (`users`, `owner`, `worker`) spojené primárním klíčem, ale v kódu
jde o jednu hierarchii tříd. 

**Životní cyklus poptávky** (`Demand.status`, enum `DemandStatus`):

```mermaid
stateDiagram-v2
    [*] --> NOVA
    NOVA --> SCHVALENA : přijetí návrhu
    SCHVALENA --> PRACE_DOKONCENY
    PRACE_DOKONCENY --> PRACE_SCHVALENY
    NOVA --> ZRUSENA
    SCHVALENA --> ZRUSENA
    PRACE_SCHVALENY --> [*]
    ZRUSENA --> [*]
```

Do stavu `NOVA` se poptávka dostane vytvořením a v tomto stavu je jediná viditelná ve veřejném
katalogu pro zahradníky i jediná, na kterou lze podat návrh (`ProposalService#create`). Přechod
do `SCHVALENA` nastává výhradně přijetím jednoho z návrhů.

**Životní cyklus návrhu** (`Proposal.status`, enum `ProposalStatus`):

```mermaid
stateDiagram-v2
    [*] --> NOVY
    NOVY --> SCHVALEN : accept
    NOVY --> ZAMITNUT : reject / zamítnutí ostatních při accept
    NOVY --> [*] : withdraw (smazání)
    SCHVALEN --> [*]
    ZAMITNUT --> [*]
```

`ProposalService#accept` provede všechny změny najednou v jedné transakci. Pokud je jeden návrh
přijat, tento návrh se označí jako `SCHVALEN`, všechny ostatní návrhy k téže poptávce se
zamítnou a sama poptávka přejde do stavu `SCHVALENA`. 

## 5. Bezpečnost

Aplikace používá jednoduchý a přehledný model zabezpečení:

- Přístup je chráněn pomocí JWT a rolí (`OWNER`, `WORKER`).
- Klient pošle token v hlavičce `Authorization: Bearer <token>`.
- `JwtAuthenticationFilter` ověří token, ale samotné rozhodnutí o přístupu dělá až `SecurityConfig`
a `@PreAuthorize` na controlleru.
- Veřejné cesty jsou např. registrace, katalog poptávek, seznam typů služeb, uploady,
  Swagger a health endpoint.
- Přístup k cizímu záznamu vrací `404` místo `403`, aby se neprozradila existence daného záznamu.
- Hesla se ukládají jako BCrypt hash a JWT je bezstavové a má omezenou platnost.
- CORS je povolen jen pro frontend na `http://localhost:5173`.
- Nahrané fotografie se kontrolují před uložením - zkoumá se jejich typ i obsah, aby se zabránilo
  zneužití typu uploadu nebo path traversal.
- `/actuator/health` je veřejný, ostatní actuator endpointy vyžadují autentizaci.

## 6. Validace

Aplikace kombinuje tři úrovně validace:

1. **Standardní Bean Validation** - `@NotBlank`, `@Size`, `@Email`, `@Positive`, `@Pattern` atd.
   na request DTO, vyhodnocované automaticky přes `@Valid` v controlleru.
2. **Vlastní deklarativní pravidla** (balíček `validation`, dělený na `validation.rules` -
   anotace a `validation.validator` - implementace `ConstraintValidator`):
   - `@ValidCzechPhone` - telefon ve formátu `+420` a devět číslic (mezery volitelné), `null`
     nebo prázdný řetězec je platný, protože telefon je nepovinný údaj.
     
3. **Programová (business) validace** - pravidla, která závisí na stavu souvisejících záznamů
   v databázi nebo na obsahu binárních dat, ne jen na tvaru jednoho DTO, a proto je nelze
   vyjádřit deklarativní anotací nad polem. 
   
   Příklady: poptávku, ke které už existuje alespoň
   jeden návrh, nelze upravit ani smazat (`DemandService#ensureNoProposals`, HTTP 409); na
   poptávku mimo stav `NOVA` nelze podat návrh a jeden zahradník smí na poptávku podat jen jeden
   návrh (`ProposalService#create`); návrh lze přijmout/zamítnout/odvolat jen ve stavu `NOVY`
   (`ProposalService#ensureNovy`); nahraný soubor fotografie musí projít kontrolou velikosti,
   deklarovaného typu i skutečné signatury obsahu (`FileStorageService#store`, viz
   [Bezpečnost](#5-bezpečnost)). Tato pravidla vyhazují `ConflictException` (409) nebo
   `ValidationException` (400) a jsou zdokumentovaná Javadocem přímo u dané metody.

## 7. Zpracování chyb

`GlobalExceptionHandler` (`@RestControllerAdvice`) centrálně převádí všechny výjimky na jednotný
formát odpovědi `ApiError` (`timestamp`, `status`, `error`, `message`, `path`, volitelně
`fieldErrors`), takže klient nikdy nedostane surový stack trace ani netypizovanou chybu.

| Výjimka / situace | HTTP status |
|---|---|
| `NotFoundException` | 404 |
| `NoResourceFoundException`, `NoHandlerFoundException` (neexistující cesta) | 404 |
| `AuthenticationException` (neplatné přihlašovací údaje) | 401 |
| `ForbiddenException` | 403 |
| `AccessDeniedException` (zamítnutí ze Spring Security, např. `@PreAuthorize`) | 403 |
| `ConflictException` | 409 |
| `ValidationException` (vlastní programová validace) | 400 (s `fieldErrors`) |
| `MethodArgumentNotValidException` (Bean Validation na `@Valid` DTO) | 400 (s `fieldErrors`) |
| `HttpMessageNotReadableException` (poškozený/neplatný JSON) | 400 |
| `MaxUploadSizeExceededException` (soubor větší než `spring.servlet.multipart.max-file-size`) | 400 |
| `HttpRequestMethodNotSupportedException` (nepodporovaná HTTP metoda na dané cestě) | 405 |
| cokoliv jiné (`Exception`) | 500 |

## 8. Logování a monitoring

- Aplikace používá SLF4J/Logback. Hlavní logger je `upce.fei.garden`.
- Důležité akce se logují na úrovni `INFO` (registrace, vytvoření nebo změna dat, upload souboru,
  přijetí nebo zamítnutí návrhu).
- Neoprávněné akce nebo porušení pravidel se logují jako `WARN`.
- `RequestLoggingFilter` zaznamenává každý request na `/api/**` s metodou, cestou, stavem a
  dobou zpracování. Neukládá hlavičky ani tělo požadavku.
- `/actuator/health` je veřejný a umožňuje jednoduché monitorování stavu aplikace.

## 9. Testovací strategie

Projekt má dvě vrstvy testů a používá izolovanou in-memory databázi v testovém profilu.

- **Unit testy** pokrývají business logiku service vrstvy. Testují hlavní scénáře i chyby.
- **Integrační testy** ověřují celý tok přes HTTP, Spring Security, controller, service a databázi.
- Kromě toho existuje jednoduchý smoke test, který ověřuje, že se aplikace správně spustí.
- Testy jsou pojmenované podle konvence `*Test` nebo `*Tests`, aby je Maven spustil správně.
- Podrobnosti o spuštění testů jsou v `README.md`.

## 10. Známá omezení

- Entity `WorkReport`, `Review` a `ProposalComment` jsou v datovém modelu připravené pro
  navazující etapy vývoje (evidence provedené práce, hodnocení zahradníka, komentáře k návrhu),
  ale zatím k nim není žádný REST endpoint - nejsou tedy ani v Swagger dokumentaci.
- Entita `GardenPhoto` existuje v modelu (viz ER diagram výše) pro budoucí galerii více
  fotografií na zahradu, ale aktuální implementace nahrávání fotografie (`POST /api/gardens/{id}/photo`)
  ji nepoužívá - ukládá jen jednu URL do pole `Garden.mainPhotoUrl`.
- Pole `User.avatarUrl` v entitě existuje, ale nemá vlastní upload endpoint (na rozdíl od
  `Garden.mainPhotoUrl`) - avatar tak lze zatím nastavit jen nepřímo, ne přes API.
