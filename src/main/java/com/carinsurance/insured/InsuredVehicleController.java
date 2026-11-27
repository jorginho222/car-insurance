package com.carinsurance.insured;

import com.carinsurance.insured.dto.InsuredVehicleResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only: insured vehicles are created by issuing a policy ({@code POST /api/v1/policies}). */
@RestController
@RequestMapping("/api/v1/insureds/{insuredId}/vehicles")
public class InsuredVehicleController {

    private final InsuredVehicleService service;

    public InsuredVehicleController(InsuredVehicleService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public InsuredVehicleResponse findById(@PathVariable Long insuredId, @PathVariable Long id) {
        return service.findById(insuredId, id);
    }

    @GetMapping
    public PagedModel<InsuredVehicleResponse> findAll(
            @PathVariable Long insuredId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return new PagedModel<>(service.findAll(insuredId, pageable));
    }
}
