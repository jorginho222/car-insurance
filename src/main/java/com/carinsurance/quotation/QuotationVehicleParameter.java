package com.carinsurance.quotation;

import com.carinsurance.vehicle.VehicleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Base values a vehicle type uses when quoting. One per vehicle type. The civil liability base amount is a fixed base
 * premium in USD that does not depend on the vehicle valuation. Theft and damage rates are fractions of the vehicle
 * valuation (0.04 = 4%).
 */
@Entity
@Table(name = "quotation_vehicle_parameter")
public class QuotationVehicleParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, unique = true, length = 20)
    private VehicleType vehicleType;

    @Column(name = "civil_liability_base_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal civilLiabilityBaseAmount;

    @Column(name = "theft_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal theftRate;

    @Column(name = "damage_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal damageRate;

    protected QuotationVehicleParameter() {}

    public QuotationVehicleParameter(
            VehicleType vehicleType, BigDecimal civilLiabilityBaseAmount, BigDecimal theftRate, BigDecimal damageRate) {
        this.vehicleType = vehicleType;
        this.civilLiabilityBaseAmount = civilLiabilityBaseAmount;
        this.theftRate = theftRate;
        this.damageRate = damageRate;
    }

    public Long getId() {
        return id;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public BigDecimal getCivilLiabilityBaseAmount() {
        return civilLiabilityBaseAmount;
    }

    public BigDecimal getTheftRate() {
        return theftRate;
    }

    public BigDecimal getDamageRate() {
        return damageRate;
    }
}
