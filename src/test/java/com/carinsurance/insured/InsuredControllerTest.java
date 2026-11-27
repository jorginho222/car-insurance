package com.carinsurance.insured;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.insured.dto.AddressDto;
import com.carinsurance.insured.dto.InsuredResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
        return new InsuredResponse(
                id,
                "Jane",
                "Doe",
                "DOC-1",
                LocalDate.of(1990, 5, 17),
                "jane@example.com",
                "+54 11 5555 1234",
                new AddressDto("Av. Rivadavia 1234", "FLORIDA", "Buenos Aires", "1602", "Argentina"),
                "DL-998877",
                LocalDate.of(2010, 6, 1),
                Instant.now(),
                Instant.now());
    }

    @Test
    void getReturnsTheInsured() throws Exception {
        given(service.findById(1L)).willReturn(response(1L));

        mockMvc.perform(get("/api/v1/insureds/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.address.locality").value("FLORIDA"))
                .andExpect(jsonPath("$.address.province").value("Buenos Aires"));
    }

    @Test
    void getReturns404WhenMissing() throws Exception {
        given(service.findById(99L)).willThrow(new ResourceNotFoundException("Insured", 99L));

        mockMvc.perform(get("/api/v1/insureds/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Insured with id 99 not found"));
    }

    @Test
    void listReturnsPagedInsureds() throws Exception {
        given(service.findAll(any())).willReturn(new PageImpl<>(List.of(response(1L)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/insureds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    /** Insureds are created by issuing a policy. */
    @Test
    void writeMethodsAreNotExposed() throws Exception {
        mockMvc.perform(post("/api/v1/insureds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put("/api/v1/insureds/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/v1/insureds/1")).andExpect(status().isMethodNotAllowed());
    }
}
