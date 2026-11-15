package com.carinsurance.insured;

import com.carinsurance.insured.dto.AddressDto;
import com.carinsurance.insured.dto.InsuredRequest;
import com.carinsurance.insured.dto.InsuredResponse;
import org.springframework.stereotype.Component;

@Component
public class InsuredMapper {

    public Insured toEntity(InsuredRequest request) {
        Insured insured = new Insured();
        updateEntity(insured, request);
        return insured;
    }

    public void updateEntity(Insured insured, InsuredRequest request) {
        insured.setFirstName(request.firstName().trim());
        insured.setLastName(request.lastName().trim());
        insured.setDocumentNumber(request.documentNumber().trim());
        insured.setDateOfBirth(request.dateOfBirth());
        insured.setEmail(request.email().trim().toLowerCase());
        insured.setPhone(request.phone().trim());
        AddressDto a = request.address();
        insured.setAddress(new Address(a.street(), a.city(), a.state(), a.postalCode(), a.country()));
        insured.setLicenseNumber(request.licenseNumber().trim());
        insured.setLicenseIssueDate(request.licenseIssueDate());
    }

    public InsuredResponse toResponse(Insured insured) {
        Address a = insured.getAddress();
        return new InsuredResponse(
                insured.getId(),
                insured.getFirstName(),
                insured.getLastName(),
                insured.getDocumentNumber(),
                insured.getDateOfBirth(),
                insured.getEmail(),
                insured.getPhone(),
                new AddressDto(a.getStreet(), a.getCity(), a.getState(), a.getPostalCode(), a.getCountry()),
                insured.getLicenseNumber(),
                insured.getLicenseIssueDate(),
                insured.getCreatedAt(),
                insured.getUpdatedAt());
    }
}
