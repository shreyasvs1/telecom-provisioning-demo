package com.example.provisioning.model;

import java.util.List;

/**
 * A dispatchable unit of work: one or more WorkSpecs grouped together
 * because they belong to the same crew/skill and should be sent out
 * as a single job. This is the output of step 5 (hierarchy/dispatch rules).
 */
public class WorkOrder {

    private String workOrderId;
    private String dispatchGroup; // e.g. OUTSIDE_PLANT_CREW, INSIDE_WIRING_CREW
    private int sequence;         // order relative to other work orders for this request
    private List<WorkSpec> workSpecs;

    public WorkOrder() {}

    public WorkOrder(String workOrderId, String dispatchGroup, int sequence, List<WorkSpec> workSpecs) {
        this.workOrderId = workOrderId;
        this.dispatchGroup = dispatchGroup;
        this.sequence = sequence;
        this.workSpecs = workSpecs;
    }

    public String getWorkOrderId() { return workOrderId; }
    public void setWorkOrderId(String workOrderId) { this.workOrderId = workOrderId; }

    public String getDispatchGroup() { return dispatchGroup; }
    public void setDispatchGroup(String dispatchGroup) { this.dispatchGroup = dispatchGroup; }

    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }

    public List<WorkSpec> getWorkSpecs() { return workSpecs; }
    public void setWorkSpecs(List<WorkSpec> workSpecs) { this.workSpecs = workSpecs; }
}
