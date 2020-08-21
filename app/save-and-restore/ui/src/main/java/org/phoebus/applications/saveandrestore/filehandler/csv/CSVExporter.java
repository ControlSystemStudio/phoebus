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

import org.epics.util.array.ArrayByte;
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ArrayFloat;
import org.epics.util.array.ArrayInteger;
import org.epics.util.array.ArrayLong;
import org.epics.util.array.ArrayShort;
import org.epics.util.array.ArrayUByte;
import org.epics.util.array.ArrayUInteger;
import org.epics.util.array.ArrayULong;
import org.epics.util.array.ArrayUShort;
import org.epics.util.array.ListByte;
import org.epics.util.array.ListDouble;
import org.epics.util.array.ListFloat;
import org.epics.util.array.ListInteger;
import org.epics.util.array.ListLong;
import org.epics.util.array.ListShort;
import org.epics.util.array.ListUByte;
import org.epics.util.array.ListUInteger;
import org.epics.util.array.ListULong;
import org.epics.util.array.ListUShort;
import org.epics.util.number.UByte;
import org.epics.util.number.UInteger;
import org.epics.util.number.ULong;
import org.epics.util.number.UShort;
import org.epics.vtype.VEnum;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.ApplicationContextProvider;
import org.phoebus.applications.saveandrestore.DirectoryUtilities;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;

import java.io.PrintStream;
import java.time.Instant;
import java.util.List;

/**
 * Exporting saveset and snapshot to CSV format.
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public class CSVExporter extends CSVCommon {
    static private PrintStream printStream;
    static private SaveAndRestoreService saveAndRestoreService = (SaveAndRestoreService) ApplicationContextProvider.getApplicationContext().getAutowireCapableBeanFactory().getBean("saveAndRestoreService");

    private static Node nodeToExport = null;

    static public void export(Node node, String path) throws Exception {
        printStream = new PrintStream(path);
        nodeToExport = node;

        switch (node.getNodeType()) {
            case CONFIGURATION: exportSaveset();  break;
            case      SNAPSHOT: exportSnapshot(); break;
                       default: throw new Exception("The node of " + node.getNodeType() + " is not supported for export!");
        }
    }

    private static void exportSaveset() throws Exception {
        printStream.println(Comment(OPENING_TAG));

        printStream.println(Comment(HIERARCHY_TAG));
        printStream.println(Comment(DirectoryUtilities.CreateLocationString(nodeToExport, false)));

        printStream.println(Comment(SAVESETNAME_TAG));
        printStream.println(Comment(nodeToExport.getName()));

        printStream.println(Comment(DESCRIPTION_TAG));
        printStream.println(Comment(nodeToExport.getProperty("description").replaceAll("([\\r\\n])", "$1" + COMMENT_PREFIX + " ")).trim());

        printStream.println(Comment(ENDING_TAG));

        printStream.println(Record(H_PV_NAME, H_READBACK, H_READ_ONLY));
        List<ConfigPv> entries = saveAndRestoreService.getConfigPvs(nodeToExport.getUniqueId());
        for (ConfigPv entry : entries) {
            printStream.println(Record(entry.getPvName(), entry.getReadbackPvName(), entry.isReadOnly() ? "1" : "0"));
        }

        printStream.close();
    }


    private static void exportSnapshot() throws Exception {
        printStream.println(Comment(OPENING_TAG));

        printStream.println(Comment(HIERARCHY_TAG));
        printStream.println(Comment(DirectoryUtilities.CreateLocationString(nodeToExport, false)));

        printStream.println(Comment(SNAPSHOTNAME_TAG));
        printStream.println(Comment(nodeToExport.getName()));

        printStream.println(Comment(DESCRIPTION_TAG));
        printStream.println(Comment(nodeToExport.getProperty("comment").replaceAll("([\\r\\n])", "$1" + COMMENT_PREFIX)));

        printStream.println(Comment(CREATOR_TAG));
        printStream.println(Comment(nodeToExport.getUserName()));

        printStream.println(Comment(DATE_TAG));
        printStream.println(Comment(TIMESTAMP_FORMATTER.get().format(nodeToExport.getCreated())));

        printStream.println(Comment(TAGS_TAG));
        nodeToExport.getTags().forEach(tag -> printStream.println(Comment(Record(WrapDoubleQuotation(tag.getName()), WrapDoubleQuotation(tag.getComment()), tag.getUserName(), TIMESTAMP_FORMATTER.get().format(tag.getCreated())))));

        printStream.println(Comment(ENDING_TAG));

        printStream.println(Record(H_PV_NAME, H_SELECTED, H_TIMESTAMP, H_STATUS, H_SEVERITY, H_VALUE_TYPE, H_VALUE, H_READBACK, H_READBACK_VALUE, H_DELTA, H_READ_ONLY));
        List<SnapshotItem> snapshotItems = saveAndRestoreService.getSnapshotItems(nodeToExport.getUniqueId());

        for (SnapshotItem snapshotItem : snapshotItems) {
            printStream.println(Record(snapshotItem));
        }

        printStream.close();
    }

    private static String Record(SnapshotItem snapshotItem) throws Exception {
        String record = "";

        VType pv = snapshotItem.getValue();
        VType readbackPv = snapshotItem.getReadbackValue();

        String pvValue = "";
        String timestamp = "";
        String alarmStatus = "";
        String alarmSeverity = "";
        String readbackPvName = "";
        String readbackPvValue = "\"---\"";

        if (pv instanceof VNumber) {
            VNumber value = (VNumber) pv;

            pvValue = WrapDoubleQuotation(value.getValue().toString());
            timestamp = TimestampString(value.getTime().getTimestamp());
            alarmStatus = value.getAlarm().getStatus().name();
            alarmSeverity = value.getAlarm().getSeverity().name();

            if (readbackPv != null) {
                readbackPvName = snapshotItem.getConfigPv().getReadbackPvName();
                readbackPvValue = WrapDoubleQuotation(((VNumber) readbackPv).getValue().toString());
            }
        } else if (pv instanceof VNumberArray) {
            VNumberArray value = (VNumberArray) pv;

            pvValue = WrapDoubleQuotation(ConvertArrayForm(value.getData().toString()));
            timestamp = TimestampString(value.getTime().getTimestamp());
            alarmStatus = value.getAlarm().getStatus().name();
            alarmSeverity = value.getAlarm().getSeverity().name();

            if (readbackPv != null) {
                readbackPvName = snapshotItem.getConfigPv().getReadbackPvName();
                readbackPvValue = WrapDoubleQuotation(ConvertArrayForm(((VNumberArray) readbackPv).getData().toString()));
            }
        } else if (pv instanceof VEnum) {
            VEnum value = (VEnum) pv;

            pvValue = WrapDoubleQuotation(ConvertEnumForm(value));
            timestamp = TimestampString(value.getTime().getTimestamp());
            alarmStatus = value.getAlarm().getStatus().name();
            alarmSeverity = value.getAlarm().getSeverity().name();

            if (readbackPv != null) {
                readbackPvName = snapshotItem.getConfigPv().getReadbackPvName();
                readbackPvValue = WrapDoubleQuotation(ConvertEnumForm((VEnum) readbackPv));
            }
        } else if (pv instanceof VString) {
            VString value = (VString) pv;

            pvValue = WrapDoubleQuotation(value.getValue());
            timestamp = TimestampString(value.getTime().getTimestamp());
            alarmStatus = value.getAlarm().getStatus().name();
            alarmSeverity = value.getAlarm().getSeverity().name();

            if (readbackPv != null) {
                readbackPvName = snapshotItem.getConfigPv().getReadbackPvName();
                readbackPvValue = WrapDoubleQuotation(((VString) readbackPv).getValue());
            }
        } else if (pv instanceof VStringArray) {
            VStringArray value = (VStringArray) pv;

            pvValue = WrapDoubleQuotation(ConvertArrayForm(value.getData().toString()));
            timestamp = TimestampString(value.getTime().getTimestamp());
            alarmStatus = value.getAlarm().getStatus().name();
            alarmSeverity = value.getAlarm().getSeverity().name();

            if (readbackPv != null) {
                readbackPvName = snapshotItem.getConfigPv().getReadbackPvName();
                readbackPvValue = WrapDoubleQuotation(ConvertArrayForm(((VStringArray) readbackPv).getData().toString()));
            }
        }

        record += snapshotItem.getConfigPv().getPvName() + CSV_SEPARATOR; // PV
        record += "1" + CSV_SEPARATOR; // SELECTED
        record += timestamp + CSV_SEPARATOR;
        record += alarmStatus + CSV_SEPARATOR;
        record += alarmSeverity + CSV_SEPARATOR;
        record += DataTypeString(pv) + CSV_SEPARATOR;
        record += pvValue + CSV_SEPARATOR;
        record += readbackPvName + CSV_SEPARATOR;
        record += readbackPvValue + CSV_SEPARATOR;
        record += CSV_SEPARATOR; // DELTA
        record += snapshotItem.getConfigPv().isReadOnly() ? "1" : "0"; // ReadOnly

        return record;
    }

    private static String WrapDoubleQuotation(String data) {
        return "\"" + data + "\"";
    }

    private static String ConvertArrayForm(String arrayData) {
        return arrayData.replaceAll(CSV_SEPARATOR + "\\s", ARRAY_SPLITTER);
    }

    private static String ConvertEnumForm(VEnum value) {
        String enumString = "";
        enumString += value.getValue();
        enumString += ENUM_VALUE_SPLITTER;
        enumString += value.getDisplay().getChoices().toString().replaceAll(CSV_SEPARATOR + "\\s", ARRAY_SPLITTER);

        return enumString;
    }

    private static String TimestampString(Instant timestamp) {
        return timestamp.getEpochSecond() + "." + timestamp.getNano();
    }

    private static String DataTypeString(VType pv) throws Exception {
        if (pv instanceof VNumber) {
            VNumber value = (VNumber) pv;
            Class<?> clazz = value.getValue().getClass();
            if (clazz.equals(Byte.class)) {
                return "byte";
            } else if (clazz.equals(UByte.class)) {
                return "ubyte";
            } else if (clazz.equals(Short.class)) {
                return "short";
            } else if (clazz.equals(UShort.class)) {
                return "ushort";
            } else if (clazz.equals(Integer.class)) {
                return "int";
            } else if (clazz.equals(UInteger.class)) {
                return "uint";
            } else if (clazz.equals(Long.class)) {
                return "long";
            } else if (clazz.equals(ULong.class)) {
                return "ulong";
            } else if (clazz.equals(Float.class)) {
                return "float";
            } else if (clazz.equals(Double.class)){
                return "double";
            }

            throw new Exception("Data class " + value.getValue().getClass().getCanonicalName() + " not supported");
        } else if (pv instanceof VNumberArray) {
            VNumberArray value = (VNumberArray) pv;

            Class<?> clazz = value.getData().getClass();
            if (clazz.equals(ArrayByte.class) || clazz.getSuperclass().equals(ListByte.class)) {
                return "byte_array";
            } else if (clazz.equals(ArrayUByte.class) || clazz.getSuperclass().equals(ListUByte.class)) {
                return "ubyte_array";
            } else if (clazz.equals(ArrayShort.class) || clazz.getSuperclass().equals(ListShort.class)) {
                return "short_array";
            } else if (clazz.equals(ArrayUShort.class) || clazz.getSuperclass().equals(ListUShort.class)) {
                return "ushort_array";
            } else if (clazz.equals(ArrayInteger.class) || clazz.getSuperclass().equals(ListInteger.class)) {
                return "int_array";
            } else if (clazz.equals(ArrayUInteger.class) || clazz.getSuperclass().equals(ListUInteger.class)) {
                return "uint_array";
            } else if (clazz.equals(ArrayLong.class) || clazz.getSuperclass().equals(ListLong.class)) {
                return "long_array";
            } else if (clazz.equals(ArrayULong.class) || clazz.getSuperclass().equals(ListULong.class)) {
                return "ulong_array";
            } else if (clazz.equals(ArrayFloat.class) || clazz.getSuperclass().equals(ListFloat.class)) {
                return "float_array";
            } else if (clazz.equals(ArrayDouble.class) || clazz.getSuperclass().equals(ListDouble.class)) {
                return "double_array";
            }

            throw new Exception("Data class " + value.getData().getClass().getCanonicalName() + " not supported");
        } else if (pv instanceof VString) {
            return "string";
        } else if (pv instanceof VEnum) {
            return "enum";
        } else if (pv instanceof VStringArray) {
            return "string_array";
        }

        throw new Exception(String.format("Unable to perform data conversion on type %s", pv.getClass().getCanonicalName()));
    }
}