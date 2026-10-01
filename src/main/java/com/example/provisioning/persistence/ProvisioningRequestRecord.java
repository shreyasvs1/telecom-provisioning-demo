package com.example.provisioning.persistence;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Maps to the PROVISIONING_REQUEST table: one row per incoming request,
 * valid or not. The full JSON is kept in PAYLOAD so a request can be
 * replayed or audited later; the key fields are copied into their own
 * columns so they can be searched.
 */
@Entity
@Table(name = "provisioning_request")
public class ProvisioningRequestRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "order_id", length = 100)
    private String orderId;

    @Column(name = "customer_id", length = 100)
    private String customerId;

    @Column(name = "request_type", length = 100)
    private String requestType;

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RequestStatus status;

    @Lob
    @Column(name = "status_detail")
    private String statusDetail;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public String getStatusDetail() { return statusDetail; }
    public void setStatusDetail(String statusDetail) { this.statusDetail = statusDetail; }

    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
