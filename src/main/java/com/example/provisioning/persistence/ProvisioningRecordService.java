package com.example.provisioning.persistence;

import com.example.provisioning.model.ProvisioningRequest;
import com.example.provisioning.model.WorkOrder;
import com.example.provisioning.model.WorkSpec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Saves each incoming request and the work orders created for it.
 *
 * The request is saved first, in its own transaction, so that it is kept
 * even when the pipeline later rejects it or fails. Each later call then
 * records the outcome against that same row.
 */
@Service
public class ProvisioningRecordService {

    private static final int MAX_KEY_LENGTH = 100;

    private final ProvisioningRequestRepository requestRepository;
    private final WorkOrderRecordRepository workOrderRepository;
    private final ObjectMapper objectMapper;

    public ProvisioningRecordService(ProvisioningRequestRepository requestRepository,
                                     WorkOrderRecordRepository workOrderRepository,
                                     ObjectMapper objectMapper) {
        this.requestRepository = requestRepository;
        this.workOrderRepository = workOrderRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Long recordReceived(ProvisioningRequest request) {
        ProvisioningRequestRecord record = new ProvisioningRequestRecord();
        // Copied before validation, so these may be null or oversized
        record.setOrderId(truncate(request.getOrderId()));
        record.setCustomerId(truncate(request.getCustomerId()));
        record.setRequestType(truncate(request.getRequestType()));
        record.setPayload(toJson(request));
        record.setStatus(RequestStatus.RECEIVED);
        record.setReceivedAt(LocalDateTime.now());
        return requestRepository.save(record).getId();
    }

    @Transactional
    public void recordCompleted(Long requestId, List<WorkOrder> workOrders) {
        ProvisioningRequestRecord request = requestRepository.getReferenceById(requestId);
        LocalDateTime now = LocalDateTime.now();

        for (WorkOrder workOrder : workOrders) {
            WorkOrderRecord woRecord = new WorkOrderRecord();
            woRecord.setRequest(request);
            woRecord.setWorkOrderNumber(workOrder.getWorkOrderId());
            woRecord.setDispatchGroup(workOrder.getDispatchGroup());
            woRecord.setSequenceNo(workOrder.getSequence());
            woRecord.setCreatedAt(now);

            int lineNo = 1;
            for (WorkSpec spec : workOrder.getWorkSpecs()) {
                WorkOrderSpecRecord specRecord = new WorkOrderSpecRecord();
                specRecord.setLineNo(lineNo++);
                specRecord.setWorkSpecCode(spec.getWorkSpecCode());
                specRecord.setDescription(spec.getDescription());
                specRecord.setDurationMinutes(spec.getEstimatedDurationMinutes());
                specRecord.setRequiredSkill(spec.getRequiredSkill());
                woRecord.addSpec(specRecord);
            }
            workOrderRepository.save(woRecord);
        }

        updateStatus(requestId, RequestStatus.COMPLETED, null);
    }

    @Transactional
    public void recordRejected(Long requestId, List<String> violations) {
        updateStatus(requestId, RequestStatus.REJECTED, String.join("\n", violations));
    }

    @Transactional
    public void recordFailed(Long requestId, Exception error) {
        updateStatus(requestId, RequestStatus.FAILED, error.getClass().getName() + ": " + error.getMessage());
    }

    private void updateStatus(Long requestId, RequestStatus status, String detail) {
        ProvisioningRequestRecord record = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("No provisioning_request row with id " + requestId));
        record.setStatus(status);
        record.setStatusDetail(detail);
        record.setCompletedAt(LocalDateTime.now());
    }

    private String toJson(ProvisioningRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize provisioning request", e);
        }
    }

    private static String truncate(String value) {
        return value == null || value.length() <= MAX_KEY_LENGTH ? value : value.substring(0, MAX_KEY_LENGTH);
    }
}
