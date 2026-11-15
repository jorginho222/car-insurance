# Car Insurance

Spring Boot 4.1 / Java 21 backend for a car insurance company. Currently provides CRUD for **insureds** (policyholders).

## Prerequisites
- Docker (always)
- JDK 21 (optional: everything can run in Docker, see below)

## Run with Docker only (no local JDK)
```bash
docker compose --profile app up -d --build   # build image, start app + Postgres
docker compose --profile app logs -f app     # follow logs
docker compose --profile app down            # stop (add -v to wipe the database)
```

Run Maven/tests in a JDK 21 container (Testcontainers uses the host Docker socket):
```bash
bin/mvnw-docker verify                        # full build + all tests
bin/mvnw-docker test -Dtest=InsuredServiceTest
```

## Run with a local JDK
```bash
./mvnw spring-boot:run
```
Spring Boot's Docker Compose support starts PostgreSQL from `compose.yaml` automatically.
Flyway applies migrations from `src/main/resources/db/migration` on startup.

- API: `http://localhost:8080/api/v1/insureds`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

Sample requests are in `requests.http`.

## Test
```bash
./mvnw verify
```
Integration tests start a throwaway PostgreSQL via Testcontainers.

## Code formatting
Java code is formatted with [Spotless](https://github.com/diffplug/spotless) using
[Palantir Java Format](https://github.com/palantir/palantir-java-format).

```bash
./mvnw spotless:apply    # format all files (or: bin/mvnw-docker spotless:apply)
./mvnw spotless:check    # verify formatting; also runs as part of ./mvnw verify
```
`./mvnw verify` fails if any file is not formatted. In IntelliJ, install the
*palantir-java-format* plugin so "Reformat Code" produces the same result.

## API

| Method | Path | Success | Errors |
|---|---|---|---|
| POST | `/api/v1/insureds` | 201 + `Location` | 400 validation, 409 duplicate document/email |
| GET | `/api/v1/insureds/{id}` | 200 | 404 |
| GET | `/api/v1/insureds?page=&size=&sort=` | 200 (paged) | |
| PUT | `/api/v1/insureds/{id}` | 200 | 400, 404, 409 |
| DELETE | `/api/v1/insureds/{id}` | 204 | 404 |

Errors use RFC 7807 `application/problem+json`; validation errors include a per-field `errors` map.

## Structure
```
com.carinsurance
├── common/      cross-cutting config and exception handling
└── insured/     entity, repository, service, controller, DTOs
```
New domains (vehicles, policies, claims, ...) should get their own feature package alongside `insured/`.
