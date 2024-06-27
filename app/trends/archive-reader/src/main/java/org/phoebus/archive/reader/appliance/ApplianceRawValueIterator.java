package org.phoebus.archive.reader.appliance;

import java.io.IOException;
import java.time.Instant;

/**
 *
 * <code>ApplianceRawValueIterator</code> loads the data using the raw operator
 * and does not apply any statistics function on the data. Data are returned as
 * they are stored on the server.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class ApplianceRawValueIterator extends ApplianceValueIterator {

    /**
     * Constructor for the raw value iterator. This iterator fetches data using the raw
     * format and does not apply any statistics calculation to it.
     *
     * @param reader instance of appliance archive reader
     * @param name name of the PV
     * @param start start of the time period
     * @param end end of the time period
     * @param listener the listener which is notified when the iterator is closed
     *
     * @throws IOException if there was an error during the data fetch process
     * @throws ArchiverApplianceException if the data cannot be loaded with this algorithm
     */
    public ApplianceRawValueIterator(ApplianceArchiveReader reader,
            String name, Instant start, Instant end, IteratorListener listener)
                    throws ArchiverApplianceException, IOException {
        super(reader,name,start,end,listener);
        fetchData();
    }
}