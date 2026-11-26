package com.carinsurance.quotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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

    @Autowired
    private QuotationExpirationJob expirationJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        return body(valuation, birthDate, "driver@example.com");
    }

    private String body(VehicleValuation valuation, LocalDate birthDate, String email) {
        return """
                {"valuationId": %d, "birthDate": "%s", "localityId": %d, "email": "%s"}""".formatted(valuation.getId(), birthDate, gbaLocality.getId(), email);
    }

    private static String uniqueEmail() {
        return "driver-" + UUID.randomUUID() + "@example.com";
    }

    /*
     * CAR (base 200, theft 0.004, damage 0.015), GBA (1.20 / 1.60 / 1.10), age 22 (1.35 / 1.40), sum insured 20000:
     *   liability 324.00, theft 128.00, damage 462.00, net 914.00
     *   1 payment: admin 45.70, fee 10.00, base 969.70, vat 203.64, total 1173.34
     *   12 installments: financing 77.58, base 1047.28, vat 219.93, total 1267.21 = 105.61 + 11 x 105.60
     */
    @Test
    void storesTheQuotationOfEveryPlan() throws Exception {
        LocalDate today = LocalDate.now();

        MvcResult created = mockMvc.perform(
                        post(BASE).contentType(MediaType.APPLICATION_JSON).content(body(today.minusYears(22))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code").value(matchesPattern("Q-" + today.getYear() + "-\\d{6}")))
                .andExpect(jsonPath("$.email").value("driver@example.com"))
                .andExpect(jsonPath("$.status").value("QUOTED"))
                .andExpect(jsonPath("$.quotedAt").isNotEmpty())
                .andExpect(jsonPath("$.plans[2].paymentOptions[1].totalPremium").value(1267.21))
                .andReturn();

        // read it back: every amount, including the JSON payment options, survives the round trip
        mockMvc.perform(get(created.getResponse().getHeader("Location")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUOTED"))
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
                {"valuationId": %d, "birthDate": "1990-01-01", "localityId": 999999999, "email": "a@example.com"}""".formatted(valuation.getId());

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void quotationPastItsValidUntilReadsExpiredAndTheJobStoresIt() throws Exception {
        long id = createQuotation();
        jdbcTemplate.update(
                "UPDATE quotation SET valid_until = ? WHERE id = ?",
                LocalDate.now().minusDays(1),
                id);

        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"));
        assertThat(storedStatus(id)).isEqualTo("QUOTED");

        expirationJob.expireQuotations();

        assertThat(storedStatus(id)).isEqualTo("EXPIRED");
    }

    @Test
    void jobLeavesQuotationsStillValid() throws Exception {
        long id = createQuotation();

        expirationJob.expireQuotations();

        assertThat(storedStatus(id)).isEqualTo("QUOTED");
    }

    @Test
    void consecutiveQuotationsGetConsecutiveCodes() throws Exception {
        String first = createdCode(valuation, uniqueEmail());
        String second = createdCode(valuation, uniqueEmail());

        assertThat(number(second)).isEqualTo(number(first) + 1);
    }

    @Test
    void searchByEmailIsGroupedByVehicleAndNewestFirst() throws Exception {
        VehicleCatalogTestData data = new VehicleCatalogTestData(catalogRepository, valuationRepository);
        VehicleValuation otherValuation = data.saveValuation(
                data.saveCatalog(VehicleCatalogTestData.uniqueMake(), "Beta", "1.6", VehicleType.CAR),
                2022,
                "15000.00",
                LocalDate.now().minusDays(10));
        String email = uniqueEmail();
        // interleaved, so the order can only come from the sort
        String firstOther = createdCode(otherValuation, email);
        String first = createdCode(valuation, email);
        String secondOther = createdCode(otherValuation, email);
        String second = createdCode(valuation, email);
        createdCode(valuation, uniqueEmail()); // someone else's

        mockMvc.perform(get(BASE).param("email", email.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(4))
                .andExpect(jsonPath("$.content[0].code").value(second))
                .andExpect(jsonPath("$.content[1].code").value(first))
                .andExpect(jsonPath("$.content[2].code").value(secondOther))
                .andExpect(jsonPath("$.content[3].code").value(firstOther))
                .andExpect(jsonPath("$.content[3].vehicle.model").value("Beta"))
                .andExpect(jsonPath("$.content[3].plans.length()").value(3));
    }

    @Test
    void searchByCodeFindsThatQuotation() throws Exception {
        String email = uniqueEmail();
        String code = createdCode(valuation, email);
        createdCode(valuation, email);

        mockMvc.perform(get(BASE).param("code", code.toLowerCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].code").value(code))
                .andExpect(jsonPath("$.content[0].email").value(email));
    }

    @Test
    void searchOnlyFindsQuotedQuotationsStillValid() throws Exception {
        String email = uniqueEmail();
        String lastDay = createdCode(valuation, email);
        String expiredByDate = createdCode(valuation, email);
        String expired = createdCode(valuation, email);
        String accepted = createdCode(valuation, email);
        jdbcTemplate.update("UPDATE quotation SET valid_until = ? WHERE code = ?", LocalDate.now(), lastDay);
        jdbcTemplate.update(
                "UPDATE quotation SET valid_until = ? WHERE code = ?",
                LocalDate.now().minusDays(1),
                expiredByDate);
        jdbcTemplate.update("UPDATE quotation SET status = 'EXPIRED' WHERE code = ?", expired);
        jdbcTemplate.update("UPDATE quotation SET status = 'ACCEPTED' WHERE code = ?", accepted);

        mockMvc.perform(get(BASE).param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].code").value(lastDay))
                .andExpect(jsonPath("$.content[0].status").value("QUOTED"));
        for (String code : new String[] {expiredByDate, expired, accepted}) {
            mockMvc.perform(get(BASE).param("code", code))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0));
        }
    }

    @Test
    void searchWithoutEmailOrCodeIsRejected() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isBadRequest());
    }

    @Test
    void searchWithBothEmailAndCodeIsRejected() throws Exception {
        mockMvc.perform(get(BASE).param("email", "driver@example.com").param("code", "Q-2026-000001"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownQuotationIsNotFound() throws Exception {
        mockMvc.perform(get(BASE + "/999999999")).andExpect(status().isNotFound());
    }

    private long createQuotation() throws Exception {
        String response = mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(LocalDate.now().minusYears(30))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private String createdCode(VehicleValuation valuation, String email) throws Exception {
        String response = mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(valuation, LocalDate.now().minusYears(30), email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.code");
    }

    private static long number(String code) {
        return Long.parseLong(code.substring(code.lastIndexOf('-') + 1));
    }

    private String storedStatus(long id) {
        return jdbcTemplate.queryForObject("SELECT status FROM quotation WHERE id = ?", String.class, id);
    }
}
