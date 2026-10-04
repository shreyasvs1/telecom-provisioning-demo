package com.example.provisioning.service;

import com.example.provisioning.persistence.ProvisioningRequestRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Generates the order ID for orders entered on the order entry screen, where
 * the operator does not type one. IDs look like ORD-1004.
 *
 * The number comes from a database sequence. Orders sent to the JSON endpoint
 * bring their own hand-written orderId, which may already use a number the
 * sequence has not reached yet, so any ID that is already saved is skipped.
 */
@Component
public class OrderIdGenerator {

    private static final String PREFIX = "ORD-";

    private final JdbcTemplate jdbcTemplate;
    private final ProvisioningRequestRepository requestRepository;

    public OrderIdGenerator(JdbcTemplate jdbcTemplate, ProvisioningRequestRepository requestRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.requestRepository = requestRepository;
    }

    public String nextOrderId() {
        String candidate;
        do {
            Long number = jdbcTemplate.queryForObject("SELECT order_id_seq.NEXTVAL FROM DUAL", Long.class);
            candidate = PREFIX + number;
        } while (requestRepository.existsByOrderId(candidate));
        return candidate;
    }
}
