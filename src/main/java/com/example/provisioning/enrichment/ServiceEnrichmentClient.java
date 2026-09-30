package com.example.provisioning.enrichment;

import com.example.provisioning.model.ServiceInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * STEP 3 (part 2): stands in for a call to an external service-eligibility /
 * product catalog API that tells us the tier and bundling status of each
 * requested service code.
 */
@Component
public class ServiceEnrichmentClient {

    public List<ServiceInfo> enrich(List<String> requestedServiceCodes) {
        List<ServiceInfo> results = new ArrayList<>();

        boolean bundle = requestedServiceCodes.size() > 1; // fake rule: 2+ services = bundled discount

        for (String code : requestedServiceCodes) {
            String tier = switch (code) {
                case "INTERNET" -> "GIGABIT";
                case "TV" -> "PREMIUM";
                default -> "BASIC";
            };
            results.add(new ServiceInfo(code, tier, bundle));
        }

        return results;
    }
}
