package com.carinsurance.quotation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Charges added on top of the net premium when quoting. There is a single row ({@link #SINGLETON_ID}). Rates are
 * fractions (0.05 = 5%); the issuance fee is a fixed amount in USD, like the vehicle valuations.
 */
@Entity
@Table(name = "quotation_charge")
public class QuotationCharge {

    public static final short SINGLETON_ID = 1;

    @Id
    private Short id;

    @Column(name = "admin_surcharge_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal adminSurchargeRate;

    @Column(name = "issuance_fee_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal issuanceFeeAmount;

    @Column(name = "financing_surcharge_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal financingSurchargeRate;

    @Column(name = "vat_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal vatRate;

    protected QuotationCharge() {}

    public QuotationCharge(
            Short id,
            BigDecimal adminSurchargeRate,
            BigDecimal issuanceFeeAmount,
            BigDecimal financingSurchargeRate,
            BigDecimal vatRate) {
        this.id = id;
        this.adminSurchargeRate = adminSurchargeRate;
        this.issuanceFeeAmount = issuanceFeeAmount;
        this.financingSurchargeRate = financingSurchargeRate;
        this.vatRate = vatRate;
    }

    public Short getId() {
        return id;
    }

    public BigDecimal getAdminSurchargeRate() {
        return adminSurchargeRate;
    }

    public BigDecimal getIssuanceFeeAmount() {
        return issuanceFeeAmount;
    }

    public BigDecimal getFinancingSurchargeRate() {
        return financingSurchargeRate;
    }

    /** The IVA rate applied to the taxable base. */
    public BigDecimal getVatRate() {
        return vatRate;
    }
}
