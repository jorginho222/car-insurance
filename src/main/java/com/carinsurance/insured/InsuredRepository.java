package com.carinsurance.insured;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InsuredRepository extends JpaRepository<Insured, Long> {

    boolean existsByDocumentNumber(String documentNumber);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByDocumentNumberAndIdNot(String documentNumber, Long id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
