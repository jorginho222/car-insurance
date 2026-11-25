package com.carinsurance.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables {@code @Scheduled} jobs, such as the quotation expiration job. */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
