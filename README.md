# gopay

A payment-gateway simulator built with Spring Boot — orders, payments, card vaulting, and a
bank that answers for itself. There's no real bank or card network behind any of it; every
outcome (pending, success, decline) is simulated so the full order → payment → settlement flow
can be exercised end to end without a sandbox integration.

A living diagram of the system (domain model, request flows, state machine) is kept here:
**[Gopay System Map](https://claude.ai/artifact/CNbD7FWSfKPT9kQofnWEp3)**.

## What's in it

- **Orders & payments** — create an order, pay it by card/UPI/netbanking, auto-capture on
  authorization, list payments per order or per merchant.
- **Card vault** — tokenize a card once, charge the token later. Envelope encryption (AES-GCM):
  each card gets its own data-encryption key, which is itself encrypted by one master key. The
  raw PAN and CVV are never stored.
- **Payment state machine** — every status change on a payment goes through a
  `(status, event) → status` lookup table, never a bare setter, with every transition written to
  an audit log (who did it, what fired, what it left and entered).
- **Bank callback simulator** — a scheduled job plays the part of the bank: it answers a
  pending payment after a per-method delay, with a per-method success rate, both derived
  deterministically from the payment's id so a demo is repeatable.
- **Two authentication schemes** — a merchant's dashboard logs in with a JWT (+ a revocable
  refresh token); a merchant's server calls the payment API with an API key id and secret.
- **Auditing** — every entity records `createdAt`/`updatedAt` and `createdBy`/`updatedBy`
  automatically, the latter resolved from whoever authenticated the request.

## Stack

Java 25 · Spring Boot 4.1 · Spring Security · Spring Data JPA (Hibernate) · PostgreSQL ·
MapStruct · Lombok · JJWT

## Running it

**Prerequisites:** JDK 21+, PostgreSQL running locally.

```bash
createdb gopaydb
```

Set these if you want anything other than the dev defaults baked into `application.yaml`
(`DB_USER`/`DB_PASS` default to empty, `VAULT_MASTER_KEY` and `JWT_SECRET` have throwaway dev
values — never reuse those outside a local machine):

```bash
export DB_URL=jdbc:postgresql://localhost:5432/gopaydb
export DB_USER=postgres
export DB_PASS=postgres
export VAULT_MASTER_KEY=$(openssl rand -base64 32)
export JWT_SECRET=$(openssl rand -base64 32)
```

```bash
./mvnw spring-boot:run
```

The app starts on `:8080`. `spring.jpa.hibernate.ddl-auto` is `update`, so the schema is kept in
sync on every start — it won't touch existing data. (If you ever see it set to `create`, that's a
one-time reset; switch it back to `update` right after, or every restart wipes the database.)

## Trying it out

Import **`gopay.postman_collection.json`** (repo root) into Postman — it's kept in sync with
every endpoint and DTO. `payloads.txt` has the same request bodies as plain text, plus a demo
section of deterministic trigger values if you'd rather use curl.

**Flow:**
1. `POST /v1/auth/signup`, then `POST /v1/auth/login` → an access token (15 min) and a refresh
   token (7 days).
2. `POST /v1/merchants/api-keys` with that access token → an API key id and secret (the secret
   is shown once — save it).
3. With that key as HTTP Basic auth: `POST /v1/orders`, then `POST /v1/payments` against it.
4. Wait a few seconds, then `GET /v1/orders/{id}/payments` — the bank callback simulator settles
   it on its own.

Deterministic demo values (always decline, so a demo isn't left to chance): card PAN
`4000000000000002`, UPI vpa `fail@upi`, netbanking bank code `FAIL001`. Set
`payment.simulator.chaos-mode` to `ALWAYS_SUCCESS`/`ALWAYS_FAIL` to force every pending payment
one way.

## Authentication

| Routes | Scheme | Notes |
|---|---|---|
| `/v1/auth/**`, `/v1/merchants/**`, `/v1/admin/**`, `/actuator/**` | JWT | `Authorization: Bearer <accessToken>`. Signup and login are public. |
| `/v1/orders/**`, `/v1/payments/**`, `/v1/vault/**` | API key | `Authorization: Basic base64(keyId:keySecret)`. |

The access token is short-lived on purpose — a JWT can't be revoked before it expires, so it's
kept small. The refresh token (`POST /v1/auth/refresh`) is a server-tracked, revocable token
instead: it rotates on every use, and reusing an already-rotated one revokes the whole session.
`POST /v1/auth/logout` revokes a refresh token directly.

## Project layout

```
common/      shared entities (BaseEntity), enums, exceptions, validation, auditing
merchant/    merchant signup/login, API keys, JWT + API-key auth, MerchantContext
payment/     orders, payments, gateway/processor routing, state machine, bank callback simulator
vault/       card tokenization, envelope encryption
operations/  settlement, webhooks, DLQ (domain models only — not wired up yet)
```

## Coming later

Refunds, settlement, and webhooks — in whatever order the build goes.
