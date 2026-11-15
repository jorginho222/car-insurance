package com.carinsurance;

import org.springframework.boot.SpringApplication;

public class TestCarInsuranceApplication {

    public static void main(String[] args) {
        SpringApplication.from(CarInsuranceApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
