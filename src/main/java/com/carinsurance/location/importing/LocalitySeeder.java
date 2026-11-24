package com.carinsurance.location.importing;

import com.carinsurance.location.LocalityRepository;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * On a fresh install (no localities yet), imports the bundled postal code CSV. Runs after the Flyway migrations.
 * Disable with {@code app.localities.import-on-startup=false}.
 */
@Component
@ConditionalOnProperty(name = "app.localities.import-on-startup", havingValue = "true", matchIfMissing = true)
public class LocalitySeeder implements ApplicationRunner {

    static final String CSV = "data/localidades_cp_maestro.csv";

    private static final Logger log = LoggerFactory.getLogger(LocalitySeeder.class);

    private final LocalityRepository localityRepository;
    private final ImportLocalitiesUseCase importLocalities;

    public LocalitySeeder(LocalityRepository localityRepository, ImportLocalitiesUseCase importLocalities) {
        this.localityRepository = localityRepository;
        this.importLocalities = importLocalities;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (localityRepository.count() > 0) {
            log.debug("Localities already imported, skipping");
            return;
        }
        log.info("No localities found, importing {}", CSV);
        try (InputStream csv = new ClassPathResource(CSV).getInputStream()) {
            ImportResult result = importLocalities.importFrom(csv);
            log.info("Imported {} localities ({} read)", result.inserted(), result.read());
        }
    }
}
