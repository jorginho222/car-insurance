package com.carinsurance.vehicle.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VehicleValuationResponse(Long id, Long catalogId, Integer year, BigDecimal value, LocalDate validFrom) {}
