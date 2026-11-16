package com.carinsurance.vehicle;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carinsurance.TestcontainersConfiguration;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VehicleCatalogIntegrationTest {

    private static final String BASE = "/api/v1/vehicle-catalog";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleCatalogRepository catalogRepository;

    @Autowired
    private VehicleValuationRepository valuationRepository;

    private VehicleCatalogTestData data;
    private String make;

    @BeforeEach
    void setUp() {
        // Catalog rows are not cleaned up: the seed data must stay, and each test uses its own unique make.
        data = new VehicleCatalogTestData(catalogRepository, valuationRepository);
        make = VehicleCatalogTestData.uniqueMake();
    }

    @Test
    void searchesAreFilteredByVehicleType() throws Exception {
        VehicleCatalog base = data.saveCatalog(make, "Alpha", "1.0 Base", VehicleType.CAR);
        VehicleCatalog top = data.saveCatalog(make, "Alpha", "2.0 Top", VehicleType.CAR);
        data.saveCatalog(make, "Beta", "X", VehicleType.CAR);
        data.saveCatalog(make, "Hauler", "H1", VehicleType.TRUCK);

        mockMvc.perform(get(BASE + "/makes").param("vehicleType", "CAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem(make)));
        mockMvc.perform(get(BASE + "/makes").param("vehicleType", "BUS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(hasItem(make))));

        mockMvc.perform(get(BASE + "/models").param("vehicleType", "CAR").param("make", make))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("Alpha"))
                .andExpect(jsonPath("$[1]").value("Beta"));
        mockMvc.perform(get(BASE + "/models").param("vehicleType", "TRUCK").param("make", make))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]").value("Hauler"));

        mockMvc.perform(get(BASE + "/versions")
                        .param("vehicleType", "CAR")
                        .param("make", make)
                        .param("model", "Alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(base.getId()))
                .andExpect(jsonPath("$[0].modelVersion").value("1.0 Base"))
                .andExpect(jsonPath("$[0].vehicleType").value("CAR"))
                .andExpect(jsonPath("$[1].id").value(top.getId()));

        mockMvc.perform(get(BASE + "/" + top.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.make").value(make))
                .andExpect(jsonPath("$.modelVersion").value("2.0 Top"));
    }

    @Test
    void valuationsReturnOnlyTheCurrentOnePerYear() throws Exception {
        LocalDate today = LocalDate.now();
        VehicleCatalog catalog = data.saveCatalog(make, "Alpha", "1.0", VehicleType.CAR);
        data.saveValuation(catalog, 2020, "10000.00", today.minusDays(100));
        VehicleValuation current2020 = data.saveValuation(catalog, 2020, "10400.00", today.minusDays(10));
        data.saveValuation(catalog, 2020, "11000.00", today.plusDays(30));
        VehicleValuation current2021 = data.saveValuation(catalog, 2021, "12000.00", today.minusDays(100));
        data.saveValuation(catalog, 2022, "15000.00", today.plusDays(30));

        mockMvc.perform(get(BASE + "/" + catalog.getId() + "/valuations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(current2021.getId()))
                .andExpect(jsonPath("$[0].year").value(2021))
                .andExpect(jsonPath("$[1].id").value(current2020.getId()))
                .andExpect(jsonPath("$[1].year").value(2020))
                .andExpect(jsonPath("$[1].value").value(10400.00))
                .andExpect(jsonPath("$[1].catalogId").value(catalog.getId()))
                .andExpect(jsonPath("$[1].validFrom").value(today.minusDays(10).toString()));
    }

    @Test
    void vehicleTypeIsRequiredAndValidated() throws Exception {
        mockMvc.perform(get(BASE + "/makes")).andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/models").param("make", "Toyota")).andExpect(status().isBadRequest());
        mockMvc.perform(get(BASE + "/makes").param("vehicleType", "PLANE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.vehicleType")
                        .value("must be one of [CAR, MOTORCYCLE, SUV, PICKUP, VAN, TRUCK, BUS]"));
    }

    @Test
    void unknownCatalogReturns404() throws Exception {
        mockMvc.perform(get(BASE + "/999999999")).andExpect(status().isNotFound());
        mockMvc.perform(get(BASE + "/999999999/valuations")).andExpect(status().isNotFound());
    }

    @Test
    void seedDataIsLoaded() throws Exception {
        mockMvc.perform(get(BASE + "/makes").param("vehicleType", "CAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem("Toyota")));
        mockMvc.perform(get(BASE + "/models").param("vehicleType", "MOTORCYCLE").param("make", "Honda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("CG 160"));
    }
}
