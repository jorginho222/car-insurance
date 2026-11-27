package com.carinsurance.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.ResultActions;

/** Quotes through the API, then issues policies from the quotations. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PolicyIntegrationTest {

    private static final String BASE = "/api/v1/policies";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleCatalogRepository catalogRepository;

    @Autowired
    private VehicleValuationRepository valuationRepository;

    @Autowired
    private LocalityRepository localityRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private VehicleValuation valuation;
    private Locality locality;
    private LocalDate today;

    /** A stored quotation: its id and the ids of its plans, in plan order. */
    private record QuotedPlans(long quotationId, long civilLiability, long fullCoverage) {}

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM policy");
        jdbcTemplate.update("DELETE FROM insured");

        today = LocalDate.now();
        VehicleCatalogTestData data = new VehicleCatalogTestData(catalogRepository, valuationRepository);
        VehicleCatalog catalog =
                data.saveCatalog(VehicleCatalogTestData.uniqueMake(), "Alpha", "1.0 Base", VehicleType.CAR);
        valuation = data.saveValuation(catalog, 2021, "20000.00", today.minusDays(10));
        locality = localityRepository.findByPostalCode("1602").stream()
                .filter(l -> l.getName().equals("FLORIDA"))
                .findFirst()
                .orElseThrow();
    }

    private QuotedPlans quote() throws Exception {
        String body = """
                {"valuationId": %d, "birthDate": "%s", "localityId": %d, "email": "driver@example.com"}""".formatted(valuation.getId(), today.minusYears(30), locality.getId());
        String response = mockMvc.perform(post("/api/v1/quotations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return new QuotedPlans(
                ((Number) JsonPath.read(response, "$.id")).longValue(),
                ((Number) JsonPath.read(response, "$.plans[0].id")).longValue(),
                ((Number) JsonPath.read(response, "$.plans[2].id")).longValue());
    }

    private ResultActions createPolicy(long quotationId, long planId, LocalDate validFrom, String plate)
            throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        return mockMvc.perform(post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(PolicyTestData.json(
                        quotationId, planId, 12, validFrom, "DOC-" + unique, unique + "@example.com", plate)));
    }

    private String quotationStatus(long quotationId) {
        return jdbcTemplate.queryForObject("SELECT status FROM quotation WHERE id = ?", String.class, quotationId);
    }

    @Test
    void issuesThePolicyFromTheQuotedPlan() throws Exception {
        QuotedPlans quotation = quote();
        LocalDate validFrom = today.plusDays(5);

        String body = createPolicy(quotation.quotationId(), quotation.fullCoverage(), validFrom, "ab 123-cd")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.number").value(matchesPattern("AU-" + today.getYear() + "-\\d{7}")))
                .andExpect(jsonPath("$.validFrom").value(validFrom.toString()))
                .andExpect(jsonPath("$.validTo").value(validFrom.plusYears(1).toString()))
                .andExpect(jsonPath("$.quotation.id").value(quotation.quotationId()))
                .andExpect(jsonPath("$.plan.id").value(quotation.fullCoverage()))
                .andExpect(jsonPath("$.plan.name").value("FULL_COVERAGE"))
                .andExpect(jsonPath("$.plan.coverages.length()").value(3))
                .andExpect(jsonPath("$.paymentOption.installments").value(12))
                .andExpect(jsonPath("$.paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.insured.dateOfBirth")
                        .value(today.minusYears(30).toString()))
                .andExpect(jsonPath("$.insured.address.street").value("Av. Rivadavia 1234"))
                .andExpect(jsonPath("$.insured.address.locality").value("FLORIDA"))
                .andExpect(jsonPath("$.insured.address.postalCode").value("1602"))
                .andExpect(jsonPath("$.insured.address.country").value("Argentina"))
                .andExpect(jsonPath("$.vehicle.licensePlate").value("AB123CD"))
                .andExpect(jsonPath("$.vehicle.valuation.id").value(valuation.getId()))
                .andExpect(jsonPath("$.locality.id").value(locality.getId()))
                .andExpect(jsonPath("$.locality.zone.name").value("GBA"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        long insuredId = ((Number) JsonPath.read(body, "$.insured.id")).longValue();
        long vehicleId = ((Number) JsonPath.read(body, "$.vehicle.id")).longValue();

        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentOption.totalPremium").isNumber())
                .andExpect(jsonPath("$.plan.netPremium").isNumber())
                .andExpect(jsonPath("$.vehicle.chassisNumber").value("8AP17000000123456"));
        mockMvc.perform(get("/api/v1/quotations/" + quotation.quotationId()))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
        mockMvc.perform(get("/api/v1/insureds/" + insuredId)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/insureds/" + insuredId + "/vehicles/" + vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.engineNumber").value("ENG-12345"));
    }

    @Test
    void consecutivePoliciesGetConsecutiveNumbers() throws Exception {
        QuotedPlans first = quote();
        QuotedPlans second = quote();

        String a = createPolicy(first.quotationId(), first.fullCoverage(), today, "AAA111")
                .andReturn()
                .getResponse()
                .getContentAsString();
        String b = createPolicy(second.quotationId(), second.fullCoverage(), today, "BBB222")
                .andReturn()
                .getResponse()
                .getContentAsString();

        String numberA = JsonPath.read(a, "$.number");
        String numberB = JsonPath.read(b, "$.number");
        assertThat(Long.parseLong(numberB.substring(8))).isEqualTo(Long.parseLong(numberA.substring(8)) + 1);
    }

    @Test
    void aQuotationBecomesOnlyOnePolicy() throws Exception {
        QuotedPlans quotation = quote();
        createPolicy(quotation.quotationId(), quotation.fullCoverage(), today, "AB111CD")
                .andExpect(status().isCreated());

        createPolicy(quotation.quotationId(), quotation.civilLiability(), today, "AB222CD")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(matchesPattern(".*is ACCEPTED.*")));
    }

    @Test
    void expiredQuotationCannotBecomeAPolicy() throws Exception {
        QuotedPlans quotation = quote();
        jdbcTemplate.update(
                "UPDATE quotation SET valid_until = ? WHERE id = ?", today.minusDays(1), quotation.quotationId());

        createPolicy(quotation.quotationId(), quotation.fullCoverage(), today, "AB333CD")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(matchesPattern(".*is EXPIRED.*")));
    }

    @Test
    void planOfAnotherQuotationIsRejected() throws Exception {
        QuotedPlans quotation = quote();
        QuotedPlans other = quote();

        createPolicy(quotation.quotationId(), other.fullCoverage(), today, "AB444CD")
                .andExpect(status().isBadRequest());
        assertThat(quotationStatus(quotation.quotationId())).isEqualTo("QUOTED");
    }

    @Test
    void plateCannotHaveOverlappingPolicies() throws Exception {
        QuotedPlans first = quote();
        createPolicy(first.quotationId(), first.fullCoverage(), today.plusDays(10), "XYZ987")
                .andExpect(status().isCreated());

        QuotedPlans second = quote();
        createPolicy(second.quotationId(), second.fullCoverage(), today, "xyz-987")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(matchesPattern(".*XYZ987.*")));
        // the failed request is rolled back: the quotation is still open
        assertThat(quotationStatus(second.quotationId())).isEqualTo("QUOTED");
    }

    @Test
    void plateCanGetAPolicyStartingWhenItsCurrentOneEnds() throws Exception {
        QuotedPlans first = quote();
        createPolicy(first.quotationId(), first.fullCoverage(), today, "REN123").andExpect(status().isCreated());
        // the first policy now ends today (validTo is exclusive)
        jdbcTemplate.update(
                "UPDATE policy SET valid_from = ?, valid_to = ? WHERE quotation_plan_id = ?",
                today.minusYears(1),
                today,
                first.fullCoverage());

        QuotedPlans renewal = quote();
        createPolicy(renewal.quotationId(), renewal.fullCoverage(), today, "REN123")
                .andExpect(status().isCreated());
    }

    @Test
    void validFromMustBeWithinTheNext30Days() throws Exception {
        QuotedPlans quotation = quote();

        createPolicy(quotation.quotationId(), quotation.fullCoverage(), today.minusDays(1), "VAL123")
                .andExpect(status().isBadRequest());
        createPolicy(quotation.quotationId(), quotation.fullCoverage(), today.plusDays(31), "VAL123")
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownQuotationReturns404() throws Exception {
        createPolicy(999_999_999L, 1L, today, "NOP123").andExpect(status().isNotFound());
        mockMvc.perform(get(BASE + "/999999999")).andExpect(status().isNotFound());
    }
}
