package com.example.provisioning.persistence;

/**
 * Lifecycle of a stored provisioning request.
 *
 *   RECEIVED  -> saved on arrival, before validation runs
 *   COMPLETED -> pipeline finished and its work orders were saved
 *   REJECTED  -> failed a business rule (validation or serviceability)
 *   FAILED    -> an unexpected error stopped the pipeline
 */
public enum RequestStatus {
    RECEIVED, COMPLETED, REJECTED, FAILED
}
