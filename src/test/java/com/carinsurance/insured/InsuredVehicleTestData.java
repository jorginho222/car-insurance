package com.carinsurance.insured;

import com.carinsurance.insured.dto.InsuredVehicleRequest;

final class InsuredVehicleTestData {

    private InsuredVehicleTestData() {}

    static InsuredVehicleRequest request(Long valuationId, String licensePlate) {
        return new InsuredVehicleRequest(valuationId, licensePlate);
    }

    static String json(Long valuationId, String licensePlate) {
        return """
                {
                  "valuationId": %s,
                  "licensePlate": "%s"
                }
                """.formatted(valuationId, licensePlate);
    }
}
