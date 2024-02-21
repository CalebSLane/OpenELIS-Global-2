package org.openelisglobal.location.controller;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Location;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.uhn.fhir.rest.gclient.TokenClientParam;

@RestController
public class LocationController {

    @Autowired
    private FhirUtil fhirUtil;

    @GetMapping("locations")
    private List<Location> getLocations() {
        Bundle locationBundle = fhirUtil.getLocalFhirClient()//
                .search()//
                .forResource(Location.class)//
                .returnBundle(Bundle.class)//
                .where(Location.STATUS.exactly().codes(Location.LocationStatus.INACTIVE.toCode(),
                        Location.LocationStatus.ACTIVE.toCode()))//
                .execute();
        int numResources = locationBundle.getEntry().size();
        List<Location> locations = new ArrayList<>(numResources);
        for (BundleEntryComponent entry : locationBundle.getEntry()) {
            if (entry.getResource().getResourceType() == ResourceType.Location) {
                locations.add((Location) entry.getResource());
            }
        }
        return locations;
    }

}
