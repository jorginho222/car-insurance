package com.carinsurance.vehicle;

import com.carinsurance.vehicle.dto.VehicleRequest;

final class VehicleTestData {

    private VehicleTestData() {}

    static VehicleRequest request(Long insuredId, String licensePlate) {
        return new VehicleRequest(insuredId, "Toyota", "Corolla", 2020, licensePlate);
    }

    static VehicleRequest request(Long insuredId, String licensePlate, int year) {
        return new VehicleRequest(insuredId, "Toyota", "Corolla", year, licensePlate);
    }

    static String json(Long insuredId, String licensePlate) {
        return """
                {
                  "insuredId": %s,
                  "brand": "Toyota",
                  "model": "Corolla",
                  "year": 2020,
                  "licensePlate": "%s"
                }
                """.formatted(insuredId, licensePlate);
    }
}
