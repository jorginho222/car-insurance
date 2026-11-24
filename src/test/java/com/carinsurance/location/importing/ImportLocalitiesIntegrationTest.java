package com.carinsurance.location.importing;

import static org.assertj.core.api.Assertions.assertThat;

import com.carinsurance.TestcontainersConfiguration;
import com.carinsurance.location.LocalityRepository;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

/** The {@link LocalitySeeder} imports the bundled CSV when the test context starts on the empty database. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ImportLocalitiesIntegrationTest {

    @Autowired
    private LocalityRepository localityRepository;

    @Autowired
    private ImportLocalitiesUseCase importLocalities;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void seederImportsEveryLocalityWithItsZone() {
        assertThat(localityRepository.count()).isEqualTo(23_190);

        Map<String, Integer> perZone = jdbcTemplate
                .queryForList(
                        "select z.name, count(*) as n from locality l join zone z on z.id = l.zone_id group by z.name")
                .stream()
                .collect(Collectors.toMap(r -> (String) r.get("name"), r -> ((Number) r.get("n")).intValue()));
        assertThat(perZone)
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        ZoneResolver.CABA, 455,
                        ZoneResolver.GBA, 163,
                        ZoneResolver.LARGE_INTERIOR_CITIES, 71,
                        ZoneResolver.REST_OF_COUNTRY, 22_501));
    }

    @Test
    void importingAgainInsertsNothing() throws Exception {
        try (InputStream csv = new ClassPathResource(LocalitySeeder.CSV).getInputStream()) {
            ImportResult result = importLocalities.importFrom(csv);

            assertThat(result).isEqualTo(new ImportResult(23_190, 0));
        }
        assertThat(localityRepository.count()).isEqualTo(23_190);
    }
}
