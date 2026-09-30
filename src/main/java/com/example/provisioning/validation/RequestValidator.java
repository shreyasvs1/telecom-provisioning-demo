package com.example.provisioning.validation;

import com.example.provisioning.exception.ValidationException;
import com.example.provisioning.model.ProvisioningRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * STEP 2 of the pipeline: validates the incoming JSON.
 *
 * In the real system this logic lives in IBM ODM business rules (decision
 * tables), managed by BAs and executed by the rules engine rather than
 * hard-coded in Java. This class deliberately keeps the same *shape* --
 * a set of named rules, each producing a violation message -- so that
 * when you practice explaining "business rule changes" with Claude, you
 * can imagine each private method below as a row in an ODM decision table.
 */
@Component
public class RequestValidator {

    private static final Set<String> VALID_REQUEST_TYPES = Set.of("NEW_INSTALL", "UPGRADE", "TRANSFER");
    private static final Set<String> VALID_SERVICE_CODES = Set.of("INTERNET", "PHONE", "TV");

    public void validate(ProvisioningRequest request) {
        List<String> violations = new ArrayList<>();

        checkRequiredFields(request, violations);
        checkRequestType(request, violations);
        checkServiceAddress(request, violations);
        checkRequestedServices(request, violations);

        if (!violations.isEmpty()) {
            throw new ValidationException(violations);
        }
    }

    // RULE: orderId and customerId are mandatory on every request
    private void checkRequiredFields(ProvisioningRequest request, List<String> violations) {
        if (isBlank(request.getOrderId())) {
            violations.add("RULE_ORDER_ID_REQUIRED: orderId must not be blank");
        }
        if (isBlank(request.getCustomerId())) {
            violations.add("RULE_CUSTOMER_ID_REQUIRED: customerId must not be blank");
        }
    }

    // RULE: requestType must be one of the known enumerations
    private void checkRequestType(ProvisioningRequest request, List<String> violations) {
        if (isBlank(request.getRequestType())) {
            violations.add("RULE_REQUEST_TYPE_REQUIRED: requestType must not be blank");
        } else if (!VALID_REQUEST_TYPES.contains(request.getRequestType())) {
            violations.add("RULE_REQUEST_TYPE_INVALID: requestType must be one of " + VALID_REQUEST_TYPES);
        }
    }

    // RULE: a service address with at least street, city, state, zip is required
    private void checkServiceAddress(ProvisioningRequest request, List<String> violations) {
        if (request.getServiceAddress() == null) {
            violations.add("RULE_ADDRESS_REQUIRED: serviceAddress must be provided");
            return;
        }
        var addr = request.getServiceAddress();
        if (isBlank(addr.getStreet()) || isBlank(addr.getCity())
                || isBlank(addr.getState()) || isBlank(addr.getZip())) {
            violations.add("RULE_ADDRESS_INCOMPLETE: street, city, state and zip are all required");
        }
    }

    // RULE: at least one requested service, and every code must be recognized
    private void checkRequestedServices(ProvisioningRequest request, List<String> violations) {
        if (request.getRequestedServices() == null || request.getRequestedServices().isEmpty()) {
            violations.add("RULE_SERVICES_REQUIRED: at least one requestedService must be provided");
            return;
        }
        for (String code : request.getRequestedServices()) {
            if (code == null || !VALID_SERVICE_CODES.contains(code)) {
                violations.add("RULE_SERVICE_CODE_UNKNOWN: '" + code + "' is not a recognized service code");
            }
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
