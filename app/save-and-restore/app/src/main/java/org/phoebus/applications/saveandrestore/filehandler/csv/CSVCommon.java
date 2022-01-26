/**
 * Copyright (C) 2020 Facility for Rare Isotope Beams
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact Information: Facility for Rare Isotope Beam,
 *                      Michigan State University,
 *                      East Lansing, MI 48824-1321
 *                      http://frib.msu.edu
 */
package org.phoebus.applications.saveandrestore.filehandler.csv;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

/**
 * Common values and methods for both exporting and importing savesets and snapshots.
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public abstract class CSVCommon {
    public static final String ARRAY_SPLITTER = ";";
    public static final String ENUM_VALUE_SPLITTER = "~";
    public static final String CSV_SEPARATOR=",";

    public static final String COMMENT_PREFIX = "#";

    public static final String HIERARCHY_TAG = "Hierarchy:";
    public static final String SAVESETNAME_TAG = "SaveSet name:";
    public static final String SNAPSHOTNAME_TAG = "Snapshot name:";
    public static final String DESCRIPTION_TAG = "Description:";
    public static final String CREATOR_TAG = "Creator:";
    public static final String DATE_TAG = "Date:";
    public static final String TAGS_TAG = "Tags:";
    public static final String OPENING_TAG = "AUTO-GENERATED. DON'T EDIT BELOW IF YOU DON'T KNOW HOW THOSE WORK!";
    public static final String ENDING_TAG = "AUTO-GENERATED. DON'T EDIT ABOVE IF YOU DON'T KNOW HOW THOSE WORK!";

    public static final String H_PV_NAME = "PV";
    public static final String H_READBACK = "READBACK";
    public static final String H_READBACK_VALUE = "READBACK_VALUE";
    public static final String H_DELTA = "DELTA";
    public static final String H_SELECTED = "SELECTED";
    public static final String H_TIMESTAMP = "TIMESTAMP";
    public static final String H_STATUS = "STATUS";
    public static final String H_SEVERITY = "SEVERITY";
    public static final String H_VALUE_TYPE = "VALUE_TYPE";
    public static final String H_VALUE = "VALUE";
    public static final String H_READ_ONLY = "READ_ONLY";

    protected static final List<String> SUPPORTED_TAGS = Arrays.asList(HIERARCHY_TAG, SAVESETNAME_TAG, SNAPSHOTNAME_TAG, DESCRIPTION_TAG, CREATOR_TAG, DATE_TAG, TAGS_TAG, OPENING_TAG, ENDING_TAG);
    protected static final List<String> SUPPORTED_COLUMNS = Arrays.asList(H_PV_NAME, H_SELECTED, H_TIMESTAMP, H_STATUS, H_SEVERITY, H_VALUE_TYPE, H_VALUE, H_READBACK, H_READBACK_VALUE, H_DELTA, H_READ_ONLY);

    protected static final ThreadLocal<DateFormat> TIMESTAMP_FORMATTER = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));

    protected static String Comment(String string) {
        return COMMENT_PREFIX + " " + string;
    }

    protected static String Uncomment(String string) {
        return string.replaceFirst(COMMENT_PREFIX, "").trim();
    }

    protected static String Record(String ... strings) {
        String record = "";

        for (int index = 0; index < strings.length; index++) {
            record += strings[index] == null ? "" : strings[index].trim();

            if (index != strings.length - 1) {
                record += CSV_SEPARATOR;
            }
        }

        return record;
    }
}
