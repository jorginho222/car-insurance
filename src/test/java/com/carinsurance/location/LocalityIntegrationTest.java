package com.carinsurance.location;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carinsurance.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** Runs against the zones from migration V7 and the localities imported on startup by the LocalitySeeder. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LocalityIntegrationTest {

    private static final String BASE = "/api/v1/localities";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalityRepository localityRepository;

    @Test
    void cabaPostalCodeIsInCabaZone() throws Exception {
        mockMvc.perform(get(BASE).param("postalCode", "1425"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].province").value("Ciudad Autonoma de Buenos Aires"))
                .andExpect(jsonPath("$[*].zone.name", everyItem(is("CABA"))));
    }

    @Test
    void gbaPostalCodeIsInGbaZone() throws Exception {
        mockMvc.perform(get(BASE).param("postalCode", "1602"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("FLORIDA")))
                .andExpect(jsonPath("$[*].zone.name", everyItem(is("GBA"))));
    }

    @Test
    void largeInteriorCityPostalCodeIsInInteriorZone() throws Exception {
        mockMvc.perform(get(BASE).param("postalCode", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].name", hasItem("ROSARIO")))
                .andExpect(jsonPath("$[*].zone.name", everyItem(is("Ciudades grandes del interior"))));
    }

    @Test
    void otherPostalCodesAreInRestOfCountryZone() throws Exception {
        mockMvc.perform(get(BASE).param("postalCode", "4139"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].province", hasItem("Catamarca")))
                .andExpect(jsonPath("$[*].zone.name", everyItem(is("Resto del país"))));
    }

    @Test
    void unknownPostalCodeReturnsEmptyList() throws Exception {
        mockMvc.perform(get(BASE).param("postalCode", "0000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void findsLocalityById() throws Exception {
        Long id = localityRepository.findByPostalCode("1900").getFirst().getId();

        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("LA PLATA"))
                .andExpect(jsonPath("$.zone.name").value("Ciudades grandes del interior"));
    }
}
