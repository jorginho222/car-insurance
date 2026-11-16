package com.carinsurance.vehicle;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleValuationRepository extends JpaRepository<VehicleValuation, Long> {

    /** For each year of the catalog entry, the valuation with the latest {@code validFrom} on or before {@code today}. */
    @Query("""
            select v from VehicleValuation v
            where v.catalog.id = :catalogId
              and v.validFrom = (
                  select max(v2.validFrom) from VehicleValuation v2
                  where v2.catalog.id = v.catalog.id and v2.year = v.year and v2.validFrom <= :today)
            order by v.year desc""")
    List<VehicleValuation> findCurrentByCatalogId(@Param("catalogId") Long catalogId, @Param("today") LocalDate today);

    /** True when another valuation for the same catalog entry and year has replaced the given one by {@code today}. */
    @Query("""
            select count(v) > 0 from VehicleValuation v
            where v.catalog.id = :catalogId and v.year = :year
              and v.validFrom > :validFrom and v.validFrom <= :today""")
    boolean existsNewerCurrent(
            @Param("catalogId") Long catalogId,
            @Param("year") Integer year,
            @Param("validFrom") LocalDate validFrom,
            @Param("today") LocalDate today);

    @EntityGraph(attributePaths = "catalog")
    Optional<VehicleValuation> findWithCatalogById(Long id);
}
