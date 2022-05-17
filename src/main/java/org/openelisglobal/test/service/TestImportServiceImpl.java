package org.openelisglobal.test.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.validator.GenericValidator;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.ocl.OCLService;
import org.openelisglobal.ocl.OCLService.MappingConceptPair;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

@Service
public class TestImportServiceImpl implements TestImportService {

    private static final String ID_FIELD = "id";
    private static final String DISPLAY_NAME_FIELD = "display_name";
    private static final String UNIT_OF_MEASURE_NAME_FIELD = "units";

    private static final String MAP_TYPE = "map_type";

    private static final String LOINC_SOURCE = "LOINC";

    private static final String TEST_CONCEPT_CLASS = "Test";
    private static final String UOM_CONCEPT_CLASS = "Units-of-Measure";
    private static final String SAMPLE_TYPE_CLASS = "specimen";
    private static final String PANEL_CLASS = "LabSet";

    private static final String TEST_DICTIONARY_RESULT_MAPPING = "Q-And-A";
    private static final String TEST_UOM_MAPPING = "";// TODO
    private static final String SAMPLE_TYPE_TEST_MAPPING = "Has-specimen";
    private static final String PANEL_TEST_MAPPING = "CONCEPT-SET";
    private static final String SAME_AS_MAPPING = "SAME-AS";
    private static final String NARROWER_THAN_MAPPING = "NARROWER-THAN";
    private static final String BROADER_THAN_MAPPING = "BROADER-THAN";

    @Value("${org.openelisglobal.ocl.base.url:}")
    private String testOCLUrl;

    @Autowired
    private OCLService oclService;
    @Autowired
    private TestService testService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;
    @Autowired
    private PanelService panelService;
    @Autowired
    private PanelItemService panelItemService;
    @Autowired
    private UnitOfMeasureService uomService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;
    @Autowired
    private LocalizationService localizationService;

    @Override
    @Transactional
    @Scheduled(initialDelay = 1000, fixedRate = 24 * 60 * 60 * 1000)
    public void importTestList() throws URISyntaxException, ParseException, IOException, Exception {
        synchronized (this) {
            if (!GenericValidator.isBlankOrNull(testOCLUrl)) {
                Map<String, Panel> panelsByOCLId = new HashMap<>();
                Map<String, TypeOfSample> sampleTypesByOCLId = new HashMap<>();
                Map<String, UnitOfMeasure> uomsByOCLId = new HashMap<>();

                LogEvent.logInfo(this.getClass().getName(), "importTestList", "trying to import tests");
                List<JSONObject> tests = oclService.getAllConceptsForConceptClass(TEST_CONCEPT_CLASS);
                List<TestMappingObjects> testObjects = tests.stream()
                        .map(e -> createTestObjects(e, panelsByOCLId, sampleTypesByOCLId, uomsByOCLId))
                        .filter(e -> e != null).collect(Collectors.toList());
                LogEvent.logInfo(this.getClass().getName(), "importTestList", "finished importing tests");
                LogEvent.logInfo(this.getClass().getName(), "importTestList",
                        "trying to deactivate/delete concepts no longer found");

                List<String> ids;
                ids = panelsByOCLId.values().stream().map(e -> e.getId()).collect(Collectors.toList());
                panelService.deactivateAllNotIn(ids);

                ids = sampleTypesByOCLId.values().stream().map(e -> e.getId()).collect(Collectors.toList());
                typeOfSampleService.deactivateAllNotIn(ids);

                ids = testObjects.stream().map(e -> e.test.getId()).collect(Collectors.toList());
                testService.deactivateAllNotIn(ids);

                ids = testObjects.stream().flatMap(e -> e.testResults.stream()).map(e -> e.getId())
                        .collect(Collectors.toList());
                testResultService.deactivateAllNotIn(ids);

                ids = testObjects.stream().flatMap(e -> e.panelItems.stream()).map(e -> e.getId())
                        .collect(Collectors.toList());
                panelItemService.deleteAllNotIn(ids);

                ids = testObjects.stream().flatMap(e -> e.sampleTypeTests.stream()).map(e -> e.getId())
                        .collect(Collectors.toList());
                typeOfSampleTestService.deleteAllNotIn(ids);
                LogEvent.logInfo(this.getClass().getName(), "importTestList",
                        "finished deactivating/deleting concepts no longer found");

            }
        }
    }

    private TestMappingObjects createTestObjects(JSONObject testConcept, Map<String, Panel> panelsByOCLId,
            Map<String, TypeOfSample> sampleTypesByOCLId, Map<String, UnitOfMeasure> uomsByOCLId) {
        TestMappingObjects testObjects = new TestMappingObjects();

        try {
            testObjects.uom = getCreateUOMForTest(testConcept, uomsByOCLId);
            testObjects.panels = getCreatePanelForTest(testConcept, panelsByOCLId);
            testObjects.sampleTypes = getCreateSampleTypesForTest(testConcept, sampleTypesByOCLId);

            testObjects.test = getCreateTest(testConcept, testObjects);
            if (testObjects.test == null) {

                return null;
            }
            testObjects.testResults = getCreateTestResultsForTest(testConcept, testObjects.test);

            testObjects.sampleTypeTests = getCreateSampleTypeTestForTest(testObjects.test, testObjects.sampleTypes);
            testObjects.panelItems = getCreatePanelItemsForTest(testObjects.test, testObjects.panels);
        } catch (Exception e) {
            LogEvent.logError("error creating test result for test - " + testConcept.get(ID_FIELD), e);
            LogEvent.logErrorStack(e);
        }

        return testObjects;
    }

    private Optional<UnitOfMeasure> getCreateUOMForTest(JSONObject testConcept,
            Map<String, UnitOfMeasure> uomsByOCLId) {
        List<MappingConceptPair> uomConcepts = oclService
                .getAllConceptsOfClassMappedFromConceptWithMapType(UOM_CONCEPT_CLASS, testConcept, TEST_UOM_MAPPING);
        if (uomConcepts.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(getCreateUOM(uomConcepts.get(0).concept, uomsByOCLId));
        }
    }

    // TODO confirm field name and correct concept is used
    private UnitOfMeasure getCreateUOM(JSONObject uomConcept, Map<String, UnitOfMeasure> uomsByOCLId) {
        if (uomsByOCLId.containsKey(uomConcept.get(ID_FIELD))) {
            return uomsByOCLId.get(uomConcept.get(ID_FIELD));
        } else {
            UnitOfMeasure uom;
            Optional<UnitOfMeasure> existingUom;
            existingUom = uomService.getMatch("name", uomConcept.get(UNIT_OF_MEASURE_NAME_FIELD));
            if (existingUom.isEmpty()) {
                uom = new UnitOfMeasure();
            } else {
                uom = existingUom.get();
            }
            uom.setName((String) uomConcept.get(UNIT_OF_MEASURE_NAME_FIELD));
            uom.setDescription((String) uomConcept.get(UNIT_OF_MEASURE_NAME_FIELD));

            uom = uomService.save(uom);
            uomsByOCLId.put((String) uomConcept.get(ID_FIELD), uom);
            return uom;
        }
    }

    private List<Panel> getCreatePanelForTest(JSONObject testConcept, Map<String, Panel> panelsByOCLId) {
        List<MappingConceptPair> panelConcepts = oclService.getAllConceptsOfClassMappedToConceptWithMapType(PANEL_CLASS,
                testConcept, PANEL_TEST_MAPPING);
        if (panelConcepts.isEmpty()) {
            return Lists.newArrayList();
        } else {
            return panelConcepts.stream().map(e -> getCreatePanel(e.concept, panelsByOCLId))
                    .collect(Collectors.toList());
        }
    }

    private Panel getCreatePanel(JSONObject panelConcept, Map<String, Panel> panelsByOCLId) {
        if (panelsByOCLId.containsKey(panelConcept.get(ID_FIELD))) {
            return panelsByOCLId.get(panelConcept.get(ID_FIELD));
        } else {
            String panelName = (String) panelConcept.get(DISPLAY_NAME_FIELD);
            Panel panel = panelService.getPanelByName(panelName);
            if (panel == null) {
                panel = new Panel();
            }
            panel.setName(panelName);
            panel.setDescription(panelName);
            panel.setIsActive("Y");
            Localization localization = panel.getLocalization();
            if (localization == null) {
                localization = new Localization();
            }
            localization.setEnglish(panelName);
            localization.setFrench(panelName);
            localization = localizationService.save(localization);
            panel.setLocalization(localization);

            panel = panelService.save(panel);
            panelsByOCLId.put((String) panelConcept.get(ID_FIELD), panel);
            return panel;
        }
    }

    private List<TypeOfSample> getCreateSampleTypesForTest(JSONObject testConcept,
            Map<String, TypeOfSample> sampleTypesByOCLId) {
        List<MappingConceptPair> sampleTypeConcepts = oclService.getAllConceptsOfClassMappedFromConceptWithMapType(
                SAMPLE_TYPE_CLASS, testConcept, SAMPLE_TYPE_TEST_MAPPING);
        if (sampleTypeConcepts.isEmpty()) {
            return Lists.newArrayList();
        } else {
            return sampleTypeConcepts.stream().map(e -> getCreateSampleType(e.concept, sampleTypesByOCLId))
                    .collect(Collectors.toList());
        }
    }

    // TODO
    private TypeOfSample getCreateSampleType(JSONObject sampleTypeConcept,
            Map<String, TypeOfSample> sampleTypesByOCLId) {
        if (sampleTypesByOCLId.containsKey(sampleTypeConcept.get(ID_FIELD))) {
            return sampleTypesByOCLId.get(sampleTypeConcept.get(ID_FIELD));
        } else {
            String sampleTypeName = (String) sampleTypeConcept.get(DISPLAY_NAME_FIELD);
            TypeOfSample sampleType = new TypeOfSample();
            sampleType.setDescription(sampleTypeName);
            sampleType.setDomain("H");
            sampleType = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(sampleType, false);
            sampleType.setIsActive(true);
            Localization localization = sampleType.getLocalization();
            if (localization == null) {
                localization = new Localization();
            }
            localization.setEnglish(sampleTypeName);
            localization.setFrench(sampleTypeName);
            localization = localizationService.save(localization);
            sampleType.setLocalization(localization);

            sampleType = typeOfSampleService.save(sampleType);

            sampleTypesByOCLId.put((String) sampleTypeConcept.get(ID_FIELD), sampleType);
            return sampleType;
        }
    }

    private Test getCreateTest(JSONObject testConcept, TestMappingObjects testObjects) {
        Test test = null;
        JSONObject loincConcept = null;
        List<MappingConceptPair> concepts = oclService.getAllConceptsToSourceMappedFromConcept(LOINC_SOURCE,
                testConcept);
        for (MappingConceptPair pairs : concepts) {
            if (SAME_AS_MAPPING.equals(pairs.mapping.get(MAP_TYPE))) {
                loincConcept = pairs.concept;
                if (loincConcept == null) {
                    LogEvent.logWarn(this.getClass().getName(), "getCreateTest",
                            "multiple loinc mappings exist for test. using " + SAME_AS_MAPPING + " mapping");
                }
            } else if (NARROWER_THAN_MAPPING.equals(pairs.mapping.get(MAP_TYPE))) {
                if (loincConcept == null) {
                    loincConcept = pairs.concept;
                } else {
                    LogEvent.logWarn(this.getClass().getName(), "getCreateTest",
                            "multiple loinc mappings exist for test. Ignoring " + NARROWER_THAN_MAPPING + " mapping");
                }
            } else if (BROADER_THAN_MAPPING.equals(pairs.mapping.get(MAP_TYPE))) {
                if (loincConcept == null) {
                    loincConcept = pairs.concept;
                } else {
                    LogEvent.logWarn(this.getClass().getName(), "getCreateTest",
                            "multiple loinc mappings exist for test. Ignoring " + BROADER_THAN_MAPPING + " mapping");
                }
            }
        }
        if (loincConcept == null) {
            LogEvent.logWarn(this.getClass().getName(), "getCreateTest",
                    "No loinc mapping exists for test. Ignoring this test");
            return test;
        }

        String loincCode = (String) loincConcept.get(ID_FIELD);

        List<Test> tests = testService.getActiveTestsByLoinc(loincCode);

        if (tests.size() == 0) {
            test = new Test();
        } else {
            test = tests.get(0);
            if (testContainsLocalModifications(test)) {
                return test;
            }
        }

        String testName = (String) testConcept.get(DISPLAY_NAME_FIELD);
        test.setLoinc(loincCode);
        test.setGuid((String) testConcept.get("uuid"));
        test.setIsActive("Y");
        Localization localization = test.getLocalizedTestName();
        if (localization == null) {
            localization = new Localization();
        }
        localization.setEnglish(testName);
        localization.setFrench(testName);
        localization.setDescription("test name");
        localization = localizationService.save(localization);
        test.setLocalizedTestName(localization);

        localization = test.getLocalizedReportingName();
        if (localization == null) {
            localization = new Localization();
        }
        localization.setEnglish(testName);
        localization.setFrench(testName);
        localization.setDescription("test report name");
        localization = localizationService.save(localization);
        test.setLocalizedReportingName(localization);

        test.setDescription(testName);
        test.setOrderable(true);
        test.setSortOrder("0");

        if (testObjects.uom.isPresent()) {
            test.setUnitOfMeasure(testObjects.uom.get());
        }

        return testService.save(test);
    }

    private boolean testContainsLocalModifications(Test test) {
        // TODO Auto-generated method stub
        return false;
    }

    private List<TestResult> getCreateTestResultsForTest(JSONObject testConcept, Test test) throws Exception {
        String dataType = (String) testConcept.get("datatype");
        List<TestResult> testResults = testResultService.getActiveTestResultsByTest(test.getId());
        switch (dataType) {
        case "Numeric":
        case "Text":
            TestResult testResult;
            if (testResults.size() >= 1) {
                testResult = testResults.get(0);
            } else {
                testResult = new TestResult();
            }
            testResult.setTest(test);
            testResult.setIsActive(true);
            testResult.setTestResultType(mapDataTypeToResultType(dataType));
            return Lists.newArrayList(testResultService.save(testResult));
        case "Boolean":
            return getCreateBooleanTestResults(testConcept, test, testResults);
        case "Coded":
            return createCodedTestResults(testConcept, test, testResults);
        default:
            return new ArrayList<>();

        }
    }

    private List<TestResult> createCodedTestResults(JSONObject testConcept, Test test,
            List<TestResult> testResultsOrignal) throws Exception {
        List<TestResult> testResults = new ArrayList<>();
        List<MappingConceptPair> resultConcepts = oclService.getAllConceptsMappedFromConceptWithMapType(testConcept,
                TEST_DICTIONARY_RESULT_MAPPING);
        for (int i = 0; i < resultConcepts.size(); ++i) {
            MappingConceptPair resultMappingConcept = resultConcepts.get(i);
            TestResult testResult = testResultsOrignal.size() > i ? testResultsOrignal.get(i) : new TestResult();
            testResult.setTest(test);
            testResults.add(getCreateDictionaryTestResult(resultMappingConcept.concept, testResult));
        }
        return testResults;
    }

    private TestResult getCreateDictionaryTestResult(JSONObject answer, TestResult testResult) {
        String testAnswer = (String) answer.get("display_name");

        Dictionary dictionary = dictionaryService.getDictionaryEntrysByNameAndCategoryDescription(testAnswer,
                "Test Result");
        if (dictionary == null) {
            dictionary = new Dictionary();
            dictionary.setDictionaryCategory(dictionaryCategoryService.getDictionaryCategoryByName("Test Result"));
            dictionary = dictionaryService.save(dictionary);
        }

        testResult.setIsActive(true);
        testResult.setValue(dictionary.getId());
        testResult.setTestResultType("D");
        return testResultService.save(testResult);
    }

    private List<TestResult> getCreateBooleanTestResults(JSONObject testConcept, Test test,
            List<TestResult> testResults) {
        TestResult testResultTrue;
        TestResult testResultFalse;

        if (testResults.size() >= 2) {
            testResultTrue = testResults.get(0);
            testResultFalse = testResults.get(1);
        } else {
            testResultTrue = new TestResult();
            testResultFalse = new TestResult();
        }
        testResultTrue.setIsActive(true);
        testResultTrue.setValue(
                dictionaryService.getDictionaryEntrysByNameAndCategoryDescription("true", "Test Result").getId());
        testResultTrue.setTestResultType("D");
        testResultTrue = testResultService.save(testResultTrue);

        testResultFalse.setIsActive(true);
        testResultTrue.setValue(
                dictionaryService.getDictionaryEntrysByNameAndCategoryDescription("false", "Test Result").getId());
        testResultFalse.setTestResultType("D");
        testResultFalse = testResultService.save(testResultFalse);
        return Lists.newArrayList(testResultTrue, testResultFalse);
    }

    private String mapDataTypeToResultType(String dataType) {
        switch (dataType) {
        case "Numeric":
            return "N";
        case "Text":
            return "A";
        case "Boolean":
            return "D";
        case "Coded":
            return "D";
        default:
            return "";
        }
    }

    private List<PanelItem> getCreatePanelItemsForTest(Test test, List<Panel> panels) {
        List<PanelItem> panelItems = new ArrayList<>();
        for (Panel panel : panels) {
            PanelItem panelItem = panelItemService.getPanelItemByTestIdAndPanelId(test.getId(), panel.getId());
            if (panelItem == null) {
                panelItem = new PanelItem();
                panelItem.setTest(test);
                panelItem.setPanel(panel);
                panelItem = panelItemService.save(panelItem);
            }
            panelItems.add(panelItem);
        }
        return panelItems;
    }

    private List<TypeOfSampleTest> getCreateSampleTypeTestForTest(Test test, List<TypeOfSample> sampleTypes) {
        List<TypeOfSampleTest> sampleTypeTests = new ArrayList<>();
        for (TypeOfSample sampleType : sampleTypes) {
            TypeOfSampleTest sampleTypeTest = typeOfSampleTestService
                    .getTypeOfSampleTestsForSampleTypeAndTest(sampleType.getId(), test.getId());
            if (sampleTypeTest == null) {
                sampleTypeTest = new TypeOfSampleTest();
                sampleTypeTest.setTestId(test.getId());
                sampleTypeTest.setTypeOfSampleId(sampleType.getId());
                sampleTypeTest = typeOfSampleTestService.save(sampleTypeTest);
            }
            sampleTypeTests.add(sampleTypeTest);
        }
        return sampleTypeTests;
    }
//
//    public class TestResultObjects {
//        public List<TestResult> activateTestResults;
//        public List<TestResult> deactivateTestResults;
//    }

    public class TestMappingObjects {
        public Test test;

        public List<TestResult> testResults;
        public Optional<UnitOfMeasure> uom;
        public List<TypeOfSample> sampleTypes;
        public List<Panel> panels;
//      public ResultLimit resultLimit;

        public List<PanelItem> panelItems;
        public List<TypeOfSampleTest> sampleTypeTests;

    }

}
