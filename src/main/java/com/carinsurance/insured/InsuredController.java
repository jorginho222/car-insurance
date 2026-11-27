package com.carinsurance.insured;

import com.carinsurance.insured.dto.InsuredResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only: insureds are created by issuing a policy ({@code POST /api/v1/policies}). */
@RestController
@RequestMapping("/api/v1/insureds")
public class InsuredController {

    private final InsuredService service;

    public InsuredController(InsuredService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public InsuredResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping
    public PagedModel<InsuredResponse> findAll(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return new PagedModel<>(service.findAll(pageable));
    }
}
