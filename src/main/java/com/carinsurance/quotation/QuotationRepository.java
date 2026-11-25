package com.carinsurance.quotation;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    /** The quotation with its valuation (and catalog entry) and locality (and zone). Plans load lazily. */
    @EntityGraph(attributePaths = {"valuation.catalog", "locality.zone"})
    Optional<Quotation> findDetailedById(Long id);

    /** Marks as expired every quoted quotation whose valid-until date is before {@code today}. */
    @Modifying
    @Query("""
            update Quotation q set q.status = com.carinsurance.quotation.QuotationStatus.EXPIRED,
                q.version = q.version + 1
            where q.status = com.carinsurance.quotation.QuotationStatus.QUOTED and q.validUntil < :today""")
    int expireQuotedBefore(@Param("today") LocalDate today);
}
