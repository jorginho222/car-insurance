package com.carinsurance.quotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuotationExpirationJobTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);

    @Mock
    private QuotationRepository repository;

    @Test
    void expiresQuotationsValidUntilBeforeToday() {
        Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        given(repository.expireQuotedBefore(TODAY)).willReturn(3);

        assertThat(new QuotationExpirationJob(repository, clock).expireQuotations())
                .isEqualTo(3);
    }
}
