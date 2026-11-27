package com.carinsurance.insured;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.carinsurance.common.exception.DuplicateResourceException;
import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.insured.dto.InsuredResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InsuredServiceTest {

    @Mock
    private InsuredRepository repository;

    private InsuredService service;

    @BeforeEach
    void setUp() {
        service = new InsuredService(repository, new InsuredMapper());
    }

    @Test
    void createPersistsNormalizedValues() {
        given(repository.saveAndFlush(any(Insured.class))).willAnswer(inv -> inv.getArgument(0));

        Insured insured = service.create(InsuredTestData.newInsured(" DOC-1 ", " Jane@Example.com "));

        assertThat(insured.getFirstName()).isEqualTo("Jane");
        assertThat(insured.getDocumentNumber()).isEqualTo("DOC-1");
        assertThat(insured.getEmail()).isEqualTo("jane@example.com");
        assertThat(insured.getAddress().getStreet()).isEqualTo("Av. Rivadavia 1234");
        assertThat(insured.getAddress().getLocality()).isEqualTo("FLORIDA");
        assertThat(insured.getAddress().getProvince()).isEqualTo("Buenos Aires");
    }

    @Test
    void createRejectsDuplicateDocument() {
        given(repository.existsByDocumentNumber("DOC-1")).willReturn(true);

        assertThatThrownBy(() -> service.create(InsuredTestData.newInsured(" DOC-1", "a@b.com")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("document number");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsDuplicateEmail() {
        given(repository.existsByEmailIgnoreCase("a@b.com")).willReturn(true);

        assertThatThrownBy(() -> service.create(InsuredTestData.newInsured("DOC-1", " A@b.com")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void findByIdMapsTheInsured() {
        Insured insured = new InsuredMapper().toEntity(InsuredTestData.newInsured("DOC-1", "a@b.com"));
        insured.setId(7L);
        given(repository.findById(7L)).willReturn(Optional.of(insured));

        InsuredResponse response = service.findById(7L);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.address().locality()).isEqualTo("FLORIDA");
        assertThat(response.address().province()).isEqualTo("Buenos Aires");
    }

    @Test
    void findByIdThrowsWhenMissing() {
        given(repository.findById(42L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(42L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
