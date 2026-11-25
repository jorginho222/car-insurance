package com.carinsurance.quotation;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** @param validityDays how many days after its issue date a quotation stays valid */
@Validated
@ConfigurationProperties("quotation")
public record QuotationProperties(
        @DefaultValue("15") @Positive int validityDays) {}
