package com.example.provisioning;

import com.example.provisioning.catalog.WorkSpecCatalogService;
import com.example.provisioning.enrichment.LocationEnrichmentClient;
import com.example.provisioning.enrichment.ServiceEnrichmentClient;
import com.example.provisioning.exception.ValidationException;
import com.example.provisioning.model.Address;
import com.example.provisioning.model.ProvisioningRequest;
import com.example.provisioning.model.WorkOrder;
import com.example.provisioning.model.WorkSpec;
import com.example.provisioning.persistence.*;
import com.example.provisioning.rules.HierarchyRulesEngine;
import com.example.provisioning.service.ProvisioningOrchestrator;
import com.example.provisioning.validation.RequestValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProvisioningPersistenceTest {

    @Autowired private ProvisioningOrchestrator orchestrator;
    @Autowired private ProvisioningRequestRepository requestRepository;
    @Autowired private WorkOrderRecordRepository workOrderRepository;
    @Autowired private ProvisioningRecordService recordService;
    @Autowired private MockMvc mockMvc;

    @Autowired private RequestValidator validator;
    @Autowired private LocationEnrichmentClient locationEnrichmentClient;
    @Autowired private ServiceEnrichmentClient serviceEnrichmentClient;
    @Autowired private WorkSpecCatalogService workSpecCatalogService;

    @Test
    void completedRequest_savesPayloadAndWorkOrdersWithSpecs() {
        String orderId = uniqueOrderId();
        orchestrator.process(request(orderId, "62702", List.of("INTERNET", "PHONE", "TV")));

        ProvisioningRequestRecord saved = onlyRecordFor(orderId);
        assertEquals(RequestStatus.COMPLETED, saved.getStatus());
        assertEquals("CUST-1", saved.getCustomerId());
        assertEquals("NEW_INSTALL", saved.getRequestType());
        assertTrue(saved.getPayload().contains("\"orderId\":\"" + orderId + "\""));
        assertTrue(saved.getPayload().contains("\"requestedServices\":[\"INTERNET\",\"PHONE\",\"TV\"]"));
        assertTrue(saved.getPayload().contains("\"zip\":\"62702\""));
        assertNotNull(saved.getReceivedAt());
        assertNotNull(saved.getCompletedAt());
        assertNull(saved.getStatusDetail());

        List<WorkOrderRecord> workOrders = workOrderRepository.findByRequest_IdOrderBySequenceNoAsc(saved.getId());
        assertEquals(2, workOrders.size());

        WorkOrderRecord first = workOrders.get(0);
        assertEquals(orderId + "-WO1", first.getWorkOrderNumber());
        assertEquals("INSIDE_WIRING_CREW", first.getDispatchGroup());
        assertEquals(1, first.getSequenceNo());
        assertEquals(List.of("CONNECT_JACK", "CHECK_INSIDE_WIRING"), specCodes(first));

        WorkOrderRecord second = workOrders.get(1);
        assertEquals(orderId + "-WO2", second.getWorkOrderNumber());
        assertEquals("ELECTRONICS_CREW", second.getDispatchGroup());
        assertEquals(2, second.getSequenceNo());
        assertEquals(List.of("INSTALL_ONT", "INSTALL_MODEM", "PROVISION_ATA", "INSTALL_SET_TOP_BOX"), specCodes(second));

        WorkOrderSpecRecord ont = second.getSpecs().get(0);
        assertEquals(1, ont.getLineNo());
        assertEquals(30, ont.getDurationMinutes());
        assertEquals("ELECTRONICS", ont.getRequiredSkill());
        assertEquals("Install and activate Optical Network Terminal", ont.getDescription());
    }

    @Test
    void invalidRequest_isSavedAsRejectedWithNoWorkOrders() {
        String orderId = uniqueOrderId();
        ProvisioningRequest request = request(orderId, "62702", List.of("INTERNET"));
        request.setCustomerId(null);

        assertThrows(ValidationException.class, () -> orchestrator.process(request));

        ProvisioningRequestRecord saved = onlyRecordFor(orderId);
        assertEquals(RequestStatus.REJECTED, saved.getStatus());
        assertNull(saved.getCustomerId());
        assertTrue(saved.getStatusDetail().contains("RULE_CUSTOMER_ID_REQUIRED"));
        assertNotNull(saved.getCompletedAt());
        assertTrue(workOrderRepository.findByRequest_IdOrderBySequenceNoAsc(saved.getId()).isEmpty());
    }

    @Test
    void unserviceableAddress_isSavedAsRejected() {
        String orderId = uniqueOrderId();

        assertThrows(ValidationException.class,
                () -> orchestrator.process(request(orderId, "99999", List.of("INTERNET"))));

        ProvisioningRequestRecord saved = onlyRecordFor(orderId);
        assertEquals(RequestStatus.REJECTED, saved.getStatus());
        assertTrue(saved.getStatusDetail().contains("RULE_LOCATION_NOT_SERVICEABLE"));
        assertTrue(workOrderRepository.findByRequest_IdOrderBySequenceNoAsc(saved.getId()).isEmpty());
    }

    @Test
    void unexpectedError_isSavedAsFailedAndRethrown() {
        HierarchyRulesEngine brokenEngine = new HierarchyRulesEngine() {
            @Override
            public List<WorkOrder> organize(String orderId, List<WorkSpec> workSpecs) {
                throw new IllegalStateException("rules engine unavailable");
            }
        };
        ProvisioningOrchestrator failingOrchestrator = new ProvisioningOrchestrator(validator,
                locationEnrichmentClient, serviceEnrichmentClient, workSpecCatalogService, brokenEngine, recordService);
        String orderId = uniqueOrderId();

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> failingOrchestrator.process(request(orderId, "62701", List.of("PHONE"))));
        assertEquals("rules engine unavailable", thrown.getMessage());

        ProvisioningRequestRecord saved = onlyRecordFor(orderId);
        assertEquals(RequestStatus.FAILED, saved.getStatus());
        assertEquals("java.lang.IllegalStateException: rules engine unavailable", saved.getStatusDetail());
        assertTrue(workOrderRepository.findByRequest_IdOrderBySequenceNoAsc(saved.getId()).isEmpty());
    }

    @Test
    void sameOrderIdSubmittedTwice_keepsBothSubmissionsAndTheirWorkOrders() {
        String orderId = uniqueOrderId();
        orchestrator.process(request(orderId, "62701", List.of("PHONE")));
        orchestrator.process(request(orderId, "62701", List.of("PHONE")));

        List<ProvisioningRequestRecord> saved = requestRepository.findByOrderIdOrderByIdAsc(orderId);
        assertEquals(2, saved.size());
        for (ProvisioningRequestRecord record : saved) {
            assertEquals(RequestStatus.COMPLETED, record.getStatus());
            List<WorkOrderRecord> workOrders = workOrderRepository.findByRequest_IdOrderBySequenceNoAsc(record.getId());
            assertEquals(1, workOrders.size());
            assertEquals(orderId + "-WO1", workOrders.get(0).getWorkOrderNumber());
        }
    }

    @Test
    void oversizedKeyFields_areTruncatedButFullPayloadIsKept() {
        String orderId = uniqueOrderId() + "-" + "X".repeat(65); // 110 chars
        orchestrator.process(request(orderId, "62701", List.of("PHONE")));

        ProvisioningRequestRecord saved = requestRepository.findAll().stream()
                .filter(r -> r.getPayload().contains(orderId))
                .findFirst().orElseThrow();
        assertEquals(100, saved.getOrderId().length());
        assertEquals(orderId.substring(0, 100), saved.getOrderId());
        assertEquals(RequestStatus.COMPLETED, saved.getStatus());
        assertEquals(orderId + "-WO1",
                workOrderRepository.findByRequest_IdOrderBySequenceNoAsc(saved.getId()).get(0).getWorkOrderNumber());
    }

    @Test
    void httpEndpoint_persistsValidAndInvalidRequests() throws Exception {
        String validId = uniqueOrderId();
        mockMvc.perform(post("/api/provisioning/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validId, "62701", "PHONE")))
                .andExpect(status().isOk());

        ProvisioningRequestRecord valid = onlyRecordFor(validId);
        assertEquals(RequestStatus.COMPLETED, valid.getStatus());
        assertEquals(1, workOrderRepository.findByRequest_IdOrderBySequenceNoAsc(valid.getId()).size());

        String invalidId = uniqueOrderId();
        mockMvc.perform(post("/api/provisioning/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(invalidId, "62701", "SATELLITE")))
                .andExpect(status().isBadRequest());

        ProvisioningRequestRecord invalid = onlyRecordFor(invalidId);
        assertEquals(RequestStatus.REJECTED, invalid.getStatus());
        assertTrue(invalid.getStatusDetail().contains("RULE_SERVICE_CODE_UNKNOWN"));
        assertTrue(invalid.getPayload().contains("SATELLITE"));
    }

    private ProvisioningRequestRecord onlyRecordFor(String orderId) {
        List<ProvisioningRequestRecord> records = requestRepository.findByOrderIdOrderByIdAsc(orderId);
        assertEquals(1, records.size(), "expected exactly one stored request for " + orderId);
        return records.get(0);
    }

    private static List<String> specCodes(WorkOrderRecord workOrder) {
        return workOrder.getSpecs().stream().map(WorkOrderSpecRecord::getWorkSpecCode).toList();
    }

    private static String uniqueOrderId() {
        return "PERSIST-" + UUID.randomUUID();
    }

    private static ProvisioningRequest request(String orderId, String zip, List<String> services) {
        ProvisioningRequest request = new ProvisioningRequest();
        request.setOrderId(orderId);
        request.setCustomerId("CUST-1");
        request.setRequestType("NEW_INSTALL");
        request.setRequestedServices(services);

        Address address = new Address();
        address.setStreet("1 Test St");
        address.setCity("Testville");
        address.setState("IL");
        address.setZip(zip);
        request.setServiceAddress(address);
        return request;
    }

    private static String json(String orderId, String zip, String service) {
        return """
                {
                  "orderId": "%s",
                  "customerId": "CUST-9",
                  "requestType": "NEW_INSTALL",
                  "serviceAddress": {"street": "9 Main St", "city": "Springfield", "state": "IL", "zip": "%s"},
                  "requestedServices": ["%s"]
                }
                """.formatted(orderId, zip, service);
    }
}
