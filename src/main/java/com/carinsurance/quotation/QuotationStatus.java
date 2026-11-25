package com.carinsurance.quotation;

public enum QuotationStatus {
    /** Issued and still open. */
    QUOTED,
    /** A policy was created from it. */
    ACCEPTED,
    /** Its valid-until date passed before it was accepted. */
    EXPIRED
}
