package com.carinsurance.insured;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.carinsurance.common.exception.DuplicateResourceException;
import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.insured.dto.InsuredRequest;
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
    void createPersistsAndNormalizesEmail() {
        InsuredRequest request = InsuredTestData.request("DOC-1", "Jane@Example.com");
        given(repository.saveAndFlush(any(Insured.class))).willAnswer(inv -> {
            Insured i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });

        InsuredResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("jane@example.com");
        assertThat(response.address().city()).isEqualTo("Springfield");
    }

    @Test
    void createRejectsDuplicateDocument() {
        given(repository.existsByDocumentNumber("DOC-1")).willReturn(true);

        assertThatThrownBy(() -> service.create(InsuredTestData.request("DOC-1", "a@b.com")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("document number");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsDuplicateEmail() {
        given(repository.existsByEmailIgnoreCase("a@b.com")).willReturn(true);

        assertThatThrownBy(() -> service.create(InsuredTestData.request("DOC-1", "a@b.com")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email");
    }

    @Test
    void findByIdThrowsWhenMissing() {
        given(repository.findById(42L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(42L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateChangesFields() {
        Insured existing = new InsuredMapper().toEntity(InsuredTestData.request("DOC-1", "a@b.com"));
        existing.setId(7L);
        given(repository.findById(7L)).willReturn(Optional.of(existing));
        given(repository.saveAndFlush(existing)).willReturn(existing);

        InsuredResponse response = service.update(7L, InsuredTestData.request("DOC-2", "new@b.com"));

        assertThat(response.documentNumber()).isEqualTo("DOC-2");
        assertThat(response.email()).isEqualTo("new@b.com");
    }

    @Test
    void updateRejectsEmailOwnedByAnotherInsured() {
        Insured existing = new InsuredMapper().toEntity(InsuredTestData.request("DOC-1", "a@b.com"));
        given(repository.findById(7L)).willReturn(Optional.of(existing));
        given(repository.existsByEmailIgnoreCaseAndIdNot("taken@b.com", 7L)).willReturn(true);

        assertThatThrownBy(() -> service.update(7L, InsuredTestData.request("DOC-1", "taken@b.com")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void deleteRemovesExisting() {
        Insured existing = new Insured();
        given(repository.findById(3L)).willReturn(Optional.of(existing));

        service.delete(3L);

        verify(repository).delete(existing);
    }

    @Test
    void deleteThrowsWhenMissing() {
        given(repository.findById(3L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(3L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
