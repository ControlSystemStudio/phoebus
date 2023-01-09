package org.epics.pva.combined;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.epics.pva.client.MonitorListener;
import org.epics.pva.client.PVAClient;
import org.epics.pva.data.PVAByte;
import org.epics.pva.data.PVAByteArray;
import org.epics.pva.data.PVAData;
import org.epics.pva.data.PVADouble;
import org.epics.pva.data.PVADoubleArray;
import org.epics.pva.data.PVAFloat;
import org.epics.pva.data.PVAFloatArray;
import org.epics.pva.data.PVAInt;
import org.epics.pva.data.PVAIntArray;
import org.epics.pva.data.PVAShort;
import org.epics.pva.data.PVAShortArray;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStringArray;
import org.epics.pva.data.PVAStructure;
import org.epics.pva.data.PVAStructures;
import org.epics.pva.data.nt.FakeDataUtil;
import org.epics.pva.data.nt.PVAAlarm;
import org.epics.pva.data.nt.PVAControl;
import org.epics.pva.data.nt.PVADisplay;
import org.epics.pva.data.nt.PVAScalar;
import org.epics.pva.data.nt.PVAScalarDescriptionNameException;
import org.epics.pva.data.nt.PVAScalarValueNameException;
import org.epics.pva.data.nt.PVATimeStamp;
import org.epics.pva.data.nt.PVADisplay.Form;
import org.epics.pva.server.PVAServer;
import org.epics.pva.server.ServerPV;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ServerClientTest {

    private PVAServer server;
    private PVAClient client;

    @BeforeEach
    public void setUp() throws Exception {
        server = new PVAServer();
        client = new PVAClient();
    }

    @AfterEach
    public void tearDown() throws Exception {
        server.close();
        client.close();
    }

    /**
     * Provides the input data for the test cases.
     * 
     * First goes over every scalar type, converting the first array of the
     * generated fake data to the PVAccess type.
     * Seconds goes over every waveform type, converting each array of the
     * generated fake data to the PVAccess type.
     * 
     * @return input data
     */
    public static Collection<Object[]> data() {
        List<List<Double>> fakeData = FakeDataUtil.fakeData(100, 1.1, 10);
        return Arrays.asList(new Object[][] {
                {
                        fakeData.get(0).stream().map((d) -> new PVAString(PVAScalar.VALUE_NAME_STRING, d.toString()))
                                .toList() },
                {
                        fakeData.get(0).stream().map(Double::shortValue)
                                .map((s) -> new PVAShort(PVAScalar.VALUE_NAME_STRING, false, s)).toList() },
                {
                        fakeData.get(0).stream().map(Double::floatValue)
                                .map((f) -> new PVAFloat(PVAScalar.VALUE_NAME_STRING, f)).toList() },
                {
                        fakeData.get(0).stream().map(Double::byteValue)
                                .map((b) -> new PVAByte(PVAScalar.VALUE_NAME_STRING, false, b)).toList() },
                {
                        fakeData.get(0).stream().map(Double::intValue)
                                .map((i) -> new PVAInt(PVAScalar.VALUE_NAME_STRING, false, i)).toList() },
                {
                        fakeData.get(0).stream().map(Double::doubleValue)
                                .map((d) -> new PVADouble(PVAScalar.VALUE_NAME_STRING, d)).toList() },
                {
                        fakeData.stream()
                                .map((dArray) -> new PVAStringArray(PVAScalar.VALUE_NAME_STRING,
                                        dArray.stream().map((d) -> d.toString()).toArray(String[]::new)))
                                .toList() },
                {
                        fakeData.stream()
                                .map((dArray) -> {
                                    short[] array = new short[dArray.size()];
                                    int count = 0;
                                    for (Double d : dArray) {
                                        array[count] = d.shortValue();
                                        count++;
                                    }
                                    return new PVAShortArray(PVAScalar.VALUE_NAME_STRING, false, array);
                                }).toList() },
                {
                        fakeData.stream()
                                .map((dArray) -> {
                                    float[] array = new float[dArray.size()];
                                    int count = 0;
                                    for (Double d : dArray) {
                                        array[count] = d.floatValue();
                                        count++;
                                    }
                                    return new PVAFloatArray(PVAScalar.VALUE_NAME_STRING, array);
                                }).toList() },
                {
                        fakeData.stream()
                                .map((dArray) -> {
                                    byte[] array = new byte[dArray.size()];
                                    int count = 0;
                                    for (Double d : dArray) {
                                        array[count] = d.byteValue();
                                        count++;
                                    }
                                    return new PVAByteArray(PVAScalar.VALUE_NAME_STRING, false, array);
                                }).toList() },
                {
                        fakeData.stream()
                                .map((dArray) -> new PVAIntArray(PVAScalar.VALUE_NAME_STRING, false,
                                        dArray.stream().mapToInt((d) -> d.intValue()).toArray()))
                                .toList() },
                {
                        fakeData.stream().map((dArray) -> new PVADoubleArray(PVAScalar.VALUE_NAME_STRING,
                                dArray.stream().mapToDouble((d) -> d.doubleValue()).toArray()))
                                .toList() },
        });
    }

    static PVAStructure buildPVAStructure(String pvName, Instant instant, PVAData value, String pvDescription) {
        PVAScalar.Builder<PVAData> builder = new PVAScalar.Builder<>();
        builder.name(pvName);
        builder.value(value);
        builder.description(new PVAString("description",
                pvDescription));
        builder.alarm(new PVAAlarm(1, 2,
                pvDescription + "alarm message"));
        builder.timeStamp(new PVATimeStamp(instant));
        builder.display(new PVADisplay(0, 1, pvDescription + "display", "units", 4, Form.STRING));
        builder.control(new PVAControl(0, 1, 1));
        try {
            return builder.build();
        } catch (PVAScalarValueNameException e) {
            e.printStackTrace();
            fail();
        } catch (PVAScalarDescriptionNameException e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    @ParameterizedTest
    @MethodSource("data")
    public <S extends PVAData> void testSinglePV(List<S> inputData) {
        String pvName = "PV:" + inputData.get(0).getClass().getSimpleName() + ":" + UUID.randomUUID().toString();

        var fakeData = inputData.get(0);
        String pvDescription = fakeData.getClass().getSimpleName() + ServerClientTest.class.getName() + " test on "
                + pvName;
        Instant instant = Instant.now();
        var instants = new ArrayList<>();
        instants.add(instant);
        PVAStructure testPV = buildPVAStructure(pvName, Instant.now(), fakeData, pvDescription);
        ServerPV serverPV = server.createPV(pvName, testPV);

        try {
            var ref = new AtomicReference<HashMap<Instant, PVAData>>();
            ref.set(new HashMap<>());
            MonitorListener listener = (ch, changes, overruns, data) -> {
                System.out.println("Got data " + data.get(PVAScalar.VALUE_NAME_STRING));
                ref.getAndUpdate((l) -> {
                    Instant recInstant = PVAStructures.getTime(data.get(PVATimeStamp.TIMESTAMP_NAME_STRING));
                    PVAData recData = data.get(PVAScalar.VALUE_NAME_STRING);
                    l.put(recInstant, recData);
                    return l;
                });
            };
            var channel = client.getChannel(pvName);
            channel.connect().get(5, TimeUnit.SECONDS);
            channel.subscribe(pvDescription, listener);

            var sentData = new HashMap<Instant, PVAData>();
            for (S input : inputData) {
                S newValue = testPV.get(PVAScalar.VALUE_NAME_STRING);
                newValue.setValue(input);
                PVATimeStamp timeStamp = testPV.get(PVATimeStamp.TIMESTAMP_NAME_STRING);
                instant = Instant.now();
                instants.add(instant);
                timeStamp.set(instant);
                sentData.put(instant, newValue);
                serverPV.update(testPV);
                TimeUnit.MILLISECONDS.sleep(10);
                System.out.println("Sent data " + testPV.get(PVAScalar.VALUE_NAME_STRING));
            }

            assertEquals(inputData.size(), ref.get().size());
            assertEquals(sentData, ref.get());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

}
