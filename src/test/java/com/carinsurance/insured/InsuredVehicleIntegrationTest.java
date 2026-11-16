package com.carinsurance.insured;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carinsurance.TestcontainersConfiguration;
import com.carinsurance.vehicle.VehicleCatalog;
import com.carinsurance.vehicle.VehicleCatalogRepository;
import com.carinsurance.vehicle.VehicleCatalogTestData;
import com.carinsurance.vehicle.VehicleType;
import com.carinsurance.vehicle.VehicleValuation;
import com.carinsurance.vehicle.VehicleValuationRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class InsuredVehicleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InsuredVehicleRepository vehicleRepository;

    @Autowired
    private InsuredRepository insuredRepository;

    @Autowired
    private VehicleCatalogRepository catalogRepository;

    @Autowired
    private VehicleValuationRepository valuationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private VehicleCatalogTestData data;
    private VehicleCatalog catalog;
    private VehicleValuation old2020;
    private VehicleValuation current2020;
    private VehicleValuation current2021;

    @BeforeEach
    void setUp() {
        vehicleRepository.deleteAllInBatch();
        insuredRepository.deleteAllInBatch();

        LocalDate today = LocalDate.now();
        data = new VehicleCatalogTestData(catalogRepository, valuationRepository);
        catalog = data.saveCatalog(VehicleCatalogTestData.uniqueMake(), "Alpha", "1.0 Base", VehicleType.CAR);
        old2020 = data.saveValuation(catalog, 2020, "10000.00", today.minusDays(100));
        current2020 = data.saveValuation(catalog, 2020, "10400.00", today.minusDays(10));
        current2021 = data.saveValuation(catalog, 2021, "12000.00", today.minusDays(100));
    }

    private static String base(long insuredId) {
        return "/api/v1/insureds/" + insuredId + "/vehicles";
    }

    private long createInsured(String documentNumber, String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/insureds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredTestData.json(documentNumber, email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createVehicle(long insuredId, long valuationId, String plate) throws Exception {
        String body = mockMvc.perform(post(base(insuredId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredVehicleTestData.json(valuationId, plate)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void fullCrudRoundTrip() throws Exception {
        long owner = createInsured("DOC-IV1", "owner@example.com");

        String body = mockMvc.perform(post(base(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredVehicleTestData.json(current2020.getId(), "abc-123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.licensePlate").value("ABC-123"))
                .andExpect(jsonPath("$.insuredId").value(owner))
                .andExpect(jsonPath("$.catalog.id").value(catalog.getId()))
                .andExpect(jsonPath("$.catalog.make").value(catalog.getMake()))
                .andExpect(jsonPath("$.catalog.vehicleType").value("CAR"))
                .andExpect(jsonPath("$.valuation.id").value(current2020.getId()))
                .andExpect(jsonPath("$.valuation.year").value(2020))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number id = JsonPath.read(body, "$.id");

        mockMvc.perform(get(base(owner) + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.catalog.modelVersion").value("1.0 Base"));

        mockMvc.perform(put(base(owner) + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredVehicleTestData.json(current2021.getId(), "XYZ-999")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valuation.year").value(2021))
                .andExpect(jsonPath("$.licensePlate").value("XYZ-999"));

        mockMvc.perform(get(base(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].valuation.year").value(2021));

        mockMvc.perform(delete(base(owner) + "/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get(base(owner) + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void supersededValuationIsRejectedOnCreate() throws Exception {
        long owner = createInsured("DOC-IV2", "a@example.com");

        mockMvc.perform(post(base(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredVehicleTestData.json(old2020.getId(), "OLD-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("not the current")));
    }

    @Test
    void updateKeepsValuationThatWasSupersededAfterSelection() throws Exception {
        long owner = createInsured("DOC-IV3", "b@example.com");
        long id = createVehicle(owner, current2020.getId(), "KEEP-1");
        data.saveValuation(catalog, 2020, "10800.00", LocalDate.now().minusDays(1));

        mockMvc.perform(put(base(owner) + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredVehicleTestData.json(current2020.getId(), "KEEP-2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valuation.id").value(current2020.getId()))
                .andExpect(jsonPath("$.licensePlate").value("KEEP-2"));

        mockMvc.perform(put(base(owner) + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredVehicleTestData.json(old2020.getId(), "KEEP-2")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownInsuredOrValuationReturns404() throws Exception {
        long owner = createInsured("DOC-IV4", "c@example.com");

        mockMvc.perform(post(base(999_999L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredVehicleTestData.json(current2020.getId(), "NOPE-1")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(base(999_999L))).andExpect(status().isNotFound());
        mockMvc.perform(post(base(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredVehicleTestData.json(999_999_999L, "NOPE-2")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Vehicle valuation with id 999999999 not found"));
    }

    @Test
    void duplicatePlateReturns409EvenForAnotherInsured() throws Exception {
        long owner = createInsured("DOC-IV5", "d@example.com");
        long other = createInsured("DOC-IV6", "e@example.com");
        createVehicle(owner, current2020.getId(), "DUP-1");

        mockMvc.perform(post(base(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredVehicleTestData.json(current2021.getId(), "dup-1")))
                .andExpect(status().isConflict());
    }

    @Test
    void vehicleIsNotReachableThroughAnotherInsured() throws Exception {
        long owner = createInsured("DOC-IV7", "f@example.com");
        long other = createInsured("DOC-IV8", "g@example.com");
        long id = createVehicle(owner, current2020.getId(), "MINE-1");

        mockMvc.perform(get(base(other) + "/" + id)).andExpect(status().isNotFound());
        mockMvc.perform(delete(base(other) + "/" + id)).andExpect(status().isNotFound());
        mockMvc.perform(get(base(owner) + "/" + id)).andExpect(status().isOk());
    }

    @Test
    void deletingInsuredCascadesToVehicles() throws Exception {
        long owner = createInsured("DOC-IV9", "h@example.com");
        createVehicle(owner, current2020.getId(), "CAS-1");
        createVehicle(owner, current2021.getId(), "CAS-2");

        mockMvc.perform(delete("/api/v1/insureds/" + owner)).andExpect(status().isNoContent());

        assertThat(vehicleRepository.count()).isZero();
    }

    @Test
    void valuationInUseCannotBeDeleted() throws Exception {
        long owner = createInsured("DOC-IV10", "i@example.com");
        createVehicle(owner, current2020.getId(), "USED-1");

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM vehicle_valuation WHERE id = ?", current2020.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
