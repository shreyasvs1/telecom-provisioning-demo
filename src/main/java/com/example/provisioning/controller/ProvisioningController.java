package com.example.provisioning.controller;

import com.example.provisioning.exception.ValidationException;
import com.example.provisioning.model.ProvisioningRequest;
import com.example.provisioning.model.ProvisioningResponse;
import com.example.provisioning.service.ProvisioningOrchestrator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * STEP 1 of the pipeline: accepts the incoming JSON payload over HTTP.
 * Everything downstream (validation, enrichment, catalog lookup, hierarchy
 * rules) is delegated to ProvisioningOrchestrator.
 */
@RestController
@RequestMapping("/api/provisioning")
public class ProvisioningController {

    private final ProvisioningOrchestrator orchestrator;

    public ProvisioningController(ProvisioningOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/process")
    public ResponseEntity<?> process(@RequestBody ProvisioningRequest request) {
        try {
            ProvisioningResponse response = orchestrator.process(request);
            return ResponseEntity.ok(response);
        } catch (ValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "VALIDATION_FAILED", "violations", e.getRuleViolations()));
        }
    }
}
