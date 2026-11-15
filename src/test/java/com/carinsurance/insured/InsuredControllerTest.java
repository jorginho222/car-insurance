package com.carinsurance.insured;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carinsurance.common.exception.DuplicateResourceException;
import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.insured.dto.InsuredRequest;
import com.carinsurance.insured.dto.InsuredResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InsuredController.class)
class InsuredControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InsuredService service;

    private static InsuredResponse response(long id) {
        InsuredRequest r = InsuredTestData.request("DOC-1", "jane@example.com");
        return new InsuredResponse(
                id,
                r.firstName(),
                r.lastName(),
                r.documentNumber(),
                r.dateOfBirth(),
                r.email(),
                r.phone(),
                r.address(),
                r.licenseNumber(),
                r.licenseIssueDate(),
                Instant.now(),
                Instant.now());
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        given(service.create(any())).willReturn(response(1L));

        mockMvc.perform(post("/api/v1/insureds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredTestData.json("DOC-1", "jane@example.com")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/insureds/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.address.city").value("Springfield"));
    }

    @Test
    void createReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/insureds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredTestData.json("", "not-an-email")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.documentNumber").exists())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void createReturns409OnDuplicate() throws Exception {
        given(service.create(any())).willThrow(new DuplicateResourceException("dup"));

        mockMvc.perform(post("/api/v1/insureds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredTestData.json("DOC-1", "jane@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("dup"));
    }

    @Test
    void getReturns404WhenMissing() throws Exception {
        given(service.findById(99L)).willThrow(new ResourceNotFoundException("Insured", 99L));

        mockMvc.perform(get("/api/v1/insureds/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Insured with id 99 not found"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/insureds/1")).andExpect(status().isNoContent());
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        willThrow(new ResourceNotFoundException("Insured", 5L)).given(service).delete(5L);

        mockMvc.perform(delete("/api/v1/insureds/5")).andExpect(status().isNotFound());
    }
}
