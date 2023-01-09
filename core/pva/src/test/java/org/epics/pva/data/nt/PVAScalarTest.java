package org.epics.pva.data.nt;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.epics.pva.data.PVADouble;
import org.epics.pva.data.PVAString;
import org.junit.Test;

public class PVAScalarTest {
        @Test
        public void testDouble() throws PVAScalarValueNameException, PVAScalarDescriptionNameException {
                PVAScalar<PVADouble> doubleScalar = (new PVAScalar.Builder<PVADouble>()).name("pvDoubleName")
                                .value(new PVADouble("value", 1.1)).build();
                assertEquals(new PVADouble("value", 1.1), doubleScalar.get("value"));
        }

        @Test
        public void testString() throws PVAScalarValueNameException, PVAScalarDescriptionNameException {
                PVAScalar<PVAString> stringScalar = (new PVAScalar.Builder<PVAString>()).name("pvStringName")
                                .value(new PVAString("value", "1.1")).build();
                assertEquals(new PVAString("value", "1.1"), stringScalar.get("value"));
        }

        @Test
        public void testPVAScalarValueNameException() {

                PVAScalar.Builder<PVAString> builder = (new PVAScalar.Builder<PVAString>()).name("pvName")
                                .value(new PVAString("notvalue", "the value"));

                assertThrows(PVAScalarValueNameException.class, () -> {
                        builder.build();
                });
        }

        @Test
        public void testPVAScalarDescriptionNameException() {

                PVAScalar.Builder<PVAString> builder = (new PVAScalar.Builder<PVAString>()).name("pvName")
                                .value(new PVAString("value", "the value"))
                                .description(new PVAString("notdescription"));

                assertThrows(PVAScalarDescriptionNameException.class, () -> {
                        builder.build();
                });
        }

        @Test
        public void smokeTestBuilders() {
                List<List<Double>> fakeData = FakeDataUtil.fakeData(100, 1.1, 10);
                String pvName = "pvName";
                fakeData.get(0).stream()
                                .map((d) -> {
                                        try {
                                                return PVAScalar.stringScalarBuilder(d.toString()).name(pvName).build();
                                        } catch (PVAScalarValueNameException | PVAScalarDescriptionNameException e) {
                                                e.printStackTrace();
                                                fail();
                                        }
                                        return null;
                                })
                                .toList();
                fakeData.get(0).stream().map(Double::shortValue)
                                .map((s) -> {
                                        try {
                                                return PVAScalar.shortScalarBuilder(false, s).name(pvName).build();
                                        } catch (PVAScalarValueNameException | PVAScalarDescriptionNameException e) {
                                                e.printStackTrace();
                                                fail();
                                        }
                                        return null;
                                })
                                .toList();
                fakeData.get(0).stream().map(Double::floatValue)
                                .map((f) -> {
                                        try {
                                                return PVAScalar.floatScalarBuilder(f).name(pvName).build();
                                        } catch (PVAScalarValueNameException | PVAScalarDescriptionNameException e) {
                                                e.printStackTrace();
                                                fail();
                                        }
                                        return null;
                                }).toList();
                fakeData.get(0).stream().map(Double::byteValue)
                                .map((b) -> {
                                        try {
                                                return PVAScalar.byteScalarBuilder(false, b).name(pvName).build();
                                        } catch (PVAScalarValueNameException | PVAScalarDescriptionNameException e) {
                                                e.printStackTrace();
                                                fail();
                                        }
                                        return null;
                                })
                                .toList();
                fakeData.get(0).stream().map(Double::intValue)
                                .map((i) -> {
                                        try {
                                                return PVAScalar.intScalarBuilder(i).name(pvName).build();
                                        } catch (PVAScalarValueNameException | PVAScalarDescriptionNameException e) {
                                                e.printStackTrace();
                                                fail();
                                        }
                                        return null;
                                }).toList();
                fakeData.get(0).stream().map(Double::doubleValue)
                                .map((d) -> {
                                        try {
                                                return PVAScalar.doubleScalarBuilder(d).name(pvName).build();
                                        } catch (PVAScalarValueNameException | PVAScalarDescriptionNameException e) {
                                                e.printStackTrace();
                                                fail();
                                        }
                                        return null;
                                }).toList();
                fakeData.stream()
                                .map((dArray) -> {
                                        try {
                                                return PVAScalar.stringArrayScalarBuilder(
                                                                dArray.stream().map((d) -> d.toString()).toArray(String[]::new))
                                                                .name(pvName).build();
                                        } catch (PVAScalarValueNameException | PVAScalarDescriptionNameException e) {
                                                e.printStackTrace();
                                                fail();
                                        }
                                        return null;
                                })
                                .toList();
                fakeData.stream()
                                .map((dArray) -> {
                                        short[] array = new short[dArray.size()];
                                        int count = 0;
                                        for (Double d : dArray) {
                                                array[count] = d.shortValue();
                                                count++;
                                        }
                                        try {
                                                return PVAScalar.shortArrayScalarBuilder(false, array).name(pvName)
                                                                .build();
                                        } catch (PVAScalarValueNameException | PVAScalarDescriptionNameException e) {
                                                e.printStackTrace();
                                                fail();
                                        }
                                        return null;
                                }).toList();
                fakeData.stream()
                                .map((dArray) -> {
                                        float[] array = new float[dArray.size()];
                                        int count = 0;
                                        for (Double d : dArray) {
                                                array[count] = d.floatValue();
                                                count++;
                                        }
                                        try {
                                                return PVAScalar.floatArrayScalarBuilder(array).name(pvName).build();
                                        } catch (PVAScalarValueNameException | PVAScalarDescriptionNameException e) {
                                                e.printStackTrace();
                                                fail();
                                        }
                                        return null;
                                }).toList();
                fakeData.stream()
                                .map((dArray) -> {
                                        byte[] array = new byte[dArray.size()];
                                        int count = 0;
                                        for (Double d : dArray) {
                                                array[count] = d.byteValue();
                                                count++;
                                        }
                                        try {
                                                return PVAScalar.byteArrayScalarBuilder(false, array).name(pvName)
                                                                .build();
                                        } catch (PVAScalarValueNameException | PVAScalarDescriptionNameException e) {
                                                e.printStackTrace();
                                                fail();
                                        }
                                        return null;
                                }).toList();
                fakeData.stream()
                                .map((dArray) -> {
                                        try {
                                                return PVAScalar.intArrayScalarBuilder(false,
                                                                dArray.stream().mapToInt((d) -> d.intValue()).toArray())
                                                                .name(pvName).build();
                                        } catch (PVAScalarValueNameException | PVAScalarDescriptionNameException e) {
                                                e.printStackTrace();
                                                fail();
                                        }
                                        return null;
                                })
                                .toList();
                fakeData.stream().map((dArray) -> {
                        try {
                                return PVAScalar.doubleArrayScalarBuilder(
                                                dArray.stream().mapToDouble((d) -> d.doubleValue()).toArray()).name(pvName)
                                                .build();
                        } catch (PVAScalarValueNameException | PVAScalarDescriptionNameException e) {
                                e.printStackTrace();
                                fail();
                        }
                        return null;
                })
                                .toList();

        }

}
