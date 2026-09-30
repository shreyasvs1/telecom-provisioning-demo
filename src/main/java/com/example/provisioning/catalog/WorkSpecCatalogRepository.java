package com.example.provisioning.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkSpecCatalogRepository extends JpaRepository<WorkSpecCatalogEntry, Long> {

    List<WorkSpecCatalogEntry> findByServiceCodeAndNetworkType(String serviceCode, String networkType);
}
