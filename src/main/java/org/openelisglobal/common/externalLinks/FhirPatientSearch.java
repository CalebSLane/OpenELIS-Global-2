package org.openelisglobal.common.externalLinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import org.apache.commons.validator.GenericValidator;
import org.apache.http.HttpStatus;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.ResourceType;
import org.openelisglobal.common.provider.query.ExtendedPatientSearchResults;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.externalconnections.valueholder.BasicAuthenticationData;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnectionAuthenticationData;
import org.openelisglobal.internationalization.MessageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;

@Service("FhirPatientSearch")
@Scope("prototype")
public class FhirPatientSearch implements IExternalPatientSearch {


    @Autowired
    private FhirContext fhirContext;
    @Autowired
    private FhirConfig fhirConfig;

    private boolean finished = false;

    private String firstName;
    private String lastName;
    private String STNumber;
    private String subjectNumber;
    private String nationalId;
    private String guid;
    private String gender;
    private String dateOfBirth;

    private String connectionName;
    private String connectionString;
    private String connectionUsername;
    private String connectionPassword;
    private int timeout = 0;

    protected String resultXML;
    protected List<ExtendedPatientSearchResults> searchResults = new ArrayList<>();
    protected List<String> errors;
    protected int returnStatus = HttpStatus.SC_ACCEPTED;

    @Override
    synchronized public void setConnectionCredentials(String connectionString, String connectionUsername,
            String password,
            int timeout_Mil) {
        if (finished) {
            throw new IllegalStateException("ServiceCredentials set after ExternalPatientSearch thread was started");
        }

        this.connectionString = connectionString;
        connectionName = "fhir";
        this.connectionUsername = connectionUsername;
        connectionPassword = password;
        timeout = timeout_Mil;
    }

    @Override
    synchronized public void setConnectionCredentials(ExternalConnection externalConnection,
            Optional<ExternalConnectionAuthenticationData> authData, int timeout_Mil) {
        if (finished) {
            throw new IllegalStateException("ServiceCredentials set after ExternalPatientSearch thread was started");
        }

        this.connectionString = externalConnection.getUri().toString();
        connectionName = externalConnection.getNameLocalization().getLocalizedValue();
        if (authData.isPresent() && authData.get() instanceof BasicAuthenticationData) {
            connectionUsername = ((BasicAuthenticationData) authData.get()).getUsername();
            connectionPassword = ((BasicAuthenticationData) authData.get()).getPassword();
        }
        timeout = timeout_Mil;
    }

    @Override
    synchronized public void setSearchCriteria(String lastName, String firstName, String STNumber, String subjectNumber,
            String nationalID, String guid, String dateOfBirth, String gender) throws IllegalStateException {

        if (finished) {
            throw new IllegalStateException("Search criteria set after ExternalPatientSearch thread was started");
        }

        this.lastName = lastName;
        this.firstName = firstName;
        this.STNumber = STNumber;
        this.subjectNumber = subjectNumber;
        this.nationalId = nationalID;
        this.guid = guid;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
    }

    @Override
    synchronized public List<ExtendedPatientSearchResults> getSearchResults() {

        if (!finished) {
            throw new IllegalStateException("Results requested before ExternalPatientSearch thread was finished");
        }

        if (searchResults == null) {
            searchResults = new ArrayList<>();
        }

        return searchResults;
    }

    public int getSearchResultStatus() {
        if (!finished) {
            throw new IllegalStateException("Result status requested ExternalPatientSearch before search was finished");
        }

        return returnStatus;
    }

    @Override
    @Async
    public Future<Integer> runExternalSearch() {
        try {
            synchronized (this) {
                if (noSearchTerms()) {
                    throw new IllegalStateException("Search requested before without any search terms.");
                }

                if (connectionCredentialsIncomplete()) {
                    throw new IllegalStateException("Search requested before connection credentials set.");
                }
                errors = new ArrayList<>();

                doSearch();
            }
        } finally {
            finished = true;
        }
        return new AsyncResult<>(getSearchResultStatus());
    }

    private boolean connectionCredentialsIncomplete() {
        return GenericValidator.isBlankOrNull(connectionString) || GenericValidator.isBlankOrNull(connectionUsername)
                || GenericValidator.isBlankOrNull(connectionPassword);
    }

    private boolean noSearchTerms() {
        return GenericValidator.isBlankOrNull(firstName) && GenericValidator.isBlankOrNull(lastName)
                && GenericValidator.isBlankOrNull(nationalId) && GenericValidator.isBlankOrNull(STNumber);
    }

    protected void doSearch() {

        IGenericClient remoteFhirClient = fhirContext.newRestfulGenericClient(connectionString);

        Bundle resultBundle;
        if (!GenericValidator.isBlankOrNull(guid)) {
            resultBundle = remoteFhirClient.search().forResource(Patient.class)//
                    .where(Patient.RES_ID.exactly().code(guid))//
                    .returnBundle(Bundle.class).execute();
            convertBundleToSearchResult(resultBundle);
        } else {
            // these are and statements
            IQuery<IBaseBundle> query = remoteFhirClient.search().forResource(Patient.class);
            if (!GenericValidator.isBlankOrNull(firstName)) {
                query.where(Patient.GIVEN.matches().value(firstName));
            }
            if (!GenericValidator.isBlankOrNull(lastName)) {
                query.where(Patient.FAMILY.matches().value(lastName));
            }
            if (!GenericValidator.isBlankOrNull(dateOfBirth)) {
                query.where(Patient.BIRTHDATE.exactly().day(dateOfBirth));
            }
            if (!GenericValidator.isBlankOrNull(gender)) {
                query.where(Patient.GENDER.exactly().code("M".equals(this.gender) ? AdministrativeGender.MALE.toCode()
                        : AdministrativeGender.FEMALE.toCode()));
            }
            if (!GenericValidator.isBlankOrNull(subjectNumber)) {
                query.where(
                        Patient.IDENTIFIER.exactly().systemAndCode(fhirConfig.getSubjectNumberSystem(), subjectNumber));
            }
            if (!GenericValidator.isBlankOrNull(STNumber)) {
                query.where(Patient.IDENTIFIER.exactly().systemAndCode(fhirConfig.getSTSystem(), STNumber));
            }
            if (!GenericValidator.isBlankOrNull(nationalId)) {
                query.where(Patient.IDENTIFIER.exactly().systemAndCode(fhirConfig.getNationalIdSystem(), nationalId));
            }

            resultBundle = query.returnBundle(Bundle.class).execute();
            convertBundleToSearchResult(resultBundle);
            while (resultBundle.getLink(Bundle.LINK_NEXT) != null) {
                // load next page
                resultBundle = remoteFhirClient.loadPage().next(resultBundle).execute();
                convertBundleToSearchResult(resultBundle);
            }
        }

        setPossibleErrors();
    }

    private void convertBundleToSearchResult(Bundle resultBundle) {
        for (BundleEntryComponent entry : resultBundle.getEntry()) {
            if (entry.hasResource() && ResourceType.Patient.equals(entry.getResource().getResourceType())) {
                Patient fhirPatient = (Patient) entry.getResource();
                ExtendedPatientSearchResults patient = new ExtendedPatientSearchResults();
                patient.setDataSourceName(GenericValidator.isBlankOrNull(connectionName)
                        ? MessageUtil.getMessage("patient.imported.source")
                        : connectionName);

                patient.setBirthdate(DateUtil.convertDateToString(fhirPatient.getBirthDate()));
                patient.setLastName(fhirPatient.getNameFirstRep().getFamily());
                patient.setFirstName(fhirPatient.getNameFirstRep().getGivenAsSingleString());
                patient.setGender(AdministrativeGender.MALE.equals(fhirPatient.getGender()) ? "M" : "F");
                for (Identifier identifier : fhirPatient.getIdentifier()) {
                    if (fhirConfig.getSubjectNumberSystem().equals(identifier.getSystem())) {
                        patient.setSubjectNumber(identifier.getValue());
                    } else if (fhirConfig.getSTSystem().equals(identifier.getSystem())) {
                        patient.setStNumber(identifier.getValue());
                    } else if (fhirConfig.getNationalIdSystem().equals(identifier.getSystem())) {
                        patient.setNationalId(identifier.getValue());
                    }
                }
                patient.setGUID(fhirPatient.getIdElement().getIdPart());
                this.searchResults.add(patient);
            }
        }

    }

    protected void setResults(String resultsAsXml) {
        resultXML = resultsAsXml;
    }

    private void setPossibleErrors() {
        switch (returnStatus) {
        case HttpStatus.SC_UNAUTHORIZED: {
            errors.add("Access denied to patient information service.");
            break;
        }
        case HttpStatus.SC_INTERNAL_SERVER_ERROR: {
            errors.add("Internal error on patient information service.");
            break;
        }
        case HttpStatus.SC_OK: {
            break; // NO-OP
        }
        default: {
            errors.add("Unknown error trying to connect to patient information service. Return status was "
                    + returnStatus);
        }
        }

    }

    @Override
    public String getConnectionString() {
        return connectionString;
    }

}
