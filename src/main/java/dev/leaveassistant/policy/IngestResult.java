package dev.leaveassistant.policy;

/** Outcome of an ingest call. status is INGESTED, or UNCHANGED when the same file was already loaded. */
public record IngestResult(String status, String document, int chunks) {
}
