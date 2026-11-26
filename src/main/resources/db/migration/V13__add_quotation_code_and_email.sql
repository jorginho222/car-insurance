-- Every quotation gets a correlative code and the email entered by whoever requested it.
-- Existing quotations are development data without an email, so they are removed (plans and coverages cascade).
DELETE FROM quotation;

-- Last correlative number issued per year. The number restarts at 1 every year. Codes are taken with an
-- INSERT ... ON CONFLICT DO UPDATE ... RETURNING, which locks the year's row until the quotation commits.
CREATE TABLE quotation_code_counter (
    year        INTEGER PRIMARY KEY,
    last_value  BIGINT  NOT NULL,
    CONSTRAINT ck_quotation_code_counter_last_value CHECK (last_value > 0)
);

-- code: 'Q-' || issue year || '-' || number padded to 6 digits, e.g. Q-2026-000123.
-- email: stored trimmed and lower-cased.
ALTER TABLE quotation
    ADD COLUMN code  VARCHAR(20)  NOT NULL,
    ADD COLUMN email VARCHAR(255) NOT NULL,
    ADD CONSTRAINT uk_quotation_code UNIQUE (code);

CREATE INDEX idx_quotation_email ON quotation (email);
