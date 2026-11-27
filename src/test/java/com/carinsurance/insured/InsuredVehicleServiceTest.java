package com.carinsurance.insured;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.carinsurance.common.exception.ResourceNotFoundException;
import com.carinsurance.vehicle.VehicleCatalog;
import com.carinsurance.vehicle.VehicleCatalogMapper;
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

    private InsuredVehicleService service;
    private VehicleValuation valuation;

    @BeforeEach
    void setUp() {
        service = new InsuredVehicleService(
                repository, insuredRepository, new InsuredVehicleMapper(new VehicleCatalogMapper()));
        VehicleCatalog catalog = VehicleCatalogTestData.catalog(1L, "Toyota", "Corolla", "2.0 XEI", VehicleType.CAR);
        valuation = VehicleCatalogTestData.valuation(catalog, 20L, 2020, "20000", LocalDate.of(2026, 1, 1));
    }

    private static Insured insured(long id) {
        Insured insured = new Insured();
        insured.setId(id);
        return insured;
    }

    @Test
    void createNormalizesAndLinksToInsuredAndValuation() {
        Insured owner = insured(1L);
        given(repository.saveAndFlush(any(InsuredVehicle.class))).willAnswer(inv -> inv.getArgument(0));

        InsuredVehicle vehicle = service.create(owner, valuation, InsuredVehicleTestData.newVehicle(" ab 123-cd "));

        assertThat(vehicle.getLicensePlate()).isEqualTo("AB123CD");
        assertThat(vehicle.getEngineNumber()).isEqualTo("ENG-12345");
        assertThat(vehicle.getChassisNumber()).isEqualTo("8AP17000000123456");
        assertThat(vehicle.getValuation()).isSameAs(valuation);
        assertThat(vehicle.getInsured()).isSameAs(owner);
        assertThat(owner.getVehicles()).containsExactly(vehicle);
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
}
