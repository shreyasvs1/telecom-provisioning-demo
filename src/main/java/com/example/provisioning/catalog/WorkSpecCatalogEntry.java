package com.example.provisioning.catalog;

import jakarta.persistence.*;

/**
 * Maps to the WORK_SPEC_CATALOG table. This is the Oracle-backed equivalent
 * of the "in-house work specification catalog" mentioned in step 4 -- in a
 * real system this table (or an equivalent service) is the source of truth
 * for which physical tasks are needed for a given service + network
 * combination.
 */
@Entity
@Table(name = "work_spec_catalog")
public class WorkSpecCatalogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "service_code", nullable = false, length = 50)
    private String serviceCode;

    @Column(name = "network_type", nullable = false, length = 20)
    private String networkType;

    @Column(name = "work_spec_code", nullable = false, length = 50)
    private String workSpecCode;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "duration_minutes")
    private int durationMinutes;

    @Column(name = "required_skill", length = 50)
    private String requiredSkill;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }

    public String getNetworkType() { return networkType; }
    public void setNetworkType(String networkType) { this.networkType = networkType; }

    public String getWorkSpecCode() { return workSpecCode; }
    public void setWorkSpecCode(String workSpecCode) { this.workSpecCode = workSpecCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getRequiredSkill() { return requiredSkill; }
    public void setRequiredSkill(String requiredSkill) { this.requiredSkill = requiredSkill; }
}
