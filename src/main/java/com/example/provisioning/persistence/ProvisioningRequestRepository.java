package com.example.provisioning.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProvisioningRequestRepository extends JpaRepository<ProvisioningRequestRecord, Long> {

    List<ProvisioningRequestRecord> findByOrderIdOrderByIdAsc(String orderId);

    boolean existsByOrderId(String orderId);
}
