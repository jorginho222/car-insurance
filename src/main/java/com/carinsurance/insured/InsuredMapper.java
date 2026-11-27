package com.carinsurance.insured;

import com.carinsurance.insured.dto.AddressDto;
import com.carinsurance.insured.dto.InsuredResponse;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class InsuredMapper {

    /** A new insured with trimmed values and the email lower-cased. */
    public Insured toEntity(NewInsured data) {
        Insured insured = new Insured();
        insured.setFirstName(data.firstName().trim());
        insured.setLastName(data.lastName().trim());
        insured.setDocumentNumber(data.documentNumber().trim());
        insured.setDateOfBirth(data.dateOfBirth());
        insured.setEmail(normalizeEmail(data.email()));
        insured.setPhone(data.phone().trim());
        Address a = data.address();
        insured.setAddress(
                new Address(a.getStreet().trim(), a.getLocality(), a.getProvince(), a.getPostalCode(), a.getCountry()));
        insured.setLicenseNumber(data.licenseNumber().trim());
        insured.setLicenseIssueDate(data.licenseIssueDate());
        return insured;
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
                new AddressDto(a.getStreet(), a.getLocality(), a.getProvince(), a.getPostalCode(), a.getCountry()),
                insured.getLicenseNumber(),
                insured.getLicenseIssueDate(),
                insured.getCreatedAt(),
                insured.getUpdatedAt());
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
