package com.carinsurance.vehicle;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.vehicle.dto.VehicleCatalogResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VehicleCatalogController.class)
class VehicleCatalogControllerTest {

    private static final String BASE = "/api/v1/vehicle-catalog";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleCatalogService service;

    @Test
    void makesAreFilteredByType() throws Exception {
        given(service.findMakes(VehicleType.PICKUP)).willReturn(List.of("Ford", "Toyota"));

        mockMvc.perform(get(BASE + "/makes").param("vehicleType", "PICKUP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Ford"))
                .andExpect(jsonPath("$[1]").value("Toyota"));
    }

    @Test
    void versionsReturnCatalogEntries() throws Exception {
        given(service.findVersions(VehicleType.CAR, "Toyota", "Corolla"))
                .willReturn(List.of(new VehicleCatalogResponse(7L, "Toyota", "Corolla", "2.0 XEI", VehicleType.CAR)));

        mockMvc.perform(get(BASE + "/versions")
                        .param("vehicleType", "CAR")
                        .param("make", "Toyota")
                        .param("model", "Corolla"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].modelVersion").value("2.0 XEI"));
    }

    @Test
    void missingParametersReturn400() throws Exception {
        mockMvc.perform(get(BASE + "/makes")).andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/models").param("vehicleType", "CAR")).andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/versions").param("vehicleType", "CAR").param("make", "Toyota"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownVehicleTypeReturns400ListingAllowedValues() throws Exception {
        mockMvc.perform(get(BASE + "/makes").param("vehicleType", "PLANE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.vehicleType")
                        .value("must be one of [CAR, MOTORCYCLE, SUV, PICKUP, VAN, TRUCK, BUS]"));
    }

    @Test
    void valuationsOfUnknownCatalogReturn404() throws Exception {
        given(service.findCurrentValuations(5L)).willThrow(new ResourceNotFoundException("Vehicle catalog", 5L));

        mockMvc.perform(get(BASE + "/5/valuations"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Vehicle catalog with id 5 not found"));
    }
}
