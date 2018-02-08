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

import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.archive.vtype.ArchiveVEnum;
import org.phoebus.archive.vtype.ArchiveVNumber;
import org.phoebus.archive.vtype.ArchiveVNumberArray;
import org.phoebus.archive.vtype.ArchiveVString;
import org.phoebus.archive.vtype.VTypeHelper;
import org.phoebus.framework.persistence.XMLUtil;
import org.phoebus.util.text.NumberFormats;
import org.phoebus.vtype.Display;
import org.phoebus.vtype.VType;
import org.phoebus.vtype.ValueFactory;
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
    private List<String> labels = List.of();
    private Display display = ValueFactory.displayNone();
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
            final Instant time = Instant.ofEpochSecond(secs, nano);

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

            // Decode value
            VType sample = null;
            if (type == Double.class)
            {
                final double[] values = new double[array_size];
                int i = 0;
                for (Element val : XmlRpc.getArrayValues(XmlRpc.getStructMember(value_struct, "value")))
                    values[i++] = XmlRpc.getValue(val);

                if (values.length == 1)
                    sample = new ArchiveVNumber(time, sevr.getSeverity(), status, display, values[0]);
                else
                    sample = new ArchiveVNumberArray(time, sevr.getSeverity(), status, display, values);
            }
            else if (type == Enum.class)
            {
                final int[] values = new int[array_size];
                int i = 0;
                for (Element val : XmlRpc.getArrayValues(XmlRpc.getStructMember(value_struct, "value")))
                    values[i++] = XmlRpc.getValue(val);

                if (values.length == 1)
                    sample = new ArchiveVEnum(time, sevr.getSeverity(), status, labels, values[0]);
                else // Return indices..
                    sample = new ArchiveVNumberArray(time, sevr.getSeverity(), status, display, values);
            }
            else if (type == String.class)
            {
                final String value = XmlRpc.getValue(XmlRpc.getFirstArrayValue(XmlRpc.getStructMember(value_struct, "value")));
                sample = new ArchiveVString(time, sevr.getSeverity(), status, value);
            }
            // TODO Decode more types
            else
            {

                throw new Exception("Cannot decode samples for " + type.getSimpleName() + ":\n" + XMLUtil.elementToString(value_struct, true));
            }
            result.add(sample);
        }

        return result;
    }

    private void decodeMeta(final Element meta) throws Exception
    {
        final Integer meta_type = XmlRpc.getValue(XmlRpc.getStructMember(meta, "type"));
        if (meta_type == 0)
        {   // Enum labels
            labels = new ArrayList<>();
            for (Element e : XmlRpc.getArrayValues(XmlRpc.getStructMember(meta, "states")))
                labels.add(XmlRpc.getValue(e));
        }
        else
        {   // Numeric display
            final String units = XmlRpc.getValue(XmlRpc.getStructMember(meta, "units"));
            final Integer prec = XmlRpc.getValue(XmlRpc.getStructMember(meta, "prec"));
            display = ValueFactory.newDisplay(0.0, 0.0, 0.0,
                                              units,
                                              NumberFormats.format(prec), 0.0, 0.0, 10.0, 0.0, 10.0);
        }
    }

    @Override
    public void close()
    {
        samples = null;
    }
}
