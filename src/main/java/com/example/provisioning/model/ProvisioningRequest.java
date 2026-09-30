package com.example.provisioning.model;

import java.util.List;

/**
 * This is the "incoming JSON" from step 1 of the pipeline.
 * Mirrors, at a small scale, the kind of payload a real telecom
 * order-intake system would send: who the customer is, where the
 * work is happening, and what services they're ordering.
 */
public class ProvisioningRequest {

    private String orderId;
    private String customerId;
    private String requestType; // NEW_INSTALL, UPGRADE, TRANSFER
    private Address serviceAddress;
    private List<String> requestedServices; // e.g. ["INTERNET", "PHONE"]

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public Address getServiceAddress() { return serviceAddress; }
    public void setServiceAddress(Address serviceAddress) { this.serviceAddress = serviceAddress; }

    public List<String> getRequestedServices() { return requestedServices; }
    public void setRequestedServices(List<String> requestedServices) { this.requestedServices = requestedServices; }
}
