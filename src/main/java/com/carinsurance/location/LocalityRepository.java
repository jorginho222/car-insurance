package com.carinsurance.location;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LocalityRepository extends JpaRepository<Locality, Long> {

    @Query("""
            select l from Locality l join fetch l.zone
            where l.postalCode = :postalCode
            order by l.province, l.name""")
    List<Locality> findByPostalCode(@Param("postalCode") String postalCode);

    @Query("select l from Locality l join fetch l.zone where l.id = :id")
    Optional<Locality> findWithZoneById(@Param("id") Long id);
}
