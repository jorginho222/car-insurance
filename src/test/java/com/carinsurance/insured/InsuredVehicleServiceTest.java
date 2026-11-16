package com.carinsurance.insured;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.carinsurance.common.exception.DuplicateResourceException;
import com.carinsurance.common.exception.InvalidRequestException;
import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.insured.dto.InsuredVehicleResponse;
import com.carinsurance.vehicle.VehicleCatalog;
import com.carinsurance.vehicle.VehicleCatalogMapper;
import com.carinsurance.vehicle.VehicleCatalogService;
import com.carinsurance.vehicle.VehicleCatalogTestData;
import com.carinsurance.vehicle.VehicleType;
import com.carinsurance.vehicle.VehicleValuation;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InsuredVehicleServiceTest {

    @Mock
    private InsuredVehicleRepository repository;

    @Mock
    private InsuredRepository insuredRepository;

    @Mock
    private VehicleCatalogService catalogService;

    private InsuredVehicleService service;
    private VehicleValuation valuation2020;
    private VehicleValuation valuation2021;

    @BeforeEach
    void setUp() {
        service = new InsuredVehicleService(
                repository, insuredRepository, catalogService, new InsuredVehicleMapper(new VehicleCatalogMapper()));
        VehicleCatalog catalog = VehicleCatalogTestData.catalog(1L, "Toyota", "Corolla", "2.0 XEI", VehicleType.CAR);
        LocalDate validFrom = LocalDate.of(2026, 1, 1);
        valuation2020 = VehicleCatalogTestData.valuation(catalog, 20L, 2020, "20000", validFrom);
        valuation2021 = VehicleCatalogTestData.valuation(catalog, 21L, 2021, "22000", validFrom);
    }

    private static Insured insured(long id) {
        Insured insured = new Insured();
        insured.setId(id);
        return insured;
    }

    private InsuredVehicle vehicleOwnedBy(Insured owner, long id) {
        InsuredVehicle vehicle = new InsuredVehicle();
        vehicle.setId(id);
        vehicle.setLicensePlate("ABC-123");
        vehicle.setValuation(valuation2020);
        owner.addVehicle(vehicle);
        return vehicle;
    }

    @Test
    void createPersistsNormalizesPlateAndLinksToInsuredAndValuation() {
        Insured owner = insured(1L);
        given(insuredRepository.findById(1L)).willReturn(Optional.of(owner));
        given(catalogService.getSelectableValuation(20L)).willReturn(valuation2020);
        given(repository.saveAndFlush(any(InsuredVehicle.class))).willAnswer(inv -> {
            InsuredVehicle v = inv.getArgument(0);
            v.setId(10L);
            return v;
        });

        InsuredVehicleResponse response = service.create(1L, InsuredVehicleTestData.request(20L, " abc-123 "));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.insuredId()).isEqualTo(1L);
        assertThat(response.licensePlate()).isEqualTo("ABC-123");
        assertThat(response.catalog().make()).isEqualTo("Toyota");
        assertThat(response.catalog().vehicleType()).isEqualTo(VehicleType.CAR);
        assertThat(response.valuation().id()).isEqualTo(20L);
        assertThat(response.valuation().year()).isEqualTo(2020);
        assertThat(owner.getVehicles())
                .singleElement()
                .satisfies(v -> assertThat(v.getInsured()).isSameAs(owner));
    }

    @Test
    void createThrowsWhenInsuredMissing() {
        given(insuredRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(99L, InsuredVehicleTestData.request(20L, "ABC-123")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Insured");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsDuplicatePlate() {
        given(insuredRepository.findById(1L)).willReturn(Optional.of(insured(1L)));
        given(repository.existsByLicensePlate("ABC-123")).willReturn(true);

        assertThatThrownBy(() -> service.create(1L, InsuredVehicleTestData.request(20L, "abc-123")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("license plate");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsValuationThatIsNotCurrent() {
        given(insuredRepository.findById(1L)).willReturn(Optional.of(insured(1L)));
        given(catalogService.getSelectableValuation(20L)).willThrow(new InvalidRequestException("not current"));

        assertThatThrownBy(() -> service.create(1L, InsuredVehicleTestData.request(20L, "ABC-123")))
                .isInstanceOf(InvalidRequestException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void updateKeepingTheSameValuationDoesNotRecheckIt() {
        InsuredVehicle vehicle = vehicleOwnedBy(insured(1L), 10L);
        given(repository.findByIdAndInsuredId(10L, 1L)).willReturn(Optional.of(vehicle));
        given(repository.saveAndFlush(vehicle)).willReturn(vehicle);

        InsuredVehicleResponse response = service.update(1L, 10L, InsuredVehicleTestData.request(20L, "xyz-999"));

        assertThat(response.licensePlate()).isEqualTo("XYZ-999");
        assertThat(response.valuation().id()).isEqualTo(20L);
        verify(catalogService, never()).getSelectableValuation(anyLong());
    }

    @Test
    void updateChangesValuation() {
        InsuredVehicle vehicle = vehicleOwnedBy(insured(1L), 10L);
        given(repository.findByIdAndInsuredId(10L, 1L)).willReturn(Optional.of(vehicle));
        given(catalogService.getSelectableValuation(21L)).willReturn(valuation2021);
        given(repository.saveAndFlush(vehicle)).willReturn(vehicle);

        InsuredVehicleResponse response = service.update(1L, 10L, InsuredVehicleTestData.request(21L, "ABC-123"));

        assertThat(response.valuation().year()).isEqualTo(2021);
        assertThat(vehicle.getValuation()).isSameAs(valuation2021);
    }

    @Test
    void updateRejectsPlateOwnedByAnotherVehicle() {
        InsuredVehicle vehicle = vehicleOwnedBy(insured(1L), 10L);
        given(repository.findByIdAndInsuredId(10L, 1L)).willReturn(Optional.of(vehicle));
        given(repository.existsByLicensePlateAndIdNot("TAKEN-1", 10L)).willReturn(true);

        assertThatThrownBy(() -> service.update(1L, 10L, InsuredVehicleTestData.request(20L, "taken-1")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void vehicleOfAnotherInsuredIsNotFound() {
        given(repository.findByIdAndInsuredId(10L, 2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(2L, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Vehicle with id 10 not found");
    }

    @Test
    void listOfUnknownInsuredIsNotFound() {
        given(insuredRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> service.findAll(99L, null)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesVehicleFromInsured() {
        Insured owner = insured(1L);
        InsuredVehicle vehicle = vehicleOwnedBy(owner, 10L);
        given(repository.findByIdAndInsuredId(10L, 1L)).willReturn(Optional.of(vehicle));

        service.delete(1L, 10L);

        assertThat(owner.getVehicles()).isEmpty();
        verify(repository).delete(vehicle);
    }

    @Test
    void deleteThrowsWhenMissing() {
        given(repository.findByIdAndInsuredId(3L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L, 3L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
