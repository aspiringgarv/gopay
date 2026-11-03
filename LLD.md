# gopay — Low-Level Design

A payment-gateway simulator built with Spring Boot. This document covers the design patterns,
architectural decisions, and the reasoning behind them — not just what each piece does, but why
it's built the way it is. For request/response shapes, see `payloads.txt` and
`gopay.postman_collection.json`; for a visual walkthrough with diagrams, see the
[Gopay System Map](https://claude.ai/artifact/CNbD7FWSfKPT9kQofnWEp3) (requires a Claude login —
this document is the no-login version of the same material).

## Stack

Java 25 · Spring Boot 4.1 · Spring Security 7 · Spring Data JPA (Hibernate 7) · PostgreSQL ·
Redis · MapStruct · Lombok · JJWT

## Module layout

```
common/        shared entities (BaseEntity), enums, exceptions, validation, auditing,
               rate limiting, caching, idempotency — all generic, domain-agnostic infrastructure
merchant/      merchant signup/login, API keys, JWT + API-key auth filters, MerchantContext
payment/       orders, payments, gateway/processor routing, state machine, bank callback simulator
vault/         card tokenization, envelope encryption
operations/    settlement, webhooks, DLQ — domain models only, not wired up yet
```

Each top-level package follows the same internal shape: `controllers/ → services/ (+ impl/) →
repositories/`, with `dto/` (request/response records) and `converters/` (MapStruct interfaces)
sitting beside them. `entities/` holds the JPA model.

---

## Domain model

| Entity | Lifecycle | Notes |
|---|---|---|
| `Merchant` | `PENDING_KYC` → ... | Created at signup |
| `AppUser` | — | Implements `UserDetails` directly — no separate principal wrapper |
| `ApiKey` | enabled → revoked | BCrypt-hashed secret, rotation with a grace period |
| `RefreshToken` | issued → rotated/revoked | Opaque, not a JWT — see Security section |
| `OrderRecord` | `CREATED → ATTEMPTED → PAID` / `CANCELLED` | One order, many payment attempts |
| `Payment` | `CREATED → AUTHORIZING → AUTHORIZED → CAPTURING → CAPTURED` / `FAILED` / ... | Driven entirely by `PaymentStateMachine` |
| `PaymentTransitionLog` | append-only | One row per state transition, with actor and reason |
| `VaultCard` / `CardToken` | — | Envelope-encrypted PAN, token is the public handle |

**Order and Payment are deliberately separate entities**, not a single record with a status
column, so a failed card attempt doesn't erase the order underneath it — one order can carry many
payment attempts across retries and different methods.

**`BaseEntity`** (`common/entities`) is a `@MappedSuperclass` every entity extends, providing
`createdAt`/`updatedAt` (Spring Data JPA auditing, `@CreatedDate`/`@LastModifiedDate`) and
`createdBy`/`updatedBy`, filled by `SecurityAuditorAware` (`common/audit`) — it reads whoever the
request's filter put into `SecurityContextHolder`: a merchant's email on the JWT chain, an API
key's `keyId` on the API key chain, or `"SYSTEM"` with no caller at all (signup, the bank callback
simulator). `PaymentTransitionLog` deliberately does **not** extend `BaseEntity` — it already
records its own actor per row via the `Actor` enum, which is more specific than a generic string.

---

## Design patterns, by where they actually show up

### Builder, not inheritance-of-builders
Every entity uses plain Lombok `@Builder`, never `@SuperBuilder` — a `@Builder` does not include a
superclass's fields, which is intentional here: audit fields (`createdAt`, `createdBy`, ...) are
never meant to be set through a builder, only by the auditing listener itself. MapStruct converters
that map into these builders explicitly do **not** `ignore` the inherited fields (an earlier,
removed pattern) — MapStruct's builder detection doesn't see inherited properties at all, so an
`ignore = true` on one throws `Unknown property "createdAt" in result type X.XBuilder` at compile
time.

### Strategy + Adapter: payment routing
`PaymentServiceImpl.initiate()` doesn't know how to charge a card or a UPI handle — it calls
`PaymentGatewayRouter`, which resolves a `PaymentAdapter` by `PaymentMethod` (a
`Map<PaymentMethod, PaymentAdapter>` bean, built from Spring auto-wiring every `PaymentAdapter`
implementation with `@Component` and a qualifier). Every adapter funnels into a second router,
`PaymentProcessorRouter`, which picks the matching mock network simulator
(`CardPaymentProcessor`/`UpiPaymentProcessor`/`NetbankingPaymentProcessor`/`WalletPaymentProcessor`,
all implementing `PaymentProcessor`). Card is the one adapter with a real detour: it has no raw PAN
to send, only a vault token, so it calls `VaultService.charge(token, ...)` first to decrypt it.

### Sealed interfaces + pattern-matching switch
Two sealed hierarchies carry results between layers instead of exceptions or nullable fields:

```java
sealed interface PaymentResult permits Pending, Success, Failure
sealed interface PaymentProcessorResponse permits Pending, Success, Failure
```

Callers use an exhaustive `switch` with no `default` needed for the known cases — the compiler
enforces every branch is handled, and a genuinely unreachable case (a processor returning
`Success` directly, which never happens — success only ever arrives later via the bank callback)
gets a `default -> throw new IllegalStateException(...)` specifically to catch a contract
violation loudly instead of silently mis-handling it.

### State machine, not a bare `setStatus()`
`PaymentStateMachine` is a lookup table keyed on `(PaymentStatus, PaymentEvent) → PaymentStatus`.
An event that isn't legal from the current status throws, rather than silently moving a payment
somewhere it shouldn't be. `PaymentTransitionService` wraps every call with an audit row: who
triggered it (`Actor`: `CUSTOMER`/`MERCHANT`/`SYSTEM`), what event fired, what status it left and
entered. Nothing in `PaymentServiceImpl` ever calls `payment.setStatus(...)` directly.

<figure role="img" aria-label="Payment status state machine: CREATED moves through AUTHORIZING, AUTHORIZED, CAPTURING to CAPTURED on success events, or to FAILED from AUTHORIZING or CAPTURING on failure events.">
<svg viewBox="0 0 760 230" xmlns="http://www.w3.org/2000/svg">
<defs>
<marker id="arrow-state" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" fill="currentColor"/></marker>
<marker id="arrow-fail" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" fill="var(--danger)"/></marker>
</defs>
<rect x="20" y="30" width="110" height="46" rx="8" fill="var(--accent-soft)" stroke="var(--accent)"/>
<text x="75" y="58" text-anchor="middle" font-size="12" fill="currentColor">CREATED</text>
<rect x="165" y="30" width="110" height="46" rx="8" fill="var(--surface)" stroke="var(--line)"/>
<text x="220" y="58" text-anchor="middle" font-size="12" fill="currentColor">AUTHORIZING</text>
<rect x="310" y="30" width="110" height="46" rx="8" fill="var(--surface)" stroke="var(--line)"/>
<text x="365" y="58" text-anchor="middle" font-size="12" fill="currentColor">AUTHORIZED</text>
<rect x="455" y="30" width="110" height="46" rx="8" fill="var(--surface)" stroke="var(--line)"/>
<text x="510" y="58" text-anchor="middle" font-size="12" fill="currentColor">CAPTURING</text>
<rect x="600" y="30" width="110" height="46" rx="8" fill="var(--success-soft)" stroke="var(--success)"/>
<text x="655" y="58" text-anchor="middle" font-size="12" fill="currentColor">CAPTURED</text>
<rect x="310" y="155" width="110" height="46" rx="8" fill="var(--danger-soft)" stroke="var(--danger)"/>
<text x="365" y="183" text-anchor="middle" font-size="12" fill="currentColor">FAILED</text>
<line x1="130" y1="53" x2="165" y2="53" stroke="currentColor" stroke-width="1.3" marker-end="url(#arrow-state)"/>
<text x="95" y="44" font-size="9.5" fill="var(--ink-muted)">AUTHORIZE_ATTEMPT</text>
<line x1="275" y1="53" x2="310" y2="53" stroke="currentColor" stroke-width="1.3" marker-end="url(#arrow-state)"/>
<text x="245" y="44" font-size="9.5" fill="var(--ink-muted)">AUTHORIZE_SUCCESS</text>
<line x1="420" y1="53" x2="455" y2="53" stroke="currentColor" stroke-width="1.3" marker-end="url(#arrow-state)"/>
<text x="390" y="44" font-size="9.5" fill="var(--ink-muted)">CAPTURE_REQUEST</text>
<line x1="565" y1="53" x2="600" y2="53" stroke="currentColor" stroke-width="1.3" marker-end="url(#arrow-state)"/>
<text x="540" y="44" font-size="9.5" fill="var(--ink-muted)">CAPTURE_SUCCESS</text>
<line x1="235" y1="76" x2="330" y2="155" stroke="var(--danger)" stroke-width="1.3" stroke-dasharray="4 3" marker-end="url(#arrow-fail)"/>
<text x="195" y="115" font-size="9.5" fill="var(--danger)">AUTHORIZE_FAIL</text>
<line x1="495" y1="76" x2="400" y2="155" stroke="var(--danger)" stroke-width="1.3" stroke-dasharray="4 3" marker-end="url(#arrow-fail)"/>
<text x="430" y="115" font-size="9.5" fill="var(--danger)">CAPTURE_FAIL</text>
</svg>
<figcaption>Every arrow is one PaymentEvent, validated against PaymentStateMachine's (status, event) → status table and logged to PaymentTransitionLog with its actor — nothing in PaymentServiceImpl calls setStatus() directly.</figcaption>
</figure>

### Chain of Responsibility: the filter stack
Three custom `OncePerRequestFilter`s, each solving one concern, composed by `WebSecurityConfig`
into two separate Spring Security filter chains (see Security section below):

- `JwtAuthenticationFilter` / `ApiKeyAuthenticationFilter` — authenticate the caller, populate
  `MerchantContext` (a request-scoped bean — effectively a thread-local, scoped automatically by
  Spring to the current request).
- `IdempotencyFilter` — added via `addFilterAfter(..., JwtAuthenticationFilter.class)` /
  `addFilterAfter(..., ApiKeyAuthenticationFilter.class)`, i.e. deliberately positioned *after*
  authentication in each chain, specifically so `MerchantContext` is already populated when it
  runs — the idempotency key is scoped per merchant (`idempotency:<merchantId>:<key>`), not global.

None of these three is a bare `@Component`: they're constructed explicitly inside
`WebSecurityConfig` and wired with `addFilterBefore`/`addFilterAfter`, which is what lets their
relative order be controlled at all — a `@Component` filter is auto-registered by Spring Boot
outside of Spring Security's own chain entirely, with no guaranteed position relative to it.

### Repository pattern + explicit locking variants
Every aggregate root gets a plain Spring Data repository. Where a service needs to read-then-write
the same row inside one transaction, there's a second, explicit method:

```java
Optional<Payment> findByIdAndMerchantId(UUID id, UUID merchantId);                 // plain read

@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Payment> findByIdAndMerchantIdForUpdate(UUID id, UUID merchantId);         // SELECT ... FOR UPDATE
```

The locked variant is a separate method, not a flag on the existing one, so every call site makes
an explicit choice about whether it needs the lock. See Concurrency section for where this is used
and why.

---

## Security

### Two independent authentication chains
A dashboard login carries a **JWT**; a merchant's server calling the payment API carries an
**API key and secret**. Each chain claims its own URL prefixes via `securityMatcher`, so a request
is handled by exactly one:

| Chain | Routes | Credential |
|---|---|---|
| JWT (`@Order(1)`) | `/v1/auth/**`, `/v1/merchants/**`, `/v1/admin/**`, `/actuator/**` | `Authorization: Bearer <jwt>` |
| API key (`@Order(2)`) | `/v1/orders/**`, `/v1/payments/**`, `/v1/vault/**` | `Authorization: Basic base64(keyId:keySecret)` |

<figure role="img" aria-label="Two independent security filter chains, JWT and API key, both converging into the same request-scoped MerchantContext before the IdempotencyFilter and the controller run.">
<svg viewBox="0 0 720 500" xmlns="http://www.w3.org/2000/svg">
<defs>
<marker id="arrow-auth" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" fill="currentColor"/></marker>
</defs>
<rect x="280" y="10" width="160" height="40" rx="8" fill="var(--accent-soft)" stroke="var(--accent)"/>
<text x="360" y="35" text-anchor="middle" font-size="12" fill="currentColor">Client request</text>
<rect x="30" y="90" width="270" height="60" rx="8" fill="var(--surface)" stroke="var(--line)"/>
<text x="165" y="110" text-anchor="middle" font-size="11" fill="currentColor">JWT chain (@Order(1))</text>
<text x="165" y="126" text-anchor="middle" font-size="10.5" fill="var(--ink-muted)">/v1/auth/**, /v1/merchants/**,</text>
<text x="165" y="140" text-anchor="middle" font-size="10.5" fill="var(--ink-muted)">/v1/admin/**, /actuator/**</text>
<rect x="30" y="190" width="270" height="50" rx="8" fill="var(--surface)" stroke="var(--line)"/>
<text x="165" y="211" text-anchor="middle" font-size="11.5" fill="currentColor">JwtAuthenticationFilter</text>
<text x="165" y="227" text-anchor="middle" font-size="10.5" fill="var(--ink-muted)">validates Bearer JWT</text>
<rect x="420" y="90" width="270" height="60" rx="8" fill="var(--surface)" stroke="var(--line)"/>
<text x="555" y="110" text-anchor="middle" font-size="11" fill="currentColor">API key chain (@Order(2))</text>
<text x="555" y="126" text-anchor="middle" font-size="10.5" fill="var(--ink-muted)">/v1/orders/**, /v1/payments/**,</text>
<text x="555" y="140" text-anchor="middle" font-size="10.5" fill="var(--ink-muted)">/v1/vault/**</text>
<rect x="420" y="190" width="270" height="50" rx="8" fill="var(--surface)" stroke="var(--line)"/>
<text x="555" y="211" text-anchor="middle" font-size="11.5" fill="currentColor">ApiKeyAuthenticationFilter</text>
<text x="555" y="227" text-anchor="middle" font-size="10.5" fill="var(--ink-muted)">Basic keyId:secret (Redis cache-aside)</text>
<rect x="270" y="285" width="180" height="55" rx="8" fill="var(--success-soft)" stroke="var(--success)"/>
<text x="360" y="308" text-anchor="middle" font-size="11.5" fill="currentColor">MerchantContext</text>
<text x="360" y="324" text-anchor="middle" font-size="10.5" fill="var(--ink-muted)">request-scoped bean</text>
<rect x="260" y="375" width="200" height="50" rx="8" fill="var(--surface)" stroke="var(--line)"/>
<text x="360" y="396" text-anchor="middle" font-size="11.5" fill="currentColor">IdempotencyFilter</text>
<text x="360" y="412" text-anchor="middle" font-size="10.5" fill="var(--ink-muted)">addFilterAfter(...)</text>
<rect x="300" y="445" width="120" height="40" rx="8" fill="var(--accent-soft)" stroke="var(--accent)"/>
<text x="360" y="469" text-anchor="middle" font-size="11.5" fill="currentColor">Controller</text>
<line x1="330" y1="50" x2="180" y2="90" stroke="currentColor" stroke-width="1.3" marker-end="url(#arrow-auth)"/>
<text x="195" y="72" font-size="10" fill="var(--ink-muted)">Bearer &lt;jwt&gt;</text>
<line x1="390" y1="50" x2="540" y2="90" stroke="currentColor" stroke-width="1.3" marker-end="url(#arrow-auth)"/>
<text x="460" y="72" font-size="10" fill="var(--ink-muted)">Basic base64(keyId:secret)</text>
<line x1="165" y1="150" x2="165" y2="190" stroke="currentColor" stroke-width="1.3" marker-end="url(#arrow-auth)"/>
<line x1="555" y1="150" x2="555" y2="190" stroke="currentColor" stroke-width="1.3" marker-end="url(#arrow-auth)"/>
<line x1="200" y1="240" x2="320" y2="285" stroke="currentColor" stroke-width="1.3" marker-end="url(#arrow-auth)"/>
<text x="175" y="268" font-size="10" fill="var(--ink-muted)">sets merchantId + role</text>
<line x1="520" y1="240" x2="400" y2="285" stroke="currentColor" stroke-width="1.3" marker-end="url(#arrow-auth)"/>
<text x="460" y="268" font-size="10" fill="var(--ink-muted)">sets merchantId</text>
<line x1="360" y1="340" x2="360" y2="375" stroke="currentColor" stroke-width="1.3" marker-end="url(#arrow-auth)"/>
<text x="368" y="362" font-size="10" fill="var(--ink-muted)">requireMerchantId()</text>
<line x1="360" y1="425" x2="360" y2="445" stroke="currentColor" stroke-width="1.3" marker-end="url(#arrow-auth)"/>
</svg>
<figcaption>Two independent chains authenticate the caller and populate the same MerchantContext before the IdempotencyFilter — and the controller — ever run.</figcaption>
</figure>

### JWT: short-lived access token + revocable refresh token
A JWT can't be revoked early — it's trusted on signature and expiry alone, nothing is looked up.
So the access token is kept short (15 min), paired with a second, server-tracked token that
*can* be killed:

| Token | Form | Lifetime | Revocable? |
|---|---|---|---|
| `accessToken` | JWT (HS256, `sub`/`merchantId`/`role` claims) | 15 min | No — expires on its own |
| `refreshToken` | opaque `"<tokenId>:<secret>"`, not a JWT | 7 days | Yes — a row in `refresh_token` |

The refresh token is built the same way an API key is: `tokenId` is the public lookup half (plain
text), `secret` is only ever stored as a BCrypt hash. `POST /v1/auth/refresh` checks both, then
**rotates** — revokes the token just used, issues a new pair. Reusing an already-rotated refresh
token is treated as theft: every refresh token for that app user is revoked, not just the one
presented. Rotation and reuse-detection are not a JWT property — they come from checking a
server-side record on every use, which is exactly what makes a refresh token revocable and a plain
access-token JWT not.

### API keys: BCrypt + rotation grace period
A key has a public `keyId` and a secret, shown once at creation/rotation. Only a BCrypt hash is
stored. Rotating keeps the *previous* secret working for one day (`isInGracePeriod()`), so a
merchant can switch credentials without downtime. Revoking disables the key outright.

### Envelope encryption for stored cards
Each card gets its own randomly generated AES key (the **DEK**), which encrypts the PAN. The DEK
itself is encrypted by one shared master key before either touches the database. Losing the
database alone gets an attacker nothing — they'd also need the master key to unwrap even one DEK.
The raw PAN only ever exists as a local variable inside `VaultServiceImpl.charge()`, and the DEK
bytes are zeroed (`Arrays.fill`) once the call returns. CVV is accepted on `TokenizeCardRequest`
but never persisted anywhere — PCI-DSS forbids retaining it past authorization.

---

## Redis: one instance, three separate jobs

gopay points three unrelated pieces of request-handling machinery at the same Redis instance, each
with its own key prefix, and — this is the part worth being deliberate about — each with a
**different answer to "what happens if Redis itself fails right now."**

| Job | Key prefix | On a Redis failure |
|---|---|---|
| Cache (`ApiKeyCache`) | `apikey:` | **Fails open** — falls back to the database |
| Rate limit (`RateLimiter`) | `ratelimit:` | **Fails open** — allows the request rather than blocking real traffic |
| Idempotency (`IdempotencyStore`) | `idempotency:` | **Fails closed** — `setIfAbsent` throws |

<figure role="img" aria-label="One Redis instance backs three independent jobs, each key-prefixed, with two different failure policies: the cache and rate limiter fail open, the idempotency store fails closed.">
<svg viewBox="0 0 700 280" xmlns="http://www.w3.org/2000/svg">
<defs>
<marker id="arrow-redis" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" fill="currentColor"/></marker>
<marker id="arrow-redis-danger" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" fill="var(--danger)"/></marker>
</defs>
<rect x="30" y="110" width="140" height="70" rx="10" fill="var(--accent-soft)" stroke="var(--accent)"/>
<text x="100" y="150" text-anchor="middle" font-size="13" fill="currentColor">Redis</text>
<rect x="420" y="20" width="250" height="62" rx="8" fill="var(--surface)" stroke="var(--line)"/>
<text x="545" y="44" text-anchor="middle" font-size="11.5" fill="currentColor">ApiKeyCache — apikey:*</text>
<text x="545" y="62" text-anchor="middle" font-size="10.5" fill="var(--ink-muted)">fails open → DB fallback</text>
<rect x="420" y="107" width="250" height="62" rx="8" fill="var(--surface)" stroke="var(--line)"/>
<text x="545" y="131" text-anchor="middle" font-size="11.5" fill="currentColor">RateLimiter — ratelimit:*</text>
<text x="545" y="149" text-anchor="middle" font-size="10.5" fill="var(--ink-muted)">fails open → allows the request</text>
<rect x="420" y="194" width="250" height="66" rx="8" fill="var(--danger-soft)" stroke="var(--danger)"/>
<text x="545" y="219" text-anchor="middle" font-size="11.5" fill="currentColor">IdempotencyStore — idempotency:*</text>
<text x="545" y="237" text-anchor="middle" font-size="10.5" fill="var(--danger)">fails closed → setIfAbsent throws</text>
<line x1="170" y1="130" x2="420" y2="52" stroke="currentColor" stroke-width="1.3" marker-end="url(#arrow-redis)"/>
<line x1="170" y1="145" x2="420" y2="138" stroke="currentColor" stroke-width="1.3" marker-end="url(#arrow-redis)"/>
<line x1="170" y1="165" x2="420" y2="227" stroke="var(--danger)" stroke-width="1.5" marker-end="url(#arrow-redis-danger)"/>
</svg>
<figcaption>The difference that matters: a cache or rate limiter can safely fail open because something else is still correct on its own; idempotency can't, so it's the one Redis call in the codebase allowed to throw back to the caller.</figcaption>
</figure>

A cache or a rate limiter can safely fail open, because something else (the database, or simply
allowing the request) is still correct on its own. Idempotency can't: the entire reason it exists
is to stop a payment from being processed twice, so a wrong guess during an outage — in either
direction — causes real harm. `IdempotencyStore.setIfAbsent` is the one Redis call in the whole
codebase allowed to throw a real exception back to the caller instead of quietly recovering;
`store`/`get`/`delete` on the same interface *do* fail safe (log and continue), since by the time
they run, the safety-critical decision has already been made.

### Cache-aside: `ApiKeyAuthenticationFilter`
`loadAndCache(keyId)` checks Redis first, falls back to `ApiKeyRepository` only on a miss, and
caches what it finds — TTL 5 minutes **plus or minus 60 seconds of random jitter**, not a flat
5 minutes, specifically to avoid a cache stampede: a burst of keys cached around the same moment
would otherwise all expire in the same instant and all miss together. `revoke`/`rotate` evict the
cache entry immediately rather than waiting on the TTL.

### Rate limiting: three algorithms, one active
`FixedWindowRateLimiter` (the only one that's a `@Component`) is one Redis `INCR` per window —
100 requests/minute per API key, checked in the filter *before* any DB/cache lookup runs, so a
brute-force run against one key never even reaches the real authentication check. Two more
implementations exist in `common/ratelimiting/impl` purely for comparison, neither wired up:

- `SlidingWindowRateLimiter` — a true rolling window (Redis sorted set scored by request time), no
  boundary burst the way fixed windows have, at the cost of more Redis work per check.
- `TokenBucketRateLimiter` — lets a caller burst up to the full limit after being idle, then
  throttles to the steady average rate.

Each has a `*Plain` sibling, left **deliberately broken** as a teaching comparison:
`SlidingWindowRateLimiterPlain` does prune+add+count as four separate Redis round trips, which
creates a boundary-only overcount under real concurrency. `TokenBucketRateLimiterPlain` is worse —
a genuine *lost update* on its read-modify-write cycle: two concurrent requests can both read the
same token count and both get allowed on the one token that was actually available. The active,
non-`Plain` versions run the whole sequence as **one Lua script** instead; Redis guarantees nothing
else executes while a script is in flight, which is what closes both races.

### Idempotency: claim, then finalize
`IdempotencyFilter` guards `POST`/`PUT`/`PATCH` only (`GET` is already safe to repeat, `DELETE` is
naturally idempotent on its own) behind an optional `X-Idempotency-Key` header:

1. **`setIfAbsent(key, 30s)`** — atomically claims the key with a fixed `"IN_PROGRESS"` marker.
2. The real controller runs, writing into a `ContentCachingResponseWrapper` instead of the real
   response — a plain `HttpServletResponse` has no method to read back what was written to it at
   all, it only ever streams bytes outward, so this is the only way to capture the body afterward.
3. **Response was `< 400`** → `store(key, body, 24h)`, overwriting the marker with the real,
   replayable result. **`≥ 400` or an exception** → `delete(key)`, releasing it for a real retry.
4. A second request with the same key: already-stored result → replay it verbatim, `200`, no
   controller call at all. Still `"IN_PROGRESS"` → `409 DUPLICATE_IDEMPOTENCY_KEY`.

<figure role="img" aria-label="Idempotency claim and finalize flow: the first request claims the key and stores its result; a concurrent request with the same key is rejected with 409 while the claim is in progress; a later request with the same key replays the stored result without calling the controller.">
<svg viewBox="0 0 760 310" xmlns="http://www.w3.org/2000/svg">
<defs>
<marker id="arrow-idem" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" fill="currentColor"/></marker>
</defs>
<text x="10" y="25" font-size="11" fill="var(--ink-muted)">Request A</text>
<rect x="140" y="10" width="170" height="50" rx="8" fill="var(--success-soft)" stroke="var(--success)"/>
<text x="225" y="30" text-anchor="middle" font-size="10" fill="currentColor">setIfAbsent(key, 30s)</text>
<text x="225" y="46" text-anchor="middle" font-size="10" fill="var(--ink-muted)">claims IN_PROGRESS</text>
<rect x="350" y="10" width="170" height="50" rx="8" fill="var(--surface)" stroke="var(--line)"/>
<text x="435" y="30" text-anchor="middle" font-size="10" fill="currentColor">Controller runs</text>
<text x="435" y="46" text-anchor="middle" font-size="10" fill="var(--ink-muted)">ContentCachingResponseWrapper</text>
<rect x="560" y="10" width="170" height="50" rx="8" fill="var(--success-soft)" stroke="var(--success)"/>
<text x="645" y="30" text-anchor="middle" font-size="10" fill="currentColor">&lt;400 → store(key, body, 24h)</text>
<line x1="310" y1="35" x2="350" y2="35" stroke="currentColor" stroke-width="1.2" marker-end="url(#arrow-idem)"/>
<line x1="520" y1="35" x2="560" y2="35" stroke="currentColor" stroke-width="1.2" marker-end="url(#arrow-idem)"/>
<text x="10" y="135" font-size="11" fill="var(--ink-muted)">Request B</text>
<text x="10" y="150" font-size="9.5" fill="var(--ink-muted)">(same key,</text>
<text x="10" y="163" font-size="9.5" fill="var(--ink-muted)">concurrent)</text>
<rect x="140" y="120" width="170" height="50" rx="8" fill="var(--danger-soft)" stroke="var(--danger)"/>
<text x="225" y="140" text-anchor="middle" font-size="10" fill="currentColor">setIfAbsent(key, 30s)</text>
<text x="225" y="156" text-anchor="middle" font-size="10" fill="var(--danger)">fails — already IN_PROGRESS</text>
<rect x="350" y="120" width="170" height="50" rx="8" fill="var(--danger-soft)" stroke="var(--danger)"/>
<text x="435" y="140" text-anchor="middle" font-size="10" fill="currentColor">409</text>
<text x="435" y="156" text-anchor="middle" font-size="9.5" fill="var(--danger)">DUPLICATE_IDEMPOTENCY_KEY</text>
<text x="435" y="168" text-anchor="middle" font-size="8.5" fill="var(--ink-muted)">controller never runs</text>
<line x1="310" y1="145" x2="350" y2="145" stroke="currentColor" stroke-width="1.2" marker-end="url(#arrow-idem)"/>
<text x="10" y="245" font-size="11" fill="var(--ink-muted)">Request B</text>
<text x="10" y="260" font-size="9.5" fill="var(--ink-muted)">(same key,</text>
<text x="10" y="273" font-size="9.5" fill="var(--ink-muted)">after A finished)</text>
<rect x="140" y="230" width="170" height="50" rx="8" fill="var(--surface)" stroke="var(--line)"/>
<text x="225" y="250" text-anchor="middle" font-size="10" fill="currentColor">get(key)</text>
<text x="225" y="266" text-anchor="middle" font-size="10" fill="var(--ink-muted)">finds stored result</text>
<rect x="350" y="230" width="170" height="50" rx="8" fill="var(--accent-soft)" stroke="var(--accent)"/>
<text x="435" y="250" text-anchor="middle" font-size="10" fill="currentColor">replay stored body, 200</text>
<text x="435" y="266" text-anchor="middle" font-size="8.5" fill="var(--ink-muted)">controller never runs</text>
<line x1="310" y1="255" x2="350" y2="255" stroke="currentColor" stroke-width="1.2" marker-end="url(#arrow-idem)"/>
<line x1="225" y1="60" x2="225" y2="120" stroke="var(--ink-muted)" stroke-width="1" stroke-dasharray="3 3"/>
<line x1="225" y1="60" x2="225" y2="230" stroke="var(--ink-muted)" stroke-width="1" stroke-dasharray="3 3"/>
<text x="232" y="100" font-size="9" fill="var(--ink-muted)">same Redis key</text>
</svg>
<figcaption>The 30s claim and the 24h result share one Redis key — that's what makes a concurrent duplicate a 409 instead of a double-charge, and a later retry a clean replay instead of a second controller call.</figcaption>
</figure>

**Two deliberately different TTLs.** The 30s claim only needs to outlast one request's processing
time — a self-healing safety net: if the server crashes before `delete()` ever runs, the lock
expires on its own quickly instead of blocking a legitimate retry for a long time. Making the claim
long-lived would be actively harmful, not just wasteful. The 24h result TTL is the real guarantee
window — long enough for a genuinely slow client retry (a mobile app regaining connectivity) to
still get the exact same response replayed.

**Known limitation.** The Redis key is only `merchantId + the header value` — it does not
fingerprint the request path or body. A client that reuses the same idempotency key across two
genuinely different requests (different records, different payloads) will get the **first**
request's stored response silently replayed for the second one, rather than an error. The
responsibility for generating a properly unique key per distinct operation currently sits entirely
with the API caller. The standard fix (what Stripe does) is to store a hash of method+path+body
alongside the result and compare it on a replay, rejecting a mismatch with a `422` instead of
serving the wrong record's data — not yet implemented here.

---

## Concurrency control

Two distinct mechanisms, used for two distinct kinds of race:

**Pessimistic database locking** (`@Lock(LockModeType.PESSIMISTIC_WRITE)`, i.e. `SELECT ... FOR
UPDATE`) guards read-then-write sequences on a single row across a transaction:

- `PaymentServiceImpl.initiate()` locks the order before the `PAID`/`CANCELLED` checks — without
  it, two concurrent `initiate()` calls on the same order could both pass the checks and both
  create a payment.
- `PaymentServiceImpl.capture()` locks the payment before the `AUTHORIZED` check, for the same
  reason.
- `PaymentServiceImpl.resolveAuthorization()` (the bank callback path) uses a merchant-less
  `findByIdForUpdate`, since it only ever has a `paymentId`, not a merchant context.

This only takes effect inside an active `@Transactional` method — the lock is acquired when the
query runs and released when the transaction ends.

**Atomic Redis operations** guard races across requests that never share a database transaction at
all: `INCR` for the rate limiter's counter, `SET ... NX` for both the idempotency claim and (in
principle) any other "only the first caller should win" check. A single Redis command is atomic by
construction, because Redis processes commands one at a time — this is what makes `setIfAbsent`
race-free without any application-level locking, and it's also exactly why the `*Plain` rate
limiter variants, which split the same logic across several separate Redis calls, are *not*
race-free: nothing stops another request's command from landing in between two of theirs.

---

## Coming later

Refunds, settlement, webhooks — the `operations` module has domain models for these but nothing
wired up yet. The idempotency request-fingerprint check described above is the most concrete known
gap in what's already built.
