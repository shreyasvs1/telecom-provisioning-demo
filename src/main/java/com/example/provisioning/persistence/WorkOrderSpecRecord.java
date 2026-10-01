package com.example.provisioning.persistence;

import jakarta.persistence.*;

/**
 * Maps to the WORK_ORDER_SPEC table: the work specs grouped under one work
 * order, in the order step 5 listed them (LINE_NO).
 */
@Entity
@Table(name = "work_order_spec")
public class WorkOrderSpecRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrderRecord workOrder;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

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

    public WorkOrderRecord getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrderRecord workOrder) { this.workOrder = workOrder; }

    public int getLineNo() { return lineNo; }
    public void setLineNo(int lineNo) { this.lineNo = lineNo; }

    public String getWorkSpecCode() { return workSpecCode; }
    public void setWorkSpecCode(String workSpecCode) { this.workSpecCode = workSpecCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getRequiredSkill() { return requiredSkill; }
    public void setRequiredSkill(String requiredSkill) { this.requiredSkill = requiredSkill; }
}
