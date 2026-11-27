package com.carinsurance.policy;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    /** The policy with everything its response shows. Plan coverages load lazily. */
    @EntityGraph(
            attributePaths = {"insured", "insuredVehicle.valuation.catalog", "quotationPlan.quotation", "locality.zone"
            })
    Optional<Policy> findDetailedById(Long id);

    /**
     * Whether a policy of a vehicle with {@code licensePlate} (normalized) covers any day of [{@code from},
     * {@code to}).
     */
    @Query("""
            select count(p) > 0 from Policy p
            where p.insuredVehicle.licensePlate = :licensePlate and p.validFrom < :to and p.validTo > :from""")
    boolean existsOverlapping(
            @Param("licensePlate") String licensePlate, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
