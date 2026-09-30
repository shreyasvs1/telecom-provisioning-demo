package com.example.provisioning.model;

/**
 * Stands in for a "service catalog / eligibility" external API: given a
 * requested service code, what tier and bundling rules apply.
 */
public class ServiceInfo {

    private String serviceCode;   // INTERNET, PHONE, TV
    private String serviceTier;   // BASIC, PREMIUM, GIGABIT
    private boolean bundled;

    public ServiceInfo() {}

    public ServiceInfo(String serviceCode, String serviceTier, boolean bundled) {
        this.serviceCode = serviceCode;
        this.serviceTier = serviceTier;
        this.bundled = bundled;
    }

    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }

    public String getServiceTier() { return serviceTier; }
    public void setServiceTier(String serviceTier) { this.serviceTier = serviceTier; }

    public boolean isBundled() { return bundled; }
    public void setBundled(boolean bundled) { this.bundled = bundled; }
}
