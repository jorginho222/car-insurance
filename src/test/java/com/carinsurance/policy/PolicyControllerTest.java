package com.carinsurance.policy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carinsurance.common.exception.ConflictException;
import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.policy.dto.PolicyResponse;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PolicyController.class)
class PolicyControllerTest {

    private static final String BASE = "/api/v1/policies";
    private static final LocalDate VALID_FROM = LocalDate.of(2026, 12, 1);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PolicyService service;

    private static PolicyResponse response(long id) {
        return new PolicyResponse(
                id,
                "AU-2026-0000001",
                VALID_FROM,
                VALID_FROM.plusYears(1),
                new PolicyResponse.Quotation(7L, "Q-2026-000007"),
                null,
                null,
                PaymentMethod.CBU,
                null,
                null,
                null,
                Instant.now());
    }

    private static String body(String licensePlate, String engineNumber, String chassisNumber) {
        return PolicyTestData.json(
                7L, 72L, 12, VALID_FROM, "30123456", "jane@example.com", licensePlate, engineNumber, chassisNumber);
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        given(service.create(any())).willReturn(response(9L));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("AB 123 CD", "ENG-12345", "8AP17000000123456")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/policies/9"))
                .andExpect(jsonPath("$.number").value("AU-2026-0000001"))
                .andExpect(jsonPath("$.validTo").value("2027-12-01"))
                .andExpect(jsonPath("$.quotation.code").value("Q-2026-000007"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC123", "abc-123", "AB123CD", "ab 123 cd", "123ABC", "A123BCD"})
    void acceptsArgentinePlates(String plate) throws Exception {
        given(service.create(any())).willReturn(response(9L));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(plate, "ENG-12345", "8AP17000000123456")))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"AB12CD", "ABCD123", "AB-123-C", "AB_123_CD", "1234567"})
    void rejectsInvalidPlates(String plate) throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(plate, "ENG-12345", "8AP17000000123456")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.licensePlate").exists());
        verify(service, never()).create(any());
    }

    @Test
    void rejectsInvalidEngineAndChassisNumbers() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("AB123CD", "E1", "8AP1700000012345O")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.engineNumber").exists())
                .andExpect(jsonPath("$.errors.chassisNumber").exists());
    }

    @Test
    void rejectsMissingFields() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.quotationId").exists())
                .andExpect(jsonPath("$.errors.planId").exists())
                .andExpect(jsonPath("$.errors.installments").exists())
                .andExpect(jsonPath("$.errors.paymentMethod").exists())
                .andExpect(jsonPath("$.errors.validFrom").exists())
                .andExpect(jsonPath("$.errors.documentNumber").exists())
                .andExpect(jsonPath("$.errors.street").exists())
                .andExpect(jsonPath("$.errors.licensePlate").exists());
    }

    @Test
    void rejectsUnknownPaymentMethod() throws Exception {
        String body = body("AB123CD", "ENG-12345", "8AP17000000123456")
                .replace("\"paymentMethod\": \"CARD\"", "\"paymentMethod\": \"CASH\"");

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.paymentMethod").value("must be one of [CARD, CBU]"));
    }

    @Test
    void createReturns409OnConflict() throws Exception {
        given(service.create(any())).willThrow(new ConflictException("already a policy"));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("AB123CD", "ENG-12345", "8AP17000000123456")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("already a policy"));
    }

    @Test
    void getReturns404WhenMissing() throws Exception {
        given(service.findById(5L)).willThrow(new ResourceNotFoundException("Policy", 5L));

        mockMvc.perform(get(BASE + "/5"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Policy with id 5 not found"));
    }
}
