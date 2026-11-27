package com.carinsurance.insured;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carinsurance.TestcontainersConfiguration;
import com.carinsurance.vehicle.VehicleCatalog;
import com.carinsurance.vehicle.VehicleCatalogRepository;
import com.carinsurance.vehicle.VehicleCatalogTestData;
import com.carinsurance.vehicle.VehicleType;
import com.carinsurance.vehicle.VehicleValuation;
import com.carinsurance.vehicle.VehicleValuationRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Vehicles are created by issuing a policy; here they are created through the services and read through the API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class InsuredVehicleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InsuredService insuredService;

    @Autowired
    private InsuredVehicleService vehicleService;

    @Autowired
    private InsuredVehicleRepository vehicleRepository;

    @Autowired
    private InsuredRepository insuredRepository;

    @Autowired
    private VehicleCatalogRepository catalogRepository;

    @Autowired
    private VehicleValuationRepository valuationRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private VehicleCatalog catalog;
    private VehicleValuation valuation;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM policy");
        jdbcTemplate.update("DELETE FROM insured");

        VehicleCatalogTestData data = new VehicleCatalogTestData(catalogRepository, valuationRepository);
        catalog = data.saveCatalog(VehicleCatalogTestData.uniqueMake(), "Alpha", "1.0 Base", VehicleType.CAR);
        valuation =
                data.saveValuation(catalog, 2020, "10400.00", LocalDate.now().minusDays(10));
    }

    private static String base(long insuredId) {
        return "/api/v1/insureds/" + insuredId + "/vehicles";
    }

    private Insured createInsured(String documentNumber, String email) {
        return insuredService.create(InsuredTestData.newInsured(documentNumber, email));
    }

    private long createVehicle(Insured insured, String plate) {
        return transactionTemplate.execute(status -> vehicleService
                .create(insured, valuation, InsuredVehicleTestData.newVehicle(plate))
                .getId());
    }

    @Test
    void createdVehicleIsReadableUnderItsInsured() throws Exception {
        Insured owner = createInsured("DOC-IV1", "owner@example.com");
        long id = createVehicle(owner, "ab 123-cd");

        mockMvc.perform(get(base(owner.getId()) + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licensePlate").value("AB123CD"))
                .andExpect(jsonPath("$.engineNumber").value("ENG-12345"))
                .andExpect(jsonPath("$.chassisNumber").value("8AP17000000123456"))
                .andExpect(jsonPath("$.insuredId").value(owner.getId()))
                .andExpect(jsonPath("$.catalog.id").value(catalog.getId()))
                .andExpect(jsonPath("$.catalog.modelVersion").value("1.0 Base"))
                .andExpect(jsonPath("$.valuation.id").value(valuation.getId()))
                .andExpect(jsonPath("$.createdAt").exists());

        mockMvc.perform(get(base(owner.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void unknownInsuredReturns404() throws Exception {
        mockMvc.perform(get(base(999_999L))).andExpect(status().isNotFound());
    }

    @Test
    void vehicleIsNotReachableThroughAnotherInsured() throws Exception {
        Insured owner = createInsured("DOC-IV7", "f@example.com");
        Insured other = createInsured("DOC-IV8", "g@example.com");
        long id = createVehicle(owner, "MIN123");

        mockMvc.perform(get(base(other.getId()) + "/" + id)).andExpect(status().isNotFound());
        mockMvc.perform(get(base(owner.getId()) + "/" + id)).andExpect(status().isOk());
    }

    /** The plate rule (no overlapping policies) is the policy's, so vehicle rows may repeat a plate. */
    @Test
    void samePlateCanBeStoredForAnotherVehicleRow() {
        Insured owner = createInsured("DOC-IV5", "d@example.com");
        Insured other = createInsured("DOC-IV6", "e@example.com");
        createVehicle(owner, "DUP123");
        createVehicle(other, "dup-123");

        assertThat(vehicleRepository.count()).isEqualTo(2);
    }

    @Test
    void deletingInsuredCascadesToVehicles() {
        Insured owner = createInsured("DOC-IV9", "h@example.com");
        createVehicle(owner, "CAS123");

        insuredRepository.deleteById(owner.getId());

        assertThat(vehicleRepository.count()).isZero();
    }

    @Test
    void valuationInUseCannotBeDeleted() {
        Insured owner = createInsured("DOC-IV10", "i@example.com");
        createVehicle(owner, "USE123");

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM vehicle_valuation WHERE id = ?", valuation.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
