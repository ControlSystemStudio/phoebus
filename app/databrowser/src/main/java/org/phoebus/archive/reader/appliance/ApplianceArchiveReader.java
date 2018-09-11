package org.phoebus.archive.reader.appliance;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.UnknownChannelException;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.archive.vtype.TimestampHelper;
import org.phoebus.ui.text.RegExHelper;
import org.epics.archiverappliance.retrieval.client.DataRetrieval;
import org.epics.archiverappliance.retrieval.client.EpicsMessage;
import org.epics.archiverappliance.retrieval.client.GenMsgIterator;
import org.epics.archiverappliance.retrieval.client.RawDataRetrieval;

/**
 * Appliance archive reader which reads data from EPICS archiver appliance.
 *
 * @author Miha Novak <miha.novak@cosylab.com>
 */
public class ApplianceArchiveReader implements ArchiveReader, IteratorListener {

    private final String httpURL;
    private final String pbrawURL;
    private final boolean useStatistics;
    private final boolean useNewOptimizedOperator;

    private Map<ApplianceValueIterator, ApplianceArchiveReader> iterators = Collections.synchronizedMap(
               new WeakHashMap<ApplianceValueIterator, ApplianceArchiveReader>());

    /**
     * Constructor that sets appliance archiver reader url.
     *
     * @param url appliance archiver reader url (with specific prefix)
     * @param useStatistics true if statistics type data should be returned when optimized data is requested
     * @deprecated use {@link ApplianceArchiveReader#ApplianceArchiveReader(String, boolean, boolean)}
     */
    @Deprecated
    public ApplianceArchiveReader(String url, boolean useStatistics) {
        this(url, useStatistics, true);
    }

    /**
     * Constructor that sets appliance archiver reader url and other parameters that define how the data loading will
     * be done.
     *
     * @param url appliance archiver reader url (with specific prefix)
     * @param useStatistics true if statistics type data should be returned when optimized data is requested
     * @param useNewOptimizedOperator true if the optimized operator should be used when fetching optimized data or
     *              false the reader should use the old mechanism of loading the number of points and the data
     *              separately (if true, the reader will try to use the optimized post processor, but if it fails
     *              it will fall back to the old system)
     */
    public ApplianceArchiveReader(String url, boolean useStatistics, boolean useNewOptimizedOperator) {
        //if the url ends with /, strip the url of the last character
        if (url.charAt(url.length()-1) == '/') {
            url = url.substring(0,url.length()-1);
        }
        this.useStatistics = useStatistics;
        this.useNewOptimizedOperator = useNewOptimizedOperator;
        this.pbrawURL = url;
        this.httpURL = pbrawURL.replace("pbraw://", "http://");
    }

    public String getServerName() {
        return ApplianceArchiveReaderConstants.ARCHIVER_NAME;
    }

    public String getURL() {
        return pbrawURL;
    }

    @Override
    public String getDescription() {
        StringBuilder description = new StringBuilder();
        description.append("Archiver appliance v ").append(getVersion()).append('\n');
        description.append("Server url: ").append(pbrawURL).append('\n');
        return description.toString();
    }

    public int getVersion() {
        return ApplianceArchiveReaderConstants.VERSION;
    }

    @Override
    public List<String> getNamesByPattern(String glob_pattern) throws Exception {
        return search(RegExHelper.fullRegexFromGlob(glob_pattern));
    }


    @Override
    public ValueIterator getRawValues(String name, Instant start, Instant end)
            throws UnknownChannelException, Exception {
        try {
            name = stripSchema(name);
            ApplianceRawValueIterator it = new ApplianceRawValueIterator(this, name, start, end, this);
            iterators.put(it,this);
            return it;
        } catch (ArchiverApplianceException ex) {
            throw new UnknownChannelException(name);
        }
    }

    @Override
    public ValueIterator getOptimizedValues(String name, Instant start, Instant end, int count) throws UnknownChannelException, Exception {
        boolean binningSupported = true;
        ApplianceValueIterator it = null;
        name = stripSchema(name);
        if (useNewOptimizedOperator) {
            //try to fetch the data using the new optimized operator
            try {
                it = new ApplianceOptimizedValueIterator(this, name, start, end, count, useStatistics, this);
            } catch (ArchiverApplianceInvalidTypeException e) {
                //binning not supported
                binningSupported = false;
            } catch (ArchiverApplianceException e) {
                //optimized operator not supported on the server, fall back to the old way
            }
        }

        //if the optimized operator worked, we're done, otherwise fallback to the previous mechanism
        if (it == null) {
            try {
                int points = getNumberOfPoints(name, start, end);
                if (points <= count) {
                    it = new ApplianceRawValueIterator(this, name, start, end, this);
                } else {
                    //only fetch if binning is "still" supported
                    if (binningSupported) {
                        try {
                            //try to bin the values using the mean and std etc. This will work for numeric scalar PVs
                            if (useStatistics) {
                                it = new ApplianceStatisticsValueIterator(this, name, start, end, count,this);
                            } else {
                                it = new ApplianceMeanValueIterator(this, name, start, end, count,this);
                            }
                        } catch (ArchiverApplianceInvalidTypeException e) {
                            binningSupported = false;
                        }
                    }

                    if (!binningSupported) {
                        //if binning is not supported (string, waveform type), try nth operator
                        it = new ApplianceNonNumericOptimizedValueIterator(this, name, start, end, count, points,this);
                    }
                }
            } catch (ArchiverApplianceException e) {
                //fallback for older archiver appliance, which didn't have the nth operator
                try {
                    it = new ApplianceRawValueIterator(this, name, start, end, this);
                } catch (ArchiverApplianceException exc) {
                    throw new UnknownChannelException(name);
                }
            }
        }
        iterators.put(it,this);
        return it;
    }

    @Override
    public void cancel() {
        ApplianceValueIterator[] its = iterators.keySet().toArray(new ApplianceValueIterator[0]);
        for (ApplianceValueIterator a : its) {
            a.close();
        }
    }

    @Override
    public void close() {
        cancel();
    }

    /**
     * Creates and returns DataRetrieval
     *
     * @param dataRetrievalURL
     * @return dataRetrieval instance
     */
    public DataRetrieval createDataRetriveal(String dataRetrievalURL) {
        return new RawDataRetrieval(dataRetrievalURL);
    }

    /**
     * Returns data retrieval URL. A data retrieval URL looks like
     * http://domain:port/retrieval/data/getData.raw where /data/getData is
     * fixed and .raw identifies the MIME-type of the returned data.
     *
     * To retrieve data for retired PV(not archived anymore), use
     * retiredPVTemplate as suggested by Murali Shankar e.g.
     *   CSS Data Browser URL:http://server:port_number/retrieval/data/getData.raw?retiredPVTemplate=templatePVName
     * where 'templatePVName' is the name of a currently archived PV of the
     * same data type as the retired PV you are attempting to plot.
     *
     * @return data retrieval URL
     */
    public String getDataRetrievalURL() {
        if (httpURL.contains("?")){
            return httpURL;
        } else {
            return httpURL + ApplianceArchiveReaderConstants.RETRIEVAL_PATH;
        }
    }

    /**
     * Search for PV names that match to the specified regular expression.
     *
     * @param reg regular expression
     * @return array with PV names that match to the given regular expression.
     * @throws IOException
     */
    private List<String> search(String reg) throws IOException {
        String searchURL = httpURL + ApplianceArchiveReaderConstants.SEARCH_PATH + URLEncoder.encode(reg, "UTF-8");
        URL url = new URL(searchURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        ArrayList<String> names = new ArrayList<String>();
        BufferedReader br = null;
        try {
            connection.connect();
            if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String pvName = null;
                while ((pvName = br.readLine()) != null) {
                    names.add(pvName);
                }
            }
        } finally {
            if (br != null) {
                br.close();
            }
            connection.disconnect();
        }
        return names;
    }

    /**
     * Counts the number of points in the given time window using the ncount operator.
     *
     * @param pvName the name of the PV
     * @param start the start time of the data window
     * @param end the end time of the data window
     * @return the number of points in the requested time window
     * @throws IOException if there was an error loading the number of points
     */
    private int getNumberOfPoints(String pvName, Instant start, Instant end) throws IOException {
        String countName = new StringBuilder().append(ApplianceArchiveReaderConstants.OP_NCOUNT).append('(').append(pvName).append(')').toString();
        DataRetrieval dataRetrieval = createDataRetriveal(getDataRetrievalURL());
        java.sql.Timestamp sqlStartTimestamp = TimestampHelper.toSQLTimestamp(start);
        java.sql.Timestamp sqlEndTimestamp = TimestampHelper.toSQLTimestamp(end);
        GenMsgIterator iterator = dataRetrieval.getDataForPV(countName, sqlStartTimestamp, sqlEndTimestamp);

        if (iterator != null) {
            try {
                Iterator<EpicsMessage> it = iterator.iterator();
                int numberOfPoints = 0;
                while(it.hasNext()) {
                    Number m = it.next().getNumberValue();
                    if (m == null) return numberOfPoints;
                    numberOfPoints += m.intValue();
                }
                return numberOfPoints;
            } finally {
                iterator.close();
            }
        }
        return getNumberOfPointsLegacy(pvName, start, end);
    }

    /**
     * Counts the number of points in the given time window using the bin count operator.
     *
     * @param pvName the name of the PV
     * @param start the start time of the data window
     * @param end the end time of the data window
     * @return the number of points
     * @throws IOException if there was an error loading the number of points
     */
    private int getNumberOfPointsLegacy(String pvName, Instant start, Instant end) throws IOException {
        int interval = Math.max(1,(int)(end.getEpochSecond() - start.getEpochSecond()));
        String countName = new StringBuilder().append(ApplianceArchiveReaderConstants.OP_COUNT).append(interval).append('(').append(pvName).append(')').toString();
        DataRetrieval dataRetrieval = createDataRetriveal(getDataRetrievalURL());
        java.sql.Timestamp sqlStartTimestamp = TimestampHelper.toSQLTimestamp(start);
        java.sql.Timestamp sqlEndTimestamp = TimestampHelper.toSQLTimestamp(end);
        GenMsgIterator iterator = dataRetrieval.getDataForPV(countName, sqlStartTimestamp, sqlEndTimestamp);

        if (iterator != null) {
            try {
                Iterator<EpicsMessage> it = iterator.iterator();
                int numberOfPoints = 0;
                while(it.hasNext()) {
                    Number m = it.next().getNumberValue();
                    if (m == null) return 0;
                    numberOfPoints += m.intValue();
                }
                return numberOfPoints;
            } finally {
                iterator.close();
            }
        }
        return 0;
    }

    @Override
    public void finished(ApplianceValueIterator iterator) {
        iterators.remove(iterator);
    }

    private static String stripSchema(String name) {
        int idx = name.indexOf(ApplianceArchiveReaderConstants.SCHEMA_DELIMITER);
        return idx > -1 ? name.substring(idx + ApplianceArchiveReaderConstants.SCHEMA_DELIMITER.length()) : name;
    }

}
