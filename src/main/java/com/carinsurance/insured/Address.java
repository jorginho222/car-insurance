package com.carinsurance.insured;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** The insured's address. Locality, province and postal code match a {@code Locality}. */
@Embeddable
public class Address {

    @Column(nullable = false)
    private String street;

    @Column(nullable = false, length = 100)
    private String locality;

    @Column(nullable = false, length = 100)
    private String province;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(nullable = false, length = 100)
    private String country;

    protected Address() {}

    public Address(String street, String locality, String province, String postalCode, String country) {
        this.street = street;
        this.locality = locality;
        this.province = province;
        this.postalCode = postalCode;
        this.country = country;
    }

    public String getStreet() {
        return street;
    }

    public String getLocality() {
        return locality;
    }

    public String getProvince() {
        return province;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }
}
