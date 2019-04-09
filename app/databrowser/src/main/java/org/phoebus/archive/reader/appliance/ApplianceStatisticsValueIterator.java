package org.phoebus.archive.reader.appliance;

import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;

import org.epics.archiverappliance.retrieval.client.DataRetrieval;
import org.epics.archiverappliance.retrieval.client.EpicsMessage;
import org.epics.archiverappliance.retrieval.client.GenMsgIterator;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.VStatistics;
import org.epics.vtype.VType;
import org.phoebus.archive.vtype.TimestampHelper;
import org.phoebus.pv.TimeHelper;

import edu.stanford.slac.archiverappliance.PB.EPICSEvent.PayloadType;

/**
 *
 * <code>ApplianceStatisticsValueIterator</code> loads the statistical data for the bins in the selected time range. The
 * bins are defined by the selected time range and the requested number of points.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class ApplianceStatisticsValueIterator extends ApplianceMeanValueIterator {

    private Iterator<EpicsMessage> stdIterator;
    private GenMsgIterator stdStream;
    private Iterator<EpicsMessage> minIterator;
    private GenMsgIterator minStream;
    private Iterator<EpicsMessage> maxIterator;
    private GenMsgIterator maxStream;
    private Iterator<EpicsMessage> countIterator;
    private GenMsgIterator countStream;

    /**
     * Constructs a new value iterator, which uses different calls to retrieve statistical data for each bin that
     * matches the criteria.
     *
     * @param reader the reader that created this iterator
     * @param name the name of the PV
     * @param start the start time of the data window
     * @param end the end time of the data window
     * @param points the number of requested points
     * @param listener the listener that is notified when the iterator is closed
     *
     * @throws IOException if there was an error during the data fetch process
     * @throws ArchiverApplianceException if it is not possible to load optimized data for the selected PV
     * @throws ArchiverApplianceInvalidTypeException if the type of data cannot be returned in optimized format
     */
    public ApplianceStatisticsValueIterator(ApplianceArchiveReader reader, String name, Instant start, Instant end,
        int points, IteratorListener listener) throws ArchiverApplianceException, IOException {
        super(reader, name, start, end, points, listener);
    }

    @Override
    protected void fetchDataInternal(String pvName) throws ArchiverApplianceException {
        super.fetchDataInternal(pvName);
        int interval = Math.max(1, (int) ((end.getEpochSecond() - start.getEpochSecond()) / requestedPoints));
        java.sql.Timestamp sqlStartTimestamp = TimestampHelper.toSQLTimestamp(start);
        java.sql.Timestamp sqlEndTimestamp = TimestampHelper.toSQLTimestamp(end);

        String std = new StringBuilder().append(ApplianceArchiveReaderConstants.OP_STD).append(interval).append('(')
            .append(pvName).append(')').toString();
        DataRetrieval dataRetrieval = reader.createDataRetriveal(reader.getDataRetrievalURL());
        stdStream = dataRetrieval.getDataForPV(std, sqlStartTimestamp, sqlEndTimestamp);
        if (stdStream != null) {
            stdIterator = stdStream.iterator();
        } else {
            throw new ArchiverApplianceException("Could not fetch standard deviation data.");
        }

        String min = new StringBuilder().append(ApplianceArchiveReaderConstants.OP_MIN).append(interval).append('(')
            .append(pvName).append(')').toString();
        dataRetrieval = reader.createDataRetriveal(reader.getDataRetrievalURL());
        minStream = dataRetrieval.getDataForPV(min, sqlStartTimestamp, sqlEndTimestamp);
        if (minStream != null) {
            minIterator = minStream.iterator();
        } else {
            throw new ArchiverApplianceException("Could not fetch minimum data.");
        }

        String max = new StringBuilder().append(ApplianceArchiveReaderConstants.OP_MAX).append(interval).append('(')
            .append(pvName).append(')').toString();
        dataRetrieval = reader.createDataRetriveal(reader.getDataRetrievalURL());
        maxStream = dataRetrieval.getDataForPV(max, sqlStartTimestamp, sqlEndTimestamp);
        if (maxStream != null) {
            maxIterator = maxStream.iterator();
        } else {
            throw new ArchiverApplianceException("Could not fetch maximum data.");
        }

        String count = new StringBuilder().append(ApplianceArchiveReaderConstants.OP_COUNT).append(interval).append('(')
            .append(pvName).append(')').toString();
        dataRetrieval = reader.createDataRetriveal(reader.getDataRetrievalURL());
        countStream = dataRetrieval.getDataForPV(count, sqlStartTimestamp, sqlEndTimestamp);
        if (countStream != null) {
            countIterator = countStream.iterator();
        } else {
            throw new ArchiverApplianceException("Could not fetch count data.");
        }
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
                if (stdStream != null) {
                    stdStream.close();
                }
                if (minStream != null) {
                    minStream.close();
                }
                if (maxStream != null) {
                    maxStream.close();
                }
                if (countStream != null) {
                    countStream.close();
                }
                super.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public VType next() {
        PayloadType type = mainStream.getPayLoadInfo().getType();
        if (type == PayloadType.SCALAR_BYTE
            || type == PayloadType.SCALAR_DOUBLE
            || type == PayloadType.SCALAR_FLOAT
            || type == PayloadType.SCALAR_INT
            || type == PayloadType.SCALAR_SHORT) {
            synchronized (this) {
                if (closed)
                    return null;
                EpicsMessage meanResult = mainIterator.next();
                return VStatistics.of(meanResult.getNumberValue().doubleValue(),
                                      stdIterator.next().getNumberValue().doubleValue(),
                                      minIterator.next().getNumberValue().doubleValue(),
                                      maxIterator.next().getNumberValue().doubleValue(),
                                      countIterator.next().getNumberValue().intValue(),
                                      Alarm.of(getSeverity(meanResult.getSeverity()), AlarmStatus.CLIENT, String.valueOf(meanResult.getStatus())),
                                      TimeHelper.fromInstant(TimestampHelper.fromSQLTimestamp(meanResult.getTimestamp())),
                                      Display.none());
            }
        }
        throw new UnsupportedOperationException("PV type " + type + " is not supported.");
    }
}
