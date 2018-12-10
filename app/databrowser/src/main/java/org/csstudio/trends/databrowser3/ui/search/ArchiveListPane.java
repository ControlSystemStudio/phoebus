/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.search;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.ArchiveDataSource;
import org.csstudio.trends.databrowser3.preferences.Preferences;
import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.ArchiveReaders;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.DialogHelper;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

/** Pane with list of archives
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ArchiveListPane extends BorderPane
{
    private final TableView<ArchiveDataSource> archive_list;

    public ArchiveListPane()
    {
        archive_list = new TableView<>(FXCollections.observableArrayList(Preferences.archive_urls));

        final TableColumn<ArchiveDataSource, String> arch_col = new TableColumn<>(Messages.ArchiveName);
        arch_col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        arch_col.setMinWidth(0);
        archive_list.getColumns().add(arch_col);

        final MenuItem item_info = new MenuItem(Messages.ArchiveServerInfo, Activator.getIcon("info_obj"));
        item_info.setOnAction(event -> showArchiveInfo());
        ContextMenu menu = new ContextMenu(item_info);
        archive_list.setContextMenu(menu);

        archive_list.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        archive_list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        setCenter(archive_list);
    }

    /** Show dialog with info for selected archive */
    private void showArchiveInfo()
    {
        final ArchiveDataSource archive = archive_list.getSelectionModel().getSelectedItem();

        JobManager.schedule(Messages.ArchiveServerInfo, monitor ->
        {
            final StringBuilder info = new StringBuilder("URL: " + archive.getUrl() + "\n\n");
            try
            {
                final ArchiveReader reader = ArchiveReaders.createReader(archive.getUrl());
                info.append(reader.getDescription());
                reader.close();
            }
            catch (Exception ex)
            {
                info.append("Error:\n");
                final ByteArrayOutputStream buf = new ByteArrayOutputStream();
                ex.printStackTrace(new PrintStream(buf));
                info.append(buf.toString());
            }

            Platform.runLater(() ->
            {
                final Alert dialog = new Alert(AlertType.INFORMATION);
                DialogHelper.positionDialog(dialog, archive_list, -300, -200);
                dialog.setTitle(Messages.ArchiveServerInfo);
                dialog.setResizable(true);
                dialog.setHeaderText(Messages.ArchiveServerInfoHdr + archive.getName());
                final TextArea detail = new TextArea(info.toString());
                detail.setEditable(false);
                dialog.getDialogPane().setContent(detail);
                dialog.showAndWait();
            });
        });
    }

    /** Get selected archives.
     *
     *  <p>If _none_ are selected, i.e. no specific archives
     *  are selected, return _all_.
     *
     *  @return Archives that user wants us to query
     */
    public List<ArchiveDataSource> getSelectedArchives()
    {
        final List<ArchiveDataSource> result = new ArrayList<>();
        result.addAll(archive_list.getSelectionModel().getSelectedItems());
        if (result.isEmpty())
            result.addAll(archive_list.getItems());
        return result;
    }
}
