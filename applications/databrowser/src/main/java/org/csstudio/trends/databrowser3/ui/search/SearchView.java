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
import org.csstudio.trends.databrowser3.model.Model;
import org.phoebus.framework.jobs.Job;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Panel for searching the archive
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SearchView extends SplitPane
{
    private static final String SEARCH_PANEL_SPLIT = "search_panel_split";

    private final Model model;
    private final ArchiveListPane archive_list = new ArchiveListPane();
    private final TextField pattern = new TextField();
    private RadioButton result_replace;
    private final TableView<ChannelInfo> channel_table = new TableView<>();

    private Job active_job = null;


    /** Create search view
     *
     *  <p>While technically a {@link SplitPane},
     *  should be treated as generic {@link Node},
     *  using only the API defined in here
     *
     *  @param model
     */
    public SearchView(final Model model)
    {
        this.model = model;
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
        result_replace = new RadioButton(Messages.ReplaceSearchResults);
        result_replace.setTooltip(new Tooltip(Messages.ReplaceSearchResultsTT));
        final ToggleGroup result_handling = new ToggleGroup();
        result_add.setToggleGroup(result_handling);
        result_replace.setToggleGroup(result_handling);
        result_replace.setSelected(true);
        final HBox replace_row = new HBox(5.0, result_add, result_replace);
        replace_row.setAlignment(Pos.CENTER_RIGHT);

        // PV Name  |  Source
        // ---------+--------
        //          |
        final TableColumn<ChannelInfo, String> pv_col = new TableColumn<>(Messages.PVName);
        pv_col.setCellValueFactory(cell ->  new SimpleStringProperty(cell.getValue().getName()));
        pv_col.setReorderable(false);
        channel_table.getColumns().add(pv_col);

        final TableColumn<ChannelInfo, String> archive_col = new TableColumn<>(Messages.ArchiveName);
        archive_col.setCellValueFactory(cell ->  new SimpleStringProperty(cell.getValue().getArchiveDataSource().getName()));
        archive_col.setReorderable(false);
        channel_table.getColumns().add(archive_col);
        channel_table.setPlaceholder(new Label(Messages.SearchPatternTT));

        // PV name column uses most of the space, archive column the rest
        pv_col.prefWidthProperty().bind(channel_table.widthProperty().multiply(0.8));
        archive_col.prefWidthProperty().bind(channel_table.widthProperty().subtract(pv_col.widthProperty()));

        channel_table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        VBox.setVgrow(channel_table, Priority.ALWAYS);
        final VBox bottom = new VBox(5, search_row,
                                        replace_row,
                                        channel_table);
        setOrientation(Orientation.VERTICAL);
        getItems().setAll(archive_list, bottom);
        setDividerPositions(0.2f);

        final ContextMenu menu = new ContextMenu();
        channel_table.setContextMenu(menu);
        channel_table.setOnContextMenuRequested(this::updateContextMenu);

        Platform.runLater(() -> pattern.requestFocus());
    }

    private void updateContextMenu(final ContextMenuEvent event)
    {
        final ContextMenu menu = channel_table.getContextMenu();
        final List<ChannelInfo> selection = channel_table.getSelectionModel().getSelectedItems();
        if (selection.isEmpty())
            menu.getItems().clear();
        else
        {
            menu.getItems().setAll(new AddToPlotAction(model, selection),
                                   new SeparatorMenuItem());

            SelectionService.getInstance().setSelection(channel_table, selection);
            ContextMenuHelper.addSupportedEntries(channel_table, menu);
            menu.show(channel_table.getScene().getWindow(), event.getScreenX(), event.getScreenY());
        }
    }

    private void searchForChannels()
    {
        // In case 'replace' is selected, clear the list
        displayChannelInfos(List.of());

        final String pattern_txt = pattern.getText().trim();
        // Nothing to search?
        if (pattern_txt.length() <= 0)
        {
            // Move focus to search pattern
            pattern.requestFocus();
            return;
        }

        // Cancel ongoing Search
        if (active_job != null  &&  !active_job.getMonitor().isDone())
            active_job.cancel();

        final List<ArchiveDataSource> archives = archive_list.getSelectedArchives();
        active_job  = SearchJob.submit(archives, pattern_txt,
            channels -> Platform.runLater( () -> displayChannelInfos(channels)),
            (url, ex) -> ExceptionDetailsErrorDialog.openError(Messages.Error,
                                                               MessageFormat.format(Messages.ArchiveServerErrorFmt, url), ex));
    }

    private void displayChannelInfos(final List<ChannelInfo> channels)
    {
        final ObservableList<ChannelInfo> items = channel_table.getItems();
        if (result_replace.isSelected())
            // Replace displayed channels
            items.setAll(channels);
        else
            // Add new channels but avoid duplicates
            for (ChannelInfo channel : channels)
                if (! items.contains(channel))
                    items.add(channel);
    }

    /** @param memento From where to restore previously saved settings */
    public void restore(final Memento memento)
    {
        memento.getNumber(SEARCH_PANEL_SPLIT).ifPresent(pos -> setDividerPositions(pos.floatValue()));
    }

    /** @param memento Where to store current settings */
    public void save(final Memento memento)
    {
        memento.setNumber(SEARCH_PANEL_SPLIT, getDividers().get(0).getPosition());
    }
}
