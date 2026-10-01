package com.example.provisioning.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps to the WORK_ORDER table: one row per dispatchable work order that
 * step 5 produced for a completed request. WORK_ORDER_NUMBER is the
 * business ID returned in the API response (e.g. ORD-1001-WO1).
 */
@Entity
@Table(name = "work_order")
public class WorkOrderRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private ProvisioningRequestRecord request;

    @Column(name = "work_order_number", nullable = false, length = 150)
    private String workOrderNumber;

    @Column(name = "dispatch_group", nullable = false, length = 100)
    private String dispatchGroup;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<WorkOrderSpecRecord> specs = new ArrayList<>();

    public void addSpec(WorkOrderSpecRecord spec) {
        spec.setWorkOrder(this);
        specs.add(spec);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ProvisioningRequestRecord getRequest() { return request; }
    public void setRequest(ProvisioningRequestRecord request) { this.request = request; }

    public String getWorkOrderNumber() { return workOrderNumber; }
    public void setWorkOrderNumber(String workOrderNumber) { this.workOrderNumber = workOrderNumber; }

    public String getDispatchGroup() { return dispatchGroup; }
    public void setDispatchGroup(String dispatchGroup) { this.dispatchGroup = dispatchGroup; }

    public int getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(int sequenceNo) { this.sequenceNo = sequenceNo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<WorkOrderSpecRecord> getSpecs() { return specs; }
}
