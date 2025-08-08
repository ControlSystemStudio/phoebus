package org.phoebus.pv.pvws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

public class VTypeConversion {


    /*private ObjectMapper mapper;

    @Before
    public void setUp() {
        // This runs before each @Test method
        mapper = new ObjectMapper();
    }

    @Test
    public void VDouble() throws JsonProcessingException {
        String message = """
                {
                "pv":"sim://sine",
                "readonly":true,
                "type":"update",
                "vtype":"VDouble",
                "units":"a.u.",
                "description":null,
                "precision":2,
                "min":-5,
                "max":5,
                "warn_low":-3,
                "warn_high":3,
                "alarm_low":-4,
                "alarm_high":4,
                "severity":"NONE",
                "value":-3.9879211044535623e-13,
                "seconds":1754658145,
                "nanos":318849400 }
                """;

        JsonNode node = mapper.readTree(message);
        PvwsData pvObj = mapper.treeToValue(node, PvwsData.class);

        VType vVal = toVType.convert(pvObj);

        assertNotNull(vVal);
        assertInstanceOf(VDouble.class, vVal);
    }*/


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
                new TestCase("VDoubleArray", "[1.1,2.2,3.3]", VDoubleArray.class),
                new TestCase("VStringArray", "[\"a\",\"b\",\"c\"]", VStringArray.class)
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


        JsonNode node = mapper.readTree(message);
        PvwsData pvObj = mapper.treeToValue(node, PvwsData.class);
        VArrDecoder.decodeArrValue(node, pvObj);


        VType vVal = toVType.convert(pvObj);

        Assertions.assertNotNull(vVal);
        assertInstanceOf(testCase.expectedClass, vVal, "Wrong type for " + testCase.vtypeName);
    }

    private record TestCase(String vtypeName, String valueJson, Class<? extends VType> expectedClass) {}

}
