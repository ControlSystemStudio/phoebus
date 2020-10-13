/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.pv;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleObjectProperty;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.phoebus.pv.RefCountMap.ReferencedEntry;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.application.Messages;
import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import org.phoebus.ui.javafx.JFXUtil;
import org.phoebus.ui.vtype.FormatOption;
import org.phoebus.ui.vtype.FormatOptionHandler;

/** Table that lists PVs, their reference count etc.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVList extends BorderPane
{
    /** TableView row for one PV */
    private static class PVInfo
    {
        final StringProperty name;
        final BooleanProperty connected;
        final IntegerProperty references;
        final StringProperty value;
        final SimpleObjectProperty<AlarmSeverity> alarmSeverity;

        public PVInfo(final String name, final boolean connected, final int references, final String value, final AlarmSeverity alarmSeverity)
        {
            this.name = new SimpleStringProperty(name);
            this.connected = new SimpleBooleanProperty(connected);
            this.references = new SimpleIntegerProperty(references);
            this.value = new SimpleStringProperty(value);
            this.alarmSeverity = new SimpleObjectProperty<>(alarmSeverity);
        }
    }

    private final TableView<PVInfo> table = new TableView<>();

    private static final Image connected_icon = new Image(PVList.class.getResourceAsStream("/icons/connected.png"));
    private static final Image disconnected_icon = new Image(PVList.class.getResourceAsStream("/icons/disconnected.png"));

    private static class ConnectedCell extends TableCell<PVInfo, Boolean>
    {
        @Override
        protected void updateItem(final Boolean connected, final boolean empty)
        {
            super.updateItem(connected, empty);
            if (empty)
                setGraphic(null);
            else
                setGraphic(new ImageView(connected ? connected_icon : disconnected_icon));
        }
    }

    public PVList()
    {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label(Messages.PVListPlaceholder));
        createTableColumns();
        createContextMenu();

        Node toolbar = createToolbar();
        setAlignment(toolbar, Pos.CENTER_RIGHT);
        setMargin(toolbar, new Insets(5, 5, 0, 5));
        setMargin(table, new Insets(5));
        setTop(toolbar);
        setCenter(table);

        triggerRefresh();
    }

    private Node createToolbar()
    {
        final Button refresh = new Button();
        refresh.setGraphic(ImageCache.getImageView(PVList.class, "/icons/refresh.png"));
        refresh.setTooltip(new Tooltip(Messages.PVListRefreshTT));
        refresh.setOnAction(event -> triggerRefresh());
        return refresh;
        // If more buttons are added:
        // return new HBox(5, refresh);
    }

    private void createTableColumns()
    {
        final TableColumn<PVInfo, Boolean> conn_col = new TableColumn<>(Messages.PVListTblConnected);
        conn_col.setCellFactory(col -> new ConnectedCell());
        conn_col.setCellValueFactory(cell -> cell.getValue().connected);
        conn_col.setMinWidth(20.0);
        conn_col.setPrefWidth(100.0);
        conn_col.setMaxWidth(100.0);
        table.getColumns().add(conn_col);

        final TableColumn<PVInfo, String> name_col = new TableColumn<>(Messages.PVListTblPVName);
        name_col.setCellValueFactory(cell -> cell.getValue().name);
        name_col.setMaxWidth(400.0);
        table.getColumns().add(name_col);

        final TableColumn<PVInfo, Number> ref_col = new TableColumn<>(Messages.PVListTblReferences);
        ref_col.setCellValueFactory(cell -> cell.getValue().references);
        ref_col.setMaxWidth(100.0);
        table.getColumns().add(ref_col);

        final TableColumn<PVInfo, String> valueColumn = new TableColumn<>(Messages.PVListTblValue);
        valueColumn.setCellFactory(column -> new TableCell<>(){
            @Override
            public void updateItem(final String stringValue, final boolean empty){
                super.updateItem(stringValue, empty);
                if (! empty && getTableRow() != null && getTableRow().getItem() != null)
                {
                    Label label = new Label(stringValue);
                    label.setStyle("-fx-text-fill: " + JFXUtil.webRGB(SeverityColors.getTextColor(getTableRow().getItem().alarmSeverity.get())));
                    setGraphic(label);
                }
                else{
                    setGraphic(null);
                }
            }
        });
        valueColumn.setCellValueFactory(cell -> cell.getValue().value);
        valueColumn.setMaxWidth(500.0);
        table.getColumns().add(valueColumn);
    }

    private void createContextMenu()
    {
        // Publish selected items as PV
        final ListChangeListener<PVInfo> sel_changed = change ->
        {
            final List<ProcessVariable> pvs = change.getList()
                                                    .stream()
                                                    .map(info -> new ProcessVariable(info.name.get()))
                                                    .collect(Collectors.toList());
            SelectionService.getInstance().setSelection(PVListApplication.DISPLAY_NAME, pvs);
        };
        table.getSelectionModel().getSelectedItems().addListener(sel_changed);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Context menu with selection-based entries, i.e. PV contributions
        final ContextMenu menu = new ContextMenu();
        table.setOnContextMenuRequested(event ->
        {
            menu.getItems().clear();
            ContextMenuHelper.addSupportedEntries(table, menu);
            menu.show(table.getScene().getWindow());
        });
        table.setContextMenu(menu);
    }

    private void triggerRefresh()
    {
        JobManager.schedule(Messages.PVListJobName, monitor ->
        {
            // Background thread to list information
            final ObservableList<PVInfo> items = FXCollections.observableArrayList();
            final Collection<ReferencedEntry<PV>> refs = PVPool.getPVReferences();
            for (ReferencedEntry<PV> ref : refs)
            {
                final PV pv = ref.getEntry();
                VType vtype = pv.read();
                items.add(new PVInfo(pv.getName(), vtype != null, ref.getReferences(), getValueText(vtype), getAlarmSeverity(vtype)));
            }

            // Update UI
            Platform.runLater(() ->
            {
                table.getItems().clear();
                table.getItems().addAll(items);
            });
        });
    }

    private String getValueText(VType vtype){
        String text;
        if (vtype == null)
            text = Messages.PVListTblDisconnected;
        else
        {   // For arrays, show up to 10 elements.
            if (vtype instanceof VNumberArray) {
                text = VTypeHelper.formatArray((VNumberArray) vtype, 10);
            }
            else {
                text = FormatOptionHandler.format(vtype, FormatOption.DEFAULT, -1, true);
            }
            final Alarm alarm = Alarm.alarmOf(vtype);
            if (alarm != null  &&  alarm.getSeverity() != AlarmSeverity.NONE) {
                text = text + " [" + alarm.getSeverity().toString() + ", " +
                        alarm.getName() + "]";
            }
        }
        return text;
    }

    private AlarmSeverity getAlarmSeverity(VType vtype){
        if(vtype == null){
            return AlarmSeverity.UNDEFINED;
        }
        final Alarm alarm = Alarm.alarmOf(vtype);
        if (alarm == null){
            return AlarmSeverity.NONE;
        }

        else {
            return alarm.getSeverity();
        }
    }
}
