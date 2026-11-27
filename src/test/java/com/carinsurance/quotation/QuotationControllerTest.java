package com.carinsurance.quotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carinsurance.common.exception.InvalidRequestException;
import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.quotation.dto.QuotationRequest;
import com.carinsurance.quotation.dto.QuotationResponse;
import com.carinsurance.vehicle.VehicleType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    private static String body(String birthDate) {
        return """
                {"valuationId": 10, "birthDate": "%s", "localityId": 20, "email": "driver@example.com"}""".formatted(birthDate);
    }

    private static QuotationResponse sampleResponse() {
        BigDecimal total = new BigDecimal("1173.34");
        return new QuotationResponse(
                30L,
                "Q-2026-000123",
                "driver@example.com",
                QuotationStatus.QUOTED,
                Instant.parse("2026-11-25T13:00:00Z"),
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
                List.of(new QuotationResponse.PlanQuote(
                        31L,
                        Plan.CIVIL_LIABILITY,
                        List.of(new PlanCoverage(CoverageType.LIABILITY, new BigDecimal("324.00"))),
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
    }

    @Test
    void storesTheQuotationAndReturns201WithLocation() throws Exception {
        given(service.quote(any(QuotationRequest.class))).willReturn(sampleResponse());

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body("2004-05-01")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/quotations/30"))
                .andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.code").value("Q-2026-000123"))
                .andExpect(jsonPath("$.email").value("driver@example.com"))
                .andExpect(jsonPath("$.status").value("QUOTED"))
                .andExpect(jsonPath("$.quotedAt").value("2026-11-25T13:00:00Z"))
                .andExpect(jsonPath("$.validUntil").value("2026-12-10"))
                .andExpect(jsonPath("$.vehicle.description").value("Toyota Corolla 2.0 XEI CVT"))
                .andExpect(jsonPath("$.vehicle.vehicleType").value("CAR"))
                .andExpect(jsonPath("$.vehicle.sumInsured").value(20000.00))
                .andExpect(jsonPath("$.risk.zone").value("GBA"))
                .andExpect(jsonPath("$.risk.driverAge").value(22))
                .andExpect(jsonPath("$.plans[0].id").value(31))
                .andExpect(jsonPath("$.plans[0].name").value("CIVIL_LIABILITY"))
                .andExpect(jsonPath("$.plans[0].coverages[0].type").value("LIABILITY"))
                .andExpect(jsonPath("$.plans[0].netPremium").value(324.00))
                .andExpect(jsonPath("$.plans[0].paymentOptions[0].installments").value(1))
                .andExpect(jsonPath("$.plans[0].paymentOptions[0].totalPremium").value(1173.34));
    }

    @Test
    void missingFieldsReturn400() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.valuationId").exists())
                .andExpect(jsonPath("$.errors.birthDate").exists())
                .andExpect(jsonPath("$.errors.localityId").exists())
                .andExpect(jsonPath("$.errors.email").exists());
        verifyNoInteractions(service);
    }

    @Test
    void planInRequestIsIgnored() throws Exception {
        given(service.quote(any(QuotationRequest.class))).willReturn(sampleResponse());

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                                {"plan": "GOLD", "valuationId": 10, "birthDate": "1990-01-01", "localityId": 20,
                                 "email": "driver@example.com"}"""))
                .andExpect(status().isCreated());
    }

    @Test
    void futureBirthDateReturns400() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(LocalDate.now().plusDays(1).toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.birthDate").exists());
        verifyNoInteractions(service);
    }

    @Test
    void underAgeDriverReturns422() throws Exception {
        given(service.quote(any(QuotationRequest.class))).willThrow(new UnderAgeDriverException(17, 18));

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body("2009-01-01")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Driver under age"))
                .andExpect(jsonPath("$.detail").value("Driver must be at least 18 years old (age: 17)"));
    }

    @Test
    void findsAQuotationById() throws Exception {
        given(service.findById(30L)).willReturn(sampleResponse());

        mockMvc.perform(get(BASE + "/30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.status").value("QUOTED"))
                .andExpect(jsonPath("$.plans[0].name").value("CIVIL_LIABILITY"));
    }

    @Test
    void searchesByEmailGroupedByVehicleAndNewestFirst() throws Exception {
        given(service.search(eq("driver@example.com"), isNull(), any(Pageable.class)))
                .willAnswer(invocation -> new PageImpl<>(List.of(sampleResponse()), invocation.getArgument(2), 1));

        mockMvc.perform(get(BASE).param("email", "driver@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("Q-2026-000123"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(service).search(eq("driver@example.com"), isNull(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort())
                .containsExactly(Sort.Order.asc("valuation.id"), Sort.Order.desc("quotedAt"));
    }

    @Test
    void searchesByCode() throws Exception {
        given(service.search(isNull(), eq("Q-2026-000123"), any(Pageable.class)))
                .willAnswer(invocation -> new PageImpl<>(List.of(sampleResponse()), invocation.getArgument(2), 1));

        mockMvc.perform(get(BASE).param("code", "Q-2026-000123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(30));
    }

    @Test
    void searchWithInvalidEmailReturns400() throws Exception {
        mockMvc.perform(get(BASE).param("email", "not-an-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
        verifyNoInteractions(service);
    }

    @Test
    void searchWithoutEmailOrCodeReturns400() throws Exception {
        given(service.search(isNull(), isNull(), any(Pageable.class)))
                .willThrow(new InvalidRequestException("Search quotations by either an email or a code, not both"));

        mockMvc.perform(get(BASE)).andExpect(status().isBadRequest());
    }

    @Test
    void unknownQuotationReturns404() throws Exception {
        given(service.findById(99L)).willThrow(new ResourceNotFoundException("Quotation", 99L));

        mockMvc.perform(get(BASE + "/99")).andExpect(status().isNotFound());
    }
}
