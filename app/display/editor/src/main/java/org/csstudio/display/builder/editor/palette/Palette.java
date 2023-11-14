/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.palette;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.editor.Preferences;
import org.csstudio.display.builder.editor.util.WidgetIcons;
import org.csstudio.display.builder.editor.util.WidgetTransfer;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetFactory;
import org.phoebus.ui.javafx.ClearingTextField;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

/** Palette of all available widgets
 *  @author Kay Kasemir
 *  @author Claudio Rosati
 */
@SuppressWarnings("nls")
public class Palette
{
    // TODO better auto-sizing
    // All palette entries should have the same size,
    // but since each Button is in a separate TilePane,
    // it's not obvious how to get them to the same size
    // unless that's set to a fixed pixel value.
    private final static int PREFERRED_WIDTH = 160;

    private final DisplayEditor editor;

    private Collection<Pane> groups;

    private WidgetDescriptor active_widget_type = null;

    /** @param editor {@link DisplayEditor} */
    public Palette (final DisplayEditor editor)
    {
        this.editor = editor;
    }

    /** Create UI elements
     *  @return Top-level Node of the UI
     */
    @SuppressWarnings("unchecked")
    public Node create()
    {
        final VBox palette = new VBox();

        final Map<WidgetCategory, Pane> palette_groups = createWidgetCategoryPanes(palette);
        groups = palette_groups.values();
        createWidgetEntries(palette_groups);

        final ScrollPane palette_scroll = new ScrollPane(palette);
        palette_scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        palette_scroll.setFitToWidth(true);

        // TODO Determine the correct size for the main node
        // Using 2*PREFERRED_WIDTH was determined by trial and error
        palette_scroll.setMinWidth(PREFERRED_WIDTH + 12);
        palette_scroll.setPrefWidth(PREFERRED_WIDTH);

        // Copy the widgets, i.e. the children of each palette_group,
        // to the userData.
        // Actual children are now updated based on search by widget name
        palette_groups.values().forEach(group -> group.setUserData(new ArrayList<Node>(group.getChildren())));

        final TextField searchField = new ClearingTextField();
        searchField.setPromptText(Messages.SearchTextField);
        searchField.setTooltip(new Tooltip(Messages.WidgetFilterTT));
        searchField.setPrefColumnCount(9);
        searchField.textProperty().addListener( ( observable, oldValue, search_text ) ->
        {
            final String search = search_text.toLowerCase().trim();
            palette_groups.values().stream().forEach(group ->
            {
                group.getChildren().clear();
                final List<Node> all_widgets = (List<Node>)group.getUserData();
                if (search.isEmpty())
                    group.getChildren().setAll(all_widgets);
                else
                    group.getChildren().setAll(all_widgets.stream()
                                                          .filter(node ->
                                                          {
                                                             final String text = ((ToggleButton) node).getText().toLowerCase();
                                                             return text.contains(search);
                                                          })
                                                         .collect(Collectors.toList()));
            });
        });
        HBox.setHgrow(searchField, Priority.NEVER);

        final HBox toolsPane = new HBox(6);
        toolsPane.setAlignment(Pos.CENTER_RIGHT);
        toolsPane.setPadding(new Insets(6));
        toolsPane.getChildren().add(searchField);

        BorderPane paletteContainer = new BorderPane();
        paletteContainer.setTop(toolsPane);
        paletteContainer.setCenter(palette_scroll);

        return paletteContainer;
    }

    /** Create a TilePane for each WidgetCategory
     *  @param parent Parent Pane
     *  @return Map of panes for each category
     */
    private Map<WidgetCategory, Pane> createWidgetCategoryPanes(final Pane parent)
    {
        final Map<WidgetCategory, Pane> palette_groups = new HashMap<>();
        final Set<String> deprecated = Preferences.hidden_widget_types;
        for (final WidgetCategory category : WidgetCategory.values())
        {
            if (!WidgetFactory.getInstance()
            .getWidgetDescriptions()
            .stream()
            .filter(desc -> !deprecated.contains(desc.getType()))
            .filter(desc -> desc.getCategory() == category)
            .findFirst().isPresent())
                continue;
            final TilePane palette_group = new TilePane();
            palette_group.getStyleClass().add("palette_group");
            palette_group.setPrefColumns(1);
            palette_group.setMaxWidth(Double.MAX_VALUE);
            palette_groups.put(category, palette_group);
            palette_group.setHgap(2);
            palette_group.setVgap(2);
            final TitledPane pane = new TitledPane(category.getDescription(), palette_group);
            pane.getStyleClass().add("palette_category");
            parent.getChildren().add(pane);
        }
        return palette_groups;
    }

    /** Create entry for each widget type
      * @param palette_groups Map with parent panes for each widget category
     */
    private void createWidgetEntries(final Map<WidgetCategory, Pane> palette_groups)
    {
        final Set<String> deprecated = Preferences.hidden_widget_types;

        //  Sort alphabetically-case-insensitive widgets inside their group
        //  based on the widget's name, instead of the original set order or class name.
        WidgetFactory.getInstance()
                     .getWidgetDescriptions()
                     .stream()
                     .filter(desc -> !deprecated.contains(desc.getType()))
                     .sorted((d1,d2) -> String.CASE_INSENSITIVE_ORDER.compare(d1.getName(), d2.getName()))
                     .forEach(desc ->
       {
            final ToggleButton button = new ToggleButton(desc.getName());

            final Image icon = WidgetIcons.getIcon(desc.getType());
            if (icon != null)
                button.setGraphic(new ImageView(icon));

            button.setPrefWidth(PREFERRED_WIDTH);
            button.setAlignment(Pos.BASELINE_LEFT);
            button.setTooltip(new Tooltip(desc.getDescription()));
            button.setOnAction(event ->
            {
                // Remember the widget-to-create via rubberband
                active_widget_type = desc;

                // De-select all _other_ buttons
                deselectButtons(button);
            });

            palette_groups.get(desc.getCategory()).getChildren().add(button);
            WidgetTransfer.addDragSupport(button, editor, this, desc, icon);
        });
    }

    /** @return Selected widget type or <code>null</code> */
    public WidgetDescriptor getSelectedWidgetType()
    {
        return active_widget_type;
    }

    /** De-select buttons
     *  @param keep The one button to keep (or <code>null</code>)
     */
    private void deselectButtons(final ToggleButton keep)
    {
        // De-select all buttons
        for (Pane pane : groups)
            for (Node other : pane.getChildren())
                if (other instanceof ToggleButton  &&
                    other != keep)
                    ((ToggleButton)other).setSelected(false);
    }


    /** Clear the currently selected widget type */
    public void clearSelectedWidgetType()
    {
        active_widget_type = null;
        deselectButtons(null);
    }
}
