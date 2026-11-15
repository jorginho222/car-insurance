package com.carinsurance.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.carinsurance.common.exception.DuplicateResourceException;
import com.carinsurance.common.exception.InvalidRequestException;
import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.insured.Insured;
import com.carinsurance.insured.InsuredRepository;
import com.carinsurance.vehicle.dto.VehicleResponse;
import java.time.Year;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository repository;

    @Mock
    private InsuredRepository insuredRepository;

    private VehicleService service;

    @BeforeEach
    void setUp() {
        service = new VehicleService(repository, insuredRepository, new VehicleMapper());
    }

    private static Insured insured(long id) {
        Insured insured = new Insured();
        insured.setId(id);
        return insured;
    }

    private static Vehicle vehicleOwnedBy(Insured owner, long id) {
        Vehicle vehicle = new VehicleMapper().toEntity(VehicleTestData.request(owner.getId(), "ABC-123"));
        vehicle.setId(id);
        owner.addVehicle(vehicle);
        return vehicle;
    }

    @Test
    void createPersistsNormalizesPlateAndLinksToInsured() {
        Insured owner = insured(1L);
        given(insuredRepository.findById(1L)).willReturn(Optional.of(owner));
        given(repository.saveAndFlush(any(Vehicle.class))).willAnswer(inv -> {
            Vehicle v = inv.getArgument(0);
            v.setId(10L);
            return v;
        });

        VehicleResponse response = service.create(VehicleTestData.request(1L, " abc-123 "));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.insuredId()).isEqualTo(1L);
        assertThat(response.licensePlate()).isEqualTo("ABC-123");
        assertThat(owner.getVehicles()).hasSize(1);
        assertThat(owner.getVehicles().getFirst().getInsured()).isSameAs(owner);
    }

    @Test
    void createThrowsWhenInsuredMissing() {
        given(insuredRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(VehicleTestData.request(99L, "ABC-123")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Insured");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsDuplicatePlate() {
        given(insuredRepository.findById(1L)).willReturn(Optional.of(insured(1L)));
        given(repository.existsByLicensePlate("ABC-123")).willReturn(true);

        assertThatThrownBy(() -> service.create(VehicleTestData.request(1L, "abc-123")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("license plate");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsYearTooFarInFuture() {
        int tooLate = Year.now().getValue() + 2;

        assertThatThrownBy(() -> service.create(VehicleTestData.request(1L, "ABC-123", tooLate)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void updateMovesVehicleToAnotherInsured() {
        Insured oldOwner = insured(1L);
        Insured newOwner = insured(2L);
        Vehicle vehicle = vehicleOwnedBy(oldOwner, 10L);
        given(repository.findById(10L)).willReturn(Optional.of(vehicle));
        given(insuredRepository.findById(2L)).willReturn(Optional.of(newOwner));
        given(repository.saveAndFlush(vehicle)).willReturn(vehicle);

        VehicleResponse response = service.update(10L, VehicleTestData.request(2L, "XYZ-999"));

        assertThat(response.insuredId()).isEqualTo(2L);
        assertThat(response.licensePlate()).isEqualTo("XYZ-999");
        assertThat(oldOwner.getVehicles()).isEmpty();
        assertThat(newOwner.getVehicles()).containsExactly(vehicle);
    }

    @Test
    void updateRejectsPlateOwnedByAnotherVehicle() {
        Vehicle vehicle = vehicleOwnedBy(insured(1L), 10L);
        given(repository.findById(10L)).willReturn(Optional.of(vehicle));
        given(repository.existsByLicensePlateAndIdNot("TAKEN-1", 10L)).willReturn(true);

        assertThatThrownBy(() -> service.update(10L, VehicleTestData.request(1L, "taken-1")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void deleteRemovesVehicleFromInsured() {
        Insured owner = insured(1L);
        Vehicle vehicle = vehicleOwnedBy(owner, 10L);
        given(repository.findById(10L)).willReturn(Optional.of(vehicle));

        service.delete(10L);

        assertThat(owner.getVehicles()).isEmpty();
        verify(repository).delete(vehicle);
    }

    @Test
    void deleteThrowsWhenMissing() {
        given(repository.findById(3L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(3L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
