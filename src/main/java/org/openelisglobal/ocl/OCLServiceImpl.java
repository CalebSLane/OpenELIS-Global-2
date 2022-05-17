package org.openelisglobal.ocl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OCLServiceImpl implements OCLService {

    private static final String PAGE_PARAMETER = "page";
    private static final String CONCEPT_CLASS_PARAMETER = "conceptClass";
    private static final String CONCEPT_CLASS_FIELD = "concept_class";
    private static final String ID_FIELD = "id";
    private static final String CONCEPTS = "concepts";
    private static final String MAPPINGS = "mappings";
    private static final String MAP_TYPE = "map_type";
    private static final String SOURCE_FIELD = "source";
    private static final String TO_SOURCE_FIELD = "to_source_name";
    private static final String FROM_SOURCE_FIELD = "from_source_name";
    private static final String TO_CONCEPT_CODE = "to_concept_code";
    private static final String FROM_CONCEPT_CODE = "from_concept_code";
    private static final String TO_CONCEPT_URL = "to_concept_url";
    private static final String FROM_CONCEPT_URL = "from_concept_url";

    @Value("${org.openelisglobal.ocl.base.url:}")
    private String oclBaseUrl;

    @Value("${org.openelisglobal.ocl.test.collection:}")
    private String oclTestCollection;

    @Value("${org.openelisglobal.ocl.username:}")
    private String oclUsername;

    @Value("${org.openelisglobal.ocl.password:}")
    private String oclPassword;

    @Autowired
    private CloseableHttpClient httpClient;

    private String getFullOCLTestCollection() {
        return oclBaseUrl + oclTestCollection;
    }

    @Override
    public List<JSONObject> getAllConceptsForConceptClass(String conceptClass) {
        List<JSONObject> concepts = new ArrayList<>();
        JSONArray conceptPage = null;
        int page = 0;
        CloseableHttpResponse getResponse = null;
        do {
            HttpGet httpGet;
            try {
                httpGet = new HttpGet(
                        new URIBuilder(getFullOCLTestCollection() + CONCEPTS + "/")
                                .addParameter(CONCEPT_CLASS_PARAMETER, conceptClass)
                                .addParameter(PAGE_PARAMETER, String.valueOf(++page)).build());
                try {
                    getResponse = httpClient.execute(httpGet);
                    int returnStatus = getResponse.getStatusLine().getStatusCode();
                    if (returnStatus == 200) {
                        JSONParser parse = new JSONParser();
                        String result = IOUtils.toString(getResponse.getEntity().getContent(), "UTF-8");
                        conceptPage = (JSONArray) parse
                                .parse(result);
                        for (Object testObj : conceptPage) {
                            concepts.add((JSONObject) testObj);
                        }
                    }

                } catch (IOException | ParseException e) {
                    LogEvent.logError(e.toString(), e);
//                throw e;
                } catch (RuntimeException e) {
                    LogEvent.logError(e.toString(), e);
                    httpGet.abort();
                    throw e;
                } finally {
                    if (getResponse != null) {
                        try {
                            getResponse.close();
                        } catch (IOException e) {
                            LogEvent.logError(e);
                        }
                    }
                }
            } catch (URISyntaxException e) {
                LogEvent.logError(e.toString(), e);
                LogEvent.logError(e);
            }
        } while (hasNextPage(getResponse));
        return concepts;
    }

    @Override
    public List<JSONObject> getAllMappingsForConcept(JSONObject concept) {
        List<JSONObject> mappings = new ArrayList<>();
        JSONArray mappingPage = null;
        int page = 0;
        CloseableHttpResponse getResponse = null;
        do {
            HttpGet httpGet;
            try {
                httpGet = new HttpGet(
                        new URIBuilder(getFullOCLTestCollection() + CONCEPTS + "/" + concept.get(ID_FIELD) + "/"
                                + MAPPINGS + "/")
                                .addParameter(PAGE_PARAMETER, String.valueOf(++page)).build());
                try {
                    getResponse = httpClient.execute(httpGet);
                    int returnStatus = getResponse.getStatusLine().getStatusCode();
                    if (returnStatus == 200) {
                        JSONParser parse = new JSONParser();
                        mappingPage = (JSONArray) parse
                                .parse(IOUtils.toString(getResponse.getEntity().getContent(), "UTF-8"));
                        for (Object testObj : mappingPage) {
                            mappings.add((JSONObject) testObj);
                        }
                    }

                } catch (IOException | ParseException e) {
                    LogEvent.logError(e.toString(), e);
//                throw e;
                } catch (RuntimeException e) {
                    LogEvent.logError(e.toString(), e);
                    httpGet.abort();
                    throw e;
                } finally {
                    if (getResponse != null) {
                        try {
                            getResponse.close();
                        } catch (IOException e) {
                            LogEvent.logError(e);
                        }
                    }
                }
            } catch (URISyntaxException e) {
                LogEvent.logError(e.toString(), e);
                LogEvent.logError(e);
            }
        } while (hasNextPage(getResponse));
        return mappings;
    }

    @Override
    public List<JSONObject> getAllMappingsForConceptAndMapType(JSONObject concept, String mapType) {
        return this.getAllMappingsForConcept(concept).stream().filter(mapping -> mapType.equals(mapping.get(MAP_TYPE)))
                .collect(Collectors.toList());
    }

    @Override
    public List<JSONObject> getAllMappingsFromConceptForMapType(JSONObject fromConcept, String mapType) {
        return this.getAllMappingsForConcept(fromConcept).stream()
                .filter(mapping -> mapType.equals(mapping.get(MAP_TYPE))
                        && mapping.get(FROM_CONCEPT_CODE).equals(fromConcept.get(ID_FIELD)))
                .collect(Collectors.toList());
    }

    @Override
    public List<JSONObject> getAllMappingsToConceptForMapType(JSONObject toConcept, String mapType) {
        return this.getAllMappingsForConcept(toConcept).stream().filter(mapping -> mapType.equals(mapping.get(MAP_TYPE))
                && mapping.get(TO_CONCEPT_CODE).equals(toConcept.get(ID_FIELD))).collect(Collectors.toList());
    }

    @Override
    public List<MappingConceptPair> getAllConceptsMappedFromConceptWithMapType(JSONObject fromConcept, String mapType) {
        List<MappingConceptPair> concepts = new ArrayList<>();
        List<JSONObject> mappings = getAllMappingsFromConceptForMapType(fromConcept, mapType);
        for (JSONObject mapping : mappings) {
            HttpGet httpGet = new HttpGet(oclBaseUrl + mapping.get(TO_CONCEPT_URL));
            CloseableHttpResponse getResponse = null;
            try {
                getResponse = httpClient.execute(httpGet);
                int returnStatus = getResponse.getStatusLine().getStatusCode();
                if (returnStatus == 200) {
                    JSONParser parse = new JSONParser();
                    concepts.add(new MappingConceptPair(mapping,
                            (JSONObject) parse.parse(IOUtils.toString(getResponse.getEntity().getContent(), "UTF-8"))));
                }
            } catch (IOException | ParseException e) {
                LogEvent.logError(e.toString(), e);
//                throw e;
            } catch (RuntimeException e) {
                LogEvent.logError(e.toString(), e);
                httpGet.abort();
                throw e;
            } finally {
                if (getResponse != null) {
                    try {
                        getResponse.close();
                    } catch (IOException e) {
                        LogEvent.logError(e);
                    }
                }
            }
        }
        return concepts;

    }

    @Override
    public List<MappingConceptPair> getAllConceptsMappedToConceptWithMapType(JSONObject toConcept, String mapType) {
        List<MappingConceptPair> concepts = new ArrayList<>();
        List<JSONObject> mappings = getAllMappingsToConceptForMapType(toConcept, mapType);
        for (JSONObject mapping : mappings) {
            HttpGet httpGet = new HttpGet(oclBaseUrl + mapping.get(FROM_CONCEPT_URL));
            CloseableHttpResponse getResponse = null;
            try {
                getResponse = httpClient.execute(httpGet);
                int returnStatus = getResponse.getStatusLine().getStatusCode();
                if (returnStatus == 200) {
                    JSONParser parse = new JSONParser();
                    concepts.add(new MappingConceptPair(mapping,
                            (JSONObject) parse.parse(IOUtils.toString(getResponse.getEntity().getContent(), "UTF-8"))));
                }
            } catch (IOException | ParseException e) {
                LogEvent.logError(e.toString(), e);
//                throw e;
            } catch (RuntimeException e) {
                LogEvent.logError(e.toString(), e);
                httpGet.abort();
                throw e;
            } finally {
                if (getResponse != null) {
                    try {
                        getResponse.close();
                    } catch (IOException e) {
                        LogEvent.logError(e);
                    }
                }
            }
        }
        return concepts;

    }

    @Override
    public List<MappingConceptPair> getAllConceptsOfClassMappedFromConceptWithMapType(String ofClass,
            JSONObject fromConcept, String mapType) {
        List<MappingConceptPair> concepts = new ArrayList<>();
        List<JSONObject> mappings = getAllMappingsFromConceptForMapType(fromConcept, mapType);
        for (JSONObject mapping : mappings) {
            HttpGet httpGet = new HttpGet(oclBaseUrl + mapping.get(TO_CONCEPT_URL));
            CloseableHttpResponse getResponse = null;
            try {
                getResponse = httpClient.execute(httpGet);
                int returnStatus = getResponse.getStatusLine().getStatusCode();
                if (returnStatus == 200) {
                    JSONParser parse = new JSONParser();
                    JSONObject concept = (JSONObject) parse
                            .parse(IOUtils.toString(getResponse.getEntity().getContent(), "UTF-8"));
                    if (ofClass.equals(concept.get(CONCEPT_CLASS_FIELD))) {
                        concepts.add(new MappingConceptPair(mapping, concept));
                    }
                }
            } catch (IOException | ParseException e) {
                LogEvent.logError(e.toString(), e);
//                throw e;
            } catch (RuntimeException e) {
                LogEvent.logError(e.toString(), e);
                httpGet.abort();
                throw e;
            } finally {
                if (getResponse != null) {
                    try {
                        getResponse.close();
                    } catch (IOException e) {
                        LogEvent.logError(e);
                    }
                }
            }
        }
        return concepts;

    }

    @Override
    public List<MappingConceptPair> getAllConceptsOfClassMappedToConceptWithMapType(String ofClass,
            JSONObject toConcept, String mapType) {
        List<MappingConceptPair> concepts = new ArrayList<>();
        List<JSONObject> mappings = getAllMappingsToConceptForMapType(toConcept, mapType);
        for (JSONObject mapping : mappings) {
            HttpGet httpGet = new HttpGet(oclBaseUrl + mapping.get(FROM_CONCEPT_URL));
            CloseableHttpResponse getResponse = null;
            try {
                getResponse = httpClient.execute(httpGet);
                int returnStatus = getResponse.getStatusLine().getStatusCode();
                if (returnStatus == 200) {
                    JSONParser parse = new JSONParser();
                    JSONObject concept = (JSONObject) parse
                            .parse(IOUtils.toString(getResponse.getEntity().getContent(), "UTF-8"));
                    if (ofClass.equals(concept.get(CONCEPT_CLASS_FIELD))) {
                        concepts.add(new MappingConceptPair(mapping, concept));
                    }
                }
            } catch (IOException | ParseException e) {
                LogEvent.logError(e.toString(), e);
//                throw e;
            } catch (RuntimeException e) {
                LogEvent.logError(e.toString(), e);
                httpGet.abort();
                throw e;
            } finally {
                if (getResponse != null) {
                    try {
                        getResponse.close();
                    } catch (IOException e) {
                        LogEvent.logError(e);
                    }
                }
            }
        }
        return concepts;

    }

    @Override
    public List<MappingConceptPair> getAllConceptsFromSourceMappedToConcept(String source, JSONObject toConcept) {
        List<MappingConceptPair> concepts = new ArrayList<>();
        List<JSONObject> mappings = getAllMappingsForConcept(toConcept);
        for (JSONObject mapping : mappings) {
            if (mapping.get(FROM_SOURCE_FIELD).equals(source)
                    && mapping.get(TO_CONCEPT_CODE).equals(toConcept.get(ID_FIELD))) {
                HttpGet httpGet = new HttpGet(oclBaseUrl + mapping.get(FROM_CONCEPT_URL));
                CloseableHttpResponse getResponse = null;
                try {
                    getResponse = httpClient.execute(httpGet);
                    int returnStatus = getResponse.getStatusLine().getStatusCode();
                    if (returnStatus == 200) {
                        JSONParser parse = new JSONParser();
                        JSONObject concept = (JSONObject) parse
                                .parse(IOUtils.toString(getResponse.getEntity().getContent(), "UTF-8"));
                        concepts.add(new MappingConceptPair(mapping, concept));
                    }
                } catch (IOException | ParseException e) {
                    LogEvent.logError(e.toString(), e);
//                throw e;
                } catch (RuntimeException e) {
                    LogEvent.logError(e.toString(), e);
                    httpGet.abort();
                    throw e;
                } finally {
                    if (getResponse != null) {
                        try {
                            getResponse.close();
                        } catch (IOException e) {
                            LogEvent.logError(e);
                        }
                    }
                }
            }
        }
        return concepts;

    }

    @Override
    public List<MappingConceptPair> getAllConceptsToSourceMappedFromConcept(String source, JSONObject fromConcept) {
        List<MappingConceptPair> concepts = new ArrayList<>();
        List<JSONObject> mappings = getAllMappingsForConcept(fromConcept);
        for (JSONObject mapping : mappings) {
            if (mapping.get(TO_SOURCE_FIELD).equals(source)
                    && mapping.get(FROM_CONCEPT_CODE).equals(fromConcept.get(ID_FIELD))) {
                HttpGet httpGet = new HttpGet(oclBaseUrl + mapping.get(TO_CONCEPT_URL));
                CloseableHttpResponse getResponse = null;
                try {
                    getResponse = httpClient.execute(httpGet);
                    int returnStatus = getResponse.getStatusLine().getStatusCode();
                    if (returnStatus == 200) {
                        JSONParser parse = new JSONParser();
                        JSONObject concept = (JSONObject) parse
                                .parse(IOUtils.toString(getResponse.getEntity().getContent(), "UTF-8"));
                        concepts.add(new MappingConceptPair(mapping, concept));
                    }
                } catch (IOException | ParseException e) {
                    LogEvent.logError(e.toString(), e);
//                throw e;
                } catch (RuntimeException e) {
                    LogEvent.logError(e.toString(), e);
                    httpGet.abort();
                    throw e;
                } finally {
                    if (getResponse != null) {
                        try {
                            getResponse.close();
                        } catch (IOException e) {
                            LogEvent.logError(e);
                        }
                    }
                }
            }
        }
        return concepts;

    }

    private boolean hasNextPage(CloseableHttpResponse response) {
        try {
            int pages = Integer.parseInt(response.getFirstHeader("pages").getValue());
            int currentPage = Integer.parseInt(response.getFirstHeader("page_number").getValue());
            return currentPage < pages;
        } catch (NumberFormatException e) {
            LogEvent.logError("unable to determine if there are more pages of tests", e);
        }
        return false;
    }

}
