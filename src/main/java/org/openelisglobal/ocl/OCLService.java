package org.openelisglobal.ocl;

import java.util.List;

import org.json.simple.JSONObject;

public interface OCLService {

    List<JSONObject> getAllConceptsForConceptClass(String conceptClass);

    List<JSONObject> getAllMappingsForConcept(JSONObject concept);

    List<JSONObject> getAllMappingsForConceptAndMapType(JSONObject concept, String mapType);

    List<JSONObject> getAllMappingsFromConceptForMapType(JSONObject fromConcept, String mapType);

    List<JSONObject> getAllMappingsToConceptForMapType(JSONObject toConcept, String mapType);

    List<MappingConceptPair> getAllConceptsMappedFromConceptWithMapType(JSONObject fromConcept, String mapType);

    List<MappingConceptPair> getAllConceptsMappedToConceptWithMapType(JSONObject toConcept, String mapType);

    List<MappingConceptPair> getAllConceptsOfClassMappedToConceptWithMapType(String ofClass, JSONObject toConcept,
            String mapType);

    List<MappingConceptPair> getAllConceptsOfClassMappedFromConceptWithMapType(String ofClass, JSONObject fromConcept,
            String mapType);

    List<MappingConceptPair> getAllConceptsFromSourceMappedToConcept(String source, JSONObject toConcept);

    List<MappingConceptPair> getAllConceptsToSourceMappedFromConcept(String source, JSONObject fromConcept);

    public class MappingConceptPair {
        public JSONObject mapping;
        public JSONObject concept;

        public MappingConceptPair(JSONObject mapping, JSONObject concept) {
            this.mapping = mapping;
            this.concept = concept;
        }
    }


}
