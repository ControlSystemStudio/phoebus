package org.phoebus.archive.reader.appliance;

import com.google.protobuf.ByteString;
import edu.stanford.slac.archiverappliance.PB.EPICSEvent;
import edu.stanford.slac.archiverappliance.PB.EPICSEvent.FieldValue;
import edu.stanford.slac.archiverappliance.PB.EPICSEvent.PayloadInfo;
import edu.stanford.slac.archiverappliance.PB.EPICSEvent.PayloadType;
import org.epics.archiverappliance.retrieval.client.EpicsMessage;
import org.epics.archiverappliance.retrieval.client.GenMsgIterator;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VNumber;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Functional tests for ApplianceValueIterator.extractData() across all supported payload types.
 *
 * Uses a minimal concrete subclass that exposes the protected extractData() method directly,
 * avoiding the need for real archive network connections.
 */
class ApplianceValueIteratorExtractDataTest {

    // ---- minimal test subclass ----

    private static class DataExtractIterator extends ApplianceValueIterator {
        DataExtractIterator(ApplianceArchiveReader reader, GenMsgIterator stream) {
            super(reader, "TEST:PV", Instant.EPOCH, Instant.EPOCH);
            this.mainStream = stream;
            this.mainIterator = stream.iterator();
        }

        VType extract(EpicsMessage msg) {
            return extractData(msg);
        }
    }

    // ---- helpers ----

    private static GenMsgIterator streamOfType(PayloadType type, FieldValue... headers) {
        GenMsgIterator s = mock(GenMsgIterator.class);
        when(s.iterator()).thenReturn(Collections.emptyIterator());
        PayloadInfo.Builder b = PayloadInfo.newBuilder().setType(type);
        for (FieldValue h : headers) b.addHeaders(h);
        when(s.getPayLoadInfo()).thenReturn(b.buildPartial());
        return s;
    }

    private static DataExtractIterator iteratorWithStream(GenMsgIterator stream) {
        FakeDataRetrieval dr = new FakeDataRetrieval(stream);
        return new DataExtractIterator(new FakeApplianceArchiveReader(dr), stream);
    }

    private static DataExtractIterator iteratorForType(PayloadType type, FieldValue... headers) {
        return iteratorWithStream(streamOfType(type, headers));
    }

    /** Mock EpicsMessage returning the given numeric value with no alarm and a current timestamp. */
    private static EpicsMessage numericMsg(Number value) {
        EpicsMessage msg = mock(EpicsMessage.class);
        when(msg.getNumberValue()).thenReturn(value);
        when(msg.getSeverity()).thenReturn(0);
        when(msg.getStatus()).thenReturn(0);
        when(msg.getTimestamp()).thenReturn(new Timestamp(System.currentTimeMillis()));
        return msg;
    }

    /** EpicsMessage backed by a real protobuf message object (needed for getMessage().getField()). */
    private static EpicsMessage pbMsg(com.google.protobuf.Message pb) {
        EpicsMessage msg = mock(EpicsMessage.class);
        when(msg.getMessage()).thenReturn(pb);
        when(msg.getSeverity()).thenReturn(0);
        when(msg.getStatus()).thenReturn(0);
        when(msg.getTimestamp()).thenReturn(new Timestamp(System.currentTimeMillis()));
        return msg;
    }

    // ---- scalar numeric types ----

    @Test
    void scalarDoubleYieldsVNumber() {
        DataExtractIterator iter = iteratorForType(PayloadType.SCALAR_DOUBLE);
        VType result = iter.extract(numericMsg(42.5));
        assertInstanceOf(VNumber.class, result);
        assertEquals(42.5, ((VNumber) result).getValue().doubleValue(), 0.001);
    }

    @Test
    void scalarIntYieldsVNumber() {
        DataExtractIterator iter = iteratorForType(PayloadType.SCALAR_INT);
        VType result = iter.extract(numericMsg(7));
        assertInstanceOf(VNumber.class, result);
        assertEquals(7, ((VNumber) result).getValue().intValue());
    }

    @Test
    void scalarByteYieldsVNumber() {
        DataExtractIterator iter = iteratorForType(PayloadType.SCALAR_BYTE);
        VType result = iter.extract(numericMsg(3));
        assertInstanceOf(VNumber.class, result);
    }

    @Test
    void scalarShortYieldsVNumber() {
        DataExtractIterator iter = iteratorForType(PayloadType.SCALAR_SHORT);
        VType result = iter.extract(numericMsg(100));
        assertInstanceOf(VNumber.class, result);
    }

    @Test
    void scalarFloatYieldsVNumber() {
        DataExtractIterator iter = iteratorForType(PayloadType.SCALAR_FLOAT);
        VType result = iter.extract(numericMsg(1.5f));
        assertInstanceOf(VNumber.class, result);
    }

    // ---- enum ----

    @Test
    void scalarEnumYieldsVEnum() {
        DataExtractIterator iter = iteratorForType(PayloadType.SCALAR_ENUM,
                FieldValue.newBuilder().setName("ENUM_0").setVal("Off").build(),
                FieldValue.newBuilder().setName("ENUM_1").setVal("On").build());
        VType result = iter.extract(numericMsg(1));
        assertInstanceOf(VEnum.class, result);
        assertEquals(1, ((VEnum) result).getIndex());
    }

    // ---- alarm ----

    @Test
    void alarmSeverityPreserved() {
        DataExtractIterator iter = iteratorForType(PayloadType.SCALAR_DOUBLE);
        EpicsMessage msg = mock(EpicsMessage.class);
        when(msg.getNumberValue()).thenReturn(0.0);
        when(msg.getSeverity()).thenReturn(2); // 2 = MAJOR
        when(msg.getStatus()).thenReturn(0);
        when(msg.getTimestamp()).thenReturn(new Timestamp(System.currentTimeMillis()));
        VType result = iter.extract(msg);
        assertInstanceOf(VNumber.class, result);
        assertEquals(AlarmSeverity.MAJOR, ((VNumber) result).getAlarm().getSeverity());
    }

    // ---- display metadata ----

    @Test
    void displayHeadersExtracted() {
        DataExtractIterator iter = iteratorForType(PayloadType.SCALAR_DOUBLE,
                FieldValue.newBuilder().setName("EGU").setVal("mA").build(),
                FieldValue.newBuilder().setName("LOPR").setVal("0.0").build(),
                FieldValue.newBuilder().setName("HOPR").setVal("100.0").build());
        VType result = iter.extract(numericMsg(50.0));
        assertInstanceOf(VNumber.class, result);
        VNumber vn = (VNumber) result;
        assertEquals("mA", vn.getDisplay().getUnit());
        assertEquals(0.0,   vn.getDisplay().getDisplayRange().getMinimum(), 0.001);
        assertEquals(100.0, vn.getDisplay().getDisplayRange().getMaximum(), 0.001);
    }

    // ---- enum label ordering ----

    @Test
    void enumLabelsExtractedAndSorted() {
        // Labels supplied out-of-order — extractData must return them sorted by index number
        DataExtractIterator iter = iteratorForType(PayloadType.SCALAR_ENUM,
                FieldValue.newBuilder().setName("ENUM_2").setVal("Error").build(),
                FieldValue.newBuilder().setName("ENUM_0").setVal("Off").build(),
                FieldValue.newBuilder().setName("ENUM_1").setVal("On").build());
        VType result = iter.extract(numericMsg(0));
        assertInstanceOf(VEnum.class, result);
        assertEquals("Off",   ((VEnum) result).getDisplay().getChoices().get(0));
        assertEquals("On",    ((VEnum) result).getDisplay().getChoices().get(1));
        assertEquals("Error", ((VEnum) result).getDisplay().getChoices().get(2));
    }

    // ---- string and waveform types via real protobuf messages ----

    @Test
    void scalarStringYieldsVString() {
        EPICSEvent.ScalarString pb = EPICSEvent.ScalarString.newBuilder()
                .setSecondsintoyear(0).setNano(0).setVal("test-value").build();
        DataExtractIterator iter = iteratorForType(PayloadType.SCALAR_STRING);
        VType result = iter.extract(pbMsg(pb));
        assertInstanceOf(VString.class, result);
        assertEquals("test-value", ((VString) result).getValue());
    }

    @Test
    void waveformDoubleYieldsVDoubleArray() {
        EPICSEvent.VectorDouble pb = EPICSEvent.VectorDouble.newBuilder()
                .setSecondsintoyear(0).setNano(0)
                .addVal(1.0).addVal(2.0).addVal(3.0).build();
        DataExtractIterator iter = iteratorForType(PayloadType.WAVEFORM_DOUBLE);
        VType result = iter.extract(pbMsg(pb));
        assertInstanceOf(VDoubleArray.class, result);
        VDoubleArray vda = (VDoubleArray) result;
        assertEquals(3, vda.getData().size());
        assertEquals(1.0, vda.getData().getDouble(0), 0.001);
        assertEquals(2.0, vda.getData().getDouble(1), 0.001);
        assertEquals(3.0, vda.getData().getDouble(2), 0.001);
    }

    @Test
    void waveformIntYieldsVIntArray() {
        EPICSEvent.VectorInt pb = EPICSEvent.VectorInt.newBuilder()
                .setSecondsintoyear(0).setNano(0)
                .addVal(10).addVal(20).addVal(30).build();
        DataExtractIterator iter = iteratorForType(PayloadType.WAVEFORM_INT);
        VType result = iter.extract(pbMsg(pb));
        assertInstanceOf(VIntArray.class, result);
        VIntArray via = (VIntArray) result;
        assertEquals(3, via.getData().size());
        assertEquals(10, via.getData().getInt(0));
        assertEquals(20, via.getData().getInt(1));
        assertEquals(30, via.getData().getInt(2));
    }

    @Test
    void waveformByteYieldsVByteArray() {
        EPICSEvent.VectorChar pb = EPICSEvent.VectorChar.newBuilder()
                .setSecondsintoyear(0).setNano(0)
                .setVal(ByteString.copyFrom(new byte[]{1, 2, 3})).build();
        DataExtractIterator iter = iteratorForType(PayloadType.WAVEFORM_BYTE);
        VType result = iter.extract(pbMsg(pb));
        assertInstanceOf(VByteArray.class, result);
        VByteArray vba = (VByteArray) result;
        assertEquals(3, vba.getData().size());
        assertEquals((byte) 1, vba.getData().getByte(0));
        assertEquals((byte) 2, vba.getData().getByte(1));
        assertEquals((byte) 3, vba.getData().getByte(2));
    }
}
