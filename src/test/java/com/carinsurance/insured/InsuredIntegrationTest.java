package com.carinsurance.insured;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carinsurance.TestcontainersConfiguration;
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
class InsuredIntegrationTest {

    private static final String BASE = "/api/v1/insureds";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InsuredRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void fullCrudRoundTrip() throws Exception {
        String body = mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredTestData.json("DOC-100", "Jane@Example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number id = JsonPath.read(body, "$.id");

        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentNumber").value("DOC-100"));

        mockMvc.perform(get(BASE).param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        mockMvc.perform(put(BASE + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredTestData.json("DOC-100", "jane.doe@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane.doe@example.com"));

        mockMvc.perform(delete(BASE + "/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get(BASE + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void duplicateDocumentOrEmailReturns409() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredTestData.json("DOC-200", "first@example.com")))
                .andExpect(status().isCreated());

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredTestData.json("DOC-200", "second@example.com")))
                .andExpect(status().isConflict());

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(InsuredTestData.json("DOC-201", "FIRST@example.com")))
                .andExpect(status().isConflict());
    }
}
