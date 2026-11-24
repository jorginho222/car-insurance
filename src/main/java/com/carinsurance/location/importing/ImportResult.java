package com.carinsurance.location.importing;

/** Outcome of a locality import: rows read from the CSV (after dropping duplicates) and rows actually inserted. */
public record ImportResult(int read, int inserted) {}
