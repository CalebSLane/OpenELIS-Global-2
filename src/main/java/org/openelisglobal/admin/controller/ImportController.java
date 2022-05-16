package org.openelisglobal.admin.controller;

import java.io.IOException;
import java.net.URISyntaxException;

import org.json.simple.parser.ParseException;
import org.openelisglobal.dataexchange.fhir.exception.FhirGeneralException;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.organization.service.OrganizationImportService;
import org.openelisglobal.provider.service.ProviderImportService;
import org.openelisglobal.test.service.TestImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/import")
public class ImportController {

    @Autowired
    @Lazy
    private OrganizationImportService organizationImportService;
    @Autowired
    @Lazy
    private ProviderImportService providerImportService;
    @Autowired
    @Lazy
    private TestImportService testImportService;

    @GetMapping(value = "/all")
    public void importAll() throws URISyntaxException, ParseException, Exception {
        organizationImportService.importOrganizationList();
        providerImportService.importPractitionerList();
        testImportService.importTestList();
    }

    @GetMapping(value = "/organization")
    public void importOrganizations() throws FhirLocalPersistingException, FhirGeneralException, IOException {
        organizationImportService.importOrganizationList();
    }

    @GetMapping(value = "/provider")
    public void importProviders() throws FhirLocalPersistingException, FhirGeneralException, IOException {
        providerImportService.importPractitionerList();
    }

    @GetMapping(value = "/test")
    public void importTests() throws URISyntaxException, ParseException, Exception {
        testImportService.importTestList();
    }

}
