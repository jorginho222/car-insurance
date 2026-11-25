package com.carinsurance.quotation;

import com.carinsurance.quotation.dto.QuotationRequest;
import com.carinsurance.quotation.dto.QuotationResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Quotes every plan for a vehicle valuation, a driver birth date and a locality. Nothing is stored. */
@RestController
@RequestMapping("/api/v1/quotations")
public class QuotationController {

    private final QuotationService service;

    public QuotationController(QuotationService service) {
        this.service = service;
    }

    @PostMapping
    public QuotationResponse quote(@Valid @RequestBody QuotationRequest request) {
        return service.quote(request);
    }
}
