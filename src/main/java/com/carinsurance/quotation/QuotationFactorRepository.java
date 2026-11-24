package com.carinsurance.quotation;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuotationFactorRepository extends JpaRepository<QuotationFactor, Long> {

    Optional<QuotationFactor> findByZoneId(Long zoneId);
}
