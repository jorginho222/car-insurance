package com.carinsurance.vehicle;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleCatalogRepository extends JpaRepository<VehicleCatalog, Long> {

    @Query("select distinct c.make from VehicleCatalog c where c.vehicleType = :type order by c.make")
    List<String> findMakes(@Param("type") VehicleType type);

    @Query("""
            select distinct c.model from VehicleCatalog c
            where c.vehicleType = :type and c.make = :make
            order by c.model""")
    List<String> findModels(@Param("type") VehicleType type, @Param("make") String make);

    List<VehicleCatalog> findByVehicleTypeAndMakeAndModelOrderByModelVersion(
            VehicleType vehicleType, String make, String model);
}
