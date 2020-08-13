package org.phoebus.archive.reader.appliance;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.epics.archiverappliance.retrieval.client.DataRetrieval;
import org.epics.archiverappliance.retrieval.client.EpicsMessage;
import org.epics.archiverappliance.retrieval.client.GenMsgIterator;
import org.epics.util.array.ArrayByte;
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ArrayInteger;
import org.epics.util.stats.Range;
import org.epics.util.text.NumberFormats;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.Time;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VNumber;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.archive.vtype.TimestampHelper;
import org.phoebus.pv.TimeHelper;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.FieldDescriptor;

import edu.stanford.slac.archiverappliance.PB.EPICSEvent.FieldValue;
import edu.stanford.slac.archiverappliance.PB.EPICSEvent.PayloadInfo;
import edu.stanford.slac.archiverappliance.PB.EPICSEvent.PayloadType;

import gov.aps.jca.dbr.Status;

/**
 *
 * <code>ApplianceValueIterator</code> is the base class for different value iterators.
 * It provides the facilities to extract the common values.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public abstract class ApplianceValueIterator implements ValueIterator {

    protected Display display;
    protected GenMsgIterator mainStream;
    protected Iterator<EpicsMessage> mainIterator;
    private FieldDescriptor valDescriptor;

    protected final ApplianceArchiveReader reader;
    protected final String name;
    protected final Instant start;
    protected final Instant end;

    private final IteratorListener listener;

    protected boolean closed = false;

    private static Object lock = new Object();

    /**
     * Constructs a new ApplianceValueIterator.
     *
     * @param reader the reader to use
     * @param name the name of the pv to load the data for
     * @param start the start of the time window of the data
     * @param end the end of the time window of the data
     */
    protected ApplianceValueIterator(ApplianceArchiveReader reader, String name, Instant start, Instant end,
            IteratorListener listener) {
        this.reader = reader;
        this.name = name;
        this.start = start;
        this.end = end;
        this.listener = listener;
    }

    /**
     * Fetches data from appliance archiver reader using the parameters provided to the constructor.
     *
     * @throws ArchiverApplianceException if the data for the pv could not be loaded
     */
    public void fetchData() throws ArchiverApplianceException {
        fetchDataInternal(name);
    }

    /**
     * Fetches data from appliance archiver reader for the given pv name.
     *
     * @param pvName name of the PV as used in the request made to the server
     *
     * @throws ArchiverApplianceException if the data for the pv could not be loaded
     */
    protected void fetchDataInternal(String pvName) throws ArchiverApplianceException {
        java.sql.Timestamp sqlStartTimestamp = TimestampHelper.toSQLTimestamp(start);
        java.sql.Timestamp sqlEndTimestamp = TimestampHelper.toSQLTimestamp(end);

        DataRetrieval dataRetrieval = reader.createDataRetriveal(reader.getDataRetrievalURL());
        synchronized(lock){
            mainStream = dataRetrieval.getDataForPV(pvName, sqlStartTimestamp, sqlEndTimestamp);
        }
        if (mainStream != null) {
            mainIterator = mainStream.iterator();
        } else {
            throw new ArchiverApplianceException("Could not fetch data.");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.archive.reader.ValueIterator#hasNext()
     */
    @Override
    public synchronized boolean hasNext() {
        return !closed && mainIterator != null && mainIterator.hasNext();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.archive.reader.ValueIterator#next()
     */
    @Override
    public VType next() {
        EpicsMessage message;
        synchronized (this) {
            if (closed)
                return null;
            message = mainIterator.next();
        }
        return extractData(message);
    }

    /**
     * Extracts the data from the given epics message based on the payload type.
     *
     * @param dataMessage source of data
     * @return the appropriate VType data object
     * @throws IOException
     */
    protected VType extractData(EpicsMessage dataMessage) {
        PayloadType type = mainStream.getPayLoadInfo().getType();
        final Alarm alarm = Alarm.of(getSeverity(dataMessage.getSeverity()), AlarmStatus.CLIENT, getStatus(dataMessage.getStatus()));
        final Time time = TimeHelper.fromInstant(TimestampHelper.fromSQLTimestamp(dataMessage.getTimestamp()));

        if (type == PayloadType.SCALAR_BYTE ||
            type == PayloadType.SCALAR_DOUBLE ||
            type == PayloadType.SCALAR_FLOAT ||
            type == PayloadType.SCALAR_INT ||
            type == PayloadType.SCALAR_SHORT) {
            return VNumber.of(dataMessage.getNumberValue(),
                              alarm, time,
                              display == null ? getDisplay(mainStream.getPayLoadInfo()) : display);
        } else if (type == PayloadType.SCALAR_ENUM) {
            return VEnum.of(dataMessage.getNumberValue().intValue(),
                            EnumDisplay.of(), //TODO get the labels from somewhere
                            alarm, time);
        } else if (type == PayloadType.SCALAR_STRING) {
            if (valDescriptor == null) {
                valDescriptor = getValDescriptor(dataMessage);
            }
            return VString.of(String.valueOf(dataMessage.getMessage().getField(valDescriptor)), alarm, time);
        } else if (type == PayloadType.WAVEFORM_DOUBLE
                || type == PayloadType.WAVEFORM_FLOAT){
            if (valDescriptor == null) {
                valDescriptor = getValDescriptor(dataMessage);
            }
            //we could load the data directly using result.getNumberAt(index), but this is faster
            List<?> o = (List<?>)dataMessage.getMessage().getField(valDescriptor);
            double[] val = new double[o.size()];
            if (type == PayloadType.WAVEFORM_DOUBLE) {
                int i = 0;
                for (Object d : o) {
                    val[i++] = ((Double)d).doubleValue();
                }
            } else {
                int i = 0;
                for (Object d : o) {
                    val[i++] = ((Float)d).doubleValue();
                }
            }
            return VDoubleArray.of(ArrayDouble.of(val),
                                   alarm, time,
                                   display == null ? getDisplay(mainStream.getPayLoadInfo()) : display);
        } else if (type == PayloadType.WAVEFORM_INT
                || type == PayloadType.WAVEFORM_SHORT) {
            if (valDescriptor == null) {
                valDescriptor = getValDescriptor(dataMessage);
            }
            //we could load the data directly using result.getNumberAt(index), but this is faster
            List<?> o = (List<?>)dataMessage.getMessage().getField(valDescriptor);
            int[] val = new int[o.size()];
            int i = 0;
            for (Object d : o) {
                val[i++] = ((Integer)d).intValue();
            }

            return VIntArray.of(ArrayInteger.of(val),
                                alarm, time,
                                display == null ? getDisplay(mainStream.getPayLoadInfo()) : display);
        } else if (type == PayloadType.WAVEFORM_BYTE) {
            if (valDescriptor == null) {
                valDescriptor = getValDescriptor(dataMessage);
            }
            //we could load the data directly using result.getNumberAt(index), but this is faster
            return VByteArray.of(ArrayByte.of(((ByteString)dataMessage.getMessage().getField(valDescriptor)).toByteArray()),
                                 alarm, time,
                                 display == null ? getDisplay(mainStream.getPayLoadInfo()) : display);
        }
        throw new UnsupportedOperationException("PV type " + type + " is not supported.");
    }

    /**
     * Extracts the descriptor for the value field so it can be reused on each iteration.
     *
     * @param message the epics message to extract the descriptor from
     * @return the descriptor if it was found or null if not found
     */
    private FieldDescriptor getValDescriptor(EpicsMessage message) {
        Iterator<FieldDescriptor> it = message.getMessage().getAllFields().keySet().iterator();
        FieldDescriptor fd;
        while (it.hasNext()) {
            fd = it.next();
            if (fd.getName().equalsIgnoreCase(ApplianceArchiveReaderConstants.VAL)) {
                return fd;
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.archive.reader.ValueIterator#close()
     */
    @Override
    public void close() {
        try {
            synchronized (this) {
                if (mainStream != null) {
                    mainStream.close();
                }
                closed = true;
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        listener.finished(this);
    }

    /**
     * Extract the display properties (min, max, alarm limits) from the given payloadinfo.
     *
     * @param info the info to extract the limits from
     * @return the display
     */
    protected Display getDisplay(PayloadInfo info) {
        Map<String, String> headers = new HashMap<>();
        for (FieldValue fieldValue : info.getHeadersList()) {
            if (!headers.containsKey(fieldValue.getName())) {
                headers.put(fieldValue.getName(), fieldValue.getVal());
            }
        }

        String lopr = headers.get(ApplianceArchiveReaderConstants.LOPR);
        String low = headers.get(ApplianceArchiveReaderConstants.LOW);
        String lolo = headers.get(ApplianceArchiveReaderConstants.LOLO);
        String egu = headers.get(ApplianceArchiveReaderConstants.EGU);
        String prec = headers.get(ApplianceArchiveReaderConstants.PREC);
        String high = headers.get(ApplianceArchiveReaderConstants.HIGH);
        String hihi = headers.get(ApplianceArchiveReaderConstants.HIHI);
        String hopr = headers.get(ApplianceArchiveReaderConstants.HOPR);

        final Range range = Range.of((lopr != null) ? Double.parseDouble(lopr) : Double.NaN,
                                     (hopr != null) ? Double.parseDouble(hopr) : Double.NaN);
        return Display.of(range,
                Range.of((lolo != null) ? Double.parseDouble(lolo) : Double.NaN,
                         (hihi != null) ? Double.parseDouble(hihi) : Double.NaN),
                Range.of((low != null) ? Double.parseDouble(low) : Double.NaN,
                         (high != null) ? Double.parseDouble(high) : Double.NaN),
                range,
                (egu != null) ? egu : "",
                (prec != null) ? NumberFormats.precisionFormat((int) Math.round(Double.parseDouble(prec)))
                               : NumberFormats.toStringFormat());
    }


    /**
     * Determines alarm severity from the given numerical representation.
     *
     * @param severity numerical representation of alarm severity
     *
     * @return alarm severity
     */
    protected static AlarmSeverity getSeverity(int severity) {
        if (severity == 0) {
            return AlarmSeverity.NONE;
        } else if (severity == 1) {
            return AlarmSeverity.MINOR;
        } else if (severity == 2) {
            return AlarmSeverity.MAJOR;
        } else if (severity == 3) {
            return AlarmSeverity.INVALID;
        } else {
            return AlarmSeverity.UNDEFINED;
        }
    }

    /**
     * Determines alarm status from the given numerical representation.
     *
     * @param status numerical representation of alarm severity
     *
     * @return alarm status
     */
    protected static String getStatus(int status) {
        return Status.forValue(status).getName();
    }
}
