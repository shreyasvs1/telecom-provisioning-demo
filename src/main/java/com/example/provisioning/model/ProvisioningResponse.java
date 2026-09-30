package com.example.provisioning.model;

import java.util.List;

public class ProvisioningResponse {

    private String orderId;
    private List<WorkOrder> workOrders;
    private int totalDispatchableWorkOrders;

    public ProvisioningResponse() {}

    public ProvisioningResponse(String orderId, List<WorkOrder> workOrders) {
        this.orderId = orderId;
        this.workOrders = workOrders;
        this.totalDispatchableWorkOrders = workOrders.size();
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public List<WorkOrder> getWorkOrders() { return workOrders; }
    public void setWorkOrders(List<WorkOrder> workOrders) { this.workOrders = workOrders; }

    public int getTotalDispatchableWorkOrders() { return totalDispatchableWorkOrders; }
    public void setTotalDispatchableWorkOrders(int totalDispatchableWorkOrders) { this.totalDispatchableWorkOrders = totalDispatchableWorkOrders; }
}
