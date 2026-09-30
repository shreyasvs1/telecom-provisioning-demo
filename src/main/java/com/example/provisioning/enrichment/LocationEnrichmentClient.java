package com.example.provisioning.enrichment;

import com.example.provisioning.model.Address;
import com.example.provisioning.model.LocationInfo;
import org.springframework.stereotype.Component;

/**
 * STEP 3 (part 1): stands in for a call to an external location/serviceability
 * API. In the real system this would be a REST/SOAP client hitting a vendor
 * or internal network-inventory service. Here it's deterministic and fake,
 * based on the zip code, so the demo is reproducible without real network
 * calls.
 */
@Component
public class LocationEnrichmentClient {

    public LocationInfo enrich(Address address) {
        LocationInfo info = new LocationInfo();

        if (address == null || address.getZip() == null) {
            info.setServiceable(false);
            return info;
        }

        // Fake rule: zip codes ending in an even digit are "fiber" territory,
        // odd digits are "copper", and zip 99999 is deliberately unserviceable.
        String zip = address.getZip();
        if ("99999".equals(zip)) {
            info.setServiceable(false);
            return info;
        }

        char lastDigit = zip.charAt(zip.length() - 1);
        boolean isEven = Character.isDigit(lastDigit) && (Character.getNumericValue(lastDigit) % 2 == 0);

        info.setServiceable(true);
        info.setNetworkType(isEven ? "FIBER" : "COPPER");
        info.setCentralOfficeId("CO-" + zip.substring(0, Math.min(3, zip.length())));
        // Pretend existing drops are present for "UPGRADE"-style zips (even zips) only, for variety
        info.setExistingDrop(isEven);

        return info;
    }
}
