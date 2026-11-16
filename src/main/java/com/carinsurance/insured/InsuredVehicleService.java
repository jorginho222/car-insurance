package com.carinsurance.insured;

import com.carinsurance.common.exception.DuplicateResourceException;
import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.insured.dto.InsuredVehicleRequest;
import com.carinsurance.insured.dto.InsuredVehicleResponse;
import com.carinsurance.vehicle.VehicleCatalogService;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InsuredVehicleService {

    private static final String RESOURCE = "Vehicle";

    private final InsuredVehicleRepository repository;
    private final InsuredRepository insuredRepository;
    private final VehicleCatalogService catalogService;
    private final InsuredVehicleMapper mapper;

    public InsuredVehicleService(
            InsuredVehicleRepository repository,
            InsuredRepository insuredRepository,
            VehicleCatalogService catalogService,
            InsuredVehicleMapper mapper) {
        this.repository = repository;
        this.insuredRepository = insuredRepository;
        this.catalogService = catalogService;
        this.mapper = mapper;
    }

    @Transactional
    public InsuredVehicleResponse create(Long insuredId, InsuredVehicleRequest request) {
        Insured insured = getInsuredOrThrow(insuredId);
        String plate = InsuredVehicleMapper.normalizePlate(request.licensePlate());
        if (repository.existsByLicensePlate(plate)) {
            throw duplicatePlate(plate);
        }
        InsuredVehicle vehicle = new InsuredVehicle();
        vehicle.setValuation(catalogService.getSelectableValuation(request.valuationId()));
        vehicle.setLicensePlate(plate);
        insured.addVehicle(vehicle);
        return mapper.toResponse(repository.saveAndFlush(vehicle));
    }

    public InsuredVehicleResponse findById(Long insuredId, Long id) {
        return mapper.toResponse(getOrThrow(insuredId, id));
    }

    public Page<InsuredVehicleResponse> findAll(Long insuredId, Pageable pageable) {
        if (!insuredRepository.existsById(insuredId)) {
            throw new ResourceNotFoundException("Insured", insuredId);
        }
        return repository.findByInsuredId(insuredId, pageable).map(mapper::toResponse);
    }

    /**
     * Keeps the vehicle's valuation when the request repeats it, even if a newer price list has replaced it since;
     * a different valuation must be the current one for its year.
     */
    @Transactional
    public InsuredVehicleResponse update(Long insuredId, Long id, InsuredVehicleRequest request) {
        InsuredVehicle vehicle = getOrThrow(insuredId, id);
        String plate = InsuredVehicleMapper.normalizePlate(request.licensePlate());
        if (repository.existsByLicensePlateAndIdNot(plate, id)) {
            throw duplicatePlate(plate);
        }
        if (!Objects.equals(vehicle.getValuation().getId(), request.valuationId())) {
            vehicle.setValuation(catalogService.getSelectableValuation(request.valuationId()));
        }
        vehicle.setLicensePlate(plate);
        return mapper.toResponse(repository.saveAndFlush(vehicle));
    }

    @Transactional
    public void delete(Long insuredId, Long id) {
        InsuredVehicle vehicle = getOrThrow(insuredId, id);
        vehicle.getInsured().removeVehicle(vehicle);
        repository.delete(vehicle);
    }

    private InsuredVehicle getOrThrow(Long insuredId, Long id) {
        return repository
                .findByIdAndInsuredId(id, insuredId)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }

    private Insured getInsuredOrThrow(Long insuredId) {
        return insuredRepository
                .findById(insuredId)
                .orElseThrow(() -> new ResourceNotFoundException("Insured", insuredId));
    }

    private static DuplicateResourceException duplicatePlate(String plate) {
        return new DuplicateResourceException("A vehicle with license plate '%s' already exists".formatted(plate));
    }
}
