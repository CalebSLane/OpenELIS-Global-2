package org.openelisglobal.fhir.provider;

import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.IResourceProvider;
import java.util.UUID;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Practitioner;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PractitionerProvider implements IResourceProvider {

    @Autowired
    private FhirTransformService fhirTransformService;
    @Autowired
    private FhirPersistanceService fhirPersistenceService;
    @Autowired
    private ProviderService providerService;

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Practitioner.class;
    }

    @Create()
    public MethodOutcome create(@ResourceParam Practitioner practitioner) throws FhirLocalPersistingException {
        MethodOutcome method = new MethodOutcome();
        method.setCreated(true);
        OperationOutcome opOutcome = new OperationOutcome();
        method.setOperationOutcome(opOutcome);

        Provider provider = fhirTransformService.transformToProvider(practitioner);
        providerService.insert(provider);
        fhirPersistenceService.updateFhirResourceInFhirStore(practitioner);
        return method;
    }

    @Update()
    public MethodOutcome update(@IdParam IdType theId, @ResourceParam Practitioner practitioner)
            throws FhirLocalPersistingException {
        String resourceId = theId.getIdPart();

        Provider provider = providerService.getProviderByFhirId(UUID.fromString(resourceId));
        Provider updatedProvider = fhirTransformService.transformToProvider(practitioner);
        if (provider != null) {
            updatedProvider.setId(provider.getId());
        }
        providerService.save(updatedProvider);
        fhirPersistenceService.updateFhirResourceInFhirStore(practitioner);

        return new MethodOutcome();
    }

}
