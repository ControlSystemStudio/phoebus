/*
 * Copyright (C) 2018 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.saveandrestore.model.json;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.json.Json;
import javax.json.JsonReader;

import org.epics.vtype.VType;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.epics.vtype.json.VTypeToJson;

/**
 * Custom JSON deserializer for VTypes.
 * @author georgweiss
 * Created 30 Nov 2018
 */
public class VTypeDeserializer extends JsonDeserializer<VType> {
	
	@Override 
    public VType deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
		String valueAsJson = jsonParser.getCodec().readTree(jsonParser).toString();
		JsonReader jsonReader = null;
		try {
			jsonReader = Json.createReader(new ByteArrayInputStream(valueAsJson.getBytes()));
			return VTypeToJson.toVType(jsonReader.readObject());
		} 
		finally {
			if(jsonReader != null) {
				jsonReader.close();
			}
		}
    }
}
