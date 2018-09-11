package org.phoebus.archive.reader.appliance;


/**
 * Interface that defines constants which are used in archive reader appliance.
 *
 * @author Miha Novak <miha.novak@cosylab.com>
 */
public class ApplianceArchiveReaderConstants {
    /** The name of the archiver appliance reader as it appears in the preferences */
    public static final String ARCHIVER_NAME = "EPICS Archiver Appliance";
    /** The description of the archiver appliance reader */
    public static final String ARCHIVER_DESCRIPTION = "http://epicsarchiverap.sourceforge.net/";
    /** The version of the appliance reader */
    public static final int VERSION = 1;
    /** The path from which the data is collected */
    public static final String RETRIEVAL_PATH = "/data/getData.raw";
    /** The path to retrieve the list of archived PVs from */
    public static final String SEARCH_PATH = "/bpl/searchForPVsRegex?regex=";
    /** Lower operating limit field */
    public static final String LOPR = "LOPR";
    /** Minor alarm lower limit field */
    public static final String LOW = "LOW";
    /** Major alarm lower limit field */
    public static final String LOLO = "LOLO";
    /** Minor alarm upper limit field */
    public static final String HIGH = "HIGH";
    /** Major alarm upper limit field */
    public static final String HIHI = "HIHI";
    /** Upper operating limit field */
    public static final String HOPR = "HOPR";
    /** Engineering units field */
    public static final String EGU = "EGU";
    /** Precision field */
    public static final String PREC = "PREC";
    /** Val field */
    public static final String VAL = "val";

    /** Operator for every nth value in the archive */
    public static final String OP_NTH = "nth_";
    /** Operator for counting number of samples in the archive */
    public static final String OP_NCOUNT = "ncount";
    /** Operator for the minimum bin value */
    public static final String OP_MIN = "min_";
    /** Operator for the maximum bin value */
    public static final String OP_MAX = "max_";
    /** Operator for the number of points in the bin */
    public static final String OP_COUNT = "count_";
    /** Operator for the standard deviation in the bin */
    public static final String OP_STD = "std_";
    /** Operator for the mean bin value */
    public static final String OP_MEAN = "mean_";
    /** Operator for the optimized post processor */
    public static final String OP_OPTIMIZED = "optimized_";

    /** The schema delimiter: ca://pvName or pva://pvname */
    static final String SCHEMA_DELIMITER = "://";

}
