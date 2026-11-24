package com.carinsurance.location.importing;

/** One locality as read from the postal code CSV. */
public record LocalityRow(String province, String name, String postalCode) {}
