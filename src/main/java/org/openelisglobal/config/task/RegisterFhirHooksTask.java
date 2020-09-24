package org.openelisglobal.config.task;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Narrative.NarrativeStatus;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelComponent;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelType;
import org.hl7.fhir.r4.model.Subscription.SubscriptionStatus;
import org.hl7.fhir.utilities.xhtml.NodeType;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.itech.fhir.dataexport.core.model.DataExportTask;
import org.itech.fhir.dataexport.core.service.DataExportTaskService;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationListener;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.externalconnections.service.ExternalConnectionService;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.ProgrammedConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

@Component
public class RegisterFhirHooksTask implements ConfigurationListener {

    private enum SubscriptionType {
        CONSOLIDATED_SERVER("consolidatedServerSubscription"), PATIENT_SERVER("patientServerSuscription");

        private String prefix;

        SubscriptionType(String prefix) {
            this.prefix = prefix;
        }

        String getPrefix() {
            return prefix;
        }
    }

    @Autowired
    FhirContext fhirContext;
    @Autowired
    private FhirConfig fhirConfig;

    @Autowired
    private DataExportTaskService dataExportTaskService;
    @Autowired
    private ExternalConnectionService externalConnectionService;

    @PostConstruct
    public void startTask() {
        Optional<String> fhirSubscriber = fhirConfig.getConsolidatedServerPath();
        String[] fhirSubscriptionResources = fhirConfig.getConsolidatedServerResources();
        if (fhirSubscriber.isPresent() && !GenericValidator.isBlankOrNull(fhirSubscriber.get())) {
            createOrRenewExportTask(SubscriptionType.CONSOLIDATED_SERVER, fhirSubscriber.get(),
                    Arrays.asList(fhirSubscriptionResources));
            subscribeRemoteServerToLocal(SubscriptionType.CONSOLIDATED_SERVER, fhirSubscriber.get(),
                    Arrays.asList(fhirSubscriptionResources));
        }

        Optional<ExternalConnection> externalConnection = externalConnectionService.getMatch("programmedConnection",
                ProgrammedConnection.FHIR_PATIENT_SERVER.name());
        if (externalConnection.isPresent() && externalConnection.get().getActive()) {
            createOrRenewExportTask(SubscriptionType.PATIENT_SERVER, externalConnection.get().getUri().toString(),
                    Arrays.asList(ResourceType.Patient.toString()));
            subscribeRemoteServerToLocal(SubscriptionType.PATIENT_SERVER, fhirSubscriber.get(),
                    Arrays.asList(ResourceType.Patient.toString()));
        }
    }


    private void createOrRenewExportTask(SubscriptionType subType, String fhirSubscriberPath,
            List<String> fhirSubscriptionResources) {
        DataExportTask dataExportTask = dataExportTaskService.getDAO().findByEndpoint(fhirSubscriberPath)
                .orElse(new DataExportTask());
        Map<String, String> headers = new HashMap<>();
        headers.put("Server-Name", ConfigurationProperties.getInstance().getPropertyValue(Property.SiteName));
        headers.put("Server-Code", ConfigurationProperties.getInstance().getPropertyValue(Property.SiteCode));

        dataExportTask.setFhirResources(
                fhirSubscriptionResources.stream().map(ResourceType::fromCode)
                .collect(Collectors.toList()));
        dataExportTask.setHeaders(headers);
        dataExportTask.setMaxDataExportInterval(60 * 24); // minutes
        dataExportTask.setDataRequestAttemptTimeout(60 * 10); // seconds // currently unused
        dataExportTask.setEndpoint(fhirSubscriberPath);
        dataExportTask.setActive(true);
        dataExportTaskService.getDAO().save(dataExportTask);
    }

    private void subscribeRemoteServerToLocal(SubscriptionType subType, String fhirSubscriberPath,
            List<String> fhirSubscriptionResources) {
        IGenericClient fhirClient = fhirContext.newRestfulGenericClient(fhirConfig.getLocalFhirStorePath());

        removeOldSubscription(subType);

        Bundle subscriptionBundle = new Bundle();
        subscriptionBundle.setType(BundleType.TRANSACTION);

        for (String fhirSubscriptionResource : fhirSubscriptionResources) {
            ResourceType resourceType = ResourceType.fromCode(fhirSubscriptionResource);
            Subscription subscription = createSubscriptionForResource(fhirSubscriberPath, resourceType);
            BundleEntryComponent bundleEntry = new BundleEntryComponent();
            bundleEntry.setResource(subscription);
            bundleEntry.setRequest(new BundleEntryRequestComponent().setMethod(HTTPVerb.PUT).setUrl(
                    ResourceType.Subscription.name() + "/"
                            + createSubscriptionIdForResourceType(subType, resourceType)));

            subscriptionBundle.addEntry(bundleEntry);

        }
        try {
            Bundle returnedBundle = fhirClient.transaction().withBundle(subscriptionBundle).encodedJson().execute();
            LogEvent.logDebug(this.getClass().getName(), "startTask", "subscription bundle returned:\n"
                    + fhirContext.newJsonParser().encodeResourceToString(returnedBundle));
        } catch (UnprocessableEntityException | DataFormatException e) {
            LogEvent.logError(
                    "error while communicating subscription bundle to " + fhirConfig.getLocalFhirStorePath() + " for "
                            + fhirSubscriberPath,
                    e);
        }
    }

    private void removeOldSubscription(SubscriptionType subType) {
        IGenericClient fhirClient = fhirContext.newRestfulGenericClient(fhirConfig.getLocalFhirStorePath());

        Bundle deleteTransactionBundle = new Bundle();
        deleteTransactionBundle.setType(BundleType.TRANSACTION);
        for (ResourceType resourceType : ResourceType.values()) {
            Bundle responseBundle = (Bundle) fhirClient.search().forResource(Subscription.class)
                    .where(Subscription.RES_ID.exactly()
                            .code(createSubscriptionIdForResourceType(subType, resourceType)))
                    .execute();
            if (responseBundle.hasEntry()) {
                BundleEntryComponent bundleEntry = new BundleEntryComponent();
                bundleEntry.setRequest(new BundleEntryRequestComponent().setMethod(HTTPVerb.DELETE).setUrl(
                        ResourceType.Subscription.name() + "/"
                                + createSubscriptionIdForResourceType(subType, resourceType)));

                deleteTransactionBundle.addEntry(bundleEntry);
            }
        }
        Bundle returnedBundle = fhirClient.transaction().withBundle(deleteTransactionBundle).encodedJson().execute();
        LogEvent.logDebug(this.getClass().getName(), "removeOldSubscription",
                "delete old bundle returned:\n" + fhirContext.newJsonParser().encodeResourceToString(returnedBundle));

    }

    private String createSubscriptionIdForResourceType(SubscriptionType subType, ResourceType resourceType) {
        return subType.getPrefix() + resourceType.toString();
    }

    private Subscription createSubscriptionForResource(String fhirSubscriber, ResourceType resourceType) {
        Subscription subscription = new Subscription();
        subscription.setText(new Narrative().setStatus(NarrativeStatus.GENERATED).setDiv(new XhtmlNode(NodeType.Text)));
        subscription.setStatus(SubscriptionStatus.REQUESTED);
        subscription
                .setReason("bulk subscription to detect any Creates or Updates to resources of type " + resourceType);

        SubscriptionChannelComponent channel = new SubscriptionChannelComponent();
        channel.setType(SubscriptionChannelType.RESTHOOK).setEndpoint(fhirSubscriber);
        channel.addHeader("Server-Name: " + ConfigurationProperties.getInstance().getPropertyValue(Property.SiteName));
        channel.addHeader("Server-Code: " + ConfigurationProperties.getInstance().getPropertyValue(Property.SiteCode));
        channel.setPayload("application/fhir+json");
        subscription.setChannel(channel);

        subscription.setCriteria(createCriteriaString(resourceType));
        return subscription;
    }

    private String createCriteriaString(ResourceType resourceType) {
        return resourceType.name() + "?";
    }

    @Override
    public void refreshConfiguration() {
        startTask();
    }

}
