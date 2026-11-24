package com.carinsurance.quotation;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuotationDriverAgeFactorRepository extends JpaRepository<QuotationDriverAgeFactor, Long> {

    /** The range that contains {@code age}, if any. Ranges don't overlap, so there is at most one. */
    @Query("select f from QuotationDriverAgeFactor f where f.minAge <= :age and (f.maxAge is null or f.maxAge >= :age)")
    Optional<QuotationDriverAgeFactor> findByAge(@Param("age") int age);
}
