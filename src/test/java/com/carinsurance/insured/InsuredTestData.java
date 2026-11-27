package com.carinsurance.insured;

import java.time.LocalDate;

final class InsuredTestData {

    private InsuredTestData() {}

    static NewInsured newInsured(String documentNumber, String email) {
        return new NewInsured(
                " Jane ",
                "Doe",
                documentNumber,
                LocalDate.of(1990, 5, 17),
                email,
                "+54 11 5555 1234",
                new Address(" Av. Rivadavia 1234 ", "FLORIDA", "Buenos Aires", "1602", "Argentina"),
                "DL-998877",
                LocalDate.of(2010, 6, 1));
    }
}
