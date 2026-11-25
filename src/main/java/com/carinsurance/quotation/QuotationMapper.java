package com.carinsurance.quotation;

import com.carinsurance.location.Locality;
import com.carinsurance.quotation.dto.QuotationResponse;
import com.carinsurance.vehicle.VehicleCatalog;
import com.carinsurance.vehicle.VehicleValuation;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class QuotationMapper {

    /** Maps a quotation with its status as of {@code today} (see {@link Quotation#statusOn(LocalDate)}). */
    public QuotationResponse toResponse(Quotation quotation, LocalDate today) {
        VehicleValuation valuation = quotation.getValuation();
        VehicleCatalog catalog = valuation.getCatalog();
        Locality locality = quotation.getLocality();
        return new QuotationResponse(
                quotation.getId(),
                quotation.statusOn(today),
                quotation.getQuotedAt(),
                quotation.getValidUntil(),
                new QuotationResponse.Vehicle(
                        "%s %s %s".formatted(catalog.getMake(), catalog.getModel(), catalog.getModelVersion()),
                        catalog.getMake(),
                        catalog.getModel(),
                        catalog.getModelVersion(),
                        valuation.getYear(),
                        catalog.getVehicleType(),
                        valuation.getValue()),
                new QuotationResponse.Risk(
                        locality.getPostalCode(),
                        locality.getName(),
                        locality.getProvince(),
                        locality.getZone().getName(),
                        quotation.getDriverAge()),
                quotation.getPlans().stream()
                        .map(plan -> new QuotationResponse.PlanQuote(
                                plan.getName(),
                                // copies, so lazy collections are loaded here and not while writing the JSON
                                List.copyOf(plan.getCoverages()),
                                plan.getNetPremium(),
                                List.copyOf(plan.getPaymentOptions())))
                        .toList());
    }
}
