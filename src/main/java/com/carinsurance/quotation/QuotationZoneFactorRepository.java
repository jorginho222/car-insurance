package com.carinsurance.quotation;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuotationZoneFactorRepository extends JpaRepository<QuotationZoneFactor, Long> {

    Optional<QuotationZoneFactor> findByZoneId(Long zoneId);
}
