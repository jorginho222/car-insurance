package com.carinsurance.vehicle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * The value of a catalog vehicle for one model year, effective from {@link #validFrom}. A catalog entry and year can
 * have several valuations over time; the current one is the latest whose {@code validFrom} is not in the future.
 */
@Entity
@Table(name = "vehicle_valuation")
@EntityListeners(AuditingEntityListener.class)
public class VehicleValuation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catalog_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private VehicleCatalog catalog;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal value;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public VehicleCatalog getCatalog() {
        return catalog;
    }

    /**
     * Only meant to be called by {@link VehicleCatalog#addValuation(VehicleValuation)} and
     * {@link VehicleCatalog#removeValuation(VehicleValuation)}, which keep both sides of the association in sync.
     */
    public void setCatalog(VehicleCatalog catalog) {
        this.catalog = catalog;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
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
