package com.carinsurance.vehicle;

import com.carinsurance.common.exception.DuplicateResourceException;
import com.carinsurance.common.exception.InvalidRequestException;
import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.insured.Insured;
import com.carinsurance.insured.InsuredRepository;
import com.carinsurance.vehicle.dto.VehicleRequest;
import com.carinsurance.vehicle.dto.VehicleResponse;
import java.time.Year;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VehicleService {

    private static final String RESOURCE = "Vehicle";

    private final VehicleRepository repository;
    private final InsuredRepository insuredRepository;
    private final VehicleMapper mapper;

    public VehicleService(VehicleRepository repository, InsuredRepository insuredRepository, VehicleMapper mapper) {
        this.repository = repository;
        this.insuredRepository = insuredRepository;
        this.mapper = mapper;
    }

    @Transactional
    public VehicleResponse create(VehicleRequest request) {
        validateYear(request);
        Insured insured = getInsuredOrThrow(request.insuredId());
        String plate = VehicleMapper.normalizePlate(request.licensePlate());
        if (repository.existsByLicensePlate(plate)) {
            throw duplicatePlate(plate);
        }
        Vehicle vehicle = mapper.toEntity(request);
        insured.addVehicle(vehicle);
        return mapper.toResponse(repository.saveAndFlush(vehicle));
    }

    public VehicleResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    public Page<VehicleResponse> findAll(Long insuredId, Pageable pageable) {
        Page<Vehicle> page =
                insuredId == null ? repository.findAll(pageable) : repository.findByInsuredId(insuredId, pageable);
        return page.map(mapper::toResponse);
    }

    @Transactional
    public VehicleResponse update(Long id, VehicleRequest request) {
        validateYear(request);
        Vehicle vehicle = getOrThrow(id);
        String plate = VehicleMapper.normalizePlate(request.licensePlate());
        if (repository.existsByLicensePlateAndIdNot(plate, id)) {
            throw duplicatePlate(plate);
        }
        Insured current = vehicle.getInsured();
        if (!Objects.equals(current.getId(), request.insuredId())) {
            Insured target = getInsuredOrThrow(request.insuredId());
            current.removeVehicle(vehicle);
            target.addVehicle(vehicle);
        }
        mapper.updateEntity(vehicle, request);
        return mapper.toResponse(repository.saveAndFlush(vehicle));
    }

    @Transactional
    public void delete(Long id) {
        Vehicle vehicle = getOrThrow(id);
        vehicle.getInsured().removeVehicle(vehicle);
        repository.delete(vehicle);
    }

    private Vehicle getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }

    private Insured getInsuredOrThrow(Long insuredId) {
        return insuredRepository
                .findById(insuredId)
                .orElseThrow(() -> new ResourceNotFoundException("Insured", insuredId));
    }

    private static void validateYear(VehicleRequest request) {
        int maxYear = Year.now().getValue() + 1;
        if (request.year() > maxYear) {
            throw new InvalidRequestException("Vehicle year must not be after %d".formatted(maxYear));
        }
    }

    private static DuplicateResourceException duplicatePlate(String plate) {
        return new DuplicateResourceException("A vehicle with license plate '%s' already exists".formatted(plate));
    }
}
