package com.carinsurance.vehicle;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carinsurance.common.exception.DuplicateResourceException;
import com.carinsurance.common.exception.InvalidRequestException;
import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.vehicle.dto.VehicleResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VehicleController.class)
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleService service;

    private static VehicleResponse response(long id) {
        return new VehicleResponse(id, 1L, "Toyota", "Corolla", 2020, "ABC-123", Instant.now(), Instant.now());
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        given(service.create(any())).willReturn(response(5L));

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VehicleTestData.json(1L, "ABC-123")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/vehicles/5"))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.insuredId").value(1));
    }

    @Test
    void createReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VehicleTestData.json(null, "bad_plate!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.insuredId").exists())
                .andExpect(jsonPath("$.errors.licensePlate").exists());
    }

    @Test
    void createReturns400OnInvalidYear() throws Exception {
        given(service.create(any())).willThrow(new InvalidRequestException("bad year"));

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VehicleTestData.json(1L, "ABC-123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("bad year"));
    }

    @Test
    void createReturns404WhenInsuredMissing() throws Exception {
        given(service.create(any())).willThrow(new ResourceNotFoundException("Insured", 1L));

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VehicleTestData.json(1L, "ABC-123")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Insured with id 1 not found"));
    }

    @Test
    void createReturns409OnDuplicatePlate() throws Exception {
        given(service.create(any())).willThrow(new DuplicateResourceException("dup"));

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VehicleTestData.json(1L, "ABC-123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("dup"));
    }

    @Test
    void listFiltersByInsured() throws Exception {
        given(service.findAll(eq(1L), any()))
                .willReturn(new PageImpl<>(List.of(response(5L)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/vehicles").param("insuredId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/vehicles/5")).andExpect(status().isNoContent());
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        willThrow(new ResourceNotFoundException("Vehicle", 5L)).given(service).delete(5L);

        mockMvc.perform(delete("/api/v1/vehicles/5")).andExpect(status().isNotFound());
    }
}
