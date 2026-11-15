package com.carinsurance.vehicle;

import com.carinsurance.vehicle.dto.VehicleRequest;

final class VehicleTestData {

    private VehicleTestData() {}

    static VehicleRequest request(Long insuredId, String licensePlate) {
        return request(insuredId, licensePlate, VehicleType.CAR, 2020);
    }

    static VehicleRequest request(Long insuredId, String licensePlate, int year) {
        return request(insuredId, licensePlate, VehicleType.CAR, year);
    }

    static VehicleRequest request(Long insuredId, String licensePlate, VehicleType type) {
        return request(insuredId, licensePlate, type, 2020);
    }

    private static VehicleRequest request(Long insuredId, String licensePlate, VehicleType type, int year) {
        return new VehicleRequest(insuredId, type, "Toyota", "Corolla", year, licensePlate);
    }

    static String json(Long insuredId, String licensePlate) {
        return json(insuredId, licensePlate, "CAR");
    }

    /** {@code type} is raw JSON text so tests can send invalid values; pass {@code null} to omit the field. */
    static String json(Long insuredId, String licensePlate, String type) {
        String typeField = type == null ? "" : "\"type\": \"%s\",".formatted(type);
        return """
                {
                  "insuredId": %s,
                  %s
                  "brand": "Toyota",
                  "model": "Corolla",
                  "year": 2020,
                  "licensePlate": "%s"
                }
                """.formatted(insuredId, typeField, licensePlate);
    }
}
