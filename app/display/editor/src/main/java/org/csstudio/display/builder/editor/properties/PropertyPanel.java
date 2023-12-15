/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.properties;


import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ClearingTextField;
import org.phoebus.ui.javafx.PlatformInfo;

import java.io.File;
import java.net.URI;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Property UI
 *
 *  @author Kay Kasemir
 *  @author Claudio Rosati
 */
@SuppressWarnings( "nls" )
public class PropertyPanel extends BorderPane
{
    private final DisplayEditor        editor;
    private final PropertyPanelSection section;
    private final TextField            searchField = new ClearingTextField();

    private File file;

    private final SimpleStringProperty filePathProperty = new SimpleStringProperty();

    /** @param editor {@link DisplayEditor}
     */
    public PropertyPanel (final DisplayEditor editor)
    {

        this.editor = editor;
        section = new PropertyPanelSection();

        searchField.setPromptText(Messages.SearchTextField);
        searchField.setTooltip(new Tooltip(Messages.PropertyFilterTT));
        searchField.textProperty().addListener( ( observable, oldValue, newValue ) -> setSelectedWidgets(editor.getWidgetSelectionHandler().getSelection()));
        HBox.setHgrow(searchField, Priority.NEVER);

        final HBox toolsPane = new HBox(6);
        toolsPane.setAlignment(Pos.CENTER_RIGHT);
        toolsPane.setPadding(new Insets(6));
        toolsPane.getChildren().add(searchField);

        Label filePathCategoryLabel = new Label(Messages.FilePath);
        filePathCategoryLabel.getStyleClass().add("property_category");
        filePathCategoryLabel.setMaxWidth(Double.MAX_VALUE);

        final HBox filePathPane = new HBox();
        filePathPane.getStyleClass().add("file_path_pane");
        Label filePathLabel = new Label();
        filePathLabel.setMaxWidth(Double.MAX_VALUE);
        filePathLabel.getStyleClass().add("file_path");
        filePathLabel.textProperty().bind(filePathProperty);
        filePathLabel.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        HBox.setHgrow(filePathLabel, Priority.ALWAYS);

        Button openFileBrowser = new Button("...");
        openFileBrowser.setOnAction(e -> {
            try {
                String absolutePath = this.file.getParentFile().getAbsolutePath();
                String uriString;
                if(PlatformInfo.isWindows){
                    uriString = "file:/" + absolutePath.replace('\\', '/');
                }
                else {
                    uriString = "file:" + absolutePath;
                }
                ApplicationService.createInstance("file_browser", new URI(uriString));
            } catch (Exception ex) {
                Logger.getLogger(PropertyPanel.class.getName())
                        .log(Level.WARNING, "Unable to launch file browser app", ex);
            }
        });
        openFileBrowser.setTooltip(new Tooltip(Messages.FileBrowserToolTip));
        filePathPane.getChildren().addAll(filePathLabel, openFileBrowser);

        final VBox topPane = new VBox();
        topPane.setAlignment(Pos.CENTER_LEFT);
        topPane.getChildren().addAll(toolsPane, filePathCategoryLabel, filePathPane);

        final ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setContent(section);
        scrollPane.setMinHeight(0);

        setTop(topPane);
        setCenter(scrollPane);

        // Track currently selected widgets
        editor.getWidgetSelectionHandler().addListener(this::setSelectedWidgets);

        setMinHeight(0);
    }

    /**
     *  @return Whether one of the property editors has focus
     */
    public boolean hasFocus()
    {
        return section.hasFocus();
    }

    /** Populate UI with properties of widgets
     *  @param widgets Widgets to configure
     */
    private void setSelectedWidgets(final List<Widget> widgets)
    {
        final DisplayModel model = editor.getModel();

        searchField.setDisable(widgets.isEmpty() && model == null);

        if (widgets.isEmpty())
        {   // Use the DisplayModel
            if (model != null)
                updatePropertiesView(model.getProperties(), Collections.emptyList());
        }
        else
        {   // Determine common properties
            final List<Widget> other = new ArrayList<>(widgets);
            final Widget primary = other.remove(0);
            final Set<WidgetProperty<?>> properties = commonProperties(primary, other);
            updatePropertiesView(properties, other);
        }
    }

    /** Determine common properties
     *
     *  @param primary Primary widget, the one selected first
     *  @param other Zero or more 'other' widgets
     *  @return Common properties
     */
    private Set<WidgetProperty<?>> commonProperties(final Widget primary, final List<Widget> other)
    {

        if (other.contains(primary))
            throw new IllegalArgumentException("Primary widget " + primary + " included in 'other'");

        // Start with properties of primary widget
        final Set<WidgetProperty<?>> common = new LinkedHashSet<>(primary.getProperties());
        // Keep properties shared by other widgets, i.e. remove those _not_ in
        // other
        for (Widget o : other)
            common.removeIf(prop -> !o.checkProperty(prop.getName()).isPresent());

        return common;
    }

    private void updatePropertiesView(final Set<WidgetProperty<?>> properties, final List<Widget> other)
    {
        final String search = searchField.getText().trim();
        final Set<WidgetProperty<?>> filtered;

        if (search.trim().isEmpty())
            filtered = properties;
        else
        {   // Filter properties, but preserve order (LinkedHashSet)
            filtered = new LinkedHashSet<>();
            for (WidgetProperty<?> prop : properties)
                if (prop.getDescription().toLowerCase().contains(search.toLowerCase()))
                    filtered.add(prop);
        }
        final DisplayModel model = editor.getModel();

        section.clear();
        section.setClassMode(model != null && model.isClassModel());
        section.fill(editor.getUndoableActionManager(), filtered, other);
    }

    /**
     * Sets the absolute file path text
     *
     * @param file OPI file being edited
     */
    public void setFile(File file) {
        this.file = file;
        filePathProperty.set(file.getAbsolutePath());
    }
}
