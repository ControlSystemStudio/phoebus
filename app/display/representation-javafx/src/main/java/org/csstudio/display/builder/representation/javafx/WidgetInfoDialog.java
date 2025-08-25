/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetFactory;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.util.DisplayWidgetStats;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.representation.javafx.widgets.JFXBaseRepresentation;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.framework.adapter.AdapterService;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.macros.Macros;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVFactory;
import org.phoebus.pv.PVPool;
import org.phoebus.ui.application.ContextMenuService;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ReadOnlyTextCell;
import org.phoebus.ui.javafx.StringTable;
import org.phoebus.ui.pv.SeverityColors;
import org.phoebus.ui.spi.ContextMenuEntry;
import org.phoebus.util.time.TimestampFormats;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Dialog for displaying widget information
 *
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WidgetInfoDialog extends Dialog<Boolean> {

    private Macros macros = null;
    private final Collection<NameStateValue> pvs;
    private final Widget widget;
    private DisplayWidgetStats stats;
    private Job getArchiveStatusJob;
    private final BooleanProperty getFromArchiverInProgress = new SimpleBooleanProperty();

    /**
     * PV info
     */
    public static class NameStateValue extends ProcessVariable {
        /**
         * State, incl. read-only or writable?
         */
        private final String state;
        /**
         * Last known value
         */
        private final VType value;
        /**
         * Path to Widget within display that uses the PV
         */
        private final String path;

        private final ObjectProperty<ArchivedStatus> archiverValue = new SimpleObjectProperty<>(ArchivedStatus.UNKNOWN);


        /**
         * @param name  PV Name
         * @param state State, incl. read-only or writable?
         * @param value Last known value
         * @param path  Path to Widget within display that uses the PV
         */
        public NameStateValue(final String name, final String state, final VType value, final String path) {
            super(name);
            this.state = state;
            this.value = value;
            this.path = path;
        }

        public String getState() {
            return state;
        }

        public VType getValue() {
            return value;
        }

        public String getPath() {
            return path;
        }

        public ArchivedStatus getArchiverValue() {
            return archiverValue.get();
        }

        public void setArchiverValue(ArchivedStatus archivedStatus) {
            this.archiverValue.setValue(archivedStatus);
        }
    }

    /**
     * Cell with text colored based on alarm severity
     */
    private static class AlarmColoredCell extends ReadOnlyTextCell<NameStateValue> {
        @Override
        protected void updateItem(final String item, final boolean empty) {
            super.updateItem(item, empty);
            final AlarmSeverity severity;
            if (!empty &&
                    getTableRow() != null &&
                    getTableRow().getItem() != null) {
                final VType vtype = getTableRow().getItem().value;
                if (vtype == null)
                    severity = AlarmSeverity.UNDEFINED;
                else {
                    final Alarm alarm = Alarm.alarmOf(vtype);
                    if (alarm == null)
                        severity = AlarmSeverity.NONE;
                    else
                        severity = alarm.getSeverity();
                }
            } else
                severity = AlarmSeverity.NONE;
            text.setStyle("-fx-text-fill: " + JFXUtil.webRGB(SeverityColors.getTextColor(severity)) + "; -fx-control-inner-background: " + JFXUtil.webRGB(SeverityColors.getBackgroundColor(severity)));
        }
    }

    /**
     * Create dialog
     *
     * @param widget {@link Widget}
     * @param pvs    {@link Collection<NameStateValue>}s, may be empty
     */
    public WidgetInfoDialog(final Widget widget, final Collection<NameStateValue> pvs) {
        this.pvs = pvs;
        this.widget = widget;
        setTitle(Messages.WidgetInfoDialog_Title);
        setHeaderText(MessageFormat.format(Messages.WidgetInfoDialog_Info_Fmt, widget.getName(), widget.getType()));
        final Node node = JFXBaseRepresentation.getJFXNode(widget);
        initOwner(node.getScene().getWindow());

        if (!(widget instanceof DisplayModel)) {   // Widgets (but not the DisplayModel!) have a descriptor for their icon
            try {
                final WidgetDescriptor descriptor = WidgetFactory.getInstance().getWidgetDescriptor(widget.getType());
                setGraphic(new ImageView(new Image(descriptor.getIconURL().toExternalForm())));
            } catch (Exception ex) {
                // No icon, no problem
            }
        }
        final TabPane tabs = new TabPane(createProperties(widget), createPVs(pvs), createMacros(widget.getEffectiveMacros()));

        // For display model, show stats
        if (widget instanceof DisplayModel)
            tabs.getTabs().add(createWidgetStats((DisplayModel) widget));

        tabs.getTabs().forEach(tab -> tab.setClosable(false));
        // If there are PVs, default to the "PVs" tab
        if (pvs.size() > 0)
            tabs.getSelectionModel().select(1);

        final ButtonType export = new ButtonType(Messages.ExportWidgetInfo, ButtonData.LEFT);
        final ButtonType copy = new ButtonType(Messages.CopyWidgetInfo, ButtonData.LEFT);
        final ButtonType fetchArchiverStatus = new ButtonType("Get Archiver Status", ButtonData.LEFT);

        getDialogPane().setContent(tabs);
        getDialogPane().getButtonTypes().addAll(export, ButtonType.CLOSE);
        getDialogPane().getButtonTypes().addAll(copy);
        getDialogPane().getButtonTypes().addAll(fetchArchiverStatus);
        setResizable(true);
        tabs.setMinWidth(800);

        Button exportButton = (Button) getDialogPane().lookupButton(export);
        Button copyButton = (Button) getDialogPane().lookupButton(copy);
        Button fetchArchiveStatusButton = (Button) getDialogPane().lookupButton(fetchArchiverStatus);
        fetchArchiveStatusButton.disableProperty().bind(getFromArchiverInProgress);



        fetchArchiveStatusButton.addEventFilter(ActionEvent.ACTION,
                event -> {
                    fetchArchiverStatus();
                    event.consume();
                });

        exportButton.addEventFilter(
                ActionEvent.ACTION,
                event -> {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.getExtensionFilters().addAll(
                            new FileChooser.ExtensionFilter("CSV files", "*.csv"),
                            new FileChooser.ExtensionFilter("All files", "*.*"));
                    File file = fileChooser.showSaveDialog(getDialogPane().getScene().getWindow());
                    if (file != null) {
                        exportToCSV(file);
                    }
                    event.consume();
                }
        );
        copyButton.addEventFilter(
                ActionEvent.ACTION,
                event -> {
                    pvListDisplay();
                    event.consume();
                }
        );

        setResultConverter(button -> true);

        setOnCloseRequest(e -> {
            if (getArchiveStatusJob != null) {
                getArchiveStatusJob.cancel();
            }
        });
    }


    private void pvListDisplay() {
        FlowPane layout = new FlowPane();
        layout.setVgap(10);
        layout.setHgap(20);
        layout.setAlignment(Pos.TOP_CENTER);
        Stage stage = new Stage();
        TextArea area = new TextArea();
        TilePane container = new TilePane();
        Button copyPv = new Button(Messages.CopyButton);
        Button ok = new Button("OK");
        area.setPadding(new Insets(10, 10, 10, 10));
        area.setPrefSize(250, 450);
        stage.setResizable(false);
        //stage.setMaxHeight(530);
        //stage.setMaxWidth(270);

        layout.getChildren().add(area);
        layout.getChildren().add(copyPv);
        layout.getChildren().add(ok);

        Scene scene = new Scene(layout, 250, 500);
        stage.setAlwaysOnTop(true);
        stage.setTitle("PV List");
        stage.setScene(scene);
        stage.show();


        pvs.stream().sorted(Comparator.comparing(pv -> pv.getName())).forEach(pv -> {
            if (!area.getText().contains(pv.getName()))
                area.setText(area.getText() + pv.getName() + "\n");
        });

        EventHandler<ActionEvent> copying = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                final Clipboard clipboard = Clipboard.getSystemClipboard();
                final ClipboardContent content = new ClipboardContent();
                content.putString(area.getText());
                clipboard.setContent(content);
            }
        };
        EventHandler<ActionEvent> cancel = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                stage.close();
            }
        };

        copyPv.setOnAction(copying);
        ok.setOnAction(cancel);
    }

    /**
     * Writes the table content of each tab to file.
     * <p>
     * Note that the pipe character "|" is used to separate items on each data row as comma
     * might conflict with the string content.
     * <p>
     * Upon completion an alert dialog is shown to inform of the outcome (done/failed),
     * but the widget info dialog is not closed.
     *
     * @param file The destination file as selected by user.
     */
    private void exportToCSV(File file) {
        String horizontalRuler = "---------------------------------------------------------";
        String itemSeparator = " | ";
        StringBuilder buffer = new StringBuilder();

        buffer.append("PVS (name, state, value, widget path)").append(System.lineSeparator())
                .append(horizontalRuler).append(System.lineSeparator());
        pvs.stream().sorted(Comparator.comparing(pv -> pv.getName())).forEach(pv -> {
            buffer.append(pv.getName()).append(itemSeparator)
                    .append(pv.state)
                    .append(itemSeparator)
                    .append(getPVValue(pv.value))
                    .append(itemSeparator)
                    .append(pv.path)
                    .append(System.lineSeparator());
        });
        buffer.append(System.lineSeparator());

        if (widget instanceof DisplayModel) {
            buffer.append("Widget Counts (widget type, count)").append(System.lineSeparator())
                    .append(horizontalRuler).append(System.lineSeparator());
            stats.getTypes().entrySet().stream().forEach(entry -> {
                buffer.append(entry.getKey()).append(itemSeparator).append(entry.getValue().get()).append(System.lineSeparator());
            });
            buffer.append(Messages.WidgetInfoDialog_Total).append(itemSeparator).append(stats.getTotal()).append(System.lineSeparator());
            buffer.append(Messages.RulesDialog_Title).append(itemSeparator).append(stats.getRules()).append(System.lineSeparator());
            buffer.append(Messages.ScriptsDialog_Title).append(itemSeparator).append(stats.getScripts()).append(System.lineSeparator());
            buffer.append(System.lineSeparator());
        }

        buffer.append("Macros (name, value)").append(System.lineSeparator())
                .append(horizontalRuler).append(System.lineSeparator());
        macros.forEach((name, value) -> {
            buffer.append(name).append(itemSeparator).append(value).append(System.lineSeparator());
        });
        buffer.append(System.lineSeparator());

        buffer.append("Properties (category, property, name, value)").append(System.lineSeparator())
                .append(horizontalRuler).append(System.lineSeparator());
        widget.getProperties().stream().forEach(widgetProperty -> {
            buffer.append(widgetProperty.getCategory().getDescription()).append(itemSeparator);
            buffer.append(widgetProperty.getDescription()).append(itemSeparator);
            buffer.append(widgetProperty.getName()).append(itemSeparator);
            buffer.append(widgetProperty.getValue()).append(System.lineSeparator());
        });
        buffer.append(System.lineSeparator());

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            fileOutputStream.write(buffer.toString().getBytes());
            fileOutputStream.flush();
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Information Dialog");
            alert.setHeaderText(null);
            alert.setContentText(MessageFormat.format(Messages.ExportDone, file.getAbsolutePath()));
            DialogHelper.positionDialog(alert, getDialogPane(), -200, -100);
            alert.showAndWait();
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed to export widget info", e);
            ExceptionDetailsErrorDialog.openError(getDialogPane(), Messages.ShowErrorDialogTitle, Messages.ExportFailed, e);
        }
    }

    private Tab createMacros(final Macros orig_macros) {
        macros = (orig_macros == null) ? new Macros() : orig_macros;
        // Use text field to allow copying the name and value
        // Table uses list of macro names as input
        // Name column just displays the macro name,..
        final TableColumn<String, String> name = new TableColumn<>(Messages.WidgetInfoDialog_Name);
        name.setCellFactory(col -> new ReadOnlyTextCell<>());
        name.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue()));

        // .. value column fetches the macro value
        final TableColumn<String, String> value = new TableColumn<>(Messages.WidgetInfoDialog_Value);
        value.setCellFactory(col -> new ReadOnlyTextCell<>());
        value.setCellValueFactory(param -> new ReadOnlyStringWrapper(macros.getValue(param.getValue())));

        final TableView<String> table =
                new TableView<>(FXCollections.observableArrayList(macros.getNames()));
        table.getColumns().add(name);
        table.getColumns().add(value);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        return new Tab(Messages.WidgetInfoDialog_TabMacros, table);
    }

    private Tab createPVs(final Collection<NameStateValue> pvs) {
        // Use text field to allow users to copy the name, value to clipboard
        final TableColumn<NameStateValue, String> name = new TableColumn<>(Messages.WidgetInfoDialog_Name);
        name.setCellValueFactory(new PropertyValueFactory<NameStateValue, String>("name"));

        final TableColumn<NameStateValue, String> state = new TableColumn<>(Messages.WidgetInfoDialog_State);
        state.setCellValueFactory(new PropertyValueFactory<NameStateValue, String>("state"));

        final TableColumn<NameStateValue, String> path = new TableColumn<>(Messages.WidgetInfoDialog_Path);
        path.setCellValueFactory(new PropertyValueFactory<NameStateValue, String>("path"));

        final TableColumn<NameStateValue, String> value = new TableColumn<>(Messages.WidgetInfoDialog_Value);
        value.setCellFactory(col -> new AlarmColoredCell());
        value.setCellValueFactory(param ->
        {
            String text = getPVValue(param.getValue().value);
            return new ReadOnlyStringWrapper(text);
        });

        // Check if archive data source is available
        boolean archiveDataSourceAvailable =
                ServiceLoader.load(PVFactory.class).stream().filter(f -> f.get().getType().equals("archive")).findFirst().isPresent();

        final TableColumn<NameStateValue, ArchivedStatus> archiverValue = new TableColumn<>(Messages.ArchivedStatus);
        archiverValue.setCellValueFactory(cell -> cell.getValue().archiverValue);
        archiverValue.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(ArchivedStatus item, boolean empty) {
                if (empty || item.equals(ArchivedStatus.UNKNOWN)) {
                    setText(null);
                    return;
                }
                switch (item){
                    case NOT_APPLICABLE -> setText(Messages.ArchivedStatusNotApplicable);
                    case YES ->  setText(Messages.ArchivedStatusYes);
                    case NO -> setText(Messages.ArchivedStatusNo);
                    case LOADING -> setText(Messages.ArchivedStatusPleaseWait);
                }
            }
        });
        archiverValue.setMinWidth(100.0);

        final ObservableList<NameStateValue> pv_data = FXCollections.observableArrayList(pvs);
        pv_data.sort(Comparator.comparing(a -> a.getName()));
        final TableView<NameStateValue> table = new TableView<>(pv_data);
        table.getColumns().add(name);
        table.getColumns().add(state);
        table.getColumns().add(value);
        table.getColumns().add(path);
        if (archiveDataSourceAvailable) {
            table.getColumns().add(archiverValue);
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                SelectionService.getInstance().setSelection(table, table.getSelectionModel().getSelectedItems());
            }
        });

        table.setOnContextMenuRequested(event -> {

            final ContextMenu contextMenu = new ContextMenu();

            List<ContextMenuEntry> contextEntries = ContextMenuService.getInstance().listSupportedContextMenuEntries();

            contextEntries.forEach(entry -> {
                MenuItem item = new MenuItem(entry.getName(), new ImageView(entry.getIcon()));
                item.setOnAction(e -> {
                    try {
                        ObservableList<NameStateValue> old = table.getSelectionModel().getSelectedItems();

                        List<Object> selectedPVs = SelectionService.getInstance().getSelection().getSelections().stream().map(s -> {
                            return AdapterService.adapt(s, entry.getSupportedType()).get();
                        }).collect(Collectors.toList());
                        // set the selection
                        SelectionService.getInstance().setSelection(table, selectedPVs);
                        entry.call(SelectionService.getInstance().getSelection());
                        // reset the selection
                        SelectionService.getInstance().setSelection(table, old);
                    } catch (Exception ex) {
                        //logger.log(Level.WARNING, "Failed to execute action " + entry.getName(), ex);
                    }
                });
                contextMenu.getItems().add(item);
            });

            table.setContextMenu(contextMenu);
        });

        return new Tab(Messages.WidgetInfoDialog_TabPVs, table);
    }

    private Tab createProperties(final Widget widget) {
        // Use text field to allow copying the name (for use in scripts)
        // and value, but not the localized description and category
        // which are just for information
        final TableColumn<WidgetProperty<?>, String> cat = new TableColumn<>(Messages.WidgetInfoDialog_Category);
        cat.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getCategory().getDescription()));

        final TableColumn<WidgetProperty<?>, String> descr = new TableColumn<>(Messages.WidgetInfoDialog_Property);
        descr.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getDescription()));

        final TableColumn<WidgetProperty<?>, String> name = new TableColumn<>(Messages.WidgetInfoDialog_Name);
        name.setCellFactory(col -> new ReadOnlyTextCell<>());
        name.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getName()));

        final TableColumn<WidgetProperty<?>, String> value = new TableColumn<>(Messages.WidgetInfoDialog_Value);
        value.setCellFactory(col -> new ReadOnlyTextCell<>());
        value.setCellValueFactory(param -> new ReadOnlyStringWrapper(Objects.toString(param.getValue().getValue())));

        final TableView<WidgetProperty<?>> table =
                new TableView<>(FXCollections.observableArrayList(widget.getProperties()));
        table.getColumns().add(cat);
        table.getColumns().add(descr);
        table.getColumns().add(name);
        table.getColumns().add(value);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        return new Tab(Messages.WidgetInfoDialog_TabProperties, table);
    }

    private Tab createWidgetStats(final DisplayModel model) {
        // Compute stats
        stats = new DisplayWidgetStats(model);

        // Turn map into rows of "type, count", sorted by type
        final List<List<String>> rows = new ArrayList<>();
        stats.getTypes()
                .keySet()
                .stream()
                .sorted()
                .forEach(type ->
                {
                    final String count = stats.getTypes().get(type).toString();
                    final List<String> row = List.of(type, count);
                    rows.add(row);
                });

        // Type, count table
        final StringTable table = new StringTable(false);
        table.showToolbar(false);
        table.setHeaders(List.of(Messages.WidgetInfoDialog_WidgetType, Messages.WidgetInfoDialog_Count));
        table.setData(rows);

        // Rules count
        final Label rul_lbl = new Label();
        rul_lbl.setText(Messages.RulesDialog_Title);
        final Label rul_num = new Label();
        rul_num.setText(Integer.toString(stats.getRules()));

        // Scripts count
        final Label scr_lbl = new Label();
        scr_lbl.setText(Messages.ScriptsDialog_Title);
        final Label scr_num = new Label();
        scr_num.setText(Integer.toString(stats.getScripts()));

        // Total widget count
        final Label tot_lbl = new Label();
        tot_lbl.setText(Messages.WidgetInfoDialog_Total);
        final Label total = new Label();
        total.setText(Integer.toString(stats.getTotal()));

        // [ table  ]
        // Total  100
        final HBox summary = new HBox(5, rul_lbl, rul_num, scr_lbl, scr_num, tot_lbl, total);

        VBox.setVgrow(table, Priority.ALWAYS);
        final VBox layout = new VBox(5, table, summary);

        return new Tab(Messages.WidgetInfoDialog_WidgetStats, layout);
    }

    private String getPVValue(VType vtype) {
        String text;
        if (vtype == null)
            text = Messages.WidgetInfoDialog_Disconnected;
        else {
            // For arrays, show up to 10 elements.
            if (vtype instanceof VNumberArray)
                text = VTypeHelper.formatArray((VNumberArray) vtype, 10);
            else
                text = VTypeUtil.getValueString(vtype, true);
            final Alarm alarm = Alarm.alarmOf(vtype);
            if (alarm != null && alarm.getSeverity() != AlarmSeverity.NONE)
                text = text + " [" + alarm.getSeverity().toString() + ", " +
                        alarm.getName() + "]";
        }
        return text;
    }

    private void fetchArchiverStatus() {
        getFromArchiverInProgress.setValue(true);
        for (WidgetInfoDialog.NameStateValue pv : pvs) {
            pv.setArchiverValue(ArchivedStatus.LOADING);
        }
        getArchiveStatusJob = JobManager.schedule("Get archived PV values", monitor -> {
            try {
                for (NameStateValue pv : pvs) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    ArchivedStatus archivedStatus = getFromArchiver(pv.getName());
                    pv.setArchiverValue(archivedStatus);
                }
            } finally {
                getFromArchiverInProgress.setValue(false);
            }
        });
    }

    /**
     * Uses the archive:// data source to retrieve last archived sample.
     * For loc://, sim://, sys:// and formula functions, {@link ArchivedStatus#NOT_APPLICABLE} is returned.
     * @param pvName A valid PV name.
     * @return The {@link ArchivedStatus} of the PV, if any, or {@link ArchivedStatus#UNKNOWN} if there is
     * an issue interacting with the data source.
     */
    private ArchivedStatus getFromArchiver(String pvName) {
        if (pvName.toLowerCase().startsWith("loc://") ||
            pvName.toLowerCase().startsWith("sim://") ||
            pvName.toLowerCase().startsWith("sys://") ||
            pvName.toLowerCase().startsWith("=")) {
            return ArchivedStatus.NOT_APPLICABLE;
        }
        // Check if pv name is prefixed with a scheme, e.g. pva://, ca://...
        int indexSchemeSeparator = pvName.indexOf("://");
        if (indexSchemeSeparator > 0 && pvName.length() > indexSchemeSeparator) {
            pvName = pvName.substring(indexSchemeSeparator + 1);
        }
        // Prepend "archiver://"
        pvName = "archive://" + pvName + "(" + TimestampFormats.SECONDS_FORMAT.format(Instant.now()) + ")";
        PV pv = null;
        try {
            pv = PVPool.getPV(pvName);
            VType pvValue = pv.read();
            return pvValue == null ? ArchivedStatus.NO : ArchivedStatus.YES;
        } catch (Exception e) {
            return ArchivedStatus.UNKNOWN;
        }
        finally {
            if(pv != null){
                PVPool.releasePV(pv);
            }
        }
    }

    private enum ArchivedStatus {
        YES,            // Archiver data source returns a non-null value
        NO,             // Archiver data source returns null
        NOT_APPLICABLE, // sim and loc PVs
        UNKNOWN,        // Initial state
        LOADING         // UI shows retrieval is in progress
    }
}
