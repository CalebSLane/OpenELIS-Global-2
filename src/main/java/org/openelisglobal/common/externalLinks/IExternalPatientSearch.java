package org.openelisglobal.common.externalLinks;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import org.openelisglobal.common.provider.query.ExtendedPatientSearchResults;
import org.openelisglobal.externalconnections.valueholder.ExternalConnection;
import org.openelisglobal.externalconnections.valueholder.ExternalConnectionAuthenticationData;

public interface IExternalPatientSearch {

    Future<Integer> runExternalSearch();

    void setSearchCriteria(String lastName, String firstName, String STNumber, String subjectNumber, String nationalID,
            String guid, String dateOfBirth, String gender);

    void setConnectionCredentials(String connectionString, String name, String password, int timeout_Mil);

    List<ExtendedPatientSearchResults> getSearchResults();

    String getConnectionString();

    void setConnectionCredentials(ExternalConnection externalConnection,
            Optional<ExternalConnectionAuthenticationData> authData, int timeout_Mil);
}
