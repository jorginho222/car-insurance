package com.carinsurance.insured;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carinsurance.TestcontainersConfiguration;
import com.carinsurance.common.exception.DuplicateResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Insureds are created by issuing a policy; here they are created through the service and read through the API. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class InsuredIntegrationTest {

    private static final String BASE = "/api/v1/insureds";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InsuredService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clean() {
        jdbcTemplate.update("DELETE FROM policy");
        jdbcTemplate.update("DELETE FROM insured");
    }

    @Test
    void createdInsuredIsReadable() throws Exception {
        Insured insured = service.create(InsuredTestData.newInsured("DOC-100", "Jane@Example.com"));

        mockMvc.perform(get(BASE + "/" + insured.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentNumber").value("DOC-100"))
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.address.street").value("Av. Rivadavia 1234"))
                .andExpect(jsonPath("$.address.locality").value("FLORIDA"))
                .andExpect(jsonPath("$.address.province").value("Buenos Aires"))
                .andExpect(jsonPath("$.address.postalCode").value("1602"))
                .andExpect(jsonPath("$.createdAt").exists());

        mockMvc.perform(get(BASE).param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        mockMvc.perform(get(BASE + "/999999999")).andExpect(status().isNotFound());
    }

    @Test
    void duplicateDocumentOrEmailIsRejected() {
        service.create(InsuredTestData.newInsured("DOC-200", "first@example.com"));

        assertThatThrownBy(() -> service.create(InsuredTestData.newInsured("DOC-200", "second@example.com")))
                .isInstanceOf(DuplicateResourceException.class);
        assertThatThrownBy(() -> service.create(InsuredTestData.newInsured("DOC-201", "FIRST@example.com")))
                .isInstanceOf(DuplicateResourceException.class);
    }
}
