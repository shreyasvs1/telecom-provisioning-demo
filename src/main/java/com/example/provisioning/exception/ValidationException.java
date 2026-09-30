package com.example.provisioning.exception;

import java.util.List;

/**
 * Thrown when the incoming request fails business validation rules.
 * In the real system, this is where IBM ODM would reject a payload;
 * here it's a stand-in with the same shape (a set of rule violations),
 * so a business rule change later can be modeled the same way.
 */
public class ValidationException extends RuntimeException {

    private final List<String> ruleViolations;

    public ValidationException(List<String> ruleViolations) {
        super("Request failed business validation rules: " + ruleViolations);
        this.ruleViolations = ruleViolations;
    }

    public List<String> getRuleViolations() {
        return ruleViolations;
    }
}
