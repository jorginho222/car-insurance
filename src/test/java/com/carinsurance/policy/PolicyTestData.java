package com.carinsurance.policy;

import java.time.LocalDate;

final class PolicyTestData {

    private PolicyTestData() {}

    /** A valid create-policy body for the given quotation and plan; any value can be overridden afterwards. */
    static String json(
            long quotationId,
            long planId,
            int installments,
            LocalDate validFrom,
            String documentNumber,
            String email,
            String licensePlate) {
        return json(
                quotationId,
                planId,
                installments,
                validFrom,
                documentNumber,
                email,
                licensePlate,
                "ENG-12345",
                "8AP17000000123456");
    }

    static String json(
            long quotationId,
            long planId,
            int installments,
            LocalDate validFrom,
            String documentNumber,
            String email,
            String licensePlate,
            String engineNumber,
            String chassisNumber) {
        return """
                {
                  "quotationId": %d,
                  "planId": %d,
                  "installments": %d,
                  "paymentMethod": "CARD",
                  "validFrom": "%s",
                  "firstName": "Jane",
                  "lastName": "Doe",
                  "documentNumber": "%s",
                  "email": "%s",
                  "phone": "+54 11 5555 1234",
                  "street": "Av. Rivadavia 1234",
                  "licenseNumber": "B-123456",
                  "licenseIssueDate": "2010-06-01",
                  "licensePlate": "%s",
                  "engineNumber": "%s",
                  "chassisNumber": "%s"
                }
                """.formatted(
                        quotationId,
                        planId,
                        installments,
                        validFrom,
                        documentNumber,
                        email,
                        licensePlate,
                        engineNumber,
                        chassisNumber);
    }
}
