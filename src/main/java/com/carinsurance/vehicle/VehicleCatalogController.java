package com.carinsurance.vehicle;

import com.carinsurance.vehicle.dto.VehicleCatalogResponse;
import com.carinsurance.vehicle.dto.VehicleValuationResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only catalog used to pick a vehicle: vehicle type, then make, then model, then model version (a catalog
 * entry), then the year (a valuation).
 */
@RestController
@RequestMapping("/api/v1/vehicle-catalog")
public class VehicleCatalogController {

    private final VehicleCatalogService service;

    public VehicleCatalogController(VehicleCatalogService service) {
        this.service = service;
    }

    @GetMapping("/makes")
    public List<String> findMakes(@RequestParam VehicleType vehicleType) {
        return service.findMakes(vehicleType);
    }

    @GetMapping("/models")
    public List<String> findModels(@RequestParam VehicleType vehicleType, @RequestParam String make) {
        return service.findModels(vehicleType, make);
    }

    @GetMapping("/versions")
    public List<VehicleCatalogResponse> findVersions(
            @RequestParam VehicleType vehicleType, @RequestParam String make, @RequestParam String model) {
        return service.findVersions(vehicleType, make, model);
    }

    @GetMapping("/{catalogId}")
    public VehicleCatalogResponse findById(@PathVariable Long catalogId) {
        return service.findById(catalogId);
    }

    @GetMapping("/{catalogId}/valuations")
    public List<VehicleValuationResponse> findCurrentValuations(@PathVariable Long catalogId) {
        return service.findCurrentValuations(catalogId);
    }
}
