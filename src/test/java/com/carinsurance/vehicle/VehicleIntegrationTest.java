package com.carinsurance.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carinsurance.TestcontainersConfiguration;
import com.carinsurance.insured.InsuredRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VehicleIntegrationTest {

    private static final String BASE = "/api/v1/vehicles";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private InsuredRepository insuredRepository;

    @BeforeEach
    void clean() {
        vehicleRepository.deleteAllInBatch();
        insuredRepository.deleteAllInBatch();
    }

    private long createInsured(String documentNumber, String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/insureds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(insuredJson(documentNumber, email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createVehicle(long insuredId, String plate) throws Exception {
        String body = mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VehicleTestData.json(insuredId, plate)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void fullCrudRoundTrip() throws Exception {
        long owner = createInsured("DOC-V1", "owner@example.com");
        long other = createInsured("DOC-V2", "other@example.com");

        String body = mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VehicleTestData.json(owner, "abc-123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.licensePlate").value("ABC-123"))
                .andExpect(jsonPath("$.insuredId").value(owner))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number id = JsonPath.read(body, "$.id");

        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brand").value("Toyota"));

        mockMvc.perform(put(BASE + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VehicleTestData.json(other, "XYZ-999")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insuredId").value(other))
                .andExpect(jsonPath("$.licensePlate").value("XYZ-999"));

        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insuredId").value(other));

        mockMvc.perform(delete(BASE + "/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get(BASE + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void listFiltersByInsured() throws Exception {
        long owner = createInsured("DOC-V3", "a@example.com");
        long other = createInsured("DOC-V4", "b@example.com");
        createVehicle(owner, "AAA-111");
        createVehicle(owner, "AAA-222");
        createVehicle(other, "BBB-111");

        mockMvc.perform(get(BASE).param("insuredId", String.valueOf(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(2));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    @Test
    void duplicatePlateReturns409() throws Exception {
        long owner = createInsured("DOC-V5", "c@example.com");
        createVehicle(owner, "DUP-1");

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VehicleTestData.json(owner, "dup-1")))
                .andExpect(status().isConflict());
    }

    @Test
    void unknownInsuredReturns404() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VehicleTestData.json(999_999L, "NOPE-1")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingInsuredCascadesToVehicles() throws Exception {
        long owner = createInsured("DOC-V6", "d@example.com");
        long vehicleId = createVehicle(owner, "CAS-1");
        createVehicle(owner, "CAS-2");

        mockMvc.perform(delete("/api/v1/insureds/" + owner)).andExpect(status().isNoContent());

        mockMvc.perform(get(BASE + "/" + vehicleId)).andExpect(status().isNotFound());
        assertThat(vehicleRepository.count()).isZero();
    }

    private static String insuredJson(String documentNumber, String email) {
        return """
                {
                  "firstName": "Jane",
                  "lastName": "Doe",
                  "documentNumber": "%s",
                  "dateOfBirth": "1990-05-17",
                  "email": "%s",
                  "phone": "+1 555 123 4567",
                  "address": {
                    "street": "123 Main St",
                    "city": "Springfield",
                    "state": "IL",
                    "postalCode": "62701",
                    "country": "USA"
                  },
                  "licenseNumber": "DL-998877",
                  "licenseIssueDate": "2010-06-01"
                }
                """.formatted(documentNumber, email);
    }
}
