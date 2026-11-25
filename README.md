# Car Insurance

Spring Boot 4.1 / Java 21 backend for a car insurance company. Currently provides CRUD for **insureds** (policyholders) and their **vehicles**, plus a read-only
**vehicle catalog** (make, model, version, and valuations per year) used to pick each vehicle.

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

- API: `http://localhost:8080/api/v1/insureds`, `http://localhost:8080/api/v1/vehicle-catalog`
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
| Method | Path | Success | Errors |
|---|---|---|---|
| POST | `/api/v1/insureds` | 201 + `Location` | 400 validation, 409 duplicate document/email |
| GET | `/api/v1/insureds/{id}` | 200 | 404 |
| GET | `/api/v1/insureds?page=&size=&sort=` | 200 (paged) | |
| PUT | `/api/v1/insureds/{id}` | 200 | 400, 404, 409 |
| DELETE | `/api/v1/insureds/{id}` | 204 (also deletes the insured's vehicles) | 404 |

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
  creating the insured's vehicle.
- The catalog is filled with **fake data** by migration `V5__seed_vehicle_catalog.sql` (about 9 makes, all vehicle
  types). It includes a price list from 2026-07-01 that replaces the 2026-01-01 one for some makes, and a future
  2027-01-01 list for Toyota. Later this will come from an external provider.

### Insured vehicles
An insured owns one or more vehicles (1 to N). Each vehicle references a catalog **valuation**, which gives its
make, model, version, type and year.

| Method | Path | Success | Errors |
|---|---|---|---|
| POST | `/api/v1/insureds/{insuredId}/vehicles` | 201 + `Location` | 400 validation/valuation not current, 404 unknown insured/valuation, 409 duplicate plate |
| GET | `/api/v1/insureds/{insuredId}/vehicles/{id}` | 200 | 404 |
| GET | `/api/v1/insureds/{insuredId}/vehicles?page=&size=&sort=` | 200 (paged) | 404 unknown insured |
| PUT | `/api/v1/insureds/{insuredId}/vehicles/{id}` | 200 | 400, 404, 409 |
| DELETE | `/api/v1/insureds/{insuredId}/vehicles/{id}` | 204 | 404 |

Request body:
```json
{ "valuationId": 42, "licensePlate": "ABC-123" }
```
Response:
```json
{
  "id": 1, "insuredId": 3, "licensePlate": "ABC-123",
  "catalog": { "id": 7, "make": "Toyota", "model": "Corolla", "modelVersion": "2.0 XEI CVT", "vehicleType": "CAR" },
  "valuation": { "id": 42, "catalogId": 7, "year": 2020, "value": 23456.78, "validFrom": "2026-07-01" },
  "createdAt": "...", "updatedAt": "..."
}
```
- `valuationId` must be the **current** valuation for its year (as listed in catalog step 5). Otherwise the
  request returns 400.
- On `PUT`, resending the vehicle's existing `valuationId` is always accepted, even if a newer price list has
  replaced it since. A different `valuationId` must be current.
- `licensePlate` is trimmed and upper-cased, and must be unique across all insureds (letters, digits, spaces,
  hyphens; max 20).
- A vehicle is only reachable under its own insured. Any other `insuredId` in the path returns 404. Vehicles cannot
  be transferred to another insured.
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
Quotes every plan at once so they can be compared. Nothing is stored. The client picks the vehicle with the catalog
cascade (ending at a valuation) and the locality with the postal code lookup, then sends:

| Method | Path | Success | Errors |
|---|---|---|---|
| POST | `/api/v1/quotations` | 200 | 400 validation / valuation not current, 404 valuation or locality, 422 driver under 18 |

```json
{ "valuationId": 455, "birthDate": "2004-05-01", "localityId": 812 }
```
`birthDate` must be in the past. The response has one entry in `plans` per plan, in this order: `CIVIL_LIABILITY`,
`THIRD_PARTY_EXTENDED`, `FULL_COVERAGE`.

Response (CAR worth 20,000 USD, GBA, driver aged 22):
```json
{
  "validUntil": "2026-12-10",
  "vehicle": { "description": "Toyota Corolla 2.0 XEI CVT", "make": "Toyota", "model": "Corolla",
               "modelVersion": "2.0 XEI CVT", "year": 2021, "vehicleType": "CAR", "sumInsured": 20000.00 },
  "risk": { "postalCode": "1602", "locality": "FLORIDA", "province": "Buenos Aires", "zone": "GBA", "driverAge": 22 },
  "plans": [
    {
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
- `validUntil` = issue date + `quotation.validity-days` (`application.yml`, default 15).

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
├── insured/     insureds and their vehicles (InsuredVehicle → catalog valuation)
├── location/    localities by postal code and their pricing zones (read-only reference data)
├── quotation/   quote calculation and its parameters (zone, vehicle type, driver age, charges)
└── vehicle/     vehicle catalog and valuations (read-only reference data)
```
New domains (policies, claims, ...) should get their own feature package alongside `insured/`.
