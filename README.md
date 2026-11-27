# Car Insurance

Spring Boot 4.1 / Java 21 backend for a car insurance company. It quotes every plan for a vehicle and a locality,
stores the **quotations**, and issues **policies** from a quoted plan. Issuing a policy creates the **insured**
(policyholder) and their **vehicle**. Read-only reference data: the **vehicle catalog** (make, model, version, and
valuations per year) and the **localities** with their pricing zones.

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
Flyway applies migrations from `src/main/resources/db/migration` on startup. On the first startup (empty
`locality` table) the app also imports the localities CSV, which takes a few seconds. Disable it with
`app.localities.import-on-startup=false`.

- API: `http://localhost:8080/api/v1/...` (see [API](#api))
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

### Insureds
Read-only. An insured is created by issuing a policy (see [Policies](#policies)). Updating and deleting insureds
will come later, together with reusing an existing insured for a new policy.

| Method | Path | Success | Errors |
|---|---|---|---|
| GET | `/api/v1/insureds/{id}` | 200 | 404 |
| GET | `/api/v1/insureds?page=&size=&sort=` | 200 (paged) | |

Response:
```json
{
  "id": 3, "firstName": "Jane", "lastName": "Doe", "documentNumber": "30123456", "dateOfBirth": "1990-05-17",
  "email": "jane@example.com", "phone": "+54 11 5555 1234",
  "address": { "street": "Av. Rivadavia 1234", "locality": "FLORIDA", "province": "Buenos Aires",
               "postalCode": "1602", "country": "Argentina" },
  "licenseNumber": "B-123456", "licenseIssueDate": "2010-06-01", "createdAt": "...", "updatedAt": "..."
}
```
- `documentNumber` and `email` are unique across insureds (email stored trimmed and lower-cased).
- The address `locality`, `province` and `postalCode` come from the quotation's locality, and `country` is always
  `Argentina`. Only `street` is entered. Migration `V14` renamed the columns `city` to `locality` and `state` to
  `province`.

### Vehicle catalog
Read-only reference data for picking a vehicle. A **catalog entry** is a make + model + model version (with its
vehicle type). It has many **valuations**: the vehicle's value for one model year, effective from a `validFrom` date.
A new price list adds new valuations with a later `validFrom` rather than changing the old ones.

The UI walks down the catalog one step at a time:

| Step | GET | Returns |
|---|---|---|
| 1. type | (fixed list) | `CAR`, `MOTORCYCLE`, `SUV`, `PICKUP`, `VAN`, `TRUCK`, `BUS` |
| 2. make | `/api/v1/vehicle-catalog/makes?vehicleType=` | `["Chevrolet", "Fiat", ...]` |
| 3. model | `/api/v1/vehicle-catalog/models?vehicleType=&make=` | `["Corolla", "Hilux", ...]` |
| 4. version | `/api/v1/vehicle-catalog/versions?vehicleType=&make=&model=` | catalog entries `[{ "id", "make", "model", "modelVersion", "vehicleType" }]` |
| 5. year | `/api/v1/vehicle-catalog/{catalogId}/valuations` | valuations `[{ "id", "catalogId", "year", "value", "validFrom" }]`, newest year first |

`GET /api/v1/vehicle-catalog/{catalogId}` returns one catalog entry.

- All query parameters are required. A missing one returns 400. An unknown `vehicleType` returns 400 with
  `errors.vehicleType` listing the allowed values. An unknown `catalogId` returns 404.
- The valuations list has **one row per year**: the current valuation, i.e. the one with the latest `validFrom`
  on or before today. Past and future price lists are not shown.
- The UI can show only the years, but it must keep each year's valuation `id` and send it as `valuationId` when
  requesting a quotation.
- The catalog is filled with **fake data** by migration `V5__seed_vehicle_catalog.sql` (about 9 makes, all vehicle
  types). It includes a price list from 2026-07-01 that replaces the 2026-01-01 one for some makes, and a future
  2027-01-01 list for Toyota. Later this will come from an external provider.

### Insured vehicles
Read-only. An insured owns one or more vehicles (1 to N). A vehicle is created by issuing a policy. It references the
quotation's catalog **valuation**, which gives its make, model, version, type and year.

| Method | Path | Success | Errors |
|---|---|---|---|
| GET | `/api/v1/insureds/{insuredId}/vehicles/{id}` | 200 | 404 |
| GET | `/api/v1/insureds/{insuredId}/vehicles?page=&size=&sort=` | 200 (paged) | 404 unknown insured |

Response:
```json
{
  "id": 1, "insuredId": 3, "licensePlate": "AB123CD", "engineNumber": "ENG-12345", "chassisNumber": "8AP17000000123456",
  "catalog": { "id": 7, "make": "Toyota", "model": "Corolla", "modelVersion": "2.0 XEI CVT", "vehicleType": "CAR" },
  "valuation": { "id": 42, "catalogId": 7, "year": 2020, "value": 23456.78, "validFrom": "2026-07-01" },
  "createdAt": "...", "updatedAt": "..."
}
```
- `licensePlate` is stored upper-cased without spaces or hyphens. It is **not unique** among vehicles: every policy
  creates its own vehicle row, for example on a renewal. The plate rule (no overlapping policies) belongs to the
  policy.
- `engineNumber` and `chassisNumber` are stored trimmed and upper-cased.
- A vehicle is only reachable under its own insured. Any other `insuredId` in the path returns 404.
- A valuation that a vehicle uses cannot be deleted.

### Localities
Read-only reference data for the insured's location, used to pick the pricing **zone** when quoting. The insured
enters a postal code and picks one of the matching localities.

| Method | Path | Success | Errors |
|---|---|---|---|
| GET | `/api/v1/localities?postalCode=` | 200, list (empty if none match) | 400 missing or not 4 digits |
| GET | `/api/v1/localities/{id}` | 200 | 404 |

Response item:
```json
{ "id": 812, "name": "ROSARIO", "province": "Santa Fe", "postalCode": "2000",
  "zone": { "id": 3, "name": "Ciudades grandes del interior" } }
```
- A postal code is **not unique**: it can match many localities (up to about 200), sometimes in different
  provinces. Results are ordered by province, then name.
- Zones: `CABA`, `GBA`, `Ciudades grandes del interior`, `Resto del país`. Each zone has one row in
  `quotation_zone_factor` with the multipliers it applies to each coverage price (civil liability, theft, damage),
  seeded by `V8__create_quotation_factor.sql` (table renamed in `V9`). Factors are not exposed in the API yet.
- Data comes from the postal code master file `src/main/resources/data/localidades_cp_maestro.csv` (about 23,000
  localities). On startup, if the `locality` table is empty (fresh install), `LocalitySeeder` imports it through
  `ImportLocalitiesUseCase`. Rows without a postal code and exact duplicates are skipped.
- Zone rules live in `ZoneResolver`: province or locality "Ciudad Autonoma de Buenos Aires" is `CABA`; a fixed list
  of conurbano postal codes is `GBA`; the postal codes of Gran La Plata, Rosario, Santa Fe, Tucumán, Salta, Córdoba,
  Mendoza, Mar del Plata and Neuquén are `Ciudades grandes del interior`; everything else is `Resto del país`.

### Quotations
Quotes every plan at once so they can be compared, and stores the quotation. The client picks the vehicle with the
catalog cascade (ending at a valuation) and the locality with the postal code lookup, then sends:

| Method | Path | Success | Errors |
|---|---|---|---|
| POST | `/api/v1/quotations` | 201 + `Location` | 400 validation / valuation not current, 404 valuation or locality, 422 driver under 18 |
| GET | `/api/v1/quotations/{id}` | 200 | 404 |
| GET | `/api/v1/quotations?email=&page=&size=&sort=` or `?code=` | 200 (paged) | 400 neither or both of `email` / `code`, invalid `email` |

```json
{ "valuationId": 455, "birthDate": "2004-05-01", "localityId": 812, "email": "driver@example.com" }
```
`birthDate` must be in the past. `email` is required (max 255) and is stored trimmed and lower-cased. The response has one entry in `plans` per plan, in this order: `CIVIL_LIABILITY`,
`THIRD_PARTY_EXTENDED`, `FULL_COVERAGE`. Each plan has an `id`, which is sent as `planId` to issue a policy from it.

Response (CAR worth 20,000 USD, GBA, driver aged 22). GET returns the same shape:
```json
{
  "id": 42,
  "code": "Q-2026-000123",
  "email": "driver@example.com",
  "status": "QUOTED",
  "quotedAt": "2026-11-25T13:00:00Z",
  "validUntil": "2026-12-10",
  "vehicle": { "description": "Toyota Corolla 2.0 XEI CVT", "make": "Toyota", "model": "Corolla",
               "modelVersion": "2.0 XEI CVT", "year": 2021, "vehicleType": "CAR", "sumInsured": 20000.00 },
  "risk": { "postalCode": "1602", "locality": "FLORIDA", "province": "Buenos Aires", "zone": "GBA", "driverAge": 22 },
  "plans": [
    {
      "id": 70,
      "name": "CIVIL_LIABILITY",
      "coverages": [ { "type": "LIABILITY", "premium": 324.00 } ],
      "netPremium": 324.00,
      "paymentOptions": [ { "installments": 1, ... }, { "installments": 12, ... } ]
    },
    {
      "name": "THIRD_PARTY_EXTENDED",
      "coverages": [ { "type": "LIABILITY", "premium": 324.00 }, { "type": "THEFT", "premium": 128.00 } ],
      "netPremium": 452.00,
      "paymentOptions": [ { "installments": 1, ... }, { "installments": 12, ... } ]
    },
    {
      "name": "FULL_COVERAGE",
      "coverages": [
        { "type": "LIABILITY", "premium": 324.00 },
        { "type": "THEFT", "premium": 128.00 },
        { "type": "DAMAGE", "premium": 462.00 }
      ],
      "netPremium": 914.00,
      "paymentOptions": [
        { "installments": 1, "adminSurcharge": 45.70, "issuanceFee": 10.00, "financingSurcharge": 0.00,
          "taxableBase": 969.70, "vat": 203.64, "totalPremium": 1173.34,
          "firstInstallment": 1173.34, "installmentAmount": 1173.34 },
        { "installments": 12, "adminSurcharge": 45.70, "issuanceFee": 10.00, "financingSurcharge": 77.58,
          "taxableBase": 1047.28, "vat": 219.93, "totalPremium": 1267.21,
          "firstInstallment": 105.61, "installmentAmount": 105.60 }
      ]
    }
  ]
}
```
Code:
- Every quotation gets a `code` such as `Q-2026-000123`: `Q-`, the issue year, and a correlative number padded to
  6 digits. The number **restarts at 1 every year** (first quotation of 2027 is `Q-2027-000001`). Codes are unique.
- `QuotationCodeGenerator` takes the next number from `quotation_code_counter` (one row per year) inside the
  quoting transaction. Concurrent quotations get distinct numbers, and a failed quotation gives its number back, so
  there are no gaps.

Search (`GET /api/v1/quotations`):
- Filters by `email` **or** by `code`: exactly one is required (none or both → 400). Matched ignoring case and
  surrounding spaces. No match → empty page.
- Only **open** quotations are returned: status `QUOTED` and `validUntil` today or later. Expired (stored or past
  `validUntil` but not yet updated by the job) and accepted quotations are left out; `GET /{id}` still returns them.
- Default order: grouped by vehicle (`valuation.id` ascending), newest first within each vehicle
  (`quotedAt` descending); default page size 20. Override with `sort=`, e.g. `sort=quotedAt,desc`.
- Each item has the same shape as `GET /api/v1/quotations/{id}` (its `status` is always `QUOTED`).

Calculation (`PremiumCalculator`):
- Coverage premiums, by plan (each plan includes the previous one):
  - `CIVIL_LIABILITY`: LIABILITY = `civilLiabilityBaseAmount` × zone `civilLiability` × age `civilLiability`
  - `THIRD_PARTY_EXTENDED`: + THEFT = sum insured × `theftRate` × zone `theft`
  - `FULL_COVERAGE`: + DAMAGE = sum insured × `damageRate` × zone `damage` × age `damage`
- Net premium = the sum of the coverage premiums.
- Per payment option: admin surcharge = net × `adminSurchargeRate`; plus the `issuanceFeeAmount`; for 12
  installments only, financing surcharge = (net + admin + fee) × `financingSurchargeRate`. Taxable base = net + admin
  + fee + financing. IVA = taxable base × `vatRate`. Total = taxable base + IVA.
- Every amount is rounded to cents (HALF_UP), and sums are built from the rounded parts, so the response adds up
  exactly. The installment amount is the total ÷ installments rounded down; the first installment takes the leftover
  cents.
- Driver age = completed years at the issue date. Under 18 → 422 `Driver under age`.
- `validUntil` = issue date + `quotation.validity-days` (`application.yml`, default 15). It is the last valid day.

Storage and status:
- Tables: `quotation` (code, email, quotedAt, validUntil, status, valuation, locality, driver birth date and age),
  `quotation_plan` (plan name, net premium, and `payment_options` as JSONB) and `quotation_plan_coverage` (the
  coverage type and premium of each plan, in plan order). Created by `V12__create_quotation.sql`.
  `V13__add_quotation_code_and_email.sql` adds `code` and `email` and the `quotation_code_counter` table (and deletes
  the quotations stored before it, which had no email).
- `status` is `QUOTED` when issued, `ACCEPTED` once a policy is created from it (see [Policies](#policies)) and
  `EXPIRED` once `validUntil` has passed. Reads report a `QUOTED` quotation past `validUntil` as `EXPIRED` right away;
  `QuotationExpirationJob` also updates the stored rows every night (`quotation.expiration-cron`, default 00:05).

### Policies
Issues a policy from one plan of an open quotation, with one of that plan's payment options. In the same
transaction it creates the insured and the insured vehicle and marks the quotation `ACCEPTED`.

| Method | Path | Success | Errors |
|---|---|---|---|
| POST | `/api/v1/policies` | 201 + `Location` | 400 validation / plan not in quotation / installments not offered / `validFrom` out of range, 404 quotation, 409 quotation not open / plate already insured for that period / duplicate document or email |
| GET | `/api/v1/policies/{id}` | 200 | 404 |

Request:
```json
{
  "quotationId": 42, "planId": 72, "installments": 12, "paymentMethod": "CARD", "validFrom": "2026-12-01",
  "firstName": "Jane", "lastName": "Doe", "documentNumber": "30123456", "email": "jane@example.com",
  "phone": "+54 11 5555 1234", "street": "Av. Rivadavia 1234",
  "licenseNumber": "B-123456", "licenseIssueDate": "2010-06-01",
  "licensePlate": "AB 123 CD", "engineNumber": "ENG-12345", "chassisNumber": "8AP17000000123456"
}
```
Response (GET returns the same shape):
```json
{
  "id": 1,
  "number": "AU-2026-0000001",
  "validFrom": "2026-12-01",
  "validTo": "2027-12-01",
  "quotation": { "id": 42, "code": "Q-2026-000123" },
  "plan": { "id": 72, "name": "FULL_COVERAGE", "coverages": [ ... ], "netPremium": 914.00 },
  "paymentOption": { "installments": 12, "totalPremium": 1267.21, "firstInstallment": 105.61, ... },
  "paymentMethod": "CARD",
  "insured": { ... },
  "vehicle": { ... },
  "locality": { "id": 812, "name": "FLORIDA", "province": "Buenos Aires", "postalCode": "1602",
                "zone": { "id": 2, "name": "GBA" } },
  "createdAt": "..."
}
```
`insured` and `vehicle` have the same shape as in [Insureds](#insureds) and [Insured vehicles](#insured-vehicles).

What comes from where:
- From the **quotation**: the vehicle's valuation (catalog entry and year), the locality (stored on the policy and
  used for the insured's address), the insured's `dateOfBirth` (the quoted driver birth date), and the plan with
  its coverages and premiums.
- From the **request**: the chosen plan and payment option, payment method, start date, the insured's personal
  data and street, and the vehicle's plate, engine and chassis numbers.

Rules:
- The quotation must be **open**: `QUOTED` and `validUntil` today or later. Otherwise 409. So a quotation becomes
  at most one policy, and a concurrent second request fails with 409 on the quotation's version.
- `planId` must be one of the quotation's `plans[].id` (otherwise 400).
- `installments` picks the payment option: it must be `1` or `12` (the options every plan is quoted with), otherwise
  400. The chosen option is copied to the policy as it was quoted (`payment_option` JSONB).
- `paymentMethod`: `CARD` or `CBU`.
- `validFrom`: today up to today + 30 days, inclusive (otherwise 400). `validTo` = `validFrom` + 1 year. It is
  **exclusive**: the policy covers [`validFrom`, `validTo`).
- `licensePlate`: an Argentine plate, in any case, with optional spaces or hyphens: old car `ABC123`, Mercosur car
  `AB123CD`, old motorcycle `123ABC` or Mercosur motorcycle `A123BCD`. Stored normalized (`AB123CD`).
- A plate can't have **two policies with overlapping periods**: an existing policy with
  `validFrom < new validTo` and `validTo > new validFrom` returns 409. A renewal starting on the day the current
  policy ends is allowed. Requests for the same plate are serialized with a PostgreSQL advisory lock, so two
  concurrent requests can't both pass the check.
- `engineNumber`: 5 to 20 letters, digits or hyphens. `chassisNumber`: a 17-character VIN (letters except I, O and
  Q, and digits).
- Insured fields are validated like the insured: required names, `email` format, `phone` (7–30 digits, spaces,
  `()-`, optional leading `+`), `licenseIssueDate` today or earlier. An insured with the same document number or
  email already existing returns 409. Reusing an existing insured will come later.
- Any failure rolls the whole request back: no insured, vehicle or policy is stored and the quotation stays
  `QUOTED`.

Number:
- `AU-`, the issue year, and a correlative number padded to 7 digits, e.g. `AU-2026-0000123`. It restarts at 1
  every year, the same way quotation codes do (`PolicyNumberGenerator`, table `policy_number_counter`).

Storage: table `policy` (number, insured, insured vehicle, quotation plan (unique), locality, validFrom, validTo,
payment option JSONB, payment method). Created by `V14__create_policy.sql`, which also adds the vehicle's engine
and chassis numbers, drops the unique constraint on the plate, renames the address columns, and deletes the
insureds and vehicles stored before it (development data).

### Quotation parameters
Reference data used by the quotation calculation. Not exposed in the API.

- **Zone factors** (`quotation_zone_factor`, entity `QuotationZoneFactor`): one row per zone with the multipliers
  applied to each coverage price (see Localities).
- **Vehicle parameters** (`quotation_vehicle_parameter`, entity `QuotationVehicleParameter`): one row per vehicle
  type with a `civilLiabilityBaseAmount`, which is a fixed base premium for civil liability coverage (USD, like the
  vehicle valuations, and not linked to them), and a `theftRate` and `damageRate` applied to the vehicle valuation. Rates are fractions: 4% is stored as `0.04`. They must be greater than 0 and
  less than 1. They are seeded with approximate values by `V9__rename_quotation_factor_and_add_vehicle_parameter.sql`.
- **Driver age factors** (`quotation_driver_age_factor`, entity `QuotationDriverAgeFactor`): multipliers for the
  civil liability and damage coverage prices (no theft) by driver age range: 18–20, 21–24, 25–29, 30–64, 65–74 and
  75 or more. Ranges are inclusive on both ends (21 and 24 are both in 21–24), and the last range has no `maxAge`
  (no upper limit). The database rejects overlapping ranges, gaps between ranges and a last range with a `maxAge`. The
  gap check runs at commit, so a range can be split in one transaction. Use
  `QuotationDriverAgeFactorRepository.findByAge(age)` to look one up. Seeded by
  `V10__create_quotation_driver_age_factor.sql`.
- **Charges** (`quotation_charge`, entity `QuotationCharge`): a single row (id 1) with the `adminSurchargeRate`
  (default 0.05), `issuanceFeeAmount` (default 10.00 USD), `financingSurchargeRate` (default 0.08, 12 installments
  only) and the IVA `vatRate` (default 0.21). Seeded by `V11__create_quotation_charge.sql`.

Errors use RFC 7807 `application/problem+json`; validation errors include a per-field `errors` map.

## Structure
```
com.carinsurance
├── common/      cross-cutting config and exception handling
├── insured/     insureds and their vehicles (InsuredVehicle → catalog valuation), created by policies
├── location/    localities by postal code and their pricing zones (read-only reference data)
├── policy/      policies issued from quoted plans
├── quotation/   stored quotations, their calculation and parameters (zone, vehicle type, driver age, charges)
└── vehicle/     vehicle catalog and valuations (read-only reference data)
```
New domains (claims, ...) should get their own feature package alongside `policy/`.
