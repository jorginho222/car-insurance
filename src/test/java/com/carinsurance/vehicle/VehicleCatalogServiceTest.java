package com.carinsurance.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.carinsurance.common.exception.InvalidRequestException;
import com.carinsurance.common.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VehicleCatalogServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);

    @Mock
    private VehicleCatalogRepository catalogRepository;

    @Mock
    private VehicleValuationRepository valuationRepository;

    private VehicleCatalogService service;
    private VehicleCatalog catalog;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new VehicleCatalogService(catalogRepository, valuationRepository, new VehicleCatalogMapper(), clock);
        catalog = VehicleCatalogTestData.catalog(1L, "Toyota", "Corolla", "2.0 XEI", VehicleType.CAR);
    }

    @Test
    void currentValuationIsSelectable() {
        VehicleValuation valuation = VehicleCatalogTestData.valuation(catalog, 10L, 2020, "20000", TODAY);
        given(valuationRepository.findWithCatalogById(10L)).willReturn(Optional.of(valuation));
        given(valuationRepository.existsNewerCurrent(1L, 2020, TODAY, TODAY)).willReturn(false);

        assertThat(service.getSelectableValuation(10L)).isSameAs(valuation);
    }

    @Test
    void supersededValuationIsRejected() {
        LocalDate validFrom = TODAY.minusMonths(6);
        VehicleValuation valuation = VehicleCatalogTestData.valuation(catalog, 10L, 2020, "20000", validFrom);
        given(valuationRepository.findWithCatalogById(10L)).willReturn(Optional.of(valuation));
        given(valuationRepository.existsNewerCurrent(1L, 2020, validFrom, TODAY))
                .willReturn(true);

        assertThatThrownBy(() -> service.getSelectableValuation(10L))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("not the current valuation");
    }

    @Test
    void futureValuationIsRejected() {
        VehicleValuation valuation = VehicleCatalogTestData.valuation(catalog, 10L, 2020, "20000", TODAY.plusDays(1));
        given(valuationRepository.findWithCatalogById(10L)).willReturn(Optional.of(valuation));

        assertThatThrownBy(() -> service.getSelectableValuation(10L)).isInstanceOf(InvalidRequestException.class);
        verify(valuationRepository, never()).existsNewerCurrent(any(), any(), any(), any());
    }

    @Test
    void unknownValuationIsNotFound() {
        given(valuationRepository.findWithCatalogById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSelectableValuation(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Vehicle valuation");
    }

    @Test
    void valuationsOfUnknownCatalogAreNotFound() {
        given(catalogRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> service.findCurrentValuations(99L)).isInstanceOf(ResourceNotFoundException.class);
        verify(valuationRepository, never()).findCurrentByCatalogId(any(), any());
    }

    @Test
    void currentValuationsAreQueriedForToday() {
        VehicleValuation valuation = VehicleCatalogTestData.valuation(catalog, 10L, 2020, "20000", TODAY);
        given(catalogRepository.existsById(1L)).willReturn(true);
        given(valuationRepository.findCurrentByCatalogId(1L, TODAY)).willReturn(List.of(valuation));

        assertThat(service.findCurrentValuations(1L)).singleElement().satisfies(v -> {
            assertThat(v.id()).isEqualTo(10L);
            assertThat(v.catalogId()).isEqualTo(1L);
            assertThat(v.year()).isEqualTo(2020);
        });
    }
}
