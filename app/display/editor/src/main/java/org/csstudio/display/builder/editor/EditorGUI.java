/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor;

import static org.csstudio.display.builder.editor.Plugin.logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.csstudio.display.builder.editor.actions.ActionDescription;
import org.csstudio.display.builder.editor.app.DisplayEditorInstance;
import org.csstudio.display.builder.editor.properties.PropertyPanel;
import org.csstudio.display.builder.editor.tree.FindWidgetAction;
import org.csstudio.display.builder.editor.tree.WidgetTree;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.persist.ModelLoader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** GUI with all editor components
 *
 *  <pre>
 *  Toolbar
 *  ------------------------------------------------
 *  WidgetTree | Editor (w/ palette) | PropertyPanel
 *  </pre>
 *
 *  @author Kay Kasemir
 *  @author Claudio Rosati
 */
@SuppressWarnings("nls")
public class EditorGUI
{
    public static final String SHOW_TREE = "tree",
                               SHOW_PROPS = "props";
    private static final Preferences prefs = PhoebusPreferenceService.userNodeForClass(DisplayEditorInstance.class);

    private class ActionWapper extends MenuItem
    {
        ActionWapper(ActionDescription action)
        {
            super(action.getToolTip(),
                  ImageCache.getImageView(action.getIconResourcePath()));
            setOnAction(event -> action.run(editor));
        }
    }

    private final JFXRepresentation toolkit;

    private final Parent layout;

    private DisplayEditor editor;

    private WidgetTree tree;

    private PropertyPanel property_panel;

    // Need mouse location to 'paste' widget,
    // but key handler does not receive it.
    // So track mouse in separate mouse listener

    /** Last known mouse location */
    private int mouse_x, mouse_y;

    /** Track current mouse location inside editor */
    private final EventHandler<MouseEvent> mouse_tracker = event ->
    {
        mouse_x = (int) event.getX();
        mouse_y = (int) event.getY();
    };

    private final EventHandler<KeyEvent> key_handler = event ->
    {
        final KeyCode code = event.getCode();
        // System.out.println("Editor Key: " + event);

        // Only handle delete, copy, paste when mouse inside editor
        final boolean in_editor = editor.getContextMenuNode()
                                        .getLayoutBounds()
                                        .contains(mouse_x, mouse_y);

        // Use Ctrl-C .. except on Mac, where it's Command-C ..
        final boolean meta = event.isShortcutDown();
        if (meta  &&  code == KeyCode.Z)
            editor.getUndoableActionManager().undoLast();
        else if (meta  &&  code == KeyCode.Y)
            editor.getUndoableActionManager().redoLast();
        else if (in_editor  &&  ((meta  &&  code == KeyCode.X) || code == KeyCode.DELETE || code == KeyCode.BACK_SPACE))
            editor.cutToClipboard();
        else if (in_editor  &&  meta  &&  code == KeyCode.C)
            editor.copyToClipboard();
        else if (in_editor  &&  meta  &&  code == KeyCode.V)
            editor.pasteFromClipboard(mouse_x, mouse_y);
        else // Pass on, don't consume
            return;
        event.consume();
    };

    private volatile File file = null;

    private VBox tree_box;
    private VBox properties_box;

    private SplitPane center_split;

    private volatile Consumer<DisplayModel> model_listener = null;



    public EditorGUI()
    {
        toolkit = new JFXRepresentation(true);
        layout = createElements();
    }

    /** @param listener Listener to call once a new model has been loaded and represented */
    public void setModelListener(final Consumer<DisplayModel> listener)
    {
        model_listener = listener;
    }

    /** @return Root node of the editor GUI */
    public Parent getParentNode()
    {
        return layout;
    }

    /** @return The {@link DisplayEditor} */
    public DisplayEditor getDisplayEditor()
    {
        return editor;
    }

    /** @return Is the widget tree shown? */
    public boolean isWidgetTreeShown()
    {
        return center_split.getItems().contains(tree_box);
    }

    /** @param show Show widget tree? */
    public void showWidgetTree(final boolean show)
    {
        if (show == isWidgetTreeShown())
            return;
        if (show)
        {
            center_split.getItems().add(0,  tree_box);
            if (arePropertiesShown())
                Platform.runLater(() -> setDividerPositions(0.2, 0.8));
            else
                Platform.runLater(() -> setDividerPositions(0.2));
        }
        else
        {
            center_split.getItems().remove(tree_box);
            if (arePropertiesShown())
                Platform.runLater(() -> setDividerPositions(0.8));
        }

        // Update pref about last tree state
        prefs.putBoolean(SHOW_TREE, show);
    }

    /** @return Are the properties shown? */
    public boolean arePropertiesShown()
    {
        return center_split.getItems().contains(properties_box);
    }

    /** @param show Show properties? */
    public void showProperties(final boolean show)
    {
        if (show == arePropertiesShown())
            return;

        if (show)
        {
            center_split.getItems().add(properties_box);
            if (isWidgetTreeShown())
                Platform.runLater(() -> setDividerPositions(0.2, 0.8));
            else
                Platform.runLater(() -> setDividerPositions(0.8));
        }
        else
            center_split.getItems().remove(properties_box);

        // Update pref about last prop state
        prefs.putBoolean(SHOW_PROPS, show);
    }

    /** @return Divider positions for the 'tree', 'editor' and 'properties' */
    public double[] getDividerPositions()
    {
        return center_split.getDividerPositions();
    }

    /** @param positions Divider positions for 'tree', 'editor', 'properties' */
    public void setDividerPositions(final double... positions)
    {
        center_split.setDividerPositions(positions);
    }

    private Parent createElements()
    {
        editor = new DisplayEditor(toolkit, 50);

        tree = new WidgetTree(editor);

        property_panel = new PropertyPanel(editor);

        // Left: Widget tree
        Label header = new Label("Widgets");
        header.setMaxWidth(Double.MAX_VALUE);
        header.getStyleClass().add("header");

        final Control tree_control = tree.create();
        VBox.setVgrow(tree_control, Priority.ALWAYS);
        hookWidgetTreeContextMenu(tree_control);
        tree_box = new VBox(header, tree_control);

        // Center: Editor
        final Node editor_scene = editor.create();

        // Right: Properties
        header = new Label("Properties");
        header.setMaxWidth(Double.MAX_VALUE);
        header.getStyleClass().add("header");
        properties_box = new VBox(header, property_panel);

        center_split = new SplitPane(tree_box, editor_scene, properties_box);
        center_split.setDividerPositions(0.2, 0.8);

        if (! prefs.getBoolean(SHOW_TREE, true))
            showWidgetTree(false);

        if (! prefs.getBoolean(SHOW_PROPS, true))
            showProperties(false);

        final BorderPane layout = new BorderPane();
        layout.setCenter(center_split);
        BorderPane.setAlignment(center_split, Pos.TOP_LEFT);

        editor_scene.addEventFilter(MouseEvent.MOUSE_MOVED, mouse_tracker);

        // Handle copy/paste/...
        layout.addEventFilter(KeyEvent.KEY_PRESSED, key_handler);

        // Request keyboard focus when mouse enters,
        // to allow 'paste' etc.
        // Without this filter, user would first need to select some widget
        // before 'paste' is possible
        layout.addEventFilter(MouseEvent.MOUSE_ENTERED, event ->  layout.requestFocus());

        return layout;
    }

    private void hookWidgetTreeContextMenu(final Control node)
    {
        final ContextMenu menu = new ContextMenu(
            new ActionWapper(ActionDescription.COPY),
            new ActionWapper(ActionDescription.DELETE),
            new FindWidgetAction(node, editor),
            new ActionWapper(ActionDescription.TO_BACK),
            new ActionWapper(ActionDescription.MOVE_UP),
            new ActionWapper(ActionDescription.MOVE_DOWN),
            new ActionWapper(ActionDescription.TO_FRONT));
        node.setContextMenu(menu);
    }

    /** @return Currently edited file */
    public File getFile()
    {
        return file;
    }

    /** Load model from file
     *  @param file File that contains the model
     */
    public void loadModel(final File file)
    {
        EditorUtil.getExecutor().execute(() ->
        {
            DisplayModel model;
            try
            {
                model = ModelLoader.loadModel(new FileInputStream(file), file.getCanonicalPath());
            }
            catch (final Exception ex)
            {
                logger.log(Level.SEVERE, "Cannot load model from " + file, ex);
                ExceptionDetailsErrorDialog.openError("Creating empty file",
                        "Cannot load model from\n" + file + "\n\nCreating new, empty file", ex);
                model = new DisplayModel();
                model.propName().setValue("Empty");
            }
            setModel(model);
            this.file = file;
        });
    }

    /** Save model to file
     *  @param file File into which to save the model
     */
    public void saveModelAs(final File file)
    {
        logger.log(Level.FINE, "Save as {0}", file);
        try
        (
            final ModelWriter writer = new ModelWriter(new FileOutputStream(file));
        )
        {
            writer.writeModel(editor.getModel());
            this.file = file;
            editor.getUndoableActionManager().clear();
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Cannot save as " + file, ex);
        }
    }

    private void setModel(final DisplayModel model)
    {
        // Representation needs to be created in UI thread
        toolkit.execute(() ->
        {
            editor.setModel(model);
            tree.setModel(model);

            final Consumer<DisplayModel> listener = model_listener;
            if (listener != null)
                listener.accept(model);
        });
    }

    public void dispose()
    {
        editor.dispose();
        toolkit.shutdown();
        tree.setModel(null);

        try
        {
            prefs.flush();
        }
        catch (BackingStoreException ex)
        {
            logger.log(Level.WARNING, "Unable to flush preferences", ex);
        }
    }
}
