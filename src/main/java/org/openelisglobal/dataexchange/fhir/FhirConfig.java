package org.openelisglobal.dataexchange.fhir;

import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.apache.ApacheRestfulClientFactory;
import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;

@Configuration
public class FhirConfig {

    @Autowired
    private CloseableHttpClient httpClient;

    private final static String OE_SYSTEM_CODE = "OpenELIS-Global";

    @Value("${org.openelisglobal.fhirstore.uri}")
    private String localFhirStorePath;

    @Bean
    public FhirContext fhirContext() {
        FhirContext fhirContext = new FhirContext(FhirVersionEnum.R4);
        configureFhirHttpClient(fhirContext);
        return fhirContext;
    }

    public void configureFhirHttpClient(FhirContext fhirContext) {
        IRestfulClientFactory clientFactory = new ApacheRestfulClientFactory(fhirContext);

        clientFactory.setHttpClient(httpClient);
        fhirContext.setRestfulClientFactory(clientFactory);

    }

    public String getLocalFhirStorePath() {
        if (localFhirStorePath.endsWith("/")) {
            return this.localFhirStorePath;
        } else {
            return this.localFhirStorePath + "/";
        }
    }

    public String getSubjectNumberSystem() {
        return OE_SYSTEM_CODE + "/SubjectNumber";
    }

    public String getSTSystem() {
        return OE_SYSTEM_CODE + "/STNumber";
    }

    public String getNationalIdSystem() {
        return OE_SYSTEM_CODE + "/NationalId";
    }

    public String getLabNoSystem() {
        return OE_SYSTEM_CODE + "/Lab No";
    }

}
