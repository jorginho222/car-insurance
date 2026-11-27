package com.carinsurance.insured;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsuredVehicleRepository extends JpaRepository<InsuredVehicle, Long> {

    @EntityGraph(attributePaths = {"valuation", "valuation.catalog"})
    Optional<InsuredVehicle> findByIdAndInsuredId(Long id, Long insuredId);

    @EntityGraph(attributePaths = {"valuation", "valuation.catalog"})
    Page<InsuredVehicle> findByInsuredId(Long insuredId, Pageable pageable);
}
