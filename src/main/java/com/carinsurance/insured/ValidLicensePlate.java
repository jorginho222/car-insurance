package com.carinsurance.insured;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

/**
 * An Argentine license plate, in any case and with optional spaces or hyphens (e.g. {@code "ab 123 cd"}). See
 * {@link InsuredVehicleMapper#PLATE_PATTERN}. {@code null} is valid; combine with {@code @NotBlank}.
 */
@Documented
@Constraint(validatedBy = ValidLicensePlate.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLicensePlate {

    String message() default "must be a valid license plate, e.g. ABC123 or AB123CD";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidLicensePlate, String> {

        private static final Pattern PATTERN = Pattern.compile(InsuredVehicleMapper.PLATE_PATTERN);

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null
                    || PATTERN.matcher(InsuredVehicleMapper.normalizePlate(value))
                            .matches();
        }
    }
}
