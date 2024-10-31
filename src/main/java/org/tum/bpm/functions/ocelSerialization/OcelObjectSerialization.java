package org.tum.bpm.functions.ocelSerialization;

import java.util.ArrayList;
import java.util.List;

import org.apache.flink.api.common.functions.MapFunction;
import org.tum.bpm.schemas.Resource;
import org.tum.bpm.schemas.ocel.OcelObject;
import org.tum.bpm.schemas.ocel.OcelRelationship;

public class OcelObjectSerialization implements MapFunction<Resource, OcelObject> {

    @Override
    public OcelObject map(Resource resource) throws Exception {
        // These created resource do not have any relation amongst each other at the moment
        List<OcelRelationship> ocelRelationships = new ArrayList<>();

        return new OcelObject(resource.getResourceId(), resource.getResourceModelId(), resource.getEnrichment(), ocelRelationships);
    }
    
}
