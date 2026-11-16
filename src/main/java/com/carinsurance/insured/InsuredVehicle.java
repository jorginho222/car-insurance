package com.carinsurance.insured;

import com.carinsurance.vehicle.VehicleValuation;
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
import java.time.Instant;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** A vehicle owned by an insured. Make, model, version, type and year come from the selected catalog valuation. */
@Entity
@Table(name = "insured_vehicle")
@EntityListeners(AuditingEntityListener.class)
public class InsuredVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insured_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Insured insured;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "valuation_id", nullable = false)
    private VehicleValuation valuation;

    @Column(name = "license_plate", nullable = false, unique = true, length = 20)
    private String licensePlate;

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

    public Insured getInsured() {
        return insured;
    }

    /**
     * Only meant to be called by {@link Insured#addVehicle(InsuredVehicle)} and
     * {@link Insured#removeVehicle(InsuredVehicle)}, which keep both sides of the association in sync.
     */
    void setInsured(Insured insured) {
        this.insured = insured;
    }

    public VehicleValuation getValuation() {
        return valuation;
    }

    public void setValuation(VehicleValuation valuation) {
        this.valuation = valuation;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
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
