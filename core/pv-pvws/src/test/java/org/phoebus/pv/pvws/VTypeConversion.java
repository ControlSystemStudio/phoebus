package org.phoebus.pv.pvws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.epics.vtype.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.phoebus.pv.pvws.models.pv.PvwsData;
import org.phoebus.pv.pvws.utils.pv.VArrDecoder;
import org.phoebus.pv.pvws.utils.pv.toVType;

import java.util.stream.Stream;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;


import java.util.Base64;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class VTypeConversion {


    private static final ObjectMapper mapper = new ObjectMapper();

    static Stream<TestCase> vtypeProvider() {
        return Stream.of(
                new TestCase("VDouble", "-3.9879211044535623e-13", VDouble.class),
                new TestCase("VFloat", "1.23", VFloat.class),
                new TestCase("VInt", "42", VInt.class),
                new TestCase("VLong", "123456789", VLong.class),
                new TestCase("VShort", "12", VShort.class),
                new TestCase("VByte", "7", VByte.class),
                new TestCase("VString", "\"hello\"", VString.class),
                new TestCase("VBoolean", "true", VBoolean.class),
                new TestCase("VDoubleArray", "\"xGDmkvzZC0BAq5qM2wzsv5RVPJoOJhDAclqZc/QfE0DQVwyThmfqvweT7jrRbwnA5P6Doq5UEcBofbejMXPwv5w59JqMuRJAovbbUNkkAUAmEwBR4RgSwJgwya4RYPQ/5IISWSJ8DcDY+1SCdHoJQDatWWpVRATAIk2TqBJsAMDEEvWipZPwv1AwRuPQ1eQ/qki27bRhA0CIxVSuHFwMQDCRJaSBdfE/nBEX3yUSEkAuwKcwWigDQPTBzhJDEQ7AxNftYwxNCEC6b0f/9wEAwOjHU4aZ3ANAofqkJgQeDMD4eqSywV4HwJAkDcPwgQRAiHpDeE3tDkDgTa/LTXwIQNxnDeE55vI//vJvb80zB0B21HvDS6YDwCDl9RGvFBDAcl//omAUAkDYMAhtH3f0vzAYj2AP6e8/lvQeGqvr878EdurHqeXxv7j+5F5KFPE/IDRdtid//7/408VyW2cMQFLSBm/EagHAMPQDAlOgEUBbZjRnDUESwLimeh96j/c/yN2+eH9gDUAQUjiyNJwBQCLwiL2pj/O/F2lHjsxfCcC1pmRoLwkRwMQ1hLZ4DwdAbGeQycYADUAu3U7BR+X2v5Tse39lcv8/NdAOTh5EE8Dsri7i/EABwLQMt77PUxFA1gbfxPRJE8CIgDiVqMgKwMbFa9wnWhPAcDlo7pcABEDgx2Clw08JQID1K7qoZgnAYP7UWDA5yz/NNbUzSeARwKC0QP9HONA/isv1QQnRB8DgCz7jCEnbP0wmb1THZQtAzHziz/VeE0Csyd6LkikAQJBI7hauiOg/kLbiMnoo1b9o8TCiodsQQIxYhKOmxBNA/IIZxBQSCUBU5jum1O4JQM0RxDym1hLAtxCuBRhaEcAE5SuXNKULQDgRO7RIluW/ekd+7F2mA8C6vgadaM0SQIEeybf7FBDAxsSe+mlR+79ot0hbyZUBQECSv8T/x+q/gFqLLi5XD8A/o7rSNoQGwADhyppXAqo/yM2DdvpzEkCAWn7L9o+jPzDCP3IW7dw/uLdy/dZr879kRiqSOXYIwHwgit3H4QRAsLtRCo3vB8A=\"", VDoubleArray.class),
                new TestCase("VStringArray", "[\"a\",\"b\",\"c\"]", VStringArray.class),
                new TestCase("VShortArray", "\"AAoAFAAe\"", VShortArray.class),
                new TestCase("VFloatArray", "\"P4zM1APAzM0=\"", VFloatArray.class),
                new TestCase("VIntArray", "\"AAAAGQAAAIAAAAM\"", VIntArray.class),
                new TestCase("VByteArray", "\"AQID\"", VByteArray.class)



                );
    }

    @ParameterizedTest
    @MethodSource("vtypeProvider")
    public void testAllVTypes(TestCase testCase) throws JsonProcessingException {
        String message = """
                {
                  "pv": "sim://test",
                  "readonly": true,
                  "type": "update",
                  "vtype": "%s",
                  "units": "a.u.",
                  "description": null,
                  "precision": 2,
                  "min": -5,
                  "max": 5,
                  "warn_low": -3,
                  "warn_high": 3,
                  "alarm_low": -4,
                  "alarm_high": 4,
                  "severity": "NONE",
                  "value": %s,
                  "seconds": 1754658145,
                  "nanos": 318849400
                }
                """.formatted(testCase.vtypeName, testCase.valueJson);


        message = switch (testCase.vtypeName) {
            case "VDoubleArray" -> message.replaceFirst("\"value\":", "\"b64dbl\":");
            case "VFloatArray" -> message.replaceFirst("\"value\":", "\"b64flt\":");
            case "VIntArray" -> message.replaceFirst("\"value\":", "\"b64int\":");
            case "VShortArray" -> message.replaceFirst("\"value\":", "\"b64srt\":");
            case "VByteArray" -> message.replaceFirst("\"value\":", "\"b64byt\":");
            default -> message;
        };

        JsonNode node = mapper.readTree(message);


        PvwsData pvObj = mapper.treeToValue(node, PvwsData.class);

        VArrDecoder.decodeArrValue(node, pvObj);


        VType vVal = toVType.convert(pvObj);

        Assertions.assertNotNull(vVal);
        //assertInstanceOf(testCase.expectedClass, vVal, "Wrong type for " + testCase.vtypeName);

        assertInstanceOf(testCase.expectedClass, vVal);

    }

    private record TestCase(String vtypeName, String valueJson, Class<? extends VType> expectedClass) {
    }

}
