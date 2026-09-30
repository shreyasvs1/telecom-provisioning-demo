package com.example.provisioning.model;

/**
 * Stands in for what a real "location enrichment" external API would return:
 * is this address serviceable, and what kind of network plant serves it.
 * In the real system this is one of the external API calls in step 3.
 */
public class LocationInfo {

    private boolean serviceable;
    private String networkType;   // FIBER, COPPER, COAX
    private String centralOfficeId;
    private boolean existingDrop; // is there already a physical drop/line to the premises?

    public boolean isServiceable() { return serviceable; }
    public void setServiceable(boolean serviceable) { this.serviceable = serviceable; }

    public String getNetworkType() { return networkType; }
    public void setNetworkType(String networkType) { this.networkType = networkType; }

    public String getCentralOfficeId() { return centralOfficeId; }
    public void setCentralOfficeId(String centralOfficeId) { this.centralOfficeId = centralOfficeId; }

    public boolean isExistingDrop() { return existingDrop; }
    public void setExistingDrop(boolean existingDrop) { this.existingDrop = existingDrop; }
}
