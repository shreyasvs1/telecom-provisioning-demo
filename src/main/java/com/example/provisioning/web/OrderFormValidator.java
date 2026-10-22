package com.example.provisioning.web;

import com.example.provisioning.validation.RequestValidator;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.List;

/**
 * Form-level checks for the "New order" page (KAN-6). They run before the
 * pipeline, so a form that fails them never creates an order, and each
 * problem is reported against the field it belongs to.
 *
 * These checks mirror RequestValidator, which stays the authority: every
 * order, including one that passes here, is still validated by the pipeline.
 * The allowed values come from RequestValidator so the two cannot drift.
 * There is no check on the order ID, because it is generated after this.
 */
@Component
public class OrderFormValidator implements Validator {

    @Override
    public boolean supports(Class<?> type) {
        return OrderForm.class.isAssignableFrom(type);
    }

    @Override
    public void validate(Object target, Errors errors) {
        OrderForm form = (OrderForm) target;

        // RULE_CUSTOMER_ID_REQUIRED
        if (isBlank(form.getCustomerId())) {
            errors.rejectValue("customerId", "required", "Enter the customer ID.");
        }

        // RULE_REQUEST_TYPE_REQUIRED, RULE_REQUEST_TYPE_INVALID
        if (isBlank(form.getRequestType())) {
            errors.rejectValue("requestType", "required", "Choose a request type.");
        } else if (!RequestValidator.VALID_REQUEST_TYPES.contains(form.getRequestType())) {
            errors.rejectValue("requestType", "invalid",
                    "Choose one of " + String.join(", ", RequestValidator.VALID_REQUEST_TYPES) + ".");
        }

        // RULE_ADDRESS_INCOMPLETE, reported against each missing field
        if (isBlank(form.getStreet())) {
            errors.rejectValue("street", "required", "Enter the street.");
        }
        if (isBlank(form.getCity())) {
            errors.rejectValue("city", "required", "Enter the city.");
        }
        if (isBlank(form.getState())) {
            errors.rejectValue("state", "required", "Enter the state or province.");
        }
        if (isBlank(form.getZip())) {
            errors.rejectValue("zip", "required", "Enter the zip or postal code.");
        }

        // RULE_SERVICES_REQUIRED, RULE_SERVICE_CODE_UNKNOWN
        List<String> services = form.getRequestedServices();
        if (services == null || services.isEmpty()) {
            errors.rejectValue("requestedServices", "required", "Choose at least one service.");
        } else {
            for (String code : services) {
                if (code == null || !RequestValidator.VALID_SERVICE_CODES.contains(code)) {
                    errors.rejectValue("requestedServices", "invalid",
                            "'" + code + "' is not a service that can be ordered.");
                }
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
