/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.search;

import java.text.MessageFormat;
import java.util.List;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.archive.SearchJob;
import org.csstudio.trends.databrowser3.model.ArchiveDataSource;
import org.csstudio.trends.databrowser3.model.ChannelInfo;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Panel for searching the archive
 *  @author Kay Kasemir
 */
public class SearchView extends VBox
{
    private final ArchiveListPane archive_list = new ArchiveListPane();

    private final TextField pattern = new TextField();

    private final TableView<ChannelInfo> channel_table = new TableView<>();

    public SearchView()
    {
        super(5.0);

        // Archive List

        // Pattern: ____________ [Search]
        pattern.setTooltip(new Tooltip(Messages.SearchPatternTT));
        pattern.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(pattern, Priority.ALWAYS);
        final Button search = new Button(Messages.Search);
        search.setTooltip(new Tooltip(Messages.SearchTT));
        final HBox search_row = new HBox(5.0, new Label(Messages.SearchPattern), pattern, search);
        search_row.setAlignment(Pos.CENTER_LEFT);
        pattern.setOnAction(event -> searchForChannels());
        search.setOnAction(event -> searchForChannels());

        //  ( ) Add .. (x) Replace search result
        final RadioButton result_add = new RadioButton(Messages.AppendSearchResults);
        result_add.setTooltip(new Tooltip(Messages.AppendSearchResultsTT));
        final RadioButton result_replace = new RadioButton(Messages.ReplaceSearchResults);
        result_replace.setTooltip(new Tooltip(Messages.ReplaceSearchResultsTT));
        final ToggleGroup result_handling = new ToggleGroup();
        result_add.setToggleGroup(result_handling);
        result_replace.setToggleGroup(result_handling);
        result_replace.setSelected(true);

        // PV Name  |  Source
        // ---------+--------
        //          |
        final TableColumn<ChannelInfo, String> pv_col = new TableColumn<>(Messages.PVName);
        pv_col.setCellValueFactory(cell ->  new SimpleStringProperty(cell.getValue().getProcessVariable().getName()));
        channel_table.getColumns().add(pv_col);

        final TableColumn<ChannelInfo, String> archive_col = new TableColumn<>(Messages.ArchiveName);
        archive_col.setCellValueFactory(cell ->  new SimpleStringProperty(cell.getValue().getArchiveDataSource().getName()));
        channel_table.getColumns().add(archive_col);

        channel_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        channel_table.setPlaceholder(new Label(""));

        getChildren().setAll(archive_list,
                             search_row,
                             new HBox(5.0, result_add, result_replace),
                             channel_table);

        Platform.runLater(() -> pattern.requestFocus());
    }

    private void searchForChannels()
    {
        final List<ArchiveDataSource> archives = archive_list.getSelectedArchives();

        final String pattern_txt = pattern.getText().trim();
        // Warn when searching without pattern
        if (pattern_txt.length() <= 0)
        {
            final Alert dialog = new Alert(Alert.AlertType.WARNING);
            DialogHelper.positionDialog(dialog, pattern, -200, -200);
            dialog.setTitle(Messages.Search);
            dialog.setContentText(Messages.SearchPatternEmptyMessage);
            dialog.showAndWait();
            // Aborted, move focus to search pattern
            pattern.requestFocus();
            return;
        }

        // TODO Cancel ongoing Search

        SearchJob.submit(archives, pattern_txt,
                         channels -> Platform.runLater( () -> displayChannelInfos(channels)),
                         (url, ex) -> ExceptionDetailsErrorDialog.openError(Messages.Error,
                                                                            MessageFormat.format(Messages.ArchiveServerErrorFmt, url), ex));
    }

    private void displayChannelInfos(final List<ChannelInfo> channels)
    {
        // TODO Check add-or-replace mode
        channel_table.getItems().setAll(channels);
    }
}
