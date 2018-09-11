package org.phoebus.archive.reader.appliance;

import java.io.IOException;
import java.time.Instant;

/**
 *
 * <code>ApplianceNonNumericOptimizedValueIterator</code> loads every N-th value
 * from the archive, where N is calculated as totalNumberOfPoints/requestedPoints.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class ApplianceNonNumericOptimizedValueIterator extends ApplianceValueIterator {


    private final int requestedPoints;
    private final int totalNumberOfPoints;

    /**
     * Constructor that fetches data from appliance archive reader.
     *
     * @param reader instance of appliance archive reader
     * @param name name of the PV
     * @param start start of the time period
     * @param end end of the time period
     * @param requestedPoints number of requested points
     * @param totalNumberofPoints the number of all points in the archive
     * @param listener the listener that is notified when the iterator is closed
     *
     * @throws IOException if there was an error during the data fetch process
     * @throws ArchiverApplianceException if it is not possible to load optimized data for the selected PV
     */
    public ApplianceNonNumericOptimizedValueIterator(ApplianceArchiveReader reader,
            String name, Instant start, Instant end, int requestedPoints, int totalNumberOfPoints,
            IteratorListener listener) throws ArchiverApplianceException, IOException {
        super(reader,name,start,end,listener);
        this.requestedPoints = requestedPoints;
        this.totalNumberOfPoints = totalNumberOfPoints;
        fetchData();
    }

    /*
     * (non-Javadoc)
     * @see org.csstudio.archive.reader.appliance.ApplianceValueIterator#fetchDataInternal(java.lang.String)
     */
    @Override
    protected void fetchDataInternal(String pvName) throws ArchiverApplianceException {
        int n = (int)Math.max(1,totalNumberOfPoints/requestedPoints);
        if (1.5*requestedPoints < totalNumberOfPoints && totalNumberOfPoints < 2*requestedPoints) {
            n = 2;
        }
        if (n == 1) {
            super.fetchDataInternal(pvName);
        } else {
            String nth = new StringBuilder().append(ApplianceArchiveReaderConstants.OP_NTH).append(n).append('(')
                    .append(pvName).append(')').toString();
            super.fetchDataInternal(nth);
        }
    }
}
