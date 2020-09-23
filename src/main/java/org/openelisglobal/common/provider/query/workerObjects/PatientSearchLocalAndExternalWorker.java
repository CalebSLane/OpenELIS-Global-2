/**
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
 *
 * Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.common.provider.query.workerObjects;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.address.service.AddressPartService;
import org.openelisglobal.address.valueholder.AddressPart;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.externalLinks.IExternalPatientSearch;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.query.ExtendedPatientSearchResults;
import org.openelisglobal.common.provider.query.PatientSearchResults;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.SystemConfiguration;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.externalconnections.service.BasicAuthenticationDataService;
import org.openelisglobal.externalconnections.service.ExternalConnectionService;
import org.openelisglobal.externalconnections.valueholder.BasicAuthenticationData;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection.ProgrammedConnection;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patientidentity.service.PatientIdentityService;
import org.openelisglobal.patientidentity.valueholder.PatientIdentity;
import org.openelisglobal.patientidentitytype.util.PatientIdentityTypeMap;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.search.service.SearchResultsService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.data.util.Pair;

public class PatientSearchLocalAndExternalWorker extends PatientSearchWorker {

    protected PatientService patientService = SpringContext.getBean(PatientService.class);
    protected PatientIdentityService patientIdentityService = SpringContext.getBean(PatientIdentityService.class);
    protected PersonService personService = SpringContext.getBean(PersonService.class);
    protected SearchResultsService searchResultsService = SpringContext.getBean(SearchResultsService.class);
    private AddressPartService addressPartService = SpringContext.getBean(AddressPartService.class);
    private FhirTransformService fhirTransformService = SpringContext.getBean(FhirTransformService.class);
    private ExternalConnectionService externalConnectionsService = SpringContext
            .getBean(ExternalConnectionService.class);
    private BasicAuthenticationDataService basicAuthenticationDataService = SpringContext
            .getBean(BasicAuthenticationDataService.class);

    private final String sysUserId;

    public PatientSearchLocalAndExternalWorker(String sysUserId) {
        this.sysUserId = sysUserId;
    }

    /**
     * @see org.openelisglobal.common.provider.query.workerObjects.PatientSearchWorker#createSearchResultXML(java.lang.String,
     *      java.lang.String, java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String, java.lang.StringBuilder)
     */
    @Override
    public String createSearchResultXML(String lastName, String firstName, String STNumber, String subjectNumber,
            String nationalID, String patientID, String guid, String dateOfBirth, String gender, StringBuilder xml) {

        String success = IActionConstants.VALID;

        if (GenericValidator.isBlankOrNull(lastName) && GenericValidator.isBlankOrNull(firstName)
                && GenericValidator.isBlankOrNull(STNumber) && GenericValidator.isBlankOrNull(subjectNumber)
                && GenericValidator.isBlankOrNull(nationalID) && GenericValidator.isBlankOrNull(patientID)
                && GenericValidator.isBlankOrNull(guid)) {

            xml.append("No search terms were entered");
            return IActionConstants.INVALID;
        }

        List<PatientSearchResults> allResults = new ArrayList<>();

        List<PatientSearchResults> localResults = new ArrayList<>();
        localResults = searchResultsService.getSearchResults(lastName, firstName, STNumber, subjectNumber, nationalID,
                nationalID, patientID, guid, dateOfBirth, gender);
        allResults.addAll(localResults);
        for (PatientSearchResults results : localResults) {
            results.setDataSourceName(MessageUtil.getMessage("patient.local.source"));
        }

        List<IExternalPatientSearch> externalSearches = getActiveExternalSearches(lastName, firstName, STNumber,
                subjectNumber, nationalID, guid, dateOfBirth, gender);
        List<Pair<Future<Integer>, IExternalPatientSearch>> searchResultStatuses = new ArrayList<>();

        for (IExternalPatientSearch externalSearch : externalSearches) {
            externalSearch.setSearchCriteria(lastName, firstName, STNumber, subjectNumber, nationalID, guid,
                    dateOfBirth, gender);
            searchResultStatuses.add(Pair.of(externalSearch.runExternalSearch(), externalSearch));
        }
        for (Pair<Future<Integer>, IExternalPatientSearch> searchResultStatusPair : searchResultStatuses) {
            try {
                Integer externalSearchResult = searchResultStatusPair.getFirst()
                        .get(SystemConfiguration.getInstance().getSearchTimeLimit() + 500, TimeUnit.MILLISECONDS);

                if (externalSearchResult == HttpStatus.SC_OK || externalSearchResult == HttpStatus.SC_ACCEPTED) {
                    List<ExtendedPatientSearchResults> externalResults = searchResultStatusPair.getSecond()
                            .getSearchResults();
                    List<ExtendedPatientSearchResults> newPatientsFromExternalSearch = new ArrayList<>();
                    findNewPatients(localResults, externalResults, newPatientsFromExternalSearch);
                    insertNewPatients(newPatientsFromExternalSearch);
                    allResults.addAll(newPatientsFromExternalSearch);
                } else {
                    LogEvent.logWarn(this.getClass().getName(), "createSearchResultXML",
                            "could not get external search results from "
                                    + searchResultStatusPair.getSecond().getConnectionString()
                                    + " - failed response");
                }
            } catch (InterruptedException | ExecutionException | TimeoutException | IllegalStateException e) {
                LogEvent.logError(e.getMessage(), e);
            }

        }

        sortPatients(allResults);

        if (allResults != null && allResults.size() > 0) {
            for (PatientSearchResults singleResult : allResults) {
                appendSearchResultRow(singleResult, xml);
            }
        } else {
            success = IActionConstants.INVALID;

            xml.append("No results were found for search.  Check spelling or remove some of the fields");
        }

        return success;
    }

    private List<IExternalPatientSearch> getActiveExternalSearches(String lastName, String firstName, String STNumber,
            String subjectNumber, String nationalID, String guid, String dateOfBirth, String gender) {
        // just to make the name shorter
        ConfigurationProperties config = ConfigurationProperties.getInstance();

        List<IExternalPatientSearch> externalSearches = new ArrayList<>();

        Optional<ExternalConnection> infoHighwayConnection = externalConnectionsService.getMatch("programmedConnection",
                ProgrammedConnection.INFO_HIGHWAY.name());
        if (infoHighwayConnection.isPresent()) {
            IExternalPatientSearch externalSearch = (IExternalPatientSearch) SpringContext.getBean("InfoHighwaySearch");
            Optional<BasicAuthenticationData> basicAuthData = basicAuthenticationDataService
                    .getByExternalConnection(infoHighwayConnection.get().getId());
            // optional is recreated to provide type safety
            externalSearch.setConnectionCredentials(infoHighwayConnection.get(), Optional.of(basicAuthData.get()),
                    (int) SystemConfiguration.getInstance().getSearchTimeLimit());
            externalSearches.add(externalSearch);
        }

        Optional<ExternalConnection> fhirPatientConnection = externalConnectionsService.getMatch("programmedConnection",
                ProgrammedConnection.FHIR_PATIENT_SERVER.name());
        if (fhirPatientConnection.isPresent()) {
            IExternalPatientSearch externalSearch = (IExternalPatientSearch) SpringContext.getBean("FhirPatientSearch");
            Optional<BasicAuthenticationData> basicAuthData = basicAuthenticationDataService
                    .getByExternalConnection(fhirPatientConnection.get().getId());
            // optional is recreated to provide type safety
            externalSearch.setConnectionCredentials(fhirPatientConnection.get(), Optional.of(basicAuthData.get()),
                    (int) SystemConfiguration.getInstance().getSearchTimeLimit());
            externalSearches.add(externalSearch);
        }

        if (config.getPropertyValue(Property.PatientSearchEnabled).equals("true")) {
            IExternalPatientSearch externalSearch = SpringContext.getBean(IExternalPatientSearch.class);
            externalSearch.setConnectionCredentials(config.getPropertyValue(Property.PatientSearchURL),
                    config.getPropertyValue(Property.PatientSearchUserName),
                    config.getPropertyValue(Property.PatientSearchPassword),
                    (int) SystemConfiguration.getInstance().getSearchTimeLimit());

            externalSearches.add(externalSearch);
        }
        return externalSearches;
    }

    private void insertNewPatients(List<ExtendedPatientSearchResults> newPatientsFromClinic) {
        try {
            for (ExtendedPatientSearchResults results : newPatientsFromClinic) {
                insertNewPatients(results);
            }
        } catch (LIMSRuntimeException e) {
            LogEvent.logDebug(e);
        }
    }

    private void insertNewPatients(ExtendedPatientSearchResults results) {
        Patient patient = new Patient();
        Person person = new Person();

        patient.setBirthDateForDisplay(results.getBirthdate());
        patient.setGender(results.getGender());
        patient.setNationalId(results.getNationalId());
        patient.setSysUserId(sysUserId);

        person.setLastName(results.getLastName());
        person.setFirstName(results.getFirstName());
        person.setStreetAddress(results.getStreetAddress());
        person.setZipCode(results.getPostalCode());
        person.setSysUserId(sysUserId);

        personService.insert(person);
        patient.setPerson(person);

        patientService.insert(patient);
        fhirTransformService.CreateFhirResource(fhirTransformService.CreateFhirPatientFromOEPatient(patient));

        persistIdentityType(patientIdentityService, results.getStNumber(), "ST", patient.getId());
        persistIdentityType(patientIdentityService, results.getSubjectNumber(), "SUBJECT", patient.getId());
        persistIdentityType(patientIdentityService, results.getMothersName(), "MOTHER", patient.getId());
        persistIdentityType(patientIdentityService, results.getGUID(), "GUID", patient.getId());
        persistIdentityType(patientIdentityService, results.getDataSourceId(), "ORG_SITE", patient.getId());

        String ADDRESS_PART_COMMUNE_ID = "";
        String ADDRESS_PART_VILLAGE_ID = "";

        List<AddressPart> partList = addressPartService.getAll();
        for (AddressPart addressPart : partList) {
            if ("commune".equals(addressPart.getPartName())) {
                ADDRESS_PART_COMMUNE_ID = addressPart.getId();
            } else if ("village".equals(addressPart.getPartName())) {
                ADDRESS_PART_VILLAGE_ID = addressPart.getId();
            }
        }

        patientService.insertNewPatientAddressInfo(ADDRESS_PART_COMMUNE_ID, results.getCampCommune(), "T", patient,
                sysUserId);
        patientService.insertNewPatientAddressInfo(ADDRESS_PART_VILLAGE_ID, results.getTown(), "T", patient, sysUserId);

        results.setPatientID(patient.getId());
    }

    public void persistIdentityType(PatientIdentityService patientIdentityService, String paramValue, String type,
            String patientId) throws LIMSRuntimeException {

        if (!GenericValidator.isBlankOrNull(paramValue)) {

            String typeID = PatientIdentityTypeMap.getInstance().getIDForType(type);

            PatientIdentity patientIdentity = new PatientIdentity();
            patientIdentity.setPatientId(patientId);
            patientIdentity.setIdentityTypeId(typeID);
            patientIdentity.setSysUserId(sysUserId);
            patientIdentity.setIdentityData(paramValue);
            patientIdentity.setLastupdatedFields();
            patientIdentityService.insert(patientIdentity);
        }
    }

    /*
     * This will check to see if the clinic results are in OpenELIS. If they are not
     * then they will be
     */
    private void findNewPatients(List<PatientSearchResults> results, List<ExtendedPatientSearchResults> externalResults,
            List<ExtendedPatientSearchResults> newPatientsFromExternalSearch) {

        if (externalResults != null) {
            List<String> currentGuids = new ArrayList<>();
            List<String> currentNationalIds = new ArrayList<>();
            List<String> currentSubjectNumbers = new ArrayList<>();
            List<String> currentSTNumbers = new ArrayList<>();

            for (PatientSearchResults result : results) {
                if (!GenericValidator.isBlankOrNull(result.getGUID())) {
                    currentGuids.add(result.getGUID());
                }
                if (!GenericValidator.isBlankOrNull(result.getNationalId())) {
                    currentNationalIds.add(result.getNationalId());
                }
                if (!GenericValidator.isBlankOrNull(result.getSubjectNumber())) {
                    currentSubjectNumbers.add(result.getSubjectNumber());
                }
                if (!GenericValidator.isBlankOrNull(result.getSTNumber())) {
                    currentSTNumbers.add(result.getSTNumber());
                }
            }

            for (ExtendedPatientSearchResults externalResult : externalResults) {
                if (!currentGuids.contains(externalResult.getGUID())
                        && !currentNationalIds.contains(externalResult.getNationalId())
                        && !currentSubjectNumbers.contains(externalResult.getSubjectNumber())
                        && !currentSTNumbers.contains(externalResult.getSTNumber())) {
                    newPatientsFromExternalSearch.add(externalResult);
                }
            }
        }
    }

}
