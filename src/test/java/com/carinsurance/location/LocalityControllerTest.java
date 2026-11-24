package com.carinsurance.location;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.location.dto.LocalityResponse;
import com.carinsurance.location.dto.ZoneResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LocalityController.class)
class LocalityControllerTest {

    private static final String BASE = "/api/v1/localities";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocalityService service;

    @Test
    void findsLocalitiesByPostalCode() throws Exception {
        ZoneResponse zone = new ZoneResponse(3L, "Ciudades grandes del interior");
        given(service.findByPostalCode("2000"))
                .willReturn(List.of(
                        new LocalityResponse(1L, "LA CERAMICA Y CUYO", "Santa Fe", "2000", zone),
                        new LocalityResponse(2L, "ROSARIO", "Santa Fe", "2000", zone)));

        mockMvc.perform(get(BASE).param("postalCode", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].name").value("ROSARIO"))
                .andExpect(jsonPath("$[1].province").value("Santa Fe"))
                .andExpect(jsonPath("$[1].zone.name").value("Ciudades grandes del interior"));
    }

    @Test
    void missingPostalCodeReturns400() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isBadRequest());
    }

    @Test
    void malformedPostalCodeReturns400WithFieldError() throws Exception {
        mockMvc.perform(get(BASE).param("postalCode", "C1425"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.postalCode").value("must be 4 digits"));
        verifyNoInteractions(service);
    }

    @Test
    void unknownLocalityReturns404() throws Exception {
        given(service.findById(9L)).willThrow(new ResourceNotFoundException("Locality", 9L));

        mockMvc.perform(get(BASE + "/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Locality with id 9 not found"));
    }
}
