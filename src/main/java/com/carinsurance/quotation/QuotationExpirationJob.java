package com.carinsurance.quotation;

import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moves quoted quotations past their valid-until date to {@link QuotationStatus#EXPIRED}. Reads already report them as
 * expired before this runs (see {@link Quotation#statusOn(LocalDate)}); this keeps the stored status in sync.
 */
@Component
public class QuotationExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(QuotationExpirationJob.class);

    private final QuotationRepository repository;
    private final Clock clock;

    public QuotationExpirationJob(QuotationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Scheduled(cron = "${quotation.expiration-cron}")
    @Transactional
    public int expireQuotations() {
        int expired = repository.expireQuotedBefore(LocalDate.now(clock));
        log.info("Expired {} quotation(s)", expired);
        return expired;
    }
}
