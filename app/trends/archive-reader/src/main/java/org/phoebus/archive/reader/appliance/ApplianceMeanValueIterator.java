package org.phoebus.archive.reader.appliance;

import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;

import org.epics.archiverappliance.retrieval.client.DataRetrieval;
import org.epics.archiverappliance.retrieval.client.EpicsMessage;
import org.epics.archiverappliance.retrieval.client.GenMsgIterator;
import org.epics.vtype.Display;
import org.phoebus.archive.vtype.TimestampHelper;

import edu.stanford.slac.archiverappliance.PB.EPICSEvent.PayloadInfo;
import edu.stanford.slac.archiverappliance.PB.EPICSEvent.PayloadType;

/**
 *
 * <code>ApplianceMeanValueIterator</code> retrieves the mean value of the archived data bins.
 * The size of the bin is specified by the selected time range and the requested number of points.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class ApplianceMeanValueIterator extends ApplianceValueIterator {

    protected final int requestedPoints;

    /**
     * Constructor that fetches data from appliance archive reader.
     *
     * @param reader instance of appliance archive reader
     * @param name name of the PV
     * @param start start of the time period
     * @param end end of the time period
     * @param points the number of requested points
     * @param listener the listener that is notified when the iterator is closed
     *
     * @throws IOException if there was an error during the data fetch process
     * @throws ArchiverApplianceException if it is not possible to load optimized data for the selected PV
     * @throws ArchiverApplianceInvalidTypeException if the type of data cannot be returned in optimized format
     */
    public ApplianceMeanValueIterator(ApplianceArchiveReader reader,
            String name, Instant start, Instant end, int points, IteratorListener listener)
                    throws ArchiverApplianceException, IOException {
        super(reader,name,start,end,listener);
        this.requestedPoints = points;
        this.display = determineDisplay(reader, name, end);
        fetchData();
    }

    /*
     * (non-Javadoc)
     * @see org.csstudio.archive.reader.appliance.ApplianceValueIterator#fetchDataInternal(java.lang.String)
     */
    @Override
    protected void fetchDataInternal(String pvName) throws ArchiverApplianceException {
        int interval = Math.max(1,(int)((end.getEpochSecond() - start.getEpochSecond()) / requestedPoints));
        String mean = new StringBuilder().append(ApplianceArchiveReaderConstants.OP_MEAN).append(interval).append('(').append(pvName).append(')').toString();
        super.fetchDataInternal(mean);
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
            throws ArchiverApplianceInvalidTypeException,IOException {
        //to retrieve the display, request the raw data for the end timestamp
        java.sql.Timestamp timestamp = TimestampHelper.toSQLTimestamp(time);
        DataRetrieval dataRetrieval = reader.createDataRetriveal(reader.getDataRetrievalURL());
        GenMsgIterator genMsgIterator = dataRetrieval.getDataForPV(name, timestamp, timestamp);
        if (genMsgIterator != null) {
            try {
                PayloadInfo payloadInfo = null;
                Iterator<EpicsMessage> it = genMsgIterator.iterator();
                if(it.hasNext()) {
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

        return Display.none();
    }

    /**
     * Check if the type of data is OK to be loaded in mean mode.
     * Mean mode is possible only with numeric scalars.
     *
     * @param type the type to check
     * @return true if OK or false otherwise
     */
    private boolean isDataTypeOKForOptimized(PayloadType type) {
        return type == PayloadType.SCALAR_BYTE ||
                type == PayloadType.SCALAR_DOUBLE ||
                type == PayloadType.SCALAR_FLOAT ||
                type == PayloadType.SCALAR_INT ||
                type == PayloadType.SCALAR_SHORT;
    }
}