package com.carinsurance.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carinsurance.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/** Uses far-off years so it does not interfere with the numbers of policies created by other tests. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PolicyNumberGeneratorIntegrationTest {

    @Autowired
    private PolicyNumberGenerator generator;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private String next(int year) {
        return transactionTemplate.execute(status -> generator.next(year));
    }

    @Test
    void numbersStartAtOneIncrementAndRestartEachYear() {
        assertThat(next(1911)).isEqualTo("AU-1911-0000001");
        assertThat(next(1911)).isEqualTo("AU-1911-0000002");
        assertThat(next(1912)).isEqualTo("AU-1912-0000001");
    }

    @Test
    void rolledBackNumberIsReused() {
        assertThat(next(1913)).isEqualTo("AU-1913-0000001");
        transactionTemplate.executeWithoutResult(status -> {
            generator.next(1913);
            status.setRollbackOnly();
        });

        assertThat(next(1913)).isEqualTo("AU-1913-0000002");
    }

    @Test
    void requiresATransaction() {
        assertThatThrownBy(() -> generator.next(1914)).isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void padsToSevenDigits() {
        assertThat(PolicyNumberGenerator.format(2026, 123)).isEqualTo("AU-2026-0000123");
    }
}
