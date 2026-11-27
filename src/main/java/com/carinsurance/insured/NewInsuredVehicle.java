package com.carinsurance.insured;

/**
 * The identifying data of a vehicle to create, see {@link InsuredVehicleService#create}. Values are not yet
 * normalized.
 */
public record NewInsuredVehicle(String licensePlate, String engineNumber, String chassisNumber) {}
