-- Existing rows are backfilled as CAR; the default is then dropped so new rows must set a type explicitly.
ALTER TABLE vehicle ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'CAR';
ALTER TABLE vehicle ALTER COLUMN type DROP DEFAULT;
ALTER TABLE vehicle ADD CONSTRAINT ck_vehicle_type
    CHECK (type IN ('CAR', 'MOTORCYCLE', 'SUV', 'PICKUP', 'VAN', 'TRUCK', 'BUS'));
