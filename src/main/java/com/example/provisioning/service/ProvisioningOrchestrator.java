package com.example.provisioning.service;

import com.example.provisioning.catalog.WorkSpecCatalogService;
import com.example.provisioning.enrichment.LocationEnrichmentClient;
import com.example.provisioning.enrichment.ServiceEnrichmentClient;
import com.example.provisioning.exception.ValidationException;
import com.example.provisioning.model.*;
import com.example.provisioning.persistence.ProvisioningRecordService;
import com.example.provisioning.rules.HierarchyRulesEngine;
import com.example.provisioning.validation.RequestValidator;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Ties the 5 pipeline steps together, in the same order described in the
 * real system:
 *
 *   1. Accept incoming JSON            -> ProvisioningRequest (already parsed by Spring)
 *   2. Validate (IBM ODM stand-in)     -> RequestValidator
 *   3. Enrich (external API stand-ins) -> LocationEnrichmentClient, ServiceEnrichmentClient
 *   4. Work spec catalog lookup        -> WorkSpecCatalogService (Oracle-backed)
 *   5. Hierarchy / dispatch rules      -> HierarchyRulesEngine (IBM ODM stand-in)
 *
 * Every request is also saved via ProvisioningRecordService: the payload on
 * arrival, then its outcome (work orders, rule violations, or the error).
 *
 * This class is deliberately the single place that shows the whole flow --
 * a good starting point when asking an AI assistant "trace how a request
 * becomes dispatchable work orders."
 */
@Service
public class ProvisioningOrchestrator {

    private final RequestValidator validator;
    private final LocationEnrichmentClient locationEnrichmentClient;
    private final ServiceEnrichmentClient serviceEnrichmentClient;
    private final WorkSpecCatalogService workSpecCatalogService;
    private final HierarchyRulesEngine hierarchyRulesEngine;
    private final ProvisioningRecordService recordService;

    public ProvisioningOrchestrator(RequestValidator validator,
                                     LocationEnrichmentClient locationEnrichmentClient,
                                     ServiceEnrichmentClient serviceEnrichmentClient,
                                     WorkSpecCatalogService workSpecCatalogService,
                                     HierarchyRulesEngine hierarchyRulesEngine,
                                     ProvisioningRecordService recordService) {
        this.validator = validator;
        this.locationEnrichmentClient = locationEnrichmentClient;
        this.serviceEnrichmentClient = serviceEnrichmentClient;
        this.workSpecCatalogService = workSpecCatalogService;
        this.hierarchyRulesEngine = hierarchyRulesEngine;
        this.recordService = recordService;
    }

    public ProvisioningResponse process(ProvisioningRequest request) {
        // Save the payload before anything can reject it
        Long requestId = recordService.recordReceived(request);
        try {
            List<WorkOrder> workOrders = runPipeline(request);
            recordService.recordCompleted(requestId, workOrders);
            return new ProvisioningResponse(request.getOrderId(), workOrders);
        } catch (ValidationException e) {
            recordService.recordRejected(requestId, e.getRuleViolations());
            throw e;
        } catch (RuntimeException e) {
            recordService.recordFailed(requestId, e);
            throw e;
        }
    }

    private List<WorkOrder> runPipeline(ProvisioningRequest request) {
        // STEP 2: validate
        validator.validate(request); // throws ValidationException on failure

        // STEP 3: enrich
        LocationInfo locationInfo = locationEnrichmentClient.enrich(request.getServiceAddress());
        if (!locationInfo.isServiceable()) {
            throw new ValidationException(List.of(
                    "RULE_LOCATION_NOT_SERVICEABLE: address " + request.getServiceAddress().getZip()
                            + " is not currently serviceable"));
        }
        List<ServiceInfo> serviceInfos = serviceEnrichmentClient.enrich(request.getRequestedServices());

        // STEP 4: work spec catalog lookup
        List<WorkSpec> workSpecs = workSpecCatalogService.deriveWorkSpecs(serviceInfos, locationInfo);

        // STEP 5: hierarchy / dispatch rules
        return hierarchyRulesEngine.organize(request.getOrderId(), workSpecs);
    }
}
