package com.carinsurance.location;

import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.location.dto.LocalityResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LocalityService {

    private static final String LOCALITY = "Locality";

    private final LocalityRepository repository;
    private final LocalityMapper mapper;

    public LocalityService(LocalityRepository repository, LocalityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /** All localities with this postal code (there can be many, even in different provinces), with their zone. */
    public List<LocalityResponse> findByPostalCode(String postalCode) {
        return repository.findByPostalCode(postalCode).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public LocalityResponse findById(Long id) {
        return mapper.toResponse(
                repository.findWithZoneById(id).orElseThrow(() -> new ResourceNotFoundException(LOCALITY, id)));
    }
}
