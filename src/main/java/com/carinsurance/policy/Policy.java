package com.carinsurance.policy;

import com.carinsurance.insured.Insured;
import com.carinsurance.insured.InsuredVehicle;
import com.carinsurance.location.Locality;
import com.carinsurance.quotation.PaymentOption;
import com.carinsurance.quotation.QuotationPlan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A policy issued from a quoted plan, for an insured and one of their vehicles. It covers
 * [{@link #getValidFrom() validFrom}, {@link #getValidTo() validTo}): one year.
 */
@Entity
@Table(name = "policy")
@EntityListeners(AuditingEntityListener.class)
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Correlative number such as {@code AU-2026-0000123}, see {@link PolicyNumberGenerator}. */
    @Column(nullable = false, unique = true, updatable = false, length = 20)
    private String number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insured_id", nullable = false, updatable = false)
    private Insured insured;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insured_vehicle_id", nullable = false, updatable = false)
    private InsuredVehicle insuredVehicle;

    /** The quoted plan; its quotation is reached through it. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quotation_plan_id", nullable = false, unique = true, updatable = false)
    private QuotationPlan quotationPlan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "locality_id", nullable = false, updatable = false)
    private Locality locality;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    /** Exclusive: the day after the last covered day. */
    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    /** The selected payment option of the quoted plan, copied as it was quoted. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payment_option", nullable = false, columnDefinition = "jsonb")
    private PaymentOption paymentOption;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 10)
    private PaymentMethod paymentMethod;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Policy() {}

    /** A policy covering one year from {@code validFrom}. */
    public Policy(
            String number,
            Insured insured,
            InsuredVehicle insuredVehicle,
            QuotationPlan quotationPlan,
            Locality locality,
            LocalDate validFrom,
            PaymentOption paymentOption,
            PaymentMethod paymentMethod) {
        this.number = number;
        this.insured = insured;
        this.insuredVehicle = insuredVehicle;
        this.quotationPlan = quotationPlan;
        this.locality = locality;
        this.validFrom = validFrom;
        this.validTo = validToFor(validFrom);
        this.paymentOption = paymentOption;
        this.paymentMethod = paymentMethod;
    }

    /** The (exclusive) end of a policy starting on {@code validFrom}: one year later. */
    public static LocalDate validToFor(LocalDate validFrom) {
        return validFrom.plusYears(1);
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public Insured getInsured() {
        return insured;
    }

    public InsuredVehicle getInsuredVehicle() {
        return insuredVehicle;
    }

    public QuotationPlan getQuotationPlan() {
        return quotationPlan;
    }

    public Locality getLocality() {
        return locality;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public PaymentOption getPaymentOption() {
        return paymentOption;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
