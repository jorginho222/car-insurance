package com.carinsurance.quotation;

import com.carinsurance.vehicle.VehicleType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuotationVehicleParameterRepository extends JpaRepository<QuotationVehicleParameter, Long> {

    Optional<QuotationVehicleParameter> findByVehicleType(VehicleType vehicleType);
}
