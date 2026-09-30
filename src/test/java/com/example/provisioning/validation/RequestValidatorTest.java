package com.example.provisioning.validation;

import com.example.provisioning.exception.ValidationException;
import com.example.provisioning.model.Address;
import com.example.provisioning.model.ProvisioningRequest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RequestValidatorTest {

    private final RequestValidator validator = new RequestValidator();

    @Test
    void validRequest_passes() {
        assertDoesNotThrow(() -> validator.validate(validRequest()));
    }

    @Test
    void blankOrderIdAndCustomerId_reportsBothViolations() {
        ProvisioningRequest request = validRequest();
        request.setOrderId(" ");
        request.setCustomerId(null);

        List<String> violations = violationsFor(request);

        assertEquals(2, violations.size());
        assertHasRule(violations, "RULE_ORDER_ID_REQUIRED");
        assertHasRule(violations, "RULE_CUSTOMER_ID_REQUIRED");
    }

    @Test
    void missingRequestType_isRejected() {
        ProvisioningRequest request = validRequest();
        request.setRequestType(null);

        assertHasRule(violationsFor(request), "RULE_REQUEST_TYPE_REQUIRED");
    }

    @Test
    void unknownRequestType_isRejected() {
        ProvisioningRequest request = validRequest();
        request.setRequestType("CANCEL");

        assertHasRule(violationsFor(request), "RULE_REQUEST_TYPE_INVALID");
    }

    @Test
    void missingAddress_isRejected() {
        ProvisioningRequest request = validRequest();
        request.setServiceAddress(null);

        assertHasRule(violationsFor(request), "RULE_ADDRESS_REQUIRED");
    }

    @Test
    void incompleteAddress_isRejected() {
        ProvisioningRequest request = validRequest();
        request.getServiceAddress().setZip("");

        assertHasRule(violationsFor(request), "RULE_ADDRESS_INCOMPLETE");
    }

    @Test
    void emptyServices_isRejected() {
        ProvisioningRequest request = validRequest();
        request.setRequestedServices(List.of());

        assertHasRule(violationsFor(request), "RULE_SERVICES_REQUIRED");
    }

    @Test
    void nullServices_isRejected() {
        ProvisioningRequest request = validRequest();
        request.setRequestedServices(null);

        assertHasRule(violationsFor(request), "RULE_SERVICES_REQUIRED");
    }

    @Test
    void unknownServiceCode_reportsOneViolationPerBadCode() {
        ProvisioningRequest request = validRequest();
        request.setRequestedServices(List.of("INTERNET", "SATELLITE", "internet"));

        List<String> violations = violationsFor(request);

        assertEquals(2, violations.size());
        violations.forEach(v -> assertTrue(v.startsWith("RULE_SERVICE_CODE_UNKNOWN")));
    }

    @Test
    void nullServiceCode_isReportedAsViolationNotNpe() {
        ProvisioningRequest request = validRequest();
        request.setRequestedServices(Arrays.asList("INTERNET", null)); // List.of rejects nulls

        List<String> violations = violationsFor(request);

        assertEquals(1, violations.size());
        assertHasRule(violations, "RULE_SERVICE_CODE_UNKNOWN");
    }

    private ProvisioningRequest validRequest() {
        ProvisioningRequest request = new ProvisioningRequest();
        request.setOrderId("TEST-1");
        request.setCustomerId("CUST-1");
        request.setRequestType("NEW_INSTALL");
        request.setRequestedServices(List.of("INTERNET", "PHONE"));

        Address address = new Address();
        address.setStreet("1 Test St");
        address.setCity("Testville");
        address.setState("IL");
        address.setZip("62701");
        request.setServiceAddress(address);

        return request;
    }

    private List<String> violationsFor(ProvisioningRequest request) {
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validate(request));
        return ex.getRuleViolations();
    }

    private void assertHasRule(List<String> violations, String rule) {
        assertTrue(violations.stream().anyMatch(v -> v.startsWith(rule + ":")),
                () -> "Expected " + rule + " in " + violations);
    }
}
