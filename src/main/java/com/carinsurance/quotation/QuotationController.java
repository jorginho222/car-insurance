package com.carinsurance.quotation;

import com.carinsurance.quotation.dto.QuotationRequest;
import com.carinsurance.quotation.dto.QuotationResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import java.net.URI;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Quotes every plan for a vehicle valuation, a driver birth date and a locality, and stores the quotation. Stored
 * quotations can be read by id or searched by email or by code.
 */
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

    /**
     * Searches open quotations (quoted and still valid) by email or by code (exactly one). By default grouped by vehicle (valuation) and newest first within
     * each vehicle.
     */
    @GetMapping
    public PagedModel<QuotationResponse> search(
            @RequestParam(required = false) @Email String email,
            @RequestParam(required = false) String code,
            @PageableDefault(size = 20)
                    @SortDefault.SortDefaults({
                        @SortDefault(sort = "valuation.id", direction = Sort.Direction.ASC),
                        @SortDefault(sort = "quotedAt", direction = Sort.Direction.DESC)
                    })
                    Pageable pageable) {
        return new PagedModel<>(service.search(email, code, pageable));
    }
}
