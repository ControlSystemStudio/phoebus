/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.csstudio.display.builder.editor.actions.ActionDescription;
import org.csstudio.display.builder.editor.app.CreateGroupAction;
import org.csstudio.display.builder.editor.app.DisplayEditorInstance;
import org.csstudio.display.builder.editor.app.PasteWidgets;
import org.csstudio.display.builder.editor.app.RemoveGroupAction;
import org.csstudio.display.builder.editor.properties.PropertyPanel;
import org.csstudio.display.builder.editor.tree.CollapseTreeAction;
import org.csstudio.display.builder.editor.tree.ExpandTreeAction;
import org.csstudio.display.builder.editor.tree.FindWidgetAction;
import org.csstudio.display.builder.editor.tree.WidgetTree;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.persist.ModelLoader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.SplitPane.Divider;
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
        // Don't steal delete, backspace, ... from inline editor
        if (editor.getSelectedWidgetUITracker().isInlineEditorActive())
            return;

        // Same for widget tree's name editor
        if (tree.isInlineEditorActive())
            return;

        final KeyCode code = event.getCode();
        // System.out.println("Editor Key: " + code);

        // Only handle delete, copy, paste when mouse inside editor.
        // Those keys are often used when editing text,
        // and it's easy to accidentally loose input focus
        // and then delete a widget instead of a character.
        // Also check if property panel has focus; don't want to delete
        // widget when its name is edited and the mouse happens to be
        // inside the editor
        final boolean in_editor = editor.getContextMenuNode()
                                        .getLayoutBounds()
                                        .contains(mouse_x, mouse_y) &&
                                  ! property_panel.hasFocus();

        // Use Ctrl-C .. except on Mac, where it's Command-C ..
        final boolean meta = event.isShortcutDown();
        if (meta  &&  code == KeyCode.Z)
            editor.getUndoableActionManager().undoLast();
        else if (meta  &&  code == KeyCode.Y)
            editor.getUndoableActionManager().redoLast();
        else if (in_editor  &&  meta  &&  code == KeyCode.C)
            editor.copyToClipboard();
        else if (in_editor  &&  code == KeyCode.DELETE       &&  !editor.isReadonly())
            editor.removeWidgets();
        else if (in_editor  &&  meta  &&  code == KeyCode.X  &&  !editor.isReadonly())
            editor.cutToClipboard();
        else if (in_editor  &&  meta  &&  code == KeyCode.V  &&  !editor.isReadonly())
            pasteFromClipboard();
        else if (in_editor  &&  meta  &&  code == KeyCode.D  &&  !editor.isReadonly())
        	editor.duplicateWidgets();
        else // Pass on, don't consume
            return;
        event.consume();
    };

    private volatile File file = null;

    private VBox tree_box;
    private VBox properties_box;

    private SplitPane center_split;

    private final ChangeListener<Number> divider_listener = (ObservableValue<? extends Number> observableValue, Number oldDividerPosition, Number newDividerPosition) ->
    {
        saveDividerPreferences();
    };

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

    /** Try to paste widgets from clipboard at last known mouse position */
    public void pasteFromClipboard()
    {
        editor.pasteFromClipboard(mouse_x, mouse_y);
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

        double tdiv = prefs.getDouble(DisplayEditorInstance.TREE_DIVIDER, 0.2);
        double pdiv = prefs.getDouble(DisplayEditorInstance.PROP_DIVIDER, 0.8);

        if (show)
        {
            center_split.getItems().add(0,  tree_box);
            if (arePropertiesShown())
                Platform.runLater(() -> setDividerPositions(tdiv, pdiv));
            else
                Platform.runLater(() -> setDividerPositions(tdiv));
        }
        else
        {
            center_split.getItems().remove(tree_box);
            if (arePropertiesShown())
                Platform.runLater(() -> setDividerPositions(pdiv));
        }

        for (Divider div : center_split.getDividers())
        {
            div.positionProperty().removeListener(divider_listener);
            div.positionProperty().addListener(divider_listener);
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
            double tdiv = prefs.getDouble(DisplayEditorInstance.TREE_DIVIDER, 0.2);
            double pdiv = prefs.getDouble(DisplayEditorInstance.PROP_DIVIDER, 0.8);

            center_split.getItems().add(properties_box);
            if (isWidgetTreeShown())
                Platform.runLater(() -> setDividerPositions(tdiv, pdiv));
            else
                Platform.runLater(() -> setDividerPositions(pdiv));
        }
        else
            center_split.getItems().remove(properties_box);

        for (Divider div : center_split.getDividers())
        {
            div.positionProperty().removeListener(divider_listener);
            div.positionProperty().addListener(divider_listener);
        }

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

    /** @param snap Snap Grid on/off */
    public void snapGrid(final boolean snap)
    {
        editor.setGrid(snap);
    }

    /** @param snap Snap Widgets on/off */
    public void snapWidgets(final boolean snap)
    {
        editor.setSnap(snap);
    }

    /** @param show Show Coordinates on/off */
    public void showCoords(final boolean show)
    {
        editor.setCoords(show);
    }

    /** @return Snap Grid on/off */
    public boolean getSnapGrid()
    {
        return editor.getSelectedWidgetUITracker().getEnableGrid();
    }

    /** @return Snap Widgets on/off */
    public boolean getSnapWidgets()
    {
        return editor.getSelectedWidgetUITracker().getEnableSnap();
    }

    /** @return Show Coordinates on/off */
    public boolean getShowCoords()
    {
        return editor.getSelectedWidgetUITracker().getShowLocationAndSize();
    }

    private Parent createElements()
    {
        editor = new DisplayEditor(toolkit, org.csstudio.display.builder.editor.Preferences.undoStackSize);

        tree = new WidgetTree(editor);

        property_panel = new PropertyPanel(editor);

        // Left: Widget tree
        Label header = new Label("Widgets");
        header.setMaxWidth(Double.MAX_VALUE);
        header.getStyleClass().add("header");
        tree.configureHeaderDnD(header);

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

        double ldiv = prefs.getDouble(DisplayEditorInstance.TREE_DIVIDER, 0.2);
        double rdiv = prefs.getDouble(DisplayEditorInstance.PROP_DIVIDER, 0.8);

        center_split.setDividerPositions(ldiv, rdiv);

        for (Divider div : center_split.getDividers())
        {
            div.positionProperty().addListener(divider_listener);
        }

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

        // We used to request keyboard focus when mouse enters,
        // to allow copy/paste between two windows.
        // Without this filter, user first needs to select some widget in the 'other'
        // before 'paste' is possible in the 'other' window.
        //   layout.addEventFilter(MouseEvent.MOUSE_ENTERED, event -> layout.requestFocus());
        // The side effect, however, is that this breaks the natural focus handling:
        // Use edits some property, for example a Label's text.
        // Mouse moves by accident out and back into the window
        // -> Focus now on 'layout', and that means the next Delete or Backspace
        // meant to edit the text will instead delete the widget.
        // https://github.com/kasemir/org.csstudio.display.builder/issues/486
        // Using mouse-pressed works and is also quite natural:
        // 1) Copy widgets in some other window
        // 2) Click in layout to indicate paste position (and get focus)
        // 3) Ctrl-V or context menu to paste
        layout.addEventFilter(MouseEvent.MOUSE_PRESSED, event ->
        {
            // Don't steal the focus from an active inline editor,
            // since that would close/commit it,
            // and not allow clicking into editor to set cursor
            if (! this.editor.getSelectedWidgetUITracker().isInlineEditorActive())
                layout.requestFocus();
        });

        return layout;
    }

    private void hookWidgetTreeContextMenu(final Control node)
    {
        final ContextMenu menu = new ContextMenu(new MenuItem());
        node.setContextMenu(menu);
        menu.setOnShowing(event ->
        {
            // Enable/disable menu entries based on selection
            final List<Widget> widgets = editor.getWidgetSelectionHandler().getSelection();
            final MenuItem delete = new ActionWapper(ActionDescription.DELETE);
            final MenuItem cut = new ActionWapper(ActionDescription.CUT);
            final MenuItem copy = new ActionWapper(ActionDescription.COPY);
            final MenuItem group = new CreateGroupAction(editor, widgets);
            if (widgets.size() < 0)
            {
                delete.setDisable(true);
                cut.setDisable(true);
                copy.setDisable(true);
            }
            final MenuItem ungroup;
            if (widgets.size() == 1  &&  widgets.get(0) instanceof GroupWidget)
                ungroup = new RemoveGroupAction(editor, (GroupWidget)widgets.get(0));
            else
            {
                ungroup = new RemoveGroupAction(editor, null);
                ungroup.setDisable(true);
            }
            if (editor.isReadonly())
                menu.getItems().setAll(copy,
                                       new FindWidgetAction(node, editor),
                                       new ExpandTreeAction(tree),
                                       new CollapseTreeAction(tree),
                                       new SeparatorMenuItem());
            else
                menu.getItems().setAll(delete,
                                       cut,
                                       copy,
                                       new PasteWidgets(this),
                                       new FindWidgetAction(node, editor),
                                       new ExpandTreeAction(tree),
                                       new CollapseTreeAction(tree),
                                       new SeparatorMenuItem(),
                                       group,
                                       ungroup,
                                       new SeparatorMenuItem(),
                                       new ActionWapper(ActionDescription.TO_BACK),
                                       new ActionWapper(ActionDescription.MOVE_UP),
                                       new ActionWapper(ActionDescription.MOVE_DOWN),
                                       new ActionWapper(ActionDescription.TO_FRONT));
        });
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
            String canon_path = null;
            try
            {
                canon_path = file.getCanonicalPath();
                model = ModelLoader.loadModel(new FileInputStream(file), canon_path);
                this.file = file;
            }
            catch (final Exception ex)
            {
                canon_path = null;
                logger.log(Level.SEVERE, "Cannot load model from " + file, ex);
                ExceptionDetailsErrorDialog.openError("Creating empty file",
                        "Cannot load model from\n" + file + "\n\nCreating new, empty model", ex);
                model = new DisplayModel();
                model.propName().setValue("Empty");
                // Don't associate this editor with the file we've failed to load
                this.file = null;
            }

            if (! file.canWrite())
                model.setUserData(DisplayModel.USER_DATA_READONLY, Boolean.TRUE.toString());
            setModel(model);

            try
            {
                logger.log(Level.FINE, "Waiting for representation of model " + canon_path);

                toolkit.awaitRepresentation(30, TimeUnit.SECONDS);
                logger.log(Level.FINE, "Done with representing model of " + canon_path);
            }
            catch (TimeoutException | InterruptedException ex)
            {
                logger.log(Level.SEVERE, "Cannot wait for representation of " + canon_path, ex);
            }
            catch (NullPointerException ex)
            {
                // Worst case scenario; the CountDownLatch in setModel() timed out and there is no Phaser in toolkit yet
            }

            if (canon_path != null && model.isClean() == false)
            {
                ExceptionDetailsErrorDialog.openError("Errors while loading model",
                        "There were some errors while loading model from " + file + "\nNot all widgets are displayed correctly; " +
                        "saving the display in this state might lead to losing those widgets or some of their properties." +
                        "\nPlease check the log for details.", null);
            }

        });
    }

    /** Save model to file
     *  @param file File into which to save the model
     *  @throws Exception on error
     */
    public void saveModelAs(final File file) throws Exception
    {
        logger.log(Level.FINE, "Save as {0}", file);
        try
        (
            final FileOutputStream fwriter = new FileOutputStream(file);
            final ModelWriter writer = new ModelWriter(fwriter);
        )
        {
            writer.writeModel(editor.getModel());
            fwriter.flush();
            this.file = file;
            editor.getUndoableActionManager().clear();
        }
        catch (Exception ex)
        {
            throw new Exception("Cannot save as " + file, ex);
        }
    }

    /** @param model Display Model */
    private void setModel(final DisplayModel model)
    {
        final CountDownLatch ui_started = new CountDownLatch(1);

        // Representation needs to be created in UI thread
        toolkit.execute(() ->
        {
            try
            {
                if (EditorUtil.isDisplayReadOnly(model))
                {
                    // Show only the main section.
                    // User may open widget tree via context menu,
                    // but property panel should remain hidden
                    // unless it supports a read-only mode.
                    showProperties(false);
                    showWidgetTree(false);
                }
                editor.setModel(model);
                tree.setModel(model);

                final Consumer<DisplayModel> listener = model_listener;
                if (listener != null)
                    listener.accept(model);
            }
            finally
            {
                ui_started.countDown();
            }
        });

        try
        {
            ui_started.await(30, TimeUnit.SECONDS);
        }
        catch (InterruptedException ex)
        {
        }
    }

    private void saveDividerPreferences()
    {
        final double[] dividers = getDividerPositions();
        if (dividers.length == 1)
            if (arePropertiesShown())
                prefs.putDouble(DisplayEditorInstance.PROP_DIVIDER, dividers[0]);
            else
                prefs.putDouble(DisplayEditorInstance.TREE_DIVIDER, dividers[0]);
        else if (dividers.length > 1)
        {
            prefs.putDouble(DisplayEditorInstance.TREE_DIVIDER, dividers[0]);
            prefs.putDouble(DisplayEditorInstance.PROP_DIVIDER, dividers[1]);
        }
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
