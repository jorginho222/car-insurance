package com.carinsurance.insured;

import com.carinsurance.insured.dto.InsuredRequest;
import com.carinsurance.insured.dto.InsuredResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/insureds")
public class InsuredController {

    private final InsuredService service;

    public InsuredController(InsuredService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<InsuredResponse> create(@Valid @RequestBody InsuredRequest request) {
        InsuredResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
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

    @PutMapping("/{id}")
    public InsuredResponse update(@PathVariable Long id, @Valid @RequestBody InsuredRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
