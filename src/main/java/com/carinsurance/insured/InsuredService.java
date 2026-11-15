package com.carinsurance.insured;

import com.carinsurance.common.exception.DuplicateResourceException;
import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.insured.dto.InsuredRequest;
import com.carinsurance.insured.dto.InsuredResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InsuredService {

    private static final String RESOURCE = "Insured";

    private final InsuredRepository repository;
    private final InsuredMapper mapper;

    public InsuredService(InsuredRepository repository, InsuredMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public InsuredResponse create(InsuredRequest request) {
        if (repository.existsByDocumentNumber(request.documentNumber().trim())) {
            throw duplicateDocument(request);
        }
        if (repository.existsByEmailIgnoreCase(request.email().trim())) {
            throw duplicateEmail(request);
        }
        Insured saved = repository.saveAndFlush(mapper.toEntity(request));
        return mapper.toResponse(saved);
    }

    public InsuredResponse findById(Long id) {
        return mapper.toResponse(getOrThrow(id));
    }

    public Page<InsuredResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional
    public InsuredResponse update(Long id, InsuredRequest request) {
        Insured insured = getOrThrow(id);
        if (repository.existsByDocumentNumberAndIdNot(request.documentNumber().trim(), id)) {
            throw duplicateDocument(request);
        }
        if (repository.existsByEmailIgnoreCaseAndIdNot(request.email().trim(), id)) {
            throw duplicateEmail(request);
        }
        mapper.updateEntity(insured, request);
        return mapper.toResponse(repository.saveAndFlush(insured));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    private Insured getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }

    private static DuplicateResourceException duplicateDocument(InsuredRequest request) {
        return new DuplicateResourceException(
                "An insured with document number '%s' already exists".formatted(request.documentNumber()));
    }

    private static DuplicateResourceException duplicateEmail(InsuredRequest request) {
        return new DuplicateResourceException("An insured with email '%s' already exists".formatted(request.email()));
    }
}
