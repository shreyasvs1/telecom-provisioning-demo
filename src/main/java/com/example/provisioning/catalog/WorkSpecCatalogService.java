package com.example.provisioning.catalog;

import com.example.provisioning.model.LocationInfo;
import com.example.provisioning.model.ServiceInfo;
import com.example.provisioning.model.WorkSpec;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * STEP 4 of the pipeline: derives the physical work specs needed, by looking
 * up the Oracle-backed catalog table for each requested service + the
 * network type discovered during enrichment (step 3).
 *
 * This mirrors the "in-house work specification catalog" call in the real
 * system -- there it's likely a separate HTTP service backed by its own
 * database; here it's a direct JPA repository against the same schema, to
 * keep the demo self-contained.
 */
@Service
public class WorkSpecCatalogService {

    private final WorkSpecCatalogRepository repository;

    public WorkSpecCatalogService(WorkSpecCatalogRepository repository) {
        this.repository = repository;
    }

    public List<WorkSpec> deriveWorkSpecs(List<ServiceInfo> services, LocationInfo location) {
        List<WorkSpec> workSpecs = new ArrayList<>();

        for (ServiceInfo service : services) {
            List<WorkSpecCatalogEntry> entries =
                    repository.findByServiceCodeAndNetworkType(service.getServiceCode(), location.getNetworkType());

            for (WorkSpecCatalogEntry entry : entries) {
                // Skip drop-install work if a usable drop already exists at the premises
                if ("INSTALL_DROP".equals(entry.getWorkSpecCode()) && location.isExistingDrop()) {
                    continue;
                }
                workSpecs.add(new WorkSpec(
                        entry.getWorkSpecCode(),
                        entry.getDescription(),
                        entry.getDurationMinutes(),
                        entry.getRequiredSkill()
                ));
            }
        }

        return workSpecs;
    }
}
