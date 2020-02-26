/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VStatistics;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.archive.vtype.VTypeHelper;
import org.phoebus.pv.TimeHelper;

/** Data Sample from control system ({@link VType})
 *  with interface for XYGraph
 *  @author Kay Kasemir
 *  @author Takashi Nakamoto changed PlotSample to handle waveform index.
 */
@SuppressWarnings("nls")
public class PlotSample implements PlotDataItem<Instant>
{
    final private static AtomicInteger default_waveform_index = new AtomicInteger(0);

    /** Value contained in this sample */
    final private VType value;

    /** Source of the data */
    final private String source;

    /** Info string.
     *  @see #getInfo()
     */
    private String info;

    /** Waveform index */
    private AtomicInteger waveform_index;

    /** Initialize with valid control system value
     *  @param waveform_index Waveform index
     *  @param source Info about the source of this sample
     *  @param value Value from which position, value and alarm info are read
     *  @param info Info text. If non-<code>null</code>, replaces alarm info
     */
    PlotSample(final AtomicInteger waveform_index, final  String source, final VType value, final String info)
    {
        this.waveform_index = waveform_index;
        this.value = value;
        this.source = source;
        if (info == null)
        {
            this.info = decodeAlarm(value);
            // For string PV add the text to info
            if (value instanceof VString)
                this.info = ((VString) value).getValue() + (" " + this.info).trim();
        }
        else
            this.info = info;
    }

    private static String decodeAlarm(VType value)
    {
        final Alarm alarm = Alarm.alarmOf(value);
        if (alarm != null)
        {
            if (alarm.getSeverity() == AlarmSeverity.NONE)
                return "";
            return alarm.getSeverity() + " / " + alarm.getName();
        }
        return "";
    }

    /** Initialize with valid control system value
     *  @param waveform_index Waveform index
     *  @param source Info about the source of this sample
     *  @param value
     */
    PlotSample(final AtomicInteger waveform_index, final  String source, final VType value)
    {
        this(waveform_index, source, value, null);
    }

    /** Initialize with valid control system value
     *  @param source Info about the source of this sample
     *  @param value
     */
    public PlotSample(final String source, final VType value)
    {
        this(default_waveform_index, source, value);
    }

    /** Initialize with (error) info, creating a non-plottable sample 'now'
     *  @param info Text used for info as well as error message
     */
    public PlotSample(final String source, final String info)
    {
        this(default_waveform_index, source,
             VString.of(info, Alarm.of(AlarmSeverity.UNDEFINED, AlarmStatus.UNDEFINED, info), Time.now()),
             info);
    }

    /** Package-level constructor, only used in unit tests */
    PlotSample(final double x, final double y)
    {
        this("Test",
             VDouble.of(y, Alarm.none(), TimeHelper.fromInstant(Instant.ofEpochSecond((int) x, 0)), Display.none()));
    }

    /** @param index Waveform index to plot */
    void setWaveformIndex(final AtomicInteger index)
    {
        this.waveform_index = index;
    }

    /** @return Source of the data */
    public String getSource()
    {
        return source;
    }

    /** @return Control system value */
    public VType getVType()
    {
        return value;
    }

    /** @return Control system time stamp */
    private Instant getTime()
    {
        // NOT checking if time.isValid()
        // because that actually takes quite some time.
        // We just plot what we have, and that includes
        // the case where the time stamp is invalid.
        final Time time = Time.timeOf(value);
        if (time != null)
            return time.getTimestamp();
        return Instant.now();
    }

    /** {@inheritDoc} */
    @Override
    public Instant getPosition()
    {
        return getTime();
    }

    /** {@inheritDoc} */
    @Override
    public double getValue()
    {
        return org.phoebus.core.vtypes.VTypeHelper.toDouble(value, waveform_index.get());
    }

    /** @return {@link VStatistics} or <code>null</code> */
    private VStatistics getStats()
    {
        // Although the behavior of getMinimum() method depends on archive
        // readers' implementation, at least, RDB and kblog archive readers
        // return the minimum value of the first element. This minimum value
        // does not make sense to plot error bars when the chart shows other
        // elements. Therefore, this method returns 0 if the waveform index
        // is not 0.
        if (waveform_index.get() != 0)
            return null;
        if (value instanceof VStatistics)
            return (VStatistics) value;
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public double getStdDev()
    {
        final VStatistics stats = getStats();
        return (stats != null) ? stats.getStdDev() : Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public double getMin()
    {
        final VStatistics stats = getStats();
        return (stats != null) ? stats.getMin() : Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public double getMax()
    {
        final VStatistics stats = getStats();
        return (stats != null) ? stats.getMax() : Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public String getInfo()
    {
        return info;
    }

    @Override
    public String toString()
    {
        return VTypeHelper.toString(value);
    }
}
