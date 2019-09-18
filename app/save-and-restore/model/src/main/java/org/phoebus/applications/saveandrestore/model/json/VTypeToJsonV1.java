/**
 * Copyright (C) 2010-18 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.applications.saveandrestore.model.json;

import java.util.List;
import javax.json.JsonObject;
import org.epics.util.array.ListNumber;
import org.epics.util.number.UByte;
import org.epics.util.number.UInteger;
import org.epics.util.number.ULong;
import org.epics.util.number.UShort;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.VEnum;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
/**
 *
 * @author carcassi
 */
public class VTypeToJsonV1
{
    static VType toVType(JsonObject json) {
        switch(typeNameOf(json)) {
            case "VDouble":
            case "VFloat":
            case "VULong":
            case "VLong":
            case "VUInt":
            case "VInt":
            case "VUShort":
            case "VShort":
            case "VUByte":
            case "VByte":
                return toVNumber(json);
            case "VDoubleArray":
            case "VFloatArray":
            case "VULongArray":
            case "VLongArray":
            case "VUIntArray":
            case "VIntArray":
            case "VUShortArray":
            case "VShortArray":
            case "VUByteArray":
            case "VByteArray":
                return toVNumberArray(json);
            case "VString":
                return toVString(json);
            case "VEnum":
                return toVEnum(json);
            default:
                throw new UnsupportedOperationException("Not implemented yet");
        }
    }

    static String typeNameOf(JsonObject json) {
        JsonObject type = json.getJsonObject("type");
        if (type == null) {
            return null;
        }
        return type.getString("name");
    }

    static JsonObject toJson(VType vType) {
        if (vType instanceof VNumber) {
            return toJson((VNumber) vType);
        } else if (vType instanceof VNumberArray) {
            return toJson((VNumberArray) vType);
        } else if (vType instanceof VString) {
            return toJson((VString) vType);
        } else if (vType instanceof VEnum) {
            return toJson((VEnum) vType);
        }
        throw new UnsupportedOperationException("Not implemented yet");
    }

    static VNumber toVNumber(JsonObject json) {
        VTypeJsonMapper mapper = new VTypeJsonMapper(json);
        Number value;
        switch(mapper.getTypeName()) {
            case "VDouble":
                value = mapper.getJsonNumber("value").doubleValue();
                break;
            case "VFloat":
                value = (float) mapper.getJsonNumber("value").doubleValue();
                break;
            case "VULong":
                value = new ULong(mapper.getJsonNumber("value").longValue());
                break;
            case "VLong":
                value = (long) mapper.getJsonNumber("value").longValue();
                break;
            case "VUInt":
                value = new UInteger(mapper.getJsonNumber("value").intValue());
                break;
            case "VInt":
                value = (int) mapper.getJsonNumber("value").intValue();
                break;
            case "VUShort":
                value = new UShort((short) mapper.getJsonNumber("value").intValue());
                break;
            case "VShort":
                value = (short) mapper.getJsonNumber("value").intValue();
                break;
            case "VUByte":
                value = new UByte((byte) mapper.getJsonNumber("value").intValue());
                break;
            case "VByte":
                value = (byte) mapper.getJsonNumber("value").intValue();
                break;
            default:
                throw new UnsupportedOperationException("Not implemented yet");
        }
        return VNumber.of(value, mapper.getAlarm(), mapper.getTime(), mapper.getDisplay());
    }

    static VString toVString(JsonObject json) {
        VTypeJsonMapper mapper = new VTypeJsonMapper(json);
        return VString.of(mapper.getString("value"), mapper.getAlarm(), mapper.getTime());
    }

    static VEnum toVEnum(JsonObject json) {
        VTypeJsonMapper mapper = new VTypeJsonMapper(json);
        List<String> labels = mapper.getJsonObject("enum").getListString("labels");
        return VEnum.of(mapper.getInt("value"), EnumDisplay.of(labels), mapper.getAlarm(), mapper.getTime());
    }

    static VNumberArray toVNumberArray(JsonObject json) {
        VTypeJsonMapper mapper = new VTypeJsonMapper(json);
        ListNumber value;
        switch(mapper.getTypeName()) {
            case "VDoubleArray":
                value = mapper.getListDouble("value");
                break;
            case "VFloatArray":
                value = mapper.getListFloat("value");
                break;
            case "VULongArray":
                value = mapper.getListULong("value");
                break;
            case "VLongArray":
                value = mapper.getListLong("value");
                break;
            case "VUIntArray":
                value = mapper.getListUInteger("value");
                break;
            case "VIntArray":
                value = mapper.getListInt("value");
                break;
            case "VUShortArray":
                value = mapper.getListUShort("value");
                break;
            case "VShortArray":
                value = mapper.getListShort("value");
                break;
            case "VUByteArray":
                value = mapper.getListUByte("value");
                break;
            case "VByteArray":
                value = mapper.getListByte("value");
                break;
            default:
                throw new UnsupportedOperationException("Not implemented yet");
        }
        return VNumberArray.of(value, mapper.getAlarm(), mapper.getTime(), mapper.getDisplay());
    }

    static JsonObject toJson(VNumber vNumber) {
        return new JsonVTypeBuilder()
                .addType(vNumber)
                .addObject("value", vNumber.getValue())
                .addAlarm(vNumber.getAlarm())
                .addTime(vNumber.getTime())
                .addDisplay(vNumber.getDisplay())
                .build();
    }

    static JsonObject toJson(VNumberArray vNumberArray) {
        return new JsonVTypeBuilder()
                .addType(vNumberArray)
                .addObject("value", vNumberArray.getData())
                .addAlarm(vNumberArray.getAlarm())
                .addTime(vNumberArray.getTime())
                .addDisplay(vNumberArray.getDisplay())
                .build();
    }

    static JsonObject toJson(VString vString) {
        return new JsonVTypeBuilder()
                .addType(vString)
                .add("value", vString.getValue())
                .addAlarm(vString.getAlarm())
                .addTime(vString.getTime())
                .build();
    }

    static JsonObject toJson(VEnum vEnum) {
        return new JsonVTypeBuilder()
                .addType(vEnum)
                .add("value", vEnum.getIndex())
                .addAlarm(vEnum.getAlarm())
                .addTime(vEnum.getTime())
                .addEnum(vEnum)
                .build();
    }
}
