package com.carinsurance.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.location.dto.LocalityResponse;
import com.carinsurance.location.dto.ZoneResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocalityServiceTest {

    @Mock
    private LocalityRepository repository;

    private LocalityService service;
    private Locality laPlata;

    @BeforeEach
    void setUp() {
        service = new LocalityService(repository, new LocalityMapper());
        Zone zone = new Zone("Ciudades grandes del interior");
        zone.setId(3L);
        laPlata = new Locality("LA PLATA", "Buenos Aires", "1900", zone);
        laPlata.setId(10L);
    }

    @Test
    void mapsLocalitiesWithTheirZone() {
        given(repository.findByPostalCode("1900")).willReturn(List.of(laPlata));

        assertThat(service.findByPostalCode("1900"))
                .containsExactly(new LocalityResponse(
                        10L,
                        "LA PLATA",
                        "Buenos Aires",
                        "1900",
                        new ZoneResponse(3L, "Ciudades grandes del interior")));
    }

    @Test
    void findsById() {
        given(repository.findWithZoneById(10L)).willReturn(Optional.of(laPlata));

        assertThat(service.findById(10L).name()).isEqualTo("LA PLATA");
    }

    @Test
    void unknownIdThrowsNotFound() {
        given(repository.findWithZoneById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Locality with id 99 not found");
    }
}
