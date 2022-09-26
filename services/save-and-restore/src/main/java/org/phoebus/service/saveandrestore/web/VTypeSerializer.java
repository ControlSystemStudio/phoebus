/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.phoebus.service.saveandrestore.web;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.epics.vtype.VType;
import org.epics.vtype.json.VTypeToJson;
import org.springframework.boot.jackson.JsonComponent;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@JsonComponent
public class VTypeSerializer {

    public static class Serialize extends JsonSerializer<VType> {
        @Override
        public void serialize(VType vType, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeRawValue(VTypeToJson.toJson(vType).toString());
        }
    }

    public static class Deserialize extends JsonDeserializer<VType> {
        @Override
        public VType deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException {
            String valueAsJson = jsonParser.getCodec().readTree(jsonParser).toString();
            JsonReader jsonReader = null;
            try {
                jsonReader = Json.createReader(new ByteArrayInputStream(valueAsJson.getBytes()));
                JsonObject jsonObject = jsonReader.readObject();
                return VTypeToJson.toVType(jsonObject);
            }
            finally {
                if(jsonReader != null) {
                    jsonReader.close();
                }
            }
        }
    }
}
