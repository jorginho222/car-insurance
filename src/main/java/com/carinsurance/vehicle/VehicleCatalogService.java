package com.carinsurance.vehicle;

import com.carinsurance.common.exception.InvalidRequestException;
import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.vehicle.dto.VehicleCatalogResponse;
import com.carinsurance.vehicle.dto.VehicleValuationResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VehicleCatalogService {

    private static final String CATALOG = "Vehicle catalog";
    private static final String VALUATION = "Vehicle valuation";

    private final VehicleCatalogRepository catalogRepository;
    private final VehicleValuationRepository valuationRepository;
    private final VehicleCatalogMapper mapper;
    private final Clock clock;

    public VehicleCatalogService(
            VehicleCatalogRepository catalogRepository,
            VehicleValuationRepository valuationRepository,
            VehicleCatalogMapper mapper,
            Clock clock) {
        this.catalogRepository = catalogRepository;
        this.valuationRepository = valuationRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    public List<String> findMakes(VehicleType type) {
        return catalogRepository.findMakes(type);
    }

    public List<String> findModels(VehicleType type, String make) {
        return catalogRepository.findModels(type, make);
    }

    public List<VehicleCatalogResponse> findVersions(VehicleType type, String make, String model) {
        return catalogRepository.findByVehicleTypeAndMakeAndModelOrderByModelVersion(type, make, model).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public VehicleCatalogResponse findById(Long catalogId) {
        return mapper.toResponse(catalogRepository
                .findById(catalogId)
                .orElseThrow(() -> new ResourceNotFoundException(CATALOG, catalogId)));
    }

    /** One valuation per year: the one currently in effect. Newest year first. */
    public List<VehicleValuationResponse> findCurrentValuations(Long catalogId) {
        if (!catalogRepository.existsById(catalogId)) {
            throw new ResourceNotFoundException(CATALOG, catalogId);
        }
        return valuationRepository.findCurrentByCatalogId(catalogId, today()).stream()
                .map(mapper::toResponse)
                .toList();
    }

    /**
     * Returns the valuation (with its catalog entry loaded) if it can be selected for a vehicle, i.e. it is currently
     * in effect: not in the future and not replaced by a newer valuation for the same catalog entry and year.
     */
    public VehicleValuation getSelectableValuation(Long valuationId) {
        VehicleValuation valuation = getValuation(valuationId);
        LocalDate today = today();
        if (valuation.getValidFrom().isAfter(today)
                || valuationRepository.existsNewerCurrent(
                        valuation.getCatalog().getId(), valuation.getYear(), valuation.getValidFrom(), today)) {
            throw new InvalidRequestException(
                    "Vehicle valuation %d is not the current valuation for its year".formatted(valuationId));
        }
        return valuation;
    }

    /** Returns the valuation (with its catalog entry loaded) regardless of whether it is still current. */
    public VehicleValuation getValuation(Long valuationId) {
        return valuationRepository
                .findWithCatalogById(valuationId)
                .orElseThrow(() -> new ResourceNotFoundException(VALUATION, valuationId));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
