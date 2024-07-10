/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.channelarchiver;

import static org.phoebus.archive.reader.ArchiveReaders.logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ArrayInteger;
import org.epics.util.stats.Range;
import org.epics.util.text.NumberFormats;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VInt;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VStatistics;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.pv.TimeHelper;
import org.w3c.dom.Element;

/** Value iterator for XML-RPC
 *
 *  <p>Requests data from server in batches.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class ValueRequestIterator implements ValueIterator
{
    private final XMLRPCArchiveReader reader;
    private final String name;
    private final Instant end;
    private final int method;
    private final int count;
    private int index;
    private EnumDisplay labels = EnumDisplay.of();
    private Display display = Display.none();
    private List<VType> samples;

    /** Constructor for new value request.
     *  @param reader {@link XMLRPCArchiveReader}
     *  @param name Channel name
     *  @param start Start time for retrieval
     *  @param end  End time for retrieval
     *  @param method Get optimized or raw data?
     *  @param count Number of values
     *  @throws Exception on error
     */
    public ValueRequestIterator(final XMLRPCArchiveReader reader,
                                final String name,
                                final Instant start,
                                final Instant end,
                                final int method,
                                final int count) throws Exception
    {
        this.reader = reader;
        this.name = name;
        this.end = end;
        this.method = method;
        this.count = count;

        fetch(start);
    }

    /** Fetch another batch of samples
     *
     *  @param fetch_start Start time for this batch
     *         (greater or equal to overall start time)
     *  @throws Exception on error
     */
    private void fetch(final Instant fetch_start) throws Exception
    {
       index = 0;
       samples = getSamples(fetch_start);
       // Empty samples?
       if (samples.isEmpty())
           samples = null;
    }

    @Override
    public boolean hasNext()
    {
        return samples != null;
    }

    @Override
    public VType next()
    {
        final VType result = samples.get(index);
        ++index;
        if (index < samples.size())
            return result;
        if (method == reader.method_optimized)
        {
            // For optimized (binned) data there is no reason
            // to make a second query as the first has returned
            // values for all bins in the requested time range
            close();
            return result;
        }

        // Prepare next batch of samples
        try
        {
            fetch(VTypeHelper.getTimestamp(result));
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Error fetching more samples for " + name, ex);
            close();
        }
        if (samples == null)
            return result;

        // Inspect next batch of samples
        // In most cases, this fetch should return the 'result' again:
        //   some_timestamp value A
        //   last_timestamp value B <-- last_sample of previous batch
        //   new_timestamp  value C
        // Since we ask from at-or-before last_timestamp on,
        // we get the last_sample once more and need to skip it.
        //
        // But also consider the following situation, where the last batch ended
        // in a range of data that had the same time stamp, and even some same
        // values:
        //   some_timestamp value A
        //   last_timestamp value B
        //   last_timestamp value C
        //   last_timestamp value C
        //   last_timestamp value C <-- last_sample of previous batch
        //   last_timestamp value C
        //   last_timestamp value C
        //   last_timestamp value D
        //   last_timestamp value E
        //   new_timestamp  value F
        // Reasons for same timestamps: Stuck IOC clock,
        // or sequences like .. Repeat N, next value, Disconnected, Arch. Off.
        // Reason for the same value: General mess-up.
        //
        // When we request new data from 'last_sample.getTime()' on,
        // i.e. from last_timestamp on, we could get any of the values B to E,
        // since they're all stamped at-or-before last_timestamp.
        // Which one exactly depends on optimization inside the data server.

        // From the end of the new samples, go backward:
        for (index=samples.size()-1;  index>=0;  --index)
        {   // If we find the previous batch's last sample...
            if (samples.get(index).equals(result))
            {
                // Skip all the samples up to and including it
                ++index;
                break;
            }
        }
        // Nothing to skip? Return as is.
        if (index < 0)
            index = 0;
        // Nothing left? Clear samples.
        if (index >= samples.size()-1)
            samples = null;
        return result;
    }

    private List<VType> getSamples(final Instant start) throws Exception
    {
        final String command = XmlRpc.command("archiver.values",
                                              reader.key,
                                              List.of(name),
                                              start.getEpochSecond(),
                                              start.getNano(),
                                              end.getEpochSecond(),
                                              end.getNano(),
                                              count,
                                              method);
        final Element response = XmlRpc.communicate(reader.url, command);
        // XMLUtil.writeDocument(response, System.out);

        // Requested only one channel, so get only first element of array
        final Element channel = XmlRpc.getFirstArrayValue(response);

        final List<VType> result = new ArrayList<>();

        final String act_name = XmlRpc.getValue(XmlRpc.getStructMember(channel, "name"));
        if (! name.equals(act_name))
            throw new Exception("Expected " + name + ", got " + act_name);

        final Integer type_code = XmlRpc.getValue(XmlRpc.getStructMember(channel, "type"));
        final Class<?> type;
        switch (type_code)
        {
        case 0:  type = String.class;   break;
        case 1:  type = Enum.class;     break;
        case 2:  type = Integer.class;  break;
        case 3:  type = Double.class;   break;
        default: throw new Exception("Cannot handle data type " + type_code);
        }
        final Integer array_size = XmlRpc.getValue(XmlRpc.getStructMember(channel, "count"));

        // Decode meta
        decodeMeta(XmlRpc.getStructMember(channel, "meta"));

        final Element val_arr = XmlRpc.getStructMember(channel, "values");
        for (Element value_struct : XmlRpc.getArrayValues(val_arr))
        {
            // Decode time stamp
            final Integer secs = XmlRpc.getValue(XmlRpc.getStructMember(value_struct, "secs"));
            final Integer nano = XmlRpc.getValue(XmlRpc.getStructMember(value_struct, "nano"));
            final Time time = TimeHelper.fromInstant(Instant.ofEpochSecond(secs, nano));

            // Decode severity, status
            Integer code = XmlRpc.getValue(XmlRpc.getStructMember(value_struct, "sevr"));
            final SeverityInfo sevr = reader.severities.get(code);
            code = XmlRpc.getValue(XmlRpc.getStructMember(value_struct, "stat"));
            final String status;
            if (! sevr.hasValue())
                status = sevr.getText();
            else if (sevr.statusIsText())
                status = reader.status_strings.get(code);
            else
                status = code.toString();
            final Alarm alarm = Alarm.of(sevr.getSeverity(), AlarmStatus.CLIENT, status);

            // Decode value
            VType sample = null;
            if (type == Double.class)
            {
                final double[] values = new double[array_size];
                int i = 0;
                for (Element val : XmlRpc.getArrayValues(XmlRpc.getStructMember(value_struct, "value")))
                    values[i++] = XmlRpc.getValue(val);

                if (values.length == 1)
                {   // Check for min,max,avg
                    final Element min_el = XmlRpc.getOptionalStructMember(value_struct, "min");
                    final Element max_el = XmlRpc.getOptionalStructMember(value_struct, "max");
                    if (min_el != null  &&  max_el != null)
                    {
                        final double min = XmlRpc.getValue(min_el);
                        final double max = XmlRpc.getValue(max_el);
                        sample = VStatistics.of(values[0], 0.0, min, max, 1, alarm, time, display);
                    }
                    else
                        sample = VDouble.of(values[0], alarm, time, display);
                }
                else
                    sample = VDoubleArray.of(ArrayDouble.of(values), alarm, time, display);
            }
            else if (type == Integer.class)
            {
                final int[] values = new int[array_size];
                int i = 0;
                for (Element val : XmlRpc.getArrayValues(XmlRpc.getStructMember(value_struct, "value")))
                    values[i++] = XmlRpc.getValue(val);

                if (values.length == 1)
                {   // Check for min,max,avg
                    final Element min_el = XmlRpc.getOptionalStructMember(value_struct, "min");
                    final Element max_el = XmlRpc.getOptionalStructMember(value_struct, "max");
                    if (min_el != null  &&  max_el != null)
                    {
                        final double min = XmlRpc.getValue(min_el);
                        final double max = XmlRpc.getValue(max_el);
                        sample = VStatistics.of(values[0], 0.0, min, max, 1, alarm, time, display);
                    }
                    else
                        sample = VInt.of(values[0], alarm, time, display);
                }
                else
                    sample = VIntArray.of(ArrayInteger.of(values), alarm, time, display);
            }
            else if (type == Enum.class)
            {
                final int[] values = new int[array_size];
                int i = 0;
                for (Element val : XmlRpc.getArrayValues(XmlRpc.getStructMember(value_struct, "value")))
                    values[i++] = XmlRpc.getValue(val);

                if (values.length == 1)
                    sample = VEnum.of(values[0], labels, alarm, time);
                else // Return indices..
                    sample = VEnumArray.of(ArrayInteger.of(values), labels, alarm, time);
            }
            else if (type == String.class)
            {
                final String value = XmlRpc.getValue(XmlRpc.getFirstArrayValue(XmlRpc.getStructMember(value_struct, "value")));
                sample = VString.of(value, alarm, time);
            }
            else
                throw new Exception("Cannot decode samples for " + type.getSimpleName());
            result.add(sample);
        }

        return result;
    }

    private void decodeMeta(final Element meta) throws Exception
    {
        final Integer meta_type = XmlRpc.getValue(XmlRpc.getStructMember(meta, "type"));
        if (meta_type == 0)
        {   // Enum labels
            final List<String> options = new ArrayList<>();
            for (Element e : XmlRpc.getArrayValues(XmlRpc.getStructMember(meta, "states")))
                options.add(XmlRpc.getValue(e));
            labels = EnumDisplay.of(options);
        }
        else
        {   // Numeric display
            final String units = XmlRpc.getValue(XmlRpc.getStructMember(meta, "units"));
            final Integer prec = XmlRpc.getValue(XmlRpc.getStructMember(meta, "prec"));
            display = Display.of(Range.of(0,  10),
                                 Range.undefined(),
                                 Range.undefined(),
                                 Range.undefined(),
                                 units, NumberFormats.precisionFormat(prec));
        }
    }

    @Override
    public void close()
    {
        samples = null;
    }
}
