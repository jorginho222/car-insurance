package com.carinsurance.insured;

import com.carinsurance.insured.dto.AddressDto;
import com.carinsurance.insured.dto.InsuredRequest;
import java.time.LocalDate;

final class InsuredTestData {

    private InsuredTestData() {}

    static InsuredRequest request(String documentNumber, String email) {
        return new InsuredRequest(
                "Jane",
                "Doe",
                documentNumber,
                LocalDate.of(1990, 5, 17),
                email,
                "+1 555 123 4567",
                new AddressDto("123 Main St", "Springfield", "IL", "62701", "USA"),
                "DL-998877",
                LocalDate.of(2010, 6, 1));
    }

    static String json(String documentNumber, String email) {
        return """
                {
                  "firstName": "Jane",
                  "lastName": "Doe",
                  "documentNumber": "%s",
                  "dateOfBirth": "1990-05-17",
                  "email": "%s",
                  "phone": "+1 555 123 4567",
                  "address": {
                    "street": "123 Main St",
                    "city": "Springfield",
                    "state": "IL",
                    "postalCode": "62701",
                    "country": "USA"
                  },
                  "licenseNumber": "DL-998877",
                  "licenseIssueDate": "2010-06-01"
                }
                """.formatted(documentNumber, email);
    }
}
