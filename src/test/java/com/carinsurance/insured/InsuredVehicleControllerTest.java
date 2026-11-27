package com.carinsurance.insured;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
                "AB123CD",
                "ENG-12345",
                "8AP17000000123456",
                new VehicleCatalogResponse(7L, "Toyota", "Corolla", "2.0 XEI", VehicleType.CAR),
                new VehicleValuationResponse(20L, 7L, 2020, new BigDecimal("20000.00"), LocalDate.of(2026, 1, 1)),
                Instant.now(),
                Instant.now());
    }

    @Test
    void getReturnsTheVehicle() throws Exception {
        given(service.findById(1L, 5L)).willReturn(response(5L));

        mockMvc.perform(get(BASE + "/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.insuredId").value(1))
                .andExpect(jsonPath("$.licensePlate").value("AB123CD"))
                .andExpect(jsonPath("$.engineNumber").value("ENG-12345"))
                .andExpect(jsonPath("$.chassisNumber").value("8AP17000000123456"))
                .andExpect(jsonPath("$.catalog.make").value("Toyota"))
                .andExpect(jsonPath("$.valuation.year").value(2020));
    }

    @Test
    void getReturns404WhenMissing() throws Exception {
        given(service.findById(1L, 5L)).willThrow(new ResourceNotFoundException("Vehicle", 5L));

        mockMvc.perform(get(BASE + "/5")).andExpect(status().isNotFound());
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

    /** Vehicles are created by issuing a policy. */
    @Test
    void writeMethodsAreNotExposed() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put(BASE + "/5").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete(BASE + "/5")).andExpect(status().isMethodNotAllowed());
    }
}
