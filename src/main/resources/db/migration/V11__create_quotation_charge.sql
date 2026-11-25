-- Charges added on top of the net premium when quoting. A single row (id is always 1).
-- Rates are fractions: 0.0500 = 5%. The issuance fee is a fixed amount in USD, like the vehicle valuations.
--   taxable base = net premium + net premium x admin_surcharge_rate + issuance_fee_amount
--                  (+ that subtotal x financing_surcharge_rate when paying in installments)
--   total        = taxable base + taxable base x vat_rate (IVA)
CREATE TABLE quotation_charge (
    id                        SMALLINT       PRIMARY KEY DEFAULT 1,
    admin_surcharge_rate      NUMERIC(6, 4)  NOT NULL DEFAULT 0.0500,
    issuance_fee_amount       NUMERIC(14, 2) NOT NULL DEFAULT 10.00,
    financing_surcharge_rate  NUMERIC(6, 4)  NOT NULL DEFAULT 0.0800,
    vat_rate                  NUMERIC(6, 4)  NOT NULL DEFAULT 0.2100,
    CONSTRAINT ck_quotation_charge_single_row CHECK (id = 1),
    CONSTRAINT ck_quotation_charge_admin_surcharge_rate CHECK (admin_surcharge_rate >= 0 AND admin_surcharge_rate < 1),
    CONSTRAINT ck_quotation_charge_issuance_fee_amount CHECK (issuance_fee_amount >= 0),
    CONSTRAINT ck_quotation_charge_financing_surcharge_rate
        CHECK (financing_surcharge_rate >= 0 AND financing_surcharge_rate < 1),
    CONSTRAINT ck_quotation_charge_vat_rate CHECK (vat_rate >= 0 AND vat_rate < 1)
);

INSERT INTO quotation_charge DEFAULT VALUES;
