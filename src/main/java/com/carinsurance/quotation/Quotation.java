package com.carinsurance.quotation;

import com.carinsurance.common.exception.ConflictException;
import com.carinsurance.location.Locality;
import com.carinsurance.vehicle.VehicleValuation;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A stored quotation: every plan quoted for a vehicle valuation, a locality and a driver. It is valid through
 * {@link #getValidUntil()} (inclusive). It is identified to people by its {@link #getCode() code} and belongs to the
 * {@link #getEmail() email} entered when it was requested.
 */
@Entity
@Table(name = "quotation")
public class Quotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Correlative code such as {@code Q-2026-000123}, see {@link QuotationCodeGenerator}. */
    @Column(nullable = false, unique = true, updatable = false, length = 20)
    private String code;

    /** Entered when requesting the quotation; stored trimmed and lower-cased. */
    @Column(nullable = false, updatable = false, length = 255)
    private String email;

    @Column(name = "quoted_at", nullable = false, updatable = false)
    private Instant quotedAt;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuotationStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "valuation_id", nullable = false, updatable = false)
    private VehicleValuation valuation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "locality_id", nullable = false, updatable = false)
    private Locality locality;

    @Column(name = "driver_birth_date", nullable = false, updatable = false)
    private LocalDate driverBirthDate;

    @Column(name = "driver_age", nullable = false, updatable = false)
    private Integer driverAge;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<QuotationPlan> plans = new ArrayList<>();

    @Version
    private Long version;

    protected Quotation() {}

    /** A new quotation, in status {@link QuotationStatus#QUOTED}. */
    public Quotation(
            String code,
            String email,
            Instant quotedAt,
            LocalDate validUntil,
            VehicleValuation valuation,
            Locality locality,
            LocalDate driverBirthDate,
            int driverAge) {
        this.code = code;
        this.email = email;
        this.quotedAt = quotedAt;
        this.validUntil = validUntil;
        this.status = QuotationStatus.QUOTED;
        this.valuation = valuation;
        this.locality = locality;
        this.driverBirthDate = driverBirthDate;
        this.driverAge = driverAge;
    }

    public void addPlan(QuotationPlan plan) {
        plans.add(plan);
        plan.setQuotation(this);
    }

    /**
     * The status as of {@code today}: a {@link QuotationStatus#QUOTED} quotation whose valid-until date has passed is
     * {@link QuotationStatus#EXPIRED}, even before the expiration job updates the stored status.
     */
    public QuotationStatus statusOn(LocalDate today) {
        return status == QuotationStatus.QUOTED && today.isAfter(validUntil) ? QuotationStatus.EXPIRED : status;
    }

    /**
     * Marks the quotation as {@link QuotationStatus#ACCEPTED}, when a policy is created from it.
     *
     * @throws ConflictException unless it is {@link QuotationStatus#QUOTED} as of {@code today}
     */
    public void accept(LocalDate today) {
        QuotationStatus current = statusOn(today);
        if (current != QuotationStatus.QUOTED) {
            throw new ConflictException(
                    "Quotation %s is %s; only a QUOTED quotation can become a policy".formatted(code, current));
        }
        status = QuotationStatus.ACCEPTED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public String getEmail() {
        return email;
    }

    public Instant getQuotedAt() {
        return quotedAt;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    /** The stored status. Use {@link #statusOn(LocalDate)} for the status as of a date. */
    public QuotationStatus getStatus() {
        return status;
    }

    public VehicleValuation getValuation() {
        return valuation;
    }

    public Locality getLocality() {
        return locality;
    }

    public LocalDate getDriverBirthDate() {
        return driverBirthDate;
    }

    public Integer getDriverAge() {
        return driverAge;
    }

    /** In plan order. */
    public List<QuotationPlan> getPlans() {
        return Collections.unmodifiableList(plans);
    }

    public Long getVersion() {
        return version;
    }
}
