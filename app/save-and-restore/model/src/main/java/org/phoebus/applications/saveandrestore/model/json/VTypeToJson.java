/**
 * Copyright (C) 2010-18 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.applications.saveandrestore.model.json;

import org.epics.vtype.VType;

import javax.json.JsonObject;

/**
 * Utility to serialize and de-serialize vTypes to and from JSON objects.
 * These methods convert vTypes to and from standard JSONP objects. One
 * can then use the standard library to serialize/de-serialize text streams.
 *
 * @author carcassi
 */
public class VTypeToJson {

    /**
     * Converts the given JsonObject to a vType.
     *
     * @param json a JSON object
     * @return the corresponding vType
     */
    public static VType toVType(JsonObject json) {
        return VTypeToJsonV1.toVType(json);
    }

    /**
     * Converts the given vType to a JsonObject.
     *
     * @param vType a vType
     * @return the corresponding JsonObject
     */
    public static JsonObject toJson(VType vType) {
        return VTypeToJsonV1.toJson(vType);
    }
}
