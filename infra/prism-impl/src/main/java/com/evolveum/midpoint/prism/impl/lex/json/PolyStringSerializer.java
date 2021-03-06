/**
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.lex.json;

import java.io.IOException;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;

public class PolyStringSerializer extends JsonSerializer<PolyString>{

    @Override
    public void serialize(PolyString value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        //System.out.println("wualaaaa polystring serialization");
//        jgen.writeStartObject();
        jgen.writeObject(value.getOrig());
//        jgen.writeStringField("norm", value.getNorm());
//        jgen.writeEndObject();

    }

    @Override
    public void serializeWithType(PolyString value, JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer) throws IOException, JsonProcessingException {
        // TODO Auto-generated method stub
        //System.out.println("polystring serialization with type");

//        typeSer.writeCustomTypePrefixForScalar(value, jgen, "poluStr");
        serialize(value, jgen, provider);
//        typeSer.writeCustomTypeSuffixForScalar(value, jgen, "tra");
//        jgen.writeStartObject();
//        jgen.writeString(value.getOrig());
//        jgen.writeTypeId("polyStirng");
//        jgen.writeStringField("norm", value.getNorm());
//        jgen.writeEndObject();
    }

}
