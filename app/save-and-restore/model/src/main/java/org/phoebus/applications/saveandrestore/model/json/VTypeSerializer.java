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

import org.epics.vtype.VType;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;
import org.epics.vtype.json.VTypeToJson;

/**
 * Custom JSON serializer for VTypes.	
 * @author georgweiss 
 * Created 30 Nov 2018
 */
public class VTypeSerializer extends StdSerializer<VType> {

	public VTypeSerializer() {
		super(VType.class);
	}

	@Override
	public void serialize(VType vType, JsonGenerator gen, SerializationContext serializers) {
		String s = VTypeToJson.toJson(vType).toString();
		gen.writeRawValue(s);
	}
}
