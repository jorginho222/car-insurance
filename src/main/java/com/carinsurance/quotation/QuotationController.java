package com.carinsurance.quotation;

import com.carinsurance.quotation.dto.QuotationRequest;
import com.carinsurance.quotation.dto.QuotationResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Quotes every plan for a vehicle valuation, a driver birth date and a locality, and stores the quotation. */
@RestController
@RequestMapping("/api/v1/quotations")
public class QuotationController {

    private final QuotationService service;

    public QuotationController(QuotationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<QuotationResponse> quote(@Valid @RequestBody QuotationRequest request) {
        QuotationResponse created = service.quote(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public QuotationResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }
}
