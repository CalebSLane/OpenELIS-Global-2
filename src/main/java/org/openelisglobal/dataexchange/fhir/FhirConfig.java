package org.openelisglobal.dataexchange.fhir;

import java.util.Optional;

import org.apache.http.impl.client.CloseableHttpClient;
import org.itech.fhir.dataexport.api.service.DataExportSource;
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

    @Value("${org.openelisglobal.fhir.subscriber}")
    private Optional<String> consolidatedServerPath;

    @Value("${org.openelisglobal.fhir.subscriber.resources}")
    private String[] consolidatedServerResources;

    @Bean
    public FhirContext fhirContext() {
        FhirContext fhirContext = new FhirContext(FhirVersionEnum.R4);
        configureFhirHttpClient(fhirContext);
        return fhirContext;
    }

    @Bean
    public DataExportSource dataExportSource() {
        return new DataExportSourceImpl(getLocalFhirStorePath());
    }

    public void configureFhirHttpClient(FhirContext fhirContext) {
        IRestfulClientFactory clientFactory = new ApacheRestfulClientFactory(fhirContext);

        clientFactory.setHttpClient(httpClient);
        fhirContext.setRestfulClientFactory(clientFactory);
    }

    public String getLocalFhirStorePath() {
        if (!localFhirStorePath.endsWith("/")) {
            this.localFhirStorePath = this.localFhirStorePath + "/";
        }
        return localFhirStorePath;
    }

    public Optional<String> getConsolidatedServerPath() {
        if (consolidatedServerPath.isPresent()) {
            if (consolidatedServerPath.get().startsWith("http://")) {
                consolidatedServerPath = Optional
                        .of("https://" + consolidatedServerPath.get().substring("http://".length()));
            }
            if (!consolidatedServerPath.get().startsWith("https://")) {
                consolidatedServerPath = Optional.of("https://" + consolidatedServerPath.get());
            }
            if (!consolidatedServerPath.get().endsWith("/")) {
                consolidatedServerPath = Optional.of(consolidatedServerPath.get() + "/");
            }
        }
        return consolidatedServerPath;
    }

    public String[] getConsolidatedServerResources() {
        return this.consolidatedServerResources;
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

    public class DataExportSourceImpl implements DataExportSource {

        private String localFhirStorePath;

        private DataExportSourceImpl(String localFhirStorePath) {
            this.localFhirStorePath = localFhirStorePath;
        }
        @Override
        public String getLocalFhirStorePath() {
            return localFhirStorePath;
        }

    }
}
