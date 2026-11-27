package com.carinsurance.insured;

final class InsuredVehicleTestData {

    private InsuredVehicleTestData() {}

    static NewInsuredVehicle newVehicle(String licensePlate) {
        return new NewInsuredVehicle(licensePlate, " eng-12345 ", " 8ap17000000123456 ");
    }
}
