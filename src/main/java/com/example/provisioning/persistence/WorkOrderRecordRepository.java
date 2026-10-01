package com.example.provisioning.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderRecordRepository extends JpaRepository<WorkOrderRecord, Long> {

    @EntityGraph(attributePaths = "specs")
    List<WorkOrderRecord> findByRequest_IdOrderBySequenceNoAsc(Long requestId);
}
