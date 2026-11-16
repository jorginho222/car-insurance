package com.carinsurance.insured;

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
import com.carinsurance.insured.dto.InsuredVehicleResponse;
import com.carinsurance.vehicle.VehicleType;
import com.carinsurance.vehicle.dto.VehicleCatalogResponse;
import com.carinsurance.vehicle.dto.VehicleValuationResponse;
import java.math.BigDecimal;
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

@WebMvcTest(InsuredVehicleController.class)
class InsuredVehicleControllerTest {

    private static final String BASE = "/api/v1/insureds/1/vehicles";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InsuredVehicleService service;

    private static InsuredVehicleResponse response(long id) {
        return new InsuredVehicleResponse(
                id,
                1L,
                "ABC-123",
                new VehicleCatalogResponse(7L, "Toyota", "Corolla", "2.0 XEI", VehicleType.CAR),
                new VehicleValuationResponse(20L, 7L, 2020, new BigDecimal("20000.00"), LocalDate.of(2026, 1, 1)),
                Instant.now(),
                Instant.now());
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        given(service.create(eq(1L), any())).willReturn(response(5L));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredVehicleTestData.json(20L, "ABC-123")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost" + BASE + "/5"))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.insuredId").value(1))
                .andExpect(jsonPath("$.catalog.make").value("Toyota"))
                .andExpect(jsonPath("$.valuation.year").value(2020));
    }

    @Test
    void createReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredVehicleTestData.json(null, "bad_plate!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.valuationId").exists())
                .andExpect(jsonPath("$.errors.licensePlate").exists());
    }

    @Test
    void createReturns400ForMalformedJson() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReturns400WhenValuationIsNotCurrent() throws Exception {
        given(service.create(eq(1L), any())).willThrow(new InvalidRequestException("not current"));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredVehicleTestData.json(20L, "ABC-123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("not current"));
    }

    @Test
    void createReturns404WhenInsuredMissing() throws Exception {
        given(service.create(eq(1L), any())).willThrow(new ResourceNotFoundException("Insured", 1L));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredVehicleTestData.json(20L, "ABC-123")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Insured with id 1 not found"));
    }

    @Test
    void createReturns409OnDuplicatePlate() throws Exception {
        given(service.create(eq(1L), any())).willThrow(new DuplicateResourceException("dup"));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredVehicleTestData.json(20L, "ABC-123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("dup"));
    }

    @Test
    void listReturnsPagedVehiclesOfInsured() throws Exception {
        given(service.findAll(eq(1L), any()))
                .willReturn(new PageImpl<>(List.of(response(5L)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete(BASE + "/5")).andExpect(status().isNoContent());
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        willThrow(new ResourceNotFoundException("Vehicle", 5L)).given(service).delete(1L, 5L);

        mockMvc.perform(delete(BASE + "/5")).andExpect(status().isNotFound());
    }
}
