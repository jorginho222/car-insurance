package com.carinsurance.quotation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carinsurance.TestcontainersConfiguration;
import com.carinsurance.location.Locality;
import com.carinsurance.location.LocalityRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Runs against the seeded quotation parameters (V8–V11) and localities, with a valuation created by the test so the
 * amounts don't depend on the date-dependent seeded price lists.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class QuotationIntegrationTest {

    private static final String BASE = "/api/v1/quotations";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleCatalogRepository catalogRepository;

    @Autowired
    private VehicleValuationRepository valuationRepository;

    @Autowired
    private LocalityRepository localityRepository;

    private VehicleValuation valuation;
    private Locality gbaLocality;

    @BeforeEach
    void setUp() {
        VehicleCatalogTestData data = new VehicleCatalogTestData(catalogRepository, valuationRepository);
        VehicleCatalog catalog =
                data.saveCatalog(VehicleCatalogTestData.uniqueMake(), "Alpha", "1.0 Base", VehicleType.CAR);
        valuation =
                data.saveValuation(catalog, 2021, "20000.00", LocalDate.now().minusDays(10));
        gbaLocality = localityRepository.findByPostalCode("1602").stream()
                .filter(l -> l.getName().equals("FLORIDA"))
                .findFirst()
                .orElseThrow();
    }

    private String body(LocalDate birthDate) {
        return """
                {"valuationId": %d, "birthDate": "%s", "localityId": %d}""".formatted(valuation.getId(), birthDate, gbaLocality.getId());
    }

    /*
     * CAR (base 200, theft 0.004, damage 0.015), GBA (1.20 / 1.60 / 1.10), age 22 (1.35 / 1.40), sum insured 20000:
     *   liability 324.00, theft 128.00, damage 462.00, net 914.00
     *   1 payment: admin 45.70, fee 10.00, base 969.70, vat 203.64, total 1173.34
     *   12 installments: financing 77.58, base 1047.28, vat 219.93, total 1267.21 = 105.61 + 11 x 105.60
     */
    @Test
    void quotesEveryPlan() throws Exception {
        LocalDate today = LocalDate.now();

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body(today.minusYears(22))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validUntil").value(today.plusDays(15).toString()))
                .andExpect(jsonPath("$.vehicle.model").value("Alpha"))
                .andExpect(jsonPath("$.vehicle.year").value(2021))
                .andExpect(jsonPath("$.vehicle.sumInsured").value(20000.00))
                .andExpect(jsonPath("$.risk.postalCode").value("1602"))
                .andExpect(jsonPath("$.risk.locality").value("FLORIDA"))
                .andExpect(jsonPath("$.risk.zone").value("GBA"))
                .andExpect(jsonPath("$.risk.driverAge").value(22))
                .andExpect(jsonPath("$.plans.length()").value(3))
                .andExpect(jsonPath("$.plans[0].name").value("CIVIL_LIABILITY"))
                .andExpect(jsonPath("$.plans[0].coverages.length()").value(1))
                .andExpect(jsonPath("$.plans[0].netPremium").value(324.00))
                .andExpect(jsonPath("$.plans[1].name").value("THIRD_PARTY_EXTENDED"))
                .andExpect(jsonPath("$.plans[1].coverages.length()").value(2))
                .andExpect(jsonPath("$.plans[1].netPremium").value(452.00))
                .andExpect(jsonPath("$.plans[2].name").value("FULL_COVERAGE"))
                .andExpect(jsonPath("$.plans[2].coverages[0].premium").value(324.00))
                .andExpect(jsonPath("$.plans[2].coverages[1].premium").value(128.00))
                .andExpect(jsonPath("$.plans[2].coverages[2].type").value("DAMAGE"))
                .andExpect(jsonPath("$.plans[2].coverages[2].premium").value(462.00))
                .andExpect(jsonPath("$.plans[2].netPremium").value(914.00))
                .andExpect(
                        jsonPath("$.plans[2].paymentOptions[0].adminSurcharge").value(45.70))
                .andExpect(jsonPath("$.plans[2].paymentOptions[0].issuanceFee").value(10.00))
                .andExpect(jsonPath("$.plans[2].paymentOptions[0].vat").value(203.64))
                .andExpect(jsonPath("$.plans[2].paymentOptions[0].totalPremium").value(1173.34))
                .andExpect(jsonPath("$.plans[2].paymentOptions[1].installments").value(12))
                .andExpect(jsonPath("$.plans[2].paymentOptions[1].financingSurcharge")
                        .value(77.58))
                .andExpect(jsonPath("$.plans[2].paymentOptions[1].taxableBase").value(1047.28))
                .andExpect(jsonPath("$.plans[2].paymentOptions[1].vat").value(219.93))
                .andExpect(jsonPath("$.plans[2].paymentOptions[1].totalPremium").value(1267.21))
                .andExpect(jsonPath("$.plans[2].paymentOptions[1].firstInstallment")
                        .value(105.61))
                .andExpect(jsonPath("$.plans[2].paymentOptions[1].installmentAmount")
                        .value(105.60));
    }

    @Test
    void underAgeDriverIsRejected() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(LocalDate.now().minusYears(18).plusDays(1))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Driver under age"));
    }

    @Test
    void unknownLocalityIsNotFound() throws Exception {
        String body = """
                {"valuationId": %d, "birthDate": "1990-01-01", "localityId": 999999999}""".formatted(valuation.getId());

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }
}
