package org.csstudio.trends.databrowser3.model;

import org.epics.util.stats.Range;
import org.epics.vtype.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.csstudio.trends.databrowser3.model.AlarmLimitItem.LimitType;

import java.text.DecimalFormat;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class AlarmLimitItemTest {

    /**
     * Tests that multiple archiver {@link VType} events with same {@link Display} result in but one sample
     * in the {@link AlarmLimitItem} object.
     */
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

        AlarmLimitItem alarmLimitItem = new AlarmLimitItem(pvItem, LimitType.LOLO);

        // PVSamples returns the size of raw + 1 to account for a live sample, so need to compare to 2.
        assertEquals(2, alarmLimitItem.getSamples().size());

    }

    /**
     * Tests that multiple archiver {@link VType} events where one has different {@link Display} result in two
     * samples in the {@link AlarmLimitItem} object.
     */
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

        AlarmLimitItem alarmLimitItem = new AlarmLimitItem(pvItem, LimitType.LOLO);
        // PVSamples returns the size of raw + 1 to account for a live sample, so need to compare to 3.
        assertEquals(3, alarmLimitItem.getSamples().size());

    }

    /**
     * Tests that multiple archiver {@link VType} events where all have different {@link Display} result in three
     * samples in the {@link AlarmLimitItem} object.
     */
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

        AlarmLimitItem alarmLimitItem = new AlarmLimitItem(pvItem, LimitType.LOLO);

        alarmLimitItem.getSamples().lockForWriting();
        // PVSamples returns the size of raw + 1 to account for a live sample, so need to compare to 4.
        assertEquals(4, alarmLimitItem.getSamples().size());

    }

    /**
     * Tests that a {@link LimitType#LOLO} value at {@link Double#NEGATIVE_INFINITY} is not treated
     * as a valid alarm limit and hence does not add an entry in the {@link AlarmLimitItem} samples.
     */
    @Test
    public void testInfiniteCheck1(){

        PVItem pvItem = Mockito.mock(PVItem.class);
        when(pvItem.getName()).thenReturn("name");
        when(pvItem.getScanPeriod()).thenReturn(1.0);

        PVSamples pvSamples = new PVSamples(new AtomicInteger(0));
        Range displayRange = Range.of(-20, 20);

        VType vType1 = VDouble.of(1.0, Alarm.none(), Time.timeOf(Instant.now()), Display.of(displayRange, Range.of(Double.NEGATIVE_INFINITY, 10), Range.of(-5, 5), displayRange, "unit", new DecimalFormat()));
        VType vType2 = VDouble.of(1.0, Alarm.none(), Time.timeOf(Instant.now().plusSeconds(1)), Display.of(displayRange, Range.of(-100, 10), Range.of(-5, 5), displayRange, "unit", new DecimalFormat()));
        VType vType3 = VDouble.of(1.0, Alarm.none(), Time.timeOf(Instant.now().plusSeconds(2)), Display.of(displayRange, Range.of(-1000, 10), Range.of(-5, 5), displayRange, "unit", new DecimalFormat()));

        pvSamples.mergeArchivedData("source", List.of(vType1, vType2, vType3));

        when(pvItem.getSamples()).thenReturn(pvSamples);

        AlarmLimitItem alarmLimitItem = new AlarmLimitItem(pvItem,LimitType.LOLO);
        assertEquals(3, alarmLimitItem.getSamples().size());

    }

    /**
     * Tests that a {@link LimitType#HIHI} value at {@link Double#POSITIVE_INFINITY} is not treated
     * as a valid alarm limit and hence does not add an entry in the {@link AlarmLimitItem} samples.
     */
    @Test
    public void testInfiniteCheck2(){

        PVItem pvItem = Mockito.mock(PVItem.class);
        when(pvItem.getName()).thenReturn("name");
        when(pvItem.getScanPeriod()).thenReturn(1.0);

        PVSamples pvSamples = new PVSamples(new AtomicInteger(0));
        Range displayRange = Range.of(-20, 20);

        VType vType1 = VDouble.of(1.0, Alarm.none(), Time.timeOf(Instant.now()), Display.of(displayRange, Range.of(-10, Double.POSITIVE_INFINITY), Range.of(-5, 5), displayRange, "unit", new DecimalFormat()));
        VType vType2 = VDouble.of(1.0, Alarm.none(), Time.timeOf(Instant.now().plusSeconds(1)), Display.of(displayRange, Range.of(-100, 100), Range.of(-5, 5), displayRange, "unit", new DecimalFormat()));
        VType vType3 = VDouble.of(1.0, Alarm.none(), Time.timeOf(Instant.now().plusSeconds(2)), Display.of(displayRange, Range.of(-1000, 1000), Range.of(-5, 5), displayRange, "unit", new DecimalFormat()));

        pvSamples.mergeArchivedData("source", List.of(vType1, vType2, vType3));

        when(pvItem.getSamples()).thenReturn(pvSamples);

        AlarmLimitItem alarmLimitItem = new AlarmLimitItem(pvItem, LimitType.HIHI);
        assertEquals(3, alarmLimitItem.getSamples().size());

    }

    /**
     * Tests that a {@link LimitType#LOW} value at {@link Double#NEGATIVE_INFINITY} is not treated
     * as a valid alarm limit and hence does not add an entry in the {@link AlarmLimitItem} samples.
     */
    @Test
    public void testInfiniteCheck3(){

        PVItem pvItem = Mockito.mock(PVItem.class);
        when(pvItem.getName()).thenReturn("name");
        when(pvItem.getScanPeriod()).thenReturn(1.0);

        PVSamples pvSamples = new PVSamples(new AtomicInteger(0));
        Range displayRange = Range.of(-20, 20);

        VType vType1 = VDouble.of(1.0, Alarm.none(), Time.timeOf(Instant.now()), Display.of(displayRange, Range.of(-10, 10), Range.of(Double.NEGATIVE_INFINITY, 5), displayRange, "unit", new DecimalFormat()));
        VType vType2 = VDouble.of(1.0, Alarm.none(), Time.timeOf(Instant.now().plusSeconds(1)), Display.of(displayRange, Range.of(-100, 100), Range.of(-50, 50), displayRange, "unit", new DecimalFormat()));
        VType vType3 = VDouble.of(1.0, Alarm.none(), Time.timeOf(Instant.now().plusSeconds(2)), Display.of(displayRange, Range.of(-1000, 1000), Range.of(-500, 500), displayRange, "unit", new DecimalFormat()));

        pvSamples.mergeArchivedData("source", List.of(vType1, vType2, vType3));

        when(pvItem.getSamples()).thenReturn(pvSamples);

        AlarmLimitItem alarmLimitItem = new AlarmLimitItem(pvItem, LimitType.LOW);

        assertEquals(3, alarmLimitItem.getSamples().size());

    }

    /**
     * Tests that a {@link LimitType#HIGH} value at {@link Double#POSITIVE_INFINITY} is not treated
     * as a valid alarm limit and hence does not add an entry in the {@link AlarmLimitItem} samples.
     */
    @Test
    public void testInfiniteCheck4(){

        PVItem pvItem = Mockito.mock(PVItem.class);
        when(pvItem.getName()).thenReturn("name");
        when(pvItem.getScanPeriod()).thenReturn(1.0);

        PVSamples pvSamples = new PVSamples(new AtomicInteger(0));
        Range displayRange = Range.of(-20, 20);

        VType vType1 = VDouble.of(1.0, Alarm.none(), Time.timeOf(Instant.now()), Display.of(displayRange, Range.of(-10, 10), Range.of(-5, Double.POSITIVE_INFINITY), displayRange, "unit", new DecimalFormat()));
        VType vType2 = VDouble.of(1.0, Alarm.none(), Time.timeOf(Instant.now().plusSeconds(1)), Display.of(displayRange, Range.of(-100, 100), Range.of(-50, 50), displayRange, "unit", new DecimalFormat()));
        VType vType3 = VDouble.of(1.0, Alarm.none(), Time.timeOf(Instant.now().plusSeconds(2)), Display.of(displayRange, Range.of(-1000, 1000), Range.of(-500, 500), displayRange, "unit", new DecimalFormat()));

        pvSamples.mergeArchivedData("source", List.of(vType1, vType2, vType3));

        when(pvItem.getSamples()).thenReturn(pvSamples);

        AlarmLimitItem alarmLimitItem = new AlarmLimitItem(pvItem, LimitType.HIGH);
        assertEquals(3, alarmLimitItem.getSamples().size());

    }
}
