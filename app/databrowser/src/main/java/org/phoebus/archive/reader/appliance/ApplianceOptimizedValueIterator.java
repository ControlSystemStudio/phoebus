package org.phoebus.archive.reader.appliance;

import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;

import org.phoebus.archive.vtype.ArchiveVNumber;
import org.phoebus.archive.vtype.ArchiveVStatistics;
import org.phoebus.archive.vtype.TimestampHelper;
import org.epics.archiverappliance.retrieval.client.DataRetrieval;
import org.epics.archiverappliance.retrieval.client.EpicsMessage;
import org.epics.archiverappliance.retrieval.client.GenMsgIterator;
import org.phoebus.util.text.NumberFormats;
import org.phoebus.vtype.Display;
import org.phoebus.vtype.VType;
import org.phoebus.vtype.ValueFactory;

import edu.stanford.slac.archiverappliance.PB.EPICSEvent.PayloadInfo;
import edu.stanford.slac.archiverappliance.PB.EPICSEvent.PayloadType;

/**
 *
 * <code>ApplianceOptimizedValueIterator</code> retrieves the data using the optimised iterator, which returns the
 * samples based on the time window and requested number of points. If it has less samples than requested it returns raw
 * data, if it has more samples it returns the returns data bins.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class ApplianceOptimizedValueIterator extends ApplianceValueIterator {

    private final int requestedPoints;
    private final boolean useStatistics;

    /**
     * Constructor that fetches data from appliance archive reader.
     *
     * @param reader instance of appliance archive reader
     * @param name name of the PV
     * @param start start of the time period
     * @param end end of the time period
     * @param points the number of requested points
     * @param useStatistics true if the returned data should include statistics or false if only mean value should be
     *            present
     * @param listener the listener that is notified when the iterator is closed
     *
     * @throws IOException if there was an error during the data fetch process
     * @throws ArchiverApplianceInvalidTypeException if the type of data cannot be returned in optimized format
     * @throws ArchiverApplianceException if it is not possible to load optimised data for the selected PV
     */
    public ApplianceOptimizedValueIterator(ApplianceArchiveReader reader, String name, Instant start, Instant end,
            int points, boolean useStatistics, IteratorListener listener) throws ArchiverApplianceException,
            IOException {
        super(reader, name, start, end, listener);
        this.requestedPoints = points;
        this.useStatistics = useStatistics;
        this.display = determineDisplay(reader, name, end);
        fetchData();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.csstudio.archive.reader.appliance.ApplianceValueIterator#fetchDataInternal(java.lang.String)
     */
    @Override
    protected void fetchDataInternal(String pvName) throws ArchiverApplianceException {
        String optimized = new StringBuilder().append(ApplianceArchiveReaderConstants.OP_OPTIMIZED)
                .append(requestedPoints).append('(').append(pvName).append(')').toString();
        super.fetchDataInternal(optimized);
    }

    /**
     * Determine and return display values.
     *
     * @param reader instance of appliance archive reader
     * @param name name of the PV
     *
     * @return the display
     * @throws IOException if there was an error reading data
     * @throws ArchiverApplianceInvalidTypeException if the data cannot be loaded with the optimized method
     */
    private Display determineDisplay(ApplianceArchiveReader reader, String name, Instant time)
            throws ArchiverApplianceInvalidTypeException, IOException {
        // to retrieve the display, request the raw data for the end timestamp
        java.sql.Timestamp timestamp = TimestampHelper.toSQLTimestamp(time);
        DataRetrieval dataRetrieval = reader.createDataRetriveal(reader.getDataRetrievalURL());
        GenMsgIterator genMsgIterator = dataRetrieval.getDataForPV(name, timestamp, timestamp);
        if (genMsgIterator != null) {
            try {
                PayloadInfo payloadInfo = null;
                Iterator<EpicsMessage> it = genMsgIterator.iterator();
                if (it.hasNext()) {
                    it.next();
                    payloadInfo = genMsgIterator.getPayLoadInfo();
                    if (!isDataTypeOKForOptimized(payloadInfo.getType())) {
                        throw new ArchiverApplianceInvalidTypeException("Cannot use optimized data on type "
                                + payloadInfo.getType(), name, payloadInfo.getType());
                    }
                    return getDisplay(payloadInfo);
                }
            } finally {
                genMsgIterator.close();
            }
        }

        return ValueFactory.newDisplay(Double.NaN, Double.NaN, Double.NaN, "", NumberFormats.toStringFormat(),
                Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
    }

    /**
     * Check if the type of data is OK to be loaded in mean mode. Mean mode is possible only with numeric scalars.
     *
     * @param type the type to check
     * @return true if OK or false otherwise
     */
    private boolean isDataTypeOKForOptimized(PayloadType type) {
        return type == PayloadType.SCALAR_BYTE || type == PayloadType.SCALAR_DOUBLE || type == PayloadType.SCALAR_FLOAT
                || type == PayloadType.SCALAR_INT || type == PayloadType.SCALAR_SHORT;
    }

    @Override
    public VType next() {
        EpicsMessage message;
        synchronized (this) {
            if (closed) {
                return null;
            }
            message = mainIterator.next();
        }
        PayloadType type = mainStream.getPayLoadInfo().getType();
        if (type == PayloadType.WAVEFORM_DOUBLE) {
            if (closed) {
                return null;
            }
            if (message.getElementCount() < 5) {
//                throw new ArchiverApplianceException(
//                        "The optimized post processor returned less than 5 values per sample.");
            }
            if (useStatistics) {
                return new ArchiveVStatistics(
                        TimestampHelper.fromSQLTimestamp(message.getTimestamp()),
                        getSeverity(message.getSeverity()),
                        String.valueOf(message.getStatus()),
                        display,
                        message.getNumberAt(0).doubleValue(),
                        message.getNumberAt(2).doubleValue(),
                        message.getNumberAt(3).doubleValue(),
                        message.getNumberAt(1).doubleValue(),
                        message.getNumberAt(4).intValue());
            } else {
                return new ArchiveVNumber(
                        TimestampHelper.fromSQLTimestamp(message.getTimestamp()),
                        getSeverity(message.getSeverity()),
                        String.valueOf(message.getStatus()),
                        display,
                        message.getNumberAt(0).doubleValue());
            }
        } else {
            // raw data
            return super.extractData(message);
        }
    }
}