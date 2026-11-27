package com.carinsurance.insured;

import com.carinsurance.common.exception.DuplicateResourceException;
import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.insured.dto.InsuredResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Insureds are created when a policy is issued; they can be read by id or listed. */
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

    /**
     * Creates an insured. Meant to be called while issuing a policy, in its transaction.
     *
     * @throws DuplicateResourceException if another insured has the same document number or email
     */
    @Transactional
    public Insured create(NewInsured data) {
        String documentNumber = data.documentNumber().trim();
        if (repository.existsByDocumentNumber(documentNumber)) {
            throw new DuplicateResourceException(
                    "An insured with document number '%s' already exists".formatted(documentNumber));
        }
        String email = InsuredMapper.normalizeEmail(data.email());
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An insured with email '%s' already exists".formatted(email));
        }
        return repository.saveAndFlush(mapper.toEntity(data));
    }

    public InsuredResponse findById(Long id) {
        return mapper.toResponse(
                repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id)));
    }

    public Page<InsuredResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }
}
