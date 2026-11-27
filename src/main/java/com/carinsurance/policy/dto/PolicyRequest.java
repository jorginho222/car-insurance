package com.carinsurance.policy.dto;

import com.carinsurance.insured.ValidLicensePlate;
import com.carinsurance.policy.PaymentMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Creates a policy from plan {@code planId} of quotation {@code quotationId}, paid with the payment option of
 * {@code installments} installments. The insured's birth date, locality, vehicle and premiums come from the
 * quotation; the rest comes from this request.
 */
public record PolicyRequest(
        @NotNull Long quotationId,
        @NotNull Long planId,
        @NotNull Integer installments,
        @NotNull PaymentMethod paymentMethod,
        @NotNull LocalDate validFrom,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Size(max = 50) String documentNumber,
        @NotBlank @Email @Size(max = 255) String email,

        @NotBlank @Pattern(regexp = "^\\+?[0-9 ()-]{7,30}$", message = "must be a valid phone number")
        String phone,

        @NotBlank @Size(max = 255) String street,
        @NotBlank @Size(max = 50) String licenseNumber,
        @NotNull @PastOrPresent LocalDate licenseIssueDate,
        @NotBlank @ValidLicensePlate String licensePlate,

        @NotBlank
        @Pattern(regexp = "^\\s*[A-Za-z0-9-]{5,20}\\s*$", message = "must be 5 to 20 letters, digits or hyphens")
        String engineNumber,

        @NotBlank
        @Pattern(
                regexp = "^\\s*[A-HJ-NPR-Za-hj-npr-z0-9]{17}\\s*$",
                message = "must be a 17-character VIN (letters except I, O and Q, and digits)")
        String chassisNumber) {}
