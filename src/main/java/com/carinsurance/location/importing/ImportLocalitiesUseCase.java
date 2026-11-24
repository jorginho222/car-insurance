package com.carinsurance.location.importing;

import com.carinsurance.location.Zone;
import com.carinsurance.location.ZoneRepository;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Imports localities from the postal code CSV, assigning each one its zone. Idempotent: localities that already
 * exist (same province, name and postal code) are left untouched.
 */
@Service
public class ImportLocalitiesUseCase {

    private static final int BATCH_SIZE = 1_000;

    // Plain JDBC batching: Hibernate cannot batch inserts of IDENTITY-generated entities.
    private static final String INSERT = """
            INSERT INTO locality (name, province, postal_code, zone_id) VALUES (?, ?, ?, ?)
            ON CONFLICT (province, name, postal_code) DO NOTHING""";

    private final LocalityCsvReader reader;
    private final ZoneResolver zoneResolver;
    private final ZoneRepository zoneRepository;
    private final JdbcTemplate jdbcTemplate;

    public ImportLocalitiesUseCase(
            LocalityCsvReader reader,
            ZoneResolver zoneResolver,
            ZoneRepository zoneRepository,
            JdbcTemplate jdbcTemplate) {
        this.reader = reader;
        this.zoneResolver = zoneResolver;
        this.zoneRepository = zoneRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ImportResult importFrom(InputStream csv) {
        List<LocalityRow> rows = reader.read(csv);
        Map<String, Long> zoneIds =
                zoneRepository.findAll().stream().collect(Collectors.toMap(Zone::getName, Zone::getId));

        int[][] counts = jdbcTemplate.batchUpdate(INSERT, rows, BATCH_SIZE, (ps, row) -> {
            ps.setString(1, row.name());
            ps.setString(2, row.province());
            ps.setString(3, row.postalCode());
            ps.setLong(4, zoneId(zoneIds, zoneResolver.zoneName(row)));
        });

        int inserted = 0;
        for (int[] batch : counts) {
            for (int count : batch) {
                inserted += Math.max(count, 0);
            }
        }
        return new ImportResult(rows.size(), inserted);
    }

    private static long zoneId(Map<String, Long> zoneIds, String zoneName) {
        Long id = zoneIds.get(zoneName);
        if (id == null) {
            throw new IllegalStateException("Zone '%s' does not exist".formatted(zoneName));
        }
        return id;
    }
}
