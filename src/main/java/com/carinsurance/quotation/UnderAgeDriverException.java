package com.carinsurance.quotation;

/** The driver is younger than the minimum age that can be insured. */
public class UnderAgeDriverException extends RuntimeException {

    public UnderAgeDriverException(int age, int minimumAge) {
        super("Driver must be at least %d years old (age: %d)".formatted(minimumAge, age));
    }
}
