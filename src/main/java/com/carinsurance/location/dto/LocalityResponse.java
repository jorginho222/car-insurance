package com.carinsurance.location.dto;

public record LocalityResponse(Long id, String name, String province, String postalCode, ZoneResponse zone) {}
