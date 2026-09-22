package org.csstudio.trends.databrowser3.model;

import org.epics.util.stats.Range;
import org.epics.vtype.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.phoebus.archive.reader.appliance.ApplianceArchiveReaderConstants;

import java.text.DecimalFormat;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;



public class AlarmLimitItemTest {

    @Test
    public void testConstructor1(){

        PVItem pvItem = Mockito.mock(PVItem.class);
        when(pvItem.getName()).thenReturn("name");
        when(pvItem.getScanPeriod()).thenReturn(1.0);

        PVSamples pvSamples = new PVSamples(new AtomicInteger(0));
        Range displayRange = Range.of(-20, 20);

        VType vType1 = VDouble.of(1.0, Alarm.none(), Time.timeOf(Instant.now()), Display.of(displayRange, Range.of(-10, 10), Range.of(-5, 5), displayRange, "unit", new DecimalFormat()));
        VType vType2 = VDouble.of(1.0, Alarm.none(), Time.timeOf(Instant.now().plusSeconds(1)), Display.of(displayRange, Range.of(-10, 10), Range.of(-5, 5), displayRange, "unit", new DecimalFormat()));
        VType vType3 = VDouble.of(1.0, Alarm.none(), Time.timeOf(Instant.now().plusSeconds(2)), Display.of(displayRange, Range.of(-10, 10), Range.of(-5, 5), displayRange, "unit", new DecimalFormat()));

        pvSamples.mergeArchivedData("source", List.of(vType1, vType2, vType3));

        when(pvItem.getSamples()).thenReturn(pvSamples);

        AlarmLimitItem alarmLimitItem = new AlarmLimitItem(pvItem, ApplianceArchiveReaderConstants.LOLO);

        alarmLimitItem.getSamples().lockForWriting();
        // PVSamples returns the size of raw + 1 to account for a live sample, so need to compare to 2.
        assertEquals(2, alarmLimitItem.getSamples().size());

    }

    @Test
    public void testConstructor2(){

        PVItem pvItem = Mockito.mock(PVItem.class);
        when(pvItem.getName()).thenReturn("name");
        when(pvItem.getScanPeriod()).thenReturn(1.0);

        PVSamples pvSamples = new PVSamples(new AtomicInteger(0));
        Range displayRange = Range.of(-20, 20);

        VType vType1 = VDouble.of(1.0, Alarm.none(), Time.timeOf(Instant.now()), Display.of(displayRange, Range.of(-10, 10), Range.of(-5, 5), displayRange, "unit", new DecimalFormat()));
        VType vType2 = VDouble.of(1.0, Alarm.none(), Time.timeOf(Instant.now().plusSeconds(1)), Display.of(displayRange, Range.of(-100, 10), Range.of(-5, 5), displayRange, "unit", new DecimalFormat()));
        VType vType3 = VDouble.of(1.0, Alarm.none(), Time.timeOf(Instant.now().plusSeconds(2)), Display.of(displayRange, Range.of(-100, 10), Range.of(-5, 5), displayRange, "unit", new DecimalFormat()));

        pvSamples.mergeArchivedData("source", List.of(vType1, vType2, vType3));

        when(pvItem.getSamples()).thenReturn(pvSamples);

        AlarmLimitItem alarmLimitItem = new AlarmLimitItem(pvItem, ApplianceArchiveReaderConstants.LOLO);

        alarmLimitItem.getSamples().lockForWriting();
        // PVSamples returns the size of raw + 1 to account for a live sample, so need to compare to 3.
        assertEquals(3, alarmLimitItem.getSamples().size());

    }

    @Test
    public void testConstructor3(){

        PVItem pvItem = Mockito.mock(PVItem.class);
        when(pvItem.getName()).thenReturn("name");
        when(pvItem.getScanPeriod()).thenReturn(1.0);

        PVSamples pvSamples = new PVSamples(new AtomicInteger(0));
        Range displayRange = Range.of(-20, 20);

        VType vType1 = VDouble.of(1.0, Alarm.none(), Time.timeOf(Instant.now()), Display.of(displayRange, Range.of(-10, 10), Range.of(-5, 5), displayRange, "unit", new DecimalFormat()));
        VType vType2 = VDouble.of(1.0, Alarm.none(), Time.timeOf(Instant.now().plusSeconds(1)), Display.of(displayRange, Range.of(-100, 10), Range.of(-5, 5), displayRange, "unit", new DecimalFormat()));
        VType vType3 = VDouble.of(1.0, Alarm.none(), Time.timeOf(Instant.now().plusSeconds(2)), Display.of(displayRange, Range.of(-1000, 10), Range.of(-5, 5), displayRange, "unit", new DecimalFormat()));

        pvSamples.mergeArchivedData("source", List.of(vType1, vType2, vType3));

        when(pvItem.getSamples()).thenReturn(pvSamples);

        AlarmLimitItem alarmLimitItem = new AlarmLimitItem(pvItem, ApplianceArchiveReaderConstants.LOLO);

        alarmLimitItem.getSamples().lockForWriting();
        // PVSamples returns the size of raw + 1 to account for a live sample, so need to compare to 4.
        assertEquals(4, alarmLimitItem.getSamples().size());

    }
}
