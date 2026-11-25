package com.carinsurance.quotation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carinsurance.quotation.dto.QuotationRequest;
import com.carinsurance.quotation.dto.QuotationResponse;
import com.carinsurance.quotation.dto.QuotationResponse.Coverage;
import com.carinsurance.quotation.dto.QuotationResponse.PaymentOption;
import com.carinsurance.vehicle.VehicleType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(QuotationController.class)
class QuotationControllerTest {

    private static final String BASE = "/api/v1/quotations";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuotationService service;

    private static String body(String plan, String birthDate) {
        return """
                {"plan": %s, "valuationId": 10, "birthDate": %s, "localityId": 20}""".formatted(
                        plan == null ? "null" : '"' + plan + '"', birthDate == null ? "null" : '"' + birthDate + '"');
    }

    @Test
    void returnsTheQuotation() throws Exception {
        BigDecimal total = new BigDecimal("1173.34");
        given(service.quote(any(QuotationRequest.class)))
                .willReturn(new QuotationResponse(
                        LocalDate.of(2026, 12, 10),
                        new QuotationResponse.Vehicle(
                                "Toyota Corolla 2.0 XEI CVT",
                                "Toyota",
                                "Corolla",
                                "2.0 XEI CVT",
                                2021,
                                VehicleType.CAR,
                                new BigDecimal("20000.00")),
                        new QuotationResponse.Risk("1708", "MORON", "Buenos Aires", "GBA", 22),
                        new QuotationResponse.PlanQuote(
                                Plan.CIVIL_LIABILITY,
                                List.of(new Coverage(CoverageType.LIABILITY, new BigDecimal("324.00"))),
                                new BigDecimal("324.00"),
                                List.of(new PaymentOption(
                                        1,
                                        new BigDecimal("16.20"),
                                        new BigDecimal("10.00"),
                                        new BigDecimal("0.00"),
                                        new BigDecimal("350.20"),
                                        new BigDecimal("73.54"),
                                        total,
                                        total,
                                        total)))));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("CIVIL_LIABILITY", "2004-05-01")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validUntil").value("2026-12-10"))
                .andExpect(jsonPath("$.vehicle.description").value("Toyota Corolla 2.0 XEI CVT"))
                .andExpect(jsonPath("$.vehicle.vehicleType").value("CAR"))
                .andExpect(jsonPath("$.vehicle.sumInsured").value(20000.00))
                .andExpect(jsonPath("$.risk.zone").value("GBA"))
                .andExpect(jsonPath("$.risk.driverAge").value(22))
                .andExpect(jsonPath("$.plan.name").value("CIVIL_LIABILITY"))
                .andExpect(jsonPath("$.plan.coverages[0].type").value("LIABILITY"))
                .andExpect(jsonPath("$.plan.netPremium").value(324.00))
                .andExpect(jsonPath("$.plan.paymentOptions[0].installments").value(1))
                .andExpect(jsonPath("$.plan.paymentOptions[0].totalPremium").value(1173.34));
    }

    @Test
    void missingFieldsReturn400() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.plan").exists())
                .andExpect(jsonPath("$.errors.valuationId").exists())
                .andExpect(jsonPath("$.errors.birthDate").exists())
                .andExpect(jsonPath("$.errors.localityId").exists());
        verifyNoInteractions(service);
    }

    @Test
    void unknownPlanReturns400() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body("GOLD", "1990-01-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.plan")
                        .value("must be one of [CIVIL_LIABILITY, THIRD_PARTY_EXTENDED, FULL_COVERAGE]"));
        verifyNoInteractions(service);
    }

    @Test
    void futureBirthDateReturns400() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(
                                "FULL_COVERAGE", LocalDate.now().plusDays(1).toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.birthDate").exists());
        verifyNoInteractions(service);
    }

    @Test
    void underAgeDriverReturns422() throws Exception {
        given(service.quote(any(QuotationRequest.class))).willThrow(new UnderAgeDriverException(17, 18));

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body("FULL_COVERAGE", "2009-01-01")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Driver under age"))
                .andExpect(jsonPath("$.detail").value("Driver must be at least 18 years old (age: 17)"));
    }
}
