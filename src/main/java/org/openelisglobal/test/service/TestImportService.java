package org.openelisglobal.test.service;

import java.io.IOException;
import java.net.URISyntaxException;

import org.json.simple.parser.ParseException;

public interface TestImportService {

    void importTestList() throws URISyntaxException, ParseException, IOException, Exception;

}
