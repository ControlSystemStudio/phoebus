/*
 * *
 *  * Copyright (C) 2019 European Spallation Source ERIC.
 *  * <p>
 *  * This program is free software; you can redistribute it and/or
 *  * modify it under the terms of the GNU General Public License
 *  * as published by the Free Software Foundation; either version 2
 *  * of the License, or (at your option) any later version.
 *  * <p>
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  * <p>
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package org.phoebus.applications.saveandrestore.datamigration.git;

import org.epics.util.array.*;
import org.epics.util.stats.Range;
import org.epics.vtype.*;
import org.phoebus.applications.saveandrestore.Utilities;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.ui.model.SnapshotEntry;
import org.phoebus.applications.saveandrestore.ui.model.VDisconnectedData;
import org.phoebus.applications.saveandrestore.ui.model.VSnapshot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;



/**
 *
 * <code>FileUtilities</code> provides utility methods for reading and writing snapshot and save set files. All methods
 * in this class are thread safe.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public final class FileUtilities {

    // the date tag for the snapshot files
    private static final String DATE_TAG = "Date:";
    // the description tag for the save set files
    private static final String DESCRIPTION_TAG = "Description:";
    // the names of the headers in the csv files
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
    // the complete snapshot file header
    public static final String SNAPSHOT_FILE_HEADER = H_PV_NAME + "," + H_SELECTED + "," + H_TIMESTAMP + "," + H_STATUS
        + "," + H_SEVERITY + "," + H_VALUE_TYPE + "," + H_VALUE + "," + H_READBACK + "," + H_READBACK_VALUE + ","
        + H_DELTA + "," + H_READ_ONLY;
    public static final String SAVE_SET_HEADER = H_PV_NAME + "," + H_READBACK + "," + H_DELTA + "," + H_READ_ONLY;
    // delimiter of array values
    private static final String ARRAY_SPLITTER = "\\;";
    // delimiter of enum value and enum constants
    private static final String ENUM_VALUE_SPLITTER = "\\~";
    // proposed length of snapshot file data line entry (pv name only)
    private static final int SNP_ENTRY_LENGTH = 700;
    // proposed length of save set data line entry (pv name only)
    private static final int BSD_ENTRY_LENGTH = 250;
    // the format used to store the timestamp of when the snapshot was taken
    private static final ThreadLocal<DateFormat> TIMESTAMP_FORMATTER = ThreadLocal
        .withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));
    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\n");

    /**
     * Private constructor to prevent instantiation of this class.
     */
    private FileUtilities() {
    }

    /**
     * Read the contents of the snapshot file from the given input stream.
     *
     * @param stream the source of data
     * @return the data, where the description contains the timestamp of the snapshot, names contain the pv names, and
     *         data are the pv values
     * @throws IOException if reading the file failed
     */
    public static SnapshotContent readFromSnapshot(InputStream stream) throws IOException, ParseException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        String date = null;
        List<SnapshotEntry> entries = new ArrayList<>();
        String line;
        String[] header = null;
        Map<String, Integer> headerMap = new HashMap<>();
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            } else if (header == null && line.charAt(0) == '#') {
                int idx = line.indexOf(DATE_TAG);
                if (idx > -1) {
                    date = line.substring(idx + DATE_TAG.length()).trim();
                }
            } else if (header == null) {
                header = line.split("\\,");
                for (int i = 0; i < header.length; i++) {
                    headerMap.put(header[i].toUpperCase(Locale.UK), Integer.valueOf(i));
                }
            } else {
                if (headerMap.isEmpty()) {
                    throw new IOException("The Snapshot content is invalid. No CSV header is defined.");
                }
                // there are no fields in here that may contain a comma
                String[] split = split(line);
                if (split == null) {
                    throw new IOException(String.format("Invalid content: %s.", line));
                }
                int length = split.length - 1;
                Integer idx = headerMap.get(H_PV_NAME);
                String name = idx == null || idx > length ? null : trim(split[idx]);
                idx = headerMap.get(H_SELECTED);
                String sel = idx == null || idx > length ? null : trim(split[idx]);
                idx = headerMap.get(H_TIMESTAMP);
                String timestamp = idx == null || idx > length ? null : trim(split[idx]);
                idx = headerMap.get(H_STATUS);
                String status = idx == null || idx > length ? "" : trim(split[idx]);
                idx = headerMap.get(H_SEVERITY);
                String severity = idx == null || idx > length ? "" : trim(split[idx]);
                idx = headerMap.get(H_VALUE_TYPE);
                String valueType = idx == null || idx > length ? null : trim(split[idx]);
                idx = headerMap.get(H_VALUE);
                String value = idx == null || idx > length ? null : trim(split[idx]);
                idx = headerMap.get(H_READBACK);
                String readback = idx == null || idx > length ? "" : trim(split[idx]);
                idx = headerMap.get(H_READBACK_VALUE);
                String readbackValue = idx == null || idx > length ? null : trim(split[idx]);
                idx = headerMap.get(H_DELTA);
                String delta = idx == null || idx > length ? "" : trim(split[idx]);
                idx = headerMap.get(H_READ_ONLY);
                Boolean readOnly = idx == null || idx > length ? Boolean.FALSE : Boolean.valueOf(trim(split[idx]));

                VType data = null, readbackData = null;
                try {
                    data = piecesToVType(timestamp, status, severity, value, valueType);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    // number format covers errors in parsing to VNumber, index out of bounds covers the enum parsing
                    // errors
                    data = VDisconnectedData.INSTANCE;
                }
                try {
                    readbackData = piecesToVType(timestamp, status, severity, readbackValue, valueType);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    readbackData = VDisconnectedData.INSTANCE;
                }

                boolean selected = true;
                try {
                    selected = Integer.parseInt(sel) != 0;
                } catch (NumberFormatException e) {
                    // ignore
                }
                ConfigPv configPv = ConfigPv.builder().pvName(name).readbackPvName(readback).build();
                entries.add(new SnapshotEntry(configPv, data, selected, readback, readbackData, delta, readOnly));
            }
        }
        if (date == null || date.isEmpty()) {
            throw new ParseException("Snapshot does not have a date set.", 0);
        }
        Instant d = TIMESTAMP_FORMATTER.get().parse(date).toInstant();
        return new SnapshotContent(d, entries);
    }

    /**
     * Trim the data of leading and trailing quotes and white spaces.
     *
     * @param valueToTrim the value to trim
     * @return trimmed value
     */
    private static String trim(String valueToTrim) {
        String value = valueToTrim.trim();
        if (!value.isEmpty() && value.charAt(0) == '"') {
            value = value.substring(1, value.length() - 1).trim();
        }
        return value;
    }

    /**
     * Converts a single entry to the VType.
     *
     * @param timestamp the timestamp of the entry, given in sec.nano format
     * @param status the alarm status
     * @param severity the alarm severity
     * @param value the raw value
     * @param valueType the value type
     * @return VType that contains all parameters and matches the type provided by <code>valueType</code>
     */
    private static VType piecesToVType(String timestamp, String status, String severity, String value,
        String valueType) {
        if (value == null || value.isEmpty() || "null".equalsIgnoreCase(value)
            || VDisconnectedData.INSTANCE.toString().equals(value)) {
            return VDisconnectedData.INSTANCE;
        }
        String[] t = timestamp != null && timestamp.indexOf('.') > 0 ? timestamp.split("\\.")
            : new String[] { "0", "0" };
        Time time = Time.of(Instant.ofEpochSecond(Long.parseLong(t[0]), Integer.parseInt(t[1])));
        AlarmStatus alarmStatus = null;
        try {
            alarmStatus = AlarmStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            alarmStatus = AlarmStatus.UNDEFINED;
        }
        Alarm alarm = Alarm.of(severity.isEmpty() ? AlarmSeverity.NONE : AlarmSeverity.valueOf(severity.toUpperCase(Locale.UK)),
                alarmStatus, "");
        Display display =
                Display.of(Range.of(0d, 0d), Range.of(0d, 0d), Range.of(0d, 0d), Range.of(0d, 0d), "", DecimalFormat.getInstance());
        //ValueType vtype = ValueType.forName(valueType);

        String[] valueAndLabels = value.split(ENUM_VALUE_SPLITTER);
        if (valueAndLabels.length > 0) {
            if (valueAndLabels[0].charAt(0) == '[') {
                valueAndLabels[0] = valueAndLabels[0].substring(1, valueAndLabels[0].length() - 1);
            }
            if (valueAndLabels.length > 1) {
                valueAndLabels[1] = valueAndLabels[1].substring(1, valueAndLabels[1].length() - 1);
            }
        }
        String theValue = valueAndLabels[0];
        switch (valueType) {
            case "double_array":
            case "number_array":
                String[] sd = theValue.split(ARRAY_SPLITTER, -1);
                double[] dd = new double[sd.length];
                for (int i = 0; i < sd.length; i++) {
                    if (sd[i].isEmpty()) {
                        dd[i] = 0;
                    } else {
                        dd[i] = Double.parseDouble(sd[i]);
                    }
                }
                ListDouble datad = new ArrayDouble(CollectionNumbers.toList(dd));
                return VDoubleArray.of(datad, alarm, time, display);
            case "float_array":
                String[] sf = theValue.split(ARRAY_SPLITTER, -1);
                float[] df = new float[sf.length];
                for (int i = 0; i < sf.length; i++) {
                    if (sf[i].isEmpty()) {
                        df[i] = 0f;
                    } else {
                        df[i] = Float.parseFloat(sf[i]);
                    }
                }
                ListFloat dataf = new ArrayFloat(CollectionNumbers.toList(df));
                return VFloatArray.of(dataf, alarm, time, display);
            case "long_array":
                String[] sl = theValue.split(ARRAY_SPLITTER, -1);
                long[] dl = new long[sl.length];
                for (int i = 0; i < sl.length; i++) {
                    if (sl[i].isEmpty()) {
                        dl[i] = 0L;
                    } else {
                        dl[i] = Long.parseLong(sl[i]);
                    }
                }
                ListLong datal = new ArrayLong(CollectionNumbers.toList(dl));
                return VLongArray.of(datal, alarm, time, display);
            case "int_array":
                String[] si = theValue.split(ARRAY_SPLITTER, -1);
                int[] di = new int[si.length];
                for (int i = 0; i < si.length; i++) {
                    if (si[i].isEmpty()) {
                        di[i] = 0;
                    } else {
                        di[i] = Integer.parseInt(si[i]);
                    }
                }
                ListInteger datai = new ArrayInteger(CollectionNumbers.toList(di));
                return VIntArray.of(datai, alarm, time, display);
            case "short_array":
                String[] ss = theValue.split(ARRAY_SPLITTER, -1);
                short[] ds = new short[ss.length];
                for (int i = 0; i < ss.length; i++) {
                    if (ss[i].isEmpty()) {
                        ds[i] = (short) 0;
                    } else {
                        ds[i] = Short.parseShort(ss[i]);
                    }
                }
                ListShort datas = new ArrayShort(CollectionNumbers.toList(ds));
                return VShortArray.of(datas, alarm, time, display);
            case "byte_array":
                String[] sb = theValue.split(ARRAY_SPLITTER, -1);
                byte[] db = new byte[sb.length];
                for (int i = 0; i < sb.length; i++) {
                    if (sb[i].isEmpty()) {
                        db[i] = (byte) 0;
                    } else {
                        db[i] = Byte.parseByte(sb[i]);
                    }
                }
                ListByte datab = new ArrayByte(CollectionNumbers.toList(db));
                return VByteArray.of(datab, alarm, time, display);
            case "enum_array":
                String[] se = theValue.split(ARRAY_SPLITTER, -1);
                List<String> labels = Arrays.asList(valueAndLabels[1].split(ARRAY_SPLITTER));
                int[] de = new int[se.length];
                for (int i = 0; i < se.length; i++) {
                    de[i] = labels.indexOf(se[i]);
                }
                ListInteger datae = new ArrayInteger(CollectionNumbers.toList(de));
                EnumDisplay enumDisplay =
                        EnumDisplay.of(de.length);
                return VEnumArray.of(datae, enumDisplay, alarm, time);
            case "string_array":
                String[] str = theValue.split(ARRAY_SPLITTER, -1);
                List<Integer> sizes = new ArrayList<>();
                List<String> data = new ArrayList<>();
                Arrays.stream(str).forEach(s -> sizes.add(s.length()));
                return VStringArray.of(Arrays.asList(str),
                        new ArrayInteger(CollectionNumbers.toList(sizes)), alarm, time);
            case "boolean_array":
                String[] sbo = theValue.split(ARRAY_SPLITTER, -1);
                boolean[] dbo = new boolean[sbo.length];
                for (int i = 0; i < sbo.length; i++) {
                    dbo[i] = Boolean.parseBoolean(sbo[i]);
                }
                ListBoolean databo = new ArrayBoolean(dbo);
                return VBooleanArray.of(databo, alarm, time);
            case "double":
            case "number":
                return VDouble.of(Double.parseDouble(theValue), alarm, time, display);
            case "float":
                return VFloat.of(Float.parseFloat(theValue), alarm, time, display);
            case "long":
                return VLong.of(Long.parseLong(theValue), alarm, time, display);
            case "int":
                return VInt.of(Integer.parseInt(theValue), alarm, time, display);
            case "short":
                return VShort.of(Short.parseShort(theValue), alarm, time, display);
            case "byte":
                return VByte.of(Byte.parseByte(theValue), alarm, time, display);
            case "boolean":
                return VBoolean.of(Boolean.parseBoolean(theValue), alarm, time);
            case "string":
                return VString.of(theValue, alarm, time);
            case "enum":
                List<String> lbls = new ArrayList<>(Arrays.asList(valueAndLabels[1].split("\\;", -1)));
                int idx = lbls.indexOf(theValue);
                if (idx < 0) {
                    try {
                        idx = Integer.parseInt(theValue);
                    } catch (NumberFormatException e) {
                        idx = 0;
                    }
                    if (lbls.size() <= idx) {
                        for (int i = lbls.size(); i <= idx; i++) {
                            lbls.add(String.valueOf(i));
                        }
                    }
                }
                // TODO fix
                EnumDisplay enumDisplay1 = EnumDisplay.of(lbls);
                return VEnum.of(idx, enumDisplay1, alarm, time);
            case "na":
                return VDisconnectedData.INSTANCE;
        }

        throw new IllegalArgumentException(String.format("Unknown data type %s.", valueType));
    }


    /**
     * Read the contents of the save set from the input stream.
     *
     * @param stream the source of data
     * @return the data, where the description is the description read from the file and there are no data, just names
     * @throws IOException if there was an error reading the file content
     */
    public static SaveSetContent readFromSaveSet(InputStream stream) throws IOException {
        StringBuilder description = new StringBuilder(400);
        List<SaveSetEntry> entries = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        boolean isDescriptionLine = false;
        String line;
        String[] header = null;
        int namesIndex = -1;
        int readbackIndex = -1;
        int deltaIndex = -1;
        int readOnlyIndex = -1;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            } else if (header == null && line.charAt(0) == '#') {
                line = line.substring(1).trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (isDescriptionLine) {
                    description.append(line).append('\n');
                } else if (line.contains(DESCRIPTION_TAG)) {
                    isDescriptionLine = true;
                }
            } else if (header == null) {
                isDescriptionLine = false;
                header = line.split("\\,");
                for (int i = 0; i < header.length; i++) {
                    if (H_PV_NAME.equals(header[i])) {
                        namesIndex = i;
                    } else if (H_READBACK.equals(header[i])) {
                        readbackIndex = i;
                    } else if (H_DELTA.equals(header[i])) {
                        deltaIndex = i;
                    } else if (H_READ_ONLY.equals(header[i])) {
                        readOnlyIndex = i;
                    }
                }
            } else {
                String[] split = split(line);
                if (split == null) {
                    throw new IOException(String.format("Invalid content: %s.", line));
                }
                // there are no fields in here that may contain a comma
                String name = null, readback = null, delta = null;
                boolean readOnly = false;
                if (namesIndex > -1) {
                    name = trim(split[namesIndex]);
                } else {
                    continue;
                }
                if (readbackIndex != -1) {
                    readback = trim(split[readbackIndex]);
                }
                if (deltaIndex != -1) {
                    delta = trim(split[deltaIndex]);
                }
                if (readOnlyIndex != -1) {
                    readOnly = Boolean.valueOf(trim(split[readOnlyIndex]));
                }
                entries.add(new SaveSetEntry(name, readback, delta, readOnly));
            }
        }
        return new SaveSetContent(description.toString().trim(), entries);
    }

    /**
     * Split the given content by comma. However if a part of the content is in quotes, that part should not be split if
     * it contains any commas. For example foo,bar will be returned as an array of length 2; "foo,bar" will be returned
     * as an array of 1.
     *
     * @param content the content to split
     * @return the array containing individual parts of the content
     */
    public static String[] split(String content) {
        content = content.trim();
        int idx = content.indexOf(',');
        if (idx < 0) {
            return new String[] { content };
        } else if (content.indexOf('"') < 0) {
            return content.split("\\,", -1);
        } else {
            List<String> parts = new ArrayList<>();

            while (true) {
                idx = content.indexOf(',');
                int quote = content.indexOf('"');
                int quote2 = content.indexOf('"', quote + 1);
                if (quote > -1 && quote2 < 0) {
                    // something is wrong - only one quote
                    return null;
                } else if (quote < 0) {
                    parts.addAll(Arrays.asList(content.split("\\,", -1)));
                    break;
                }

                if (quote < idx) {
                    // started with a quote
                    parts.add(content.substring(1, quote2).trim());
                } else if (quote > idx) {
                    // quoted part is in between
                    String firstPart = content.substring(0, quote - 1).trim();
                    parts.addAll(Arrays.asList(firstPart.split("\\,", -1)));
                    String secondPart = content.substring(quote + 1, quote2).trim();
                    parts.add(secondPart);
                }
                if (content.length() > quote2 + 1) {
                    content = content.substring(quote2 + 1).trim();
                    if (content.charAt(0) == ',') {
                        content = content.substring(1);
                    } else {
                        return null;
                    }
                } else {
                    break;
                }
            }
            return parts.toArray(new String[parts.size()]);
        }
    }
}
