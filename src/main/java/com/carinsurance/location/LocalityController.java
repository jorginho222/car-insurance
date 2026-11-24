package com.carinsurance.location;

import com.carinsurance.location.dto.LocalityResponse;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Read-only locality lookup: the insured enters a postal code and picks one of the matching localities. */
@RestController
@RequestMapping("/api/v1/localities")
public class LocalityController {

    private final LocalityService service;

    public LocalityController(LocalityService service) {
        this.service = service;
    }

    @GetMapping
    public List<LocalityResponse> findByPostalCode(
            @RequestParam @Pattern(regexp = "\\d{4}", message = "must be 4 digits") String postalCode) {
        return service.findByPostalCode(postalCode);
    }

    @GetMapping("/{id}")
    public LocalityResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }
}
