package com.carinsurance.insured;

import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.insured.dto.InsuredVehicleResponse;
import com.carinsurance.vehicle.VehicleValuation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Insured vehicles are created when a policy is issued; they can be read under their insured. */
@Service
@Transactional(readOnly = true)
public class InsuredVehicleService {

    private static final String RESOURCE = "Vehicle";

    private final InsuredVehicleRepository repository;
    private final InsuredRepository insuredRepository;
    private final InsuredVehicleMapper mapper;

    public InsuredVehicleService(
            InsuredVehicleRepository repository, InsuredRepository insuredRepository, InsuredVehicleMapper mapper) {
        this.repository = repository;
        this.insuredRepository = insuredRepository;
        this.mapper = mapper;
    }

    /**
     * Creates a vehicle of {@code insured} for {@code valuation}, with its plate, engine and chassis numbers
     * normalized. Meant to be called while issuing a policy, in its transaction: the valuation is the quoted one and is
     * not checked again, and plate rules are the policy's.
     */
    @Transactional
    public InsuredVehicle create(Insured insured, VehicleValuation valuation, NewInsuredVehicle data) {
        InsuredVehicle vehicle = new InsuredVehicle();
        vehicle.setValuation(valuation);
        vehicle.setLicensePlate(InsuredVehicleMapper.normalizePlate(data.licensePlate()));
        vehicle.setEngineNumber(InsuredVehicleMapper.normalizeIdentifier(data.engineNumber()));
        vehicle.setChassisNumber(InsuredVehicleMapper.normalizeIdentifier(data.chassisNumber()));
        insured.addVehicle(vehicle);
        return repository.saveAndFlush(vehicle);
    }

    public InsuredVehicleResponse findById(Long insuredId, Long id) {
        return mapper.toResponse(repository
                .findByIdAndInsuredId(id, insuredId)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id)));
    }

    public Page<InsuredVehicleResponse> findAll(Long insuredId, Pageable pageable) {
        if (!insuredRepository.existsById(insuredId)) {
            throw new ResourceNotFoundException("Insured", insuredId);
        }
        return repository.findByInsuredId(insuredId, pageable).map(mapper::toResponse);
    }
}
