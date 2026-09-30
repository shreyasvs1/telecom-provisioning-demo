package com.example.provisioning.model;

/**
 * A single unit of physical work, e.g. "INSTALL_MODEM" or "CONNECT_JACK".
 * This is what step 4 (work spec catalog) derives from the enriched request.
 */
public class WorkSpec {

    private String workSpecCode;
    private String description;
    private int estimatedDurationMinutes;
    private String requiredSkill; // e.g. INSIDE_WIRING, OUTSIDE_PLANT, ELECTRONICS

    public WorkSpec() {}

    public WorkSpec(String workSpecCode, String description, int estimatedDurationMinutes, String requiredSkill) {
        this.workSpecCode = workSpecCode;
        this.description = description;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.requiredSkill = requiredSkill;
    }

    public String getWorkSpecCode() { return workSpecCode; }
    public void setWorkSpecCode(String workSpecCode) { this.workSpecCode = workSpecCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public void setEstimatedDurationMinutes(int estimatedDurationMinutes) { this.estimatedDurationMinutes = estimatedDurationMinutes; }

    public String getRequiredSkill() { return requiredSkill; }
    public void setRequiredSkill(String requiredSkill) { this.requiredSkill = requiredSkill; }
}
