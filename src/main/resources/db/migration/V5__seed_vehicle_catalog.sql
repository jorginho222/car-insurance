-- Fake catalog and valuations for testing the vehicle selection flow.
-- To be replaced by data from an external provider (import job) in the future.

CREATE TEMPORARY TABLE seed_catalog (
    make          VARCHAR(100),
    model         VARCHAR(100),
    model_version VARCHAR(150),
    vehicle_type  VARCHAR(20),
    base_price    NUMERIC(14, 2),  -- value of a 2026 unit
    first_year    INTEGER
) ON COMMIT DROP;

INSERT INTO seed_catalog VALUES
    ('Toyota',     'Corolla',  '1.8 XLI CVT',          'CAR',        26000, 2018),
    ('Toyota',     'Corolla',  '2.0 XEI CVT',          'CAR',        29500, 2018),
    ('Toyota',     'Corolla',  '2.0 SEG CVT',          'CAR',        33000, 2018),
    ('Toyota',     'Hilux',    '2.4 TDI DX 4x2',       'PICKUP',     38000, 2016),
    ('Toyota',     'Hilux',    '2.8 TDI SRX 4x4 AT',   'PICKUP',     52000, 2016),
    ('Toyota',     'RAV4',     '2.5 Hybrid XLE',       'SUV',        45000, 2019),
    ('Toyota',     'RAV4',     '2.5 Hybrid Limited',   'SUV',        50000, 2019),
    ('Ford',       'Focus',    '1.6 S',                'CAR',        21000, 2015),
    ('Ford',       'Focus',    '2.0 Titanium AT',      'CAR',        27000, 2015),
    ('Ford',       'Ranger',   '2.2 TDCi XL 4x2',      'PICKUP',     36000, 2016),
    ('Ford',       'Ranger',   '3.2 TDCi Limited 4x4', 'PICKUP',     49000, 2016),
    ('Ford',       'Transit',  '2.2 TDCi Furgon',      'VAN',        42000, 2017),
    ('Ford',       'Cargo',    '1723 Chassis',         'TRUCK',      95000, 2014),
    ('Chevrolet',  'Onix',     '1.0 LT',               'CAR',        17000, 2020),
    ('Chevrolet',  'Onix',     '1.0 Turbo Premier AT', 'CAR',        21000, 2020),
    ('Chevrolet',  'S10',      '2.8 TD LT 4x2',        'PICKUP',     39000, 2017),
    ('Chevrolet',  'S10',      '2.8 TD High Country',  'PICKUP',     51000, 2017),
    ('Chevrolet',  'Tracker',  '1.2 Turbo LTZ AT',     'SUV',        28000, 2021),
    ('Volkswagen', 'Gol',      '1.6 Trendline',        'CAR',        14000, 2015),
    ('Volkswagen', 'Amarok',   '2.0 TDI Comfortline',  'PICKUP',     43000, 2016),
    ('Volkswagen', 'Amarok',   '3.0 V6 Extreme AT',    'PICKUP',     60000, 2017),
    ('Volkswagen', 'Taos',     '1.4 TSI Highline',     'SUV',        36000, 2021),
    ('Volkswagen', 'Saveiro',  '1.6 Cabina Simple',    'PICKUP',     18000, 2015),
    ('Fiat',       'Cronos',   '1.3 Drive',            'CAR',        17500, 2018),
    ('Fiat',       'Cronos',   '1.8 Precision AT',     'CAR',        21000, 2018),
    ('Fiat',       'Toro',     '1.8 Freedom AT',       'PICKUP',     30000, 2017),
    ('Fiat',       'Toro',     '2.0 TD Volcano 4x4',   'PICKUP',     38000, 2017),
    ('Fiat',       'Ducato',   '2.3 Multijet Cargo',   'VAN',        40000, 2016),
    ('Honda',      'Civic',    '2.0 EX CVT',           'CAR',        30000, 2017),
    ('Honda',      'Civic',    '1.5 Turbo EXL CVT',    'CAR',        35000, 2017),
    ('Honda',      'CR-V',     '1.5 Turbo EXT AWD',    'SUV',        44000, 2018),
    ('Honda',      'CG 160',   'Titan',                'MOTORCYCLE',  3200, 2016),
    ('Honda',      'CG 160',   'Fan',                  'MOTORCYCLE',  2800, 2016),
    ('Renault',    'Sandero',  '1.6 Life',             'CAR',        15000, 2017),
    ('Renault',    'Duster',   '1.6 Intens 4x2',       'SUV',        24000, 2017),
    ('Renault',    'Kangoo',   '1.6 Express Confort',  'VAN',        22000, 2016),
    ('Renault',    'Master',   '2.3 dCi Furgon L2H2',  'VAN',        45000, 2017),
    ('Yamaha',     'FZ',       'FZ-S 150 FI',          'MOTORCYCLE',  2900, 2017),
    ('Yamaha',     'MT-03',    '321 ABS',              'MOTORCYCLE',  7500, 2019),
    ('Yamaha',     'MT-03',    '321 ABS Dark Night',   'MOTORCYCLE',  7900, 2020),
    ('Mercedes-Benz', 'Sprinter', '415 CDI Minibus 19+1', 'BUS',    80000, 2016),
    ('Mercedes-Benz', 'Sprinter', '515 CDI Furgon',    'VAN',        65000, 2016),
    ('Mercedes-Benz', 'Atego',    '1726 Chassis',      'TRUCK',     110000, 2015),
    ('Mercedes-Benz', 'OF',       '1721 Urbano',       'BUS',       150000, 2015);

INSERT INTO vehicle_catalog (make, model, model_version, vehicle_type, created_at, updated_at)
SELECT make, model, model_version, vehicle_type, now(), now()
FROM seed_catalog;

-- Price list 1, valid for every catalog entry from 2026-01-01.
-- Value drops ~8% for every year of age relative to a 2026 unit.
INSERT INTO vehicle_valuation (catalog_id, year, value, valid_from, created_at, updated_at)
SELECT c.id, y.year, ROUND(s.base_price * POWER(0.92, 2026 - y.year), 2), DATE '2026-01-01', now(), now()
FROM seed_catalog s
JOIN vehicle_catalog c
  ON c.make = s.make AND c.model = s.model AND c.model_version = s.model_version
CROSS JOIN LATERAL generate_series(s.first_year, 2026) AS y(year);

-- Price list 2 (+4%), replacing list 1 from 2026-07-01 for part of the catalog.
INSERT INTO vehicle_valuation (catalog_id, year, value, valid_from, created_at, updated_at)
SELECT v.catalog_id, v.year, ROUND(v.value * 1.04, 2), DATE '2026-07-01', now(), now()
FROM vehicle_valuation v
JOIN vehicle_catalog c ON c.id = v.catalog_id
WHERE c.make IN ('Toyota', 'Ford', 'Volkswagen', 'Honda')
  AND v.valid_from = DATE '2026-01-01';

-- Future price list (+6% over list 2) for Toyota. It is hidden until 2027-01-01.
INSERT INTO vehicle_valuation (catalog_id, year, value, valid_from, created_at, updated_at)
SELECT v.catalog_id, v.year, ROUND(v.value * 1.06, 2), DATE '2027-01-01', now(), now()
FROM vehicle_valuation v
JOIN vehicle_catalog c ON c.id = v.catalog_id
WHERE c.make = 'Toyota'
  AND v.valid_from = DATE '2026-07-01';
