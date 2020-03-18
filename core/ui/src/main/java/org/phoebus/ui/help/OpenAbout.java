/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.help;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.preferences.PropertyPreferenceLoader;
import org.phoebus.framework.preferences.PropertyPreferenceWriter;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.framework.workbench.Locations;
import org.phoebus.ui.application.Messages;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.OpenFileDialog;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.ReadOnlyTextCell;
import org.phoebus.ui.spi.MenuEntry;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser.ExtensionFilter;

/** Menu entry to open 'about'
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class OpenAbout implements MenuEntry
{
    /** Non-<code>null</code> while the 'about' dialog is shown */
    private Alert dialog;

    private class OpenFileBrowserCell extends TableCell<List<String>, String>
    {
        @Override
        protected void updateItem(final String item, boolean empty)
        {
            super.updateItem(item, empty);
            if (empty || getIndex() > 3)
                setGraphic(null);
            else
            {
                final Button button = new Button("...");
                button.setTooltip(new Tooltip(Messages.HelpAboutOpenLocation));
                button.setOnAction(event ->
                {
                    // Open file browser, then close the 'about' dialog
                    ApplicationService.createInstance("file_browser", new File(item).toURI());
                    if (dialog != null)
                        dialog.close();
                });
                setGraphic(button);
            }
        }
    }

    @Override
    public String getName()
    {
        return Messages.HelpAbout;
    }

    @Override
    public String getMenuPath()
    {
        return Messages.HelpAboutMenuPath;
    }

    @Override
    public Image getIcon()
    {
        return ImageCache.getImage(getClass(), "/icons/info.png");
    }

    @Override
    public Void call()
    {
        dialog = new Alert(AlertType.INFORMATION);
        dialog.setTitle(Messages.HelpAboutTitle);
        dialog.setHeaderText(Messages.HelpAboutHdr);

        // Table with Name, Value columns
        final ObservableList<List<String>> infos = FXCollections.observableArrayList();
        // Start with most user-specific to most generic: User location, install, JDK, ...
        // Note that OpenFileBrowserCell is hard-coded to add a "..." button for the first few rows.
        infos.add(Arrays.asList(Messages.HelpAboutUser, Locations.user().toString()));
        infos.add(Arrays.asList(Messages.HelpAboutInst, Locations.install().toString()));
        infos.add(Arrays.asList(Messages.HelpAboutUserDir, System.getProperty("user.dir")));
        infos.add(Arrays.asList(Messages.HelpJavaHome, System.getProperty("java.home")));
        infos.add(Arrays.asList(Messages.AppVersionHeader, Messages.AppVersion));
        infos.add(Arrays.asList(Messages.HelpAboutJava, System.getProperty("java.specification.vendor") + " " + System.getProperty("java.runtime.version")));
        infos.add(Arrays.asList(Messages.HelpAboutJfx, System.getProperty("javafx.runtime.version")));
        infos.add(Arrays.asList(Messages.HelpAboutPID, Long.toString(ProcessHandle.current().pid())));

        // Display in TableView
        final TableView<List<String>> info_table = new TableView<>(infos);
        info_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        info_table.setPrefHeight(290.0);

        final TableColumn<List<String>, String> name_col = new TableColumn<>(Messages.HelpAboutColName);
        name_col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().get(0)));
        info_table.getColumns().add(name_col);

        final TableColumn<List<String>, String> value_col = new TableColumn<>(Messages.HelpAboutColValue);
        value_col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().get(1)));
        value_col.setCellFactory(col -> new ReadOnlyTextCell<>());
        info_table.getColumns().add(value_col);

        final TableColumn<List<String>, String> link_col = new TableColumn<>();
        link_col.setMinWidth(50);
        link_col.setMaxWidth(50);
        link_col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().get(1)));
        link_col.setCellFactory(col ->  new OpenFileBrowserCell());
        info_table.getColumns().add(link_col);

        dialog.getDialogPane().setContent(info_table);

        // Info for expandable "Show Details" section
        dialog.getDialogPane().setExpandableContent(createDetailSection());

        dialog.setResizable(true);
        dialog.getDialogPane().setPrefWidth(800);
        DialogHelper.positionDialog(dialog, DockPane.getActiveDockPane(), -400, -300);

        dialog.showAndWait();
        // Indicate that dialog is closed; allow GC
        dialog = null;

        return null;
    }

    private Node createDetailSection()
    {
        // Tabs, each with a read-only text area
        // so user can copy details out

        // List applications
        final StringBuilder apps_text = new StringBuilder();
        ApplicationService.getApplications()
                          .stream()
                          .sorted((a, b) -> a.getDisplayName().compareTo(b.getDisplayName()))
                          .forEach(app -> apps_text.append(app.getDisplayName()).append("\n"));

        TextArea area = new TextArea(apps_text.toString());
        area.setEditable(false);
        final Tab apps = new Tab(Messages.HelpAboutAppFea, area);

        // Environment variables
        final StringBuilder props_env = new StringBuilder();
        System.getenv()
              .keySet()
              .stream()
              .sorted()
              .forEach(var ->  props_env.append(var).append(" = ").append(System.getenv(var)).append("\n"));

        area = new TextArea(props_env.toString());
        area.setEditable(false);
        final Tab envs = new Tab(Messages.HelpAboutEnv, area);

        // System properties
        final StringBuilder props_text = new StringBuilder();
        System.getProperties()
              .stringPropertyNames()
              .stream()
              .sorted()
              .forEach(prop ->  props_text.append(prop).append(" = ").append(System.getProperty(prop)).append("\n"));

        area = new TextArea(props_text.toString());
        area.setEditable(false);
        final Tab props = new Tab(Messages.HelpAboutSysFea, area);

        // Preference settings
        final ByteArrayOutputStream prefs_buf = new ByteArrayOutputStream();
        try
        {
            PropertyPreferenceWriter.save(prefs_buf);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot list preferences", ex);
        }

        area = new TextArea(prefs_buf.toString());
        area.setEditable(false);

        final Button import_prefs = new Button("Import Preferences");
        import_prefs.setOnAction(event -> import_preferences());
        final HBox bottom_row = new HBox(import_prefs);
        bottom_row.setAlignment(Pos.BASELINE_RIGHT);

        VBox.setVgrow(area, Priority.ALWAYS);

        final Tab prefs = new Tab(Messages.HelpAboutPrefs, new VBox(5, area, bottom_row));

        final TabPane tabs = new TabPane(apps, envs, props, prefs);
        return tabs;
    }

    /** Prompt for settings.ini to import, then offer restart */
    private void import_preferences()
    {
        final DockPane parent = DockPane.getActiveDockPane();

        final ExtensionFilter[] ini = new ExtensionFilter[] { new ExtensionFilter("Preference settings.ini", List.of("*.ini")) };
        final File file = new OpenFileDialog().promptForFile(parent.getScene().getWindow(), Messages.Open, null, ini);
        if (file == null)
            return;

        JobManager.schedule("Load preferences", monitor ->
        {
            PropertyPreferenceLoader.load(new FileInputStream(file));
            Platform.runLater(() ->
            {
                final Alert restart = new Alert(AlertType.CONFIRMATION);
                restart.setHeaderText("Restart to activate loaded settings");
                restart.setContentText("For performance reasons, preference settings are only loaded once on startup.\n" +
                                       "Exit application so you can then start it again?");
                restart.getDialogPane().setPrefSize(500, 300);
                restart.setResizable(true);
                DialogHelper.positionDialog(restart, parent, -400, -300);
                if (restart.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK)
                    System.exit(0);
            });
        });
    }
}
