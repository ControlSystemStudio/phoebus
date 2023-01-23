/*
 * Copyright (C) 2023 European Spallation Source ERIC.
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
package org.epics.pva.combined;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.epics.pva.client.MonitorListener;
import org.epics.pva.client.PVAChannel;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ServerClientTest {

    private static final PVAServer server = testServer();
    private static final PVAClient client = testClient();

    private static final long tearDownTimeOut = 200;
    private static final long betweenEventTimeOut = 200;
    private static final long afterLastEventTimeOut = 1000;

    private static PVAServer testServer() {
        try {
            return new PVAServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static PVAClient testClient() {
        try {
            return new PVAClient();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @AfterAll
    public static void tearDown() {
        if (server != null) {
            server.close();
        }
        if (client != null) {
            client.close();
        }

        // Wait for closes to finish
        try {
            TimeUnit.MILLISECONDS.sleep(tearDownTimeOut);
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Provides the input data for the test cases.
     * <p>
     * First goes over every scalar type, converting the first array of the
     * generated fake data to the PVAccess type.
     * Second go over every waveform type, converting each array of the
     * generated fake data to the PVAccess type.
     * 
     * @return input data
     */
    public static Collection<Object[]> data() {
        List<List<Double>> fakeData = FakeDataUtil.fakeData(3, 1.1, 3);
        return Arrays.asList(new Object[][] {
                {
                        fakeData.get(0).stream().map((d) -> new PVAString(PVAScalar.VALUE_NAME_STRING, d.toString()))
                                .collect(Collectors.toList()) },
                {
                        fakeData.get(0).stream().map(Double::shortValue)
                                .map((s) -> new PVAShort(PVAScalar.VALUE_NAME_STRING, false, s))
                                .collect(Collectors.toList()) },
                {
                        fakeData.get(0).stream().map(Double::floatValue)
                                .map((f) -> new PVAFloat(PVAScalar.VALUE_NAME_STRING, f))
                                .collect(Collectors.toList()) },
                {
                        fakeData.get(0).stream().map(Double::byteValue)
                                .map((b) -> new PVAByte(PVAScalar.VALUE_NAME_STRING, false, b))
                                .collect(Collectors.toList()) },
                {
                        fakeData.get(0).stream().map(Double::intValue)
                                .map((i) -> new PVAInt(PVAScalar.VALUE_NAME_STRING, false, i))
                                .collect(Collectors.toList()) },
                {
                        fakeData.get(0).stream()
                                .map((d) -> new PVADouble(PVAScalar.VALUE_NAME_STRING, d))
                                .collect(Collectors.toList()) },
                {
                        fakeData.stream()
                                .map((dArray) -> new PVAStringArray(PVAScalar.VALUE_NAME_STRING,
                                        dArray.stream().map(Object::toString).toArray(String[]::new)))
                                .collect(Collectors.toList()) },
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
                                }).collect(Collectors.toList()) },
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
                                }).collect(Collectors.toList()) },
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
                                }).collect(Collectors.toList()) },
                {
                        fakeData.stream()
                                .map((dArray) -> new PVAIntArray(PVAScalar.VALUE_NAME_STRING, false,
                                        dArray.stream().mapToInt(Double::intValue).toArray()))
                                .collect(Collectors.toList()) },
                {
                        fakeData.stream().map((dArray) -> new PVADoubleArray(PVAScalar.VALUE_NAME_STRING,
                                dArray.stream().mapToDouble((d) -> d).toArray()))
                                .collect(Collectors.toList()) },
        });
    }

    static PVAStructure buildPVAStructure(String pvName, Instant instant, PVAData value, String pvDescription) {
        PVAScalar.Builder<PVAData> builder = new PVAScalar.Builder<>();
        builder.name(pvName);
        builder.value(value);
        builder.description(new PVAString("description",
                pvDescription));
        builder.alarm(new PVAAlarm(PVAAlarm.AlarmSeverity.MINOR, PVAAlarm.AlarmStatus.DEVICE,
                pvDescription + "alarm message"));
        builder.timeStamp(new PVATimeStamp(instant));
        builder.display(new PVADisplay(0, 1, pvDescription + "display", "units", 4, Form.STRING));
        builder.control(new PVAControl(0, 1, 1));
        try {
            return builder.build();
        } catch (PVAScalarValueNameException | PVAScalarDescriptionNameException e) {
            e.printStackTrace();
            fail();
        }
        return null;
    }

    /**
     * Test for setting up a pv in a server with all data structures.
     * Then sending some fake data.
     * Then in a client receiving the data.
     * Then assert sent and received data is the same.
     */
    @ParameterizedTest
    @MethodSource("data")
    public <S extends PVAData> void testSinglePV(List<S> inputData) {
        String pvName = "PV:" + inputData.get(0).getClass().getSimpleName() + ":" + UUID.randomUUID();

        S fakeData = inputData.get(0);
        String pvDescription = fakeData.getClass().getSimpleName() + ServerClientTest.class.getName() + " test on "
                + pvName;
        Instant instant = Instant.now();
        PVAStructure testPV = buildPVAStructure(pvName, instant, fakeData, pvDescription);

        HashMap<Instant, PVAData> sentData = new HashMap<>();
        sentData.put(instant, testPV.get(PVAScalar.VALUE_NAME_STRING));

        assert server != null;
        ServerPV serverPV = server.createPV(pvName, testPV);

        AtomicReference<HashMap<Instant, PVAData>> receivedData = new AtomicReference<>();
        receivedData.set(new HashMap<>());
        MonitorListener listener = (ch, changes, overruns, data) -> receivedData.getAndUpdate((l) -> {
            Instant recInstant = PVAStructures.getTime(data.get(PVATimeStamp.TIMESTAMP_NAME_STRING));
            PVAData recData = data.get(PVAScalar.VALUE_NAME_STRING);
            l.put(recInstant, recData);
            return l;
        });

        assert client != null;
        PVAChannel channel = client.getChannel(pvName);
        try {
            channel.connect().get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        try {
            channel.subscribe(pvDescription, listener);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        // Wait for subscribe to get the setup of the pv as an event.
        try {
            TimeUnit.MILLISECONDS.sleep(betweenEventTimeOut);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    
        for (S input : inputData) { // TODO Sometimes receiving the setup of the pv as an event and sometimes not.
            assert testPV != null;
            S newValue = testPV.get(PVAScalar.VALUE_NAME_STRING);
            try {
                newValue.setValue(input);
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            PVATimeStamp timeStamp = testPV.get(PVATimeStamp.TIMESTAMP_NAME_STRING);
            instant = Instant.now();
            timeStamp.set(instant);
            sentData.put(instant, newValue);
            try {
                serverPV.update(testPV);
            } catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
            try {
                // Sleep to allow time for client to receive requests
                TimeUnit.MILLISECONDS.sleep(betweenEventTimeOut);
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }

        // Wait for messages to clear
        try {
            TimeUnit.MILLISECONDS.sleep(afterLastEventTimeOut);
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        serverPV.close();
        channel.close();

        HashMap<Instant, PVAData> receivedDataCopy = receivedData.get();

        System.out.println("Data out " + sentData);
        System.out.println("Data in  " + receivedDataCopy);
        assertThat(receivedDataCopy, equalTo(sentData));
    }

}
