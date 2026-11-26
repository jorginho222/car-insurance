package com.carinsurance.quotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carinsurance.TestcontainersConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/** Uses far-off years so it does not interfere with the codes of quotations created by other tests. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class QuotationCodeGeneratorIntegrationTest {

    @Autowired
    private QuotationCodeGenerator generator;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private String next(int year) {
        return transactionTemplate.execute(status -> generator.next(year));
    }

    @Test
    void numbersStartAtOneAndIncrementWithinAYear() {
        assertThat(next(1901)).isEqualTo("Q-1901-000001");
        assertThat(next(1901)).isEqualTo("Q-1901-000002");
        assertThat(next(1901)).isEqualTo("Q-1901-000003");
    }

    @Test
    void numbersRestartEachYear() {
        next(1902);
        next(1902);

        assertThat(next(1903)).isEqualTo("Q-1903-000001");
    }

    @Test
    void rolledBackNumberIsReused() {
        assertThat(next(1904)).isEqualTo("Q-1904-000001");
        transactionTemplate.executeWithoutResult(status -> {
            generator.next(1904);
            status.setRollbackOnly();
        });

        assertThat(next(1904)).isEqualTo("Q-1904-000002");
    }

    @Test
    void concurrentCallsGetDistinctConsecutiveNumbers() throws Exception {
        int calls = 40;
        List<Callable<String>> tasks = new ArrayList<>();
        for (int i = 0; i < calls; i++) {
            tasks.add(() -> next(1905));
        }
        List<String> codes = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            for (Future<String> future : executor.invokeAll(tasks)) {
                codes.add(future.get());
            }
        }

        assertThat(codes)
                .doesNotHaveDuplicates()
                .hasSize(calls)
                .allMatch(code -> code.compareTo("Q-1905-000001") >= 0 && code.compareTo("Q-1905-000040") <= 0);
    }

    @Test
    void requiresATransaction() {
        assertThatThrownBy(() -> generator.next(1906)).isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void padsToSixDigits() {
        assertThat(QuotationCodeGenerator.format(2026, 123)).isEqualTo("Q-2026-000123");
        assertThat(QuotationCodeGenerator.format(2026, 1_234_567)).isEqualTo("Q-2026-1234567");
    }
}
