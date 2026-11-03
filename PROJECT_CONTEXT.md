# PROJECT_CONTEXT.md

## 1. Purpose
Corporate e-learning platform (SITP) for a company in Cameroon. Two audiences: **interns** (free, time-boxed access) and **outsiders** (paid per course, lifetime ownership). Built as a deliberate Spring Boot practice project — architecture reasoning is a goal, not just code.

## 2. Stack & Versions
- **Java 17**, Spring Boot 4.1.1 (Maven, `mvnw`)
- Spring Data JPA, Spring Security, Spring Mail, springdoc-openapi 3.1.1
- PostgreSQL (`sitp_demo`); H2 in-memory for tests
- Lombok (`@Getter/@Setter/@Builder` on all entities/DTOs)
- Frontend: Angular 21 + Tailwind 4 in `sitp-web/` (separate, proxy to `:8080`)

## 3. Architecture
**Package-by-feature** (vertical slices). Each feature owns its full stack: `model/`, `repository/`, `service/`, `controller/`, `dto/`, `exception/`. `common/` holds cross-cutting machinery (error shape, video seam, code generator).

**Auth flow:** `SessionAuthFilter` (custom `OncePerRequestFilter`) reads `sitp_session` cookie → `AuthService.resolveSessionUser(token)` → sets `SecurityContext` with principal = `Long userId` + `ROLE_<enum>`. Stateless (no HTTP sessions). CSRF disabled. Single active session per user (login invalidates prior sessions).

**Data flow:** Controller → Service (`@Transactional`) → JPA Repository → PostgreSQL. DTOs are immutable records/built via `fromEntity`. Entities use `@Builder` + Lombok.

**Email:** `EmailSender` interface with two impls — `ConsoleEmailSender` (default, `app.mail.enabled=false`) and `GmailEmailSender` (`@ConditionalOnProperty`). Verification codes are 6-digit, BCrypt-hashed, 15-min TTL.

**Captcha:** `CaptchaService` issues math challenges (BCrypt-hashed answer, 10-min TTL, single-use). Required on register + login.

## 4. Folder Structure
- `src/main/java/com/example/sitp/auth/` — register, login, logout, session, captcha
- `.../user/` — User entity, Role/AccountType/Gender/Track enums, UserRepository
- `.../course/` — Course, Chapter, Audience enum, CourseAccessGuard, catalog + detail
- `.../access/` — Session, RedeemableCode, RedeemableCodeStatus, code generation/redemption
- `.../verification/` — email verification, password reset, VerificationCode, EmailSender
- `.../common/` — ErrorResponse, GlobalExceptionHandler, SecureCodeGenerator, VideoStorageService, OpenApiConfig
- `src/test/java/...` — MockMvc integration tests + repository tests (H2)
- `sitp-web/` — Angular 21 frontend (waves 1–3 closed)
- `notes/` — living project memory: `SESSION-MEMORY.md` (snapshot, overwritten each session), `PROMPT-HISTORY.md` (append-only dev-prompt diary), `GIT-RULES.md` (locked commit/push law), `ARCHITECTURE.md` (locked decisions), `TODO.md` (slice ladder), per-slice folders
- `SITP-Docs/` — original contract (requirements, api-spec, ai-context)

## 5. Core Entities
- **User**: id, email (unique), username (unique, lowercase), passwordHash, role (BOSS/TRAINER/ADMIN_ASSISTANT/TRAINEE), accountType (INTERN/OUTSIDER), firstName, lastName, phoneNumber?, address?, country?, gender?, birthdate?, profilePictureUrl?, enabled (default false), createdAt, updatedAt
- **Session**: id, user (ManyToOne), token (unique), active (default true), expiresAt
- **Course**: id, title, description, price (BigDecimal), targetAudience (INTERN/OUTSIDER)
- **Chapter**: id, title, videoUrl, position, course (ManyToOne)
- **RedeemableCode**: id, code (unique), course (ManyToOne), claimant (ManyToOne?), validityDays?, status (ACTIVE/REVOKED), redeemedAt?, createdAt
- **VerificationCode**: id, username, purpose (VERIFY_EMAIL/RESET_PASSWORD), codeHash, expiresAt, used, createdAt
- **CaptchaChallenge**: id, question, answerHash, expiresAt, used, createdAt

## 6. Key Endpoints
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Form-urlencoded. Creates disabled user, sends email code. |
| POST | `/api/auth/login` | Form-urlencoded. Validates captcha + creds, sets `sitp_session` cookie. |
| GET | `/api/auth/captcha` | Returns `{id, question}` math challenge. |
| GET | `/api/auth/me` | Current user from session cookie. |
| POST | `/api/auth/logout` | Invalidates session, clears cookie. |
| GET | `/api/captcha` | Same as auth/captcha (public). |
| POST | `/api/verification/verify-email` | Form. Enables user after code check. |
| POST | `/api/verification/forgot-password` | Form. Sends reset code (silent if email unknown). |
| POST | `/api/verification/reset-password` | Form. Resets password, invalidates sessions. |
| GET | `/api/courses` | Public catalog. Price only if authenticated. |
| GET | `/api/courses/{id}` | Detail + chapters. Requires redeemable code access. |
| GET | `/api/courses/mine` | The logged-in user's courses (from their claimed codes). |
| POST | `/api/codes?courseId&count` | Generate codes (BOSS/ADMIN_ASSISTANT only). |
| POST | `/api/codes/{code}/redeem` | Redeem code → grants course access. |
| POST | `/api/codes/{code}/revoke` | Revoke code (BOSS/ADMIN_ASSISTANT only) — one row edit ACTIVE→REVOKED, claimant/redeemedAt untouched. |
| POST | `/api/courses/trainer` | Trainer-only create (form: title, description?, targetAudience, price?) → 201; trainer set from principal. |
| GET | `/api/courses/trainer/mine` | Trainer's own courses only (Trainer role). |
| PUT | `/api/courses/{id}` | Trainer updates OWN course (Trainer role + owner-only) — title/description/labels/thumbnailUrl/price ONLY, `targetAudience` not editable. |
| DELETE | `/api/courses/{id}` | Trainer deletes OWN course — 409 while redeemable codes exist for it; deletes its chapters first. |
| GET | `/api/courses/trainer/{id}` | Trainer reads OWN course detail + episodes WITHOUT needing a redeemable code (the studio's library view). |
| POST | `/api/courses/{id}/chapters` | Trainer adds an episode to OWN course — title/description/labels/videoUrl/thumbnailUrl/position (position auto-appends last). 201. |
| PUT | `/api/chapters/{id}` | Trainer updates an episode of OWN course (all fields incl. position). |
| DELETE | `/api/chapters/{id}` | Trainer deletes an episode of OWN course — 204, no code-refusal (episodes never carry codes). |

## 7. Conventions
- **Naming**: packages lowercase, classes PascalCase, DTOs suffixed `Request`/`Response`, exceptions suffixed `Exception`.
- **Error handling**: `GlobalExceptionHandler` (`@RestControllerAdvice`) maps domain exceptions → `ErrorResponse(status, message, timestamp)`. All exceptions are bare `RuntimeException` subclasses (no fields).
- **DTO pattern**: Immutable, `@Getter @Builder`, static `fromEntity(entity)` factory. Records for simple shapes (ErrorResponse).
- **Auth approach**: Session cookie (`sitp_session`, HttpOnly, Secure, SameSite=Lax, 7-day). Principal is `Long userId`. Roles as `ROLE_<ENUM>`.
- **Validation**: Jakarta `@Valid @ModelAttribute` on form DTOs. Custom setters normalize (trim/lowercase) email + username.
- **Code style**: Constructor injection (no `@Autowired`). `@Transactional` on service write methods. Lombok for boilerplate.

## 8. Important Decisions
- **Session over JWT** (locked): simpler revocation, no token storage on client.
- **Users born disabled**: email verification required before login. `AccountDisabledException` if not enabled.
- **Single active session**: login calls `invalidateByUserId` — financial anti-sharing measure.
- **Price visibility**: `priceVisible` flag in DTOs; price null for anonymous. Frontend never guesses.
- **Course detail access** (0d landed 2026-10-13): `CourseAccessGuard` checks `RedeemableCode`. Two halves — (1) exact: a claimed ACTIVE code covering THIS course (0f: expiry computed at read from `redeemedAt` + `validityDays`; a null `validityDays` = the deliberate lifetime null); (2) intern (0d): an INTERN holding ONE unexpired claimed code over an INTERN-targeted course opens EVERY INTERN-targeted course; an OUTSIDER only the exact course. No cover → `NoAccessGrantedException` (403). Intern minting REQUIRES a validity in days (1–3650, 0e); outsider mint forces the null.
- **Revoke door + duplicate law (0g, landed 2026-10-15):** revoke = one row edit ACTIVE→REVOKED (claimant/redeemedAt stay — the crossed-out row history); the claimant loses access at read (guard scores ACTIVE only). Re-revoking refuses 409 "This code was revoked." Duplicate redeem: an OUTSIDER claiming a code for a course they already access-hold → 409 `CourseAlreadyHeldException` ("You already have access to this course.", code NOT burned); an INTERN claiming again is ALLOWED (renewal semantics, requirements §3).
- **Trainer CRUD doors (0a–0e answers → code, Phase A 2026-10-15 + Phase B/C 2026-10-20):** `Course.trainer` (`@ManyToOne`, nullable — pre-slice courses + Boss-unassigned). Create/list/update/delete doors are TRAINER-role-only (`requireTrainerId` → else 403); writes are owner-only via `getCourseForTrainer` (Phase A's `findByTrainerIdAndId` as the isolation verdict; unknown → 404) → `CourseOwnershipException` 403 "This course belongs to another trainer." Update does NOT touch `targetAudience` (audience drives codes — decision flagged). Delete refuses 409 `CourseDeletionBlockedException` "This course has codes handed out and cannot be deleted yet." while `findByCourseId` returns rows (never crashes the FK — the crossed-out-rows law), else wipes the course's chapters then the course (204).
- **The TRAINER STUDIO (the Slice-6 correction, #161 2026-10-21, the developer's direction: "like a YouTube account — add a video, library, delete them, description, labels, thumbnails"):** the original UI phase (the old "grid + form" plan, since renumbered Phase D) was too small — re-scoped as the trainer studio and shipped WITH the slice. DECISIONS (all answered): video = URL-paste today on `Chapter.videoUrl` (real upload rides the Bunny Stream seam later — the developer: "all of them will go to bunny"; the field feeds both paste and, later, upload) · labels = free-text trainer-typed comma strings on BOTH `Course` + `Chapter` (rendered as pills) · thumbnails = BOTH (`Course.thumbnailUrl` cover + `Chapter.thumbnailUrl` episode cover) · route = dedicated `/trainer` studio + `/trainer/courses/:id` library (TRAINER-only header link; client-side role guard). Episode doors are owner-only through the same ownership law; `Chapter` also grew `description`. The owner course-detail door `GET /api/courses/trainer/{id}` exists because the public detail door demands a redeemable code the owner shouldn't need for their own content.
- **Video storage**: `VideoStorageService` interface + `InMemoryVideoStorageService` stub. Bunny Stream planned Slice 6. **Real profile-picture upload waits for the Bunny part too** (2026-10-15 decision): pictures ride the video storage seam (one account/one env-var set for both); when Bunny's turn comes, research Bunny picture-vs-video hosting + costs BEFORE wiring; `profilePictureUrl` column + URL-based avatar unchanged; local-disk (no signup) is the fallback if pictures are wanted sooner.
- **OpenApiConfig**: Customizer converts `$ref` object query params to form request bodies (form-urlencoded endpoints).

## 9. Current State
- **Done**: Slice 1 (Auth), Slice 2 (Catalog), Slice 3 (Detail + chapters), **Slice 4 CLOSED 2026-10-15 (revoke door + duplicate law landed; suite 89 green)**, UI waves 1–3, **Dashboard (profile + courses + edit-profile, #155 2026-10-14)**, **Slice 6 CLOSED 2026-10-22 (#164, commit `aee6e48`: trainer studio + robots, 128/128 at close)**, **Slice 5 CLOSED 2026-11-03 (#173: payment proofs A–F, code pushed in `8681610` + `415b049` + `39cc2fc`, suite 157/157, close walks live)**.
- **Next on the ladder**: **Bunny Stream video — Slice 5 CLOSED 2026-11-03 (#173): Phases A/B/C landed 2026-10-22, the UI (Phase D: the locked-screen "I already paid" pane + the `/payments` assistant queue page + routing/i18n) landed 2026-11-02/03 (`8681610` + `415b049`), the robots landed 2026-11-03 (`39cc2fc`, 29 pins, suite 157/157), and the Phase F close walked LIVE (backend-run27: fresh outsiders registered through the real door → locked 403 → submit 201 → approve → course opens 200 with NO code; reject → `/me` 401 + sessions 0) — all code already pushed, `main == origin/main`, clean tree. Gate answered 2026-10-22 (ARCHITECTURE entry; TASKLIST in `notes/slice-5/`).** Bunny standing order: research picture-vs-video hosting + **costs BEFORE wiring**; pictures ride the same seam ("all of them will go to bunny").
- **Ladder reordered**: Slice 6 (trainer) → Slice 5 (payments) → Bunny Stream video.
- **Tech debt**: `Track` enum exists but unused (label-only by design). `VideoStorageService` is a passthrough stub. No progress tracking (out of scope MVP).

## 10. Session Tracking & Followups
The project is **self-documenting across sessions** via `notes/`:
- **`SESSION-MEMORY.md`** — living snapshot: where we stand, locked decisions, teaching-style contract. **Overwritten** at end of each session. Read FIRST every session.
- **`PROMPT-HISTORY.md`** — append-only diary: every developer prompt verbatim + an Outcome line. Read its TAIL to recall where a disconnected session died.
- **`GIT-RULES.md`** (locked 2026-10-07) — commit message style (one plain-English line, no project vocab, no footers/trailers), single-author rule, pre-push verification ritual, `--force-with-lease` policy, history-cleanup scripts in `logs/`. Standing exclusions: `mvnw`, `.md`.
- **`ARCHITECTURE.md`** — "we decided, stop re-litigating" list. Read before ANY design discussion.
- **`TODO.md`** — all slices in one line each; the project ladder.
- **Per-slice folders** (`notes/slice-N/`) — each slice's tasklist, phases-preview, testing guide, Q&A. Append-only.
- **`notes/SCENARIOS.md`** — the boss/trainer/intern test personas + the runnable scenarios (2026-10-21).

**Followup mechanism:** each session starts by reading `SESSION-MEMORY.md` + tail of `PROMPT-HISTORY.md`; ends by overwriting `SESSION-MEMORY.md` and appending to `PROMPT-HISTORY.md`. Conflicts between code and `SITP-Docs/` are **flagged loudly** (⚠ REALITY FLAG), never silently resolved.

## 11. Gotchas
- **Form-urlencoded, not JSON**: All POST endpoints consume `application/x-www-form-urlencoded`. Use `@ModelAttribute`, not `@RequestBody`.
- **Principal is `Long`**, not `User`. Cast: `(Long) auth.getPrincipal()`.
- **Tests use `@Transactional(NOT_SUPPORTED)`** + manual `cleanTables()` — each test wipes all tables.
- **`ddl-auto: update`** in prod, `create-drop` in tests. No Flyway/Liquibase.
- **Security permits** `/api/courses` (list) but **not** `/api/courses/{id}` — detail requires auth + code.
- **Captcha is required** on register/login — tests must issue + verify captcha first.
- **Email codes are BCrypt-hashed** — cannot query plaintext. Console sender logs them.
- **`/api/captcha` and `/api/auth/captcha`** are duplicates (both public).

## 12. Key Smells & Signatures (things that look wrong but aren't)
- **`RedeemableCodeService.computeAccessUntil`** returns a raw `String` (`"lifetime"`, or `"X days left"` for an intern claimant with a validity box — display contract, not a date). Expiry itself is computed per 0f at read (a null `validityDays` row = lifetime; intern rows never mint without one since #150).
- **`RegisterRequest.setEmail/setUsername`** trim + lowercase via custom setters — normalization happens at the DTO boundary, not in the service.
- **`VerificationService`** uses `passwordEncoder.encode(codeHash)` for verification codes (BCrypt on 6-digit codes). Works but unusual — don't "fix" it to plain hashing without considering timing attacks.
- **Repository signatures** to know exist: `RedeemableCodeRepository.findByClaimantIdAndCourseIdAndStatus` + `findByClaimantIdAndStatus` (both `List`-returning — 2026-10-13 #150: the old boolean `existsBy…` was replaced because the guard's intern half needs to SCORE time boxes per row; the two-active-codes duplicate case stays safe through `anyMatch`, never `Optional`), `VerificationCodeRepository.findFirstByUsernameIgnoreCaseAndPurposeAndUsedFalseOrderByCreatedAtDesc`, `SessionRepository.invalidateByUserId` (`@Modifying` bulk update).
- **`CourseService`** reads `SecurityContextHolder` directly (not method-param `Authentication`) — consistent pattern in this codebase.

## 13. Representative Test Pattern
```java
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SomeControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired SomeRepository repo;
    // each test: cleanTables() → seed → mockMvc.perform(get("/api/..."))
    // assertions via jsonPath / status().isXxx()
}
```
- H2 in-memory, `create-drop`, no SMTP (`app.mail.enabled=false`).
- Captcha: issue via `CaptchaService` → answer the math → pass `captchaId` + `captchaAnswer`.
- Auth: POST `/api/auth/login` → capture `sitp_session` cookie → send as `Cookie` header.

## 14. Frontend API Consumption (Angular)
- **Proxy**: browser → `:4200` → `/api/*` proxied to `:8080`. Never call `:8080` directly from browser.
- **Auth**: login POST form-urlencoded → store `sitp_session` cookie (HttpOnly, so JS can't read it) → send on every request via `withCredentials` / interceptor.
- **Errors**: backend returns `ErrorResponse {status, message, timestamp}` — Angular interceptor extracts `message` to display.
- **Read `sitp-web/src/app/`** for service/interceptor/router details when working on frontend.

## 15. How to Run
```bash
# Backend (needs PostgreSQL on :5432, db sitp_demo)
./mvnw spring-boot:run

# Tests (H2 in-memory, no external deps)
./mvnw test

# Frontend (from sitp-web/)
npm install && npm start   # ng serve with proxy to :8080
```

**Env vars**: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `MAIL_HOST`, `MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `MAIL_ENABLED`, `MAIL_FROM`.

---

## Not covered — read on demand, only when the task requires it
This file is a **router**, not an encyclopedia. Don't re-read the whole tree upfront; read deep only when a command demands it:
- **Frontend components/services** → `sitp-web/src/app/`
- **Full requirements contract** → `SITP-Docs/requirements.md`
- **API specification** → `SITP-Docs/api-specification.md`
- **Slice history & decisions** → `notes/slice-N/`, `notes/general/ARCHITECTURE.md`
- **UI design system** → `notes/ui-plan/`
- **DTO field-level details** → open the DTO in `auth/dto/`, `course/dto/`, etc.
- **Test examples** → `src/test/java/...` (pattern in §13)
- **Not yet built**: Bunny Stream video (real upload; pictures ride the same seam — research costs first)
