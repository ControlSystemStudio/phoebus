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

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.epics.util.array.ArrayBoolean;
import org.epics.util.array.ArrayByte;
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ArrayFloat;
import org.epics.util.array.ArrayInteger;
import org.epics.util.array.ArrayLong;
import org.epics.util.array.ArrayShort;
import org.epics.util.array.CollectionNumbers;
import org.epics.util.array.ListBoolean;
import org.epics.util.array.ListByte;
import org.epics.util.array.ListDouble;
import org.epics.util.array.ListFloat;
import org.epics.util.array.ListInteger;
import org.epics.util.array.ListLong;
import org.epics.util.array.ListShort;
import org.epics.util.stats.Range;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.Time;
import org.epics.vtype.VBoolean;
import org.epics.vtype.VBooleanArray;
import org.epics.vtype.VByte;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VFloat;
import org.epics.vtype.VFloatArray;
import org.epics.vtype.VInt;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VLong;
import org.epics.vtype.VLongArray;
import org.epics.vtype.VShort;
import org.epics.vtype.VShortArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.ApplicationContextProvider;
import org.phoebus.applications.saveandrestore.SpringFxmlLoader;
import org.phoebus.applications.saveandrestore.datamigration.git.FileUtilities;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.model.VDisconnectedData;
import org.phoebus.applications.saveandrestore.ui.saveset.SaveSetFromSelectionController;

import java.io.File;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Imports saveset and snapshot from CSV file.
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public class CSVImporter extends CSVCommon {
    final static private SaveAndRestoreService saveAndRestoreService = (SaveAndRestoreService) ApplicationContextProvider.getApplicationContext().getAutowireCapableBeanFactory().getBean("saveAndRestoreService");

    static private CSVParser csvParser;

    static private Node parentOfImport = null;

    static private boolean duplicateSavesetFound = false;
    final static private int numColumnHeadersThreshold = 5;

    public static void importFile(Node parent, File file) throws Exception {
        parentOfImport = parent;
        csvParser = CSVParser.parse(file);

        duplicateSavesetFound = false;

        switch (parent.getNodeType()) {
            case        FOLDER: importSaveset();  break;
            case CONFIGURATION: importSnapshot(); break;
            default: throw new Exception("The node of " + parentOfImport.getNodeType() + " is not supported for import!");
        }
    }

    private static void importSaveset() {
        if (csvParser.getColumnHeaders().size() > numColumnHeadersThreshold) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("The file chosen doesn't contain saveset information!" + System.lineSeparator() + "Please check the file.");
            alert.show();

            return;
        }

        if (duplicateSavesetFound) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Existing saveset with the same name is found. Duplicate PVs will be ignored." + System.lineSeparator() + "Please review the description.");
            alert.showAndWait();

            csvParser.setDescription(parentOfImport.getProperty("description") + System.lineSeparator() + System.lineSeparator() + "Description from importing:" + System.lineSeparator() + csvParser.getDescription());
        }

        SpringFxmlLoader springFxmlLoader = new SpringFxmlLoader();
        Stage dialog = new Stage();
        dialog.setTitle("Import Save Set");
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setScene(new Scene((Parent) springFxmlLoader.load("ui/saveset/SaveSetFromSelection.fxml")));

        final SaveSetFromSelectionController controller = springFxmlLoader.getLoader().getController();
        controller.disableSaveSetSelectionInBrowsing();
        controller.setData(parentOfImport, csvParser.getSavesetName(), csvParser.getDescription(), csvParser.getEntries());
        dialog.show();
    }

    /*
    // Leave the code for the time we decide to use hierarchy. Probably never.
    private static Node getTargetNode(List<String> hierarchy, NodeType nodeType) throws Exception {
        int lastOne = hierarchy.size() - 1; // Last one is the saveset

        Node parentNode = saveAndRestoreService.getRootNode();
        for (int index = 0; index < lastOne; index++) {
            List<Node> children = saveAndRestoreService.getChildNodes(parentNode);
            boolean isFound = false;
            for (Node child : children) {
                if (child.getName().equals(hierarchy.get(index)) && child.getNodeType().equals(nodeType)) {
                    parentNode = child;
                    isFound = true;
                    break;
                }
            }

            if (!isFound) {
                Node newNode = Node.builder()
                        .name(hierarchy.get(index))
                        .nodeType(NodeType.FOLDER)
                        .build();

                saveAndRestoreService.createNode(parentNode.getUniqueId(), newNode);
                parentNode = newNode;
            }
        }

        // Check if there exists the saveset having the same name with the import
        List<Node> children = saveAndRestoreService.getChildNodes(parentNode);
        for (Node child : children) {
            if (child.getName().equals(hierarchy.get(lastOne)) && child.getNodeType().equals(NodeType.CONFIGURATION)) {
                parentNode = child;
                duplicateSavesetFound = true;
                break;
            }
        }

        return parentNode;
    }
   */

    private static void importSnapshot() throws Exception {
        if (csvParser.getColumnHeaders().size() < numColumnHeadersThreshold) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("The file chosen doesn't contain snapshot information!" + System.lineSeparator() + "Please check the file.");
            alert.show();

            return;
        }

        if (!checkSnapshotCompatibility(csvParser.getEntries())) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setContentText("PVs in the saveset, " + parentOfImport.getName() + ", are not the same as those in the importing snapshot, " + csvParser.getSnapshotName() + "." + System.lineSeparator()
                    + "Do you wish to create a compatible snapshot, then continue?");
            Optional<ButtonType> response = alert.showAndWait();

            if (response.isPresent() && response.get().equals(ButtonType.OK)) {
                SpringFxmlLoader springFxmlLoader = new SpringFxmlLoader();
                Stage dialog = new Stage();
                dialog.setTitle("Import Snapshot");
                dialog.initModality(Modality.APPLICATION_MODAL);
                dialog.setScene(new Scene((Parent) springFxmlLoader.load("ui/saveset/SaveSetFromSelection.fxml")));

                final SaveSetFromSelectionController controller = springFxmlLoader.getLoader().getController();
                controller.disableSaveSetSelectionInBrowsing();
                controller.setData(null, "", "", csvParser.getEntries());
                SimpleObjectProperty<Node> createdSaveset = new SimpleObjectProperty<>(null);
                controller.setCreatedSavesetProperty(createdSaveset);
                dialog.showAndWait();

                if (createdSaveset.get() == null) {
                    return;
                } else {
                    parentOfImport = createdSaveset.get();
                }
            } else {
                return;
            }
        }

        List<ConfigPv> configPvs = saveAndRestoreService.getConfigPvs(parentOfImport.getUniqueId());
        List<SnapshotItem> snapshotItems = new ArrayList<>();
        for (Map<String, String> snapshotEntry: csvParser.getEntries()) {
            SnapshotItem snapshotItem = SnapshotItem.builder()
                    .configPv(configPvs.stream().filter(item -> item.equals(createConfigPv(snapshotEntry))).findFirst().get())
                    .value(createVType(snapshotEntry.get(H_TIMESTAMP), snapshotEntry.get(H_STATUS), snapshotEntry.get(H_SEVERITY), snapshotEntry.get(H_VALUE), snapshotEntry.get(H_VALUE_TYPE)))
                    .readbackValue(createVType(snapshotEntry.get(H_TIMESTAMP), snapshotEntry.get(H_STATUS), snapshotEntry.get(H_SEVERITY), snapshotEntry.get(H_READBACK_VALUE), snapshotEntry.get(H_VALUE_TYPE)))
                    .build();

            snapshotItems.add(snapshotItem);
        }

        List<String> childNodeNameList = saveAndRestoreService.getChildNodes(parentOfImport).stream().map(Node::getName).collect(Collectors.toList());
        if (childNodeNameList.contains(csvParser.getSnapshotName())) {
            TextInputDialog dialog = new TextInputDialog(csvParser.getSnapshotName());
            dialog.setTitle("Change snapshot name");
            dialog.setHeaderText("Duplicate snapshot name found!" + System.lineSeparator() + "Please change the snapshot name to continue, or cancel.");
            dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
            dialog.getEditor().textProperty().addListener((observableValue, oldName, newName) -> {
                dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(childNodeNameList.contains(newName) || newName.equals(csvParser.getSnapshotName()));
            });

            Optional<String> response = dialog.showAndWait();

            if (response.isPresent()) {
                csvParser.setSnapshotName(response.get());
            } else {
                return;
            }
        }

        Node snapshot = saveAndRestoreService.saveSnapshot(parentOfImport, snapshotItems, csvParser.getSnapshotName(), csvParser.getDescription());
        snapshot.setCreated(Date.from(csvParser.getTimestamp()));
        snapshot.setUserName(csvParser.getCreator());
        csvParser.getTags().forEach(tag -> {
            tag.setSnapshotId(snapshot.getUniqueId());

            snapshot.addTag(tag);
        });
        saveAndRestoreService.updateNode(snapshot, true);
    }

    private static boolean checkSnapshotCompatibility(List<Map<String, String>> entries) throws Exception {
        List<ConfigPv> configPvs = saveAndRestoreService.getConfigPvs(parentOfImport.getUniqueId());

        int numConfigPvsInSaveset = configPvs.size();
        int numMatching = 0;

        for (Map<String, String> entry: entries) {
            if (configPvs.stream().anyMatch(item -> item.equals(createConfigPv(entry)))) {
                numMatching++;
            }
        }

        return numConfigPvsInSaveset == numMatching;
    }

    private static ConfigPv createConfigPv(Map<String, String> entry) {
        return ConfigPv.builder()
                .pvName(entry.get(H_PV_NAME))
                .readbackPvName(entry.get(H_READBACK).isEmpty() ? null : entry.get(H_READBACK))
                .readOnly(Boolean.parseBoolean(entry.get(H_READ_ONLY)) || "1".equals(entry.get(H_READ_ONLY)))
                .build();
    }

    /**
     * Converts a single entry to the VType.
     * Copied from {@link FileUtilities} class.
     *
     * @param timestamp the timestamp of the entry, given in sec.nano format
     * @param status the alarm status
     * @param severity the alarm severity
     * @param value the raw value
     * @param valueType the value type
     * @return {@link VType} that contains all parameters and matches the type provided by {@param valueType}
     */
    private static VType createVType(String timestamp, String status, String severity, String value, String valueType) {
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
        Alarm alarm = Alarm.of(severity.isEmpty() ? AlarmSeverity.NONE : AlarmSeverity.valueOf(severity.toUpperCase(Locale.US)),
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
                List<String> lbls = new ArrayList<>(Arrays.asList(valueAndLabels[1].split(ARRAY_SPLITTER, -1)));
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
                EnumDisplay enumDisplay1 = EnumDisplay.of(lbls);
                return VEnum.of(idx, enumDisplay1, alarm, time);
            case "na":
                return VDisconnectedData.INSTANCE;
        }

        throw new IllegalArgumentException(String.format("Unknown data type %s.", valueType));
    }
}
