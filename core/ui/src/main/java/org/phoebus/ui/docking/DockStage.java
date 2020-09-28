/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.docking;

import static org.phoebus.ui.docking.DockPane.logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.workbench.Locations;
import org.phoebus.ui.application.Messages;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.Styles;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Helper for stage that uses docking
 *
 *  <p>All stages should have a known layout with {@link DockPane} etc.,
 *  but since the initial Stage is passed to the application,
 *  it cannot simply be replaced with a "DockStage extends Stage".
 *
 *  <p>This is thus not a class that extends Stage, but a helper
 *  meant to be called to configure and later interface with
 *  each Stage.
 *
 *  <p>To support docking, the stage maintained by this class has the following scene graph:
 *
 *  <ul>
 *  <li>The top-level node is a {@link BorderPane}.
 *  <li>The 'center' of that layout is either a {@link DockPane}, or a {@link SplitDock}.
 *  <li>{@link DockPane}s hold {@link DockItem}s or {@link DockItemWithInput}s.
 *  <li>{@link SplitDock} holds further {@link SplitDock}s or {@link DockPane}s
 *  </ul>
 *  This means that each {@link DockStage} has at least one {@link DockPane}.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DockStage
{
    /** Property key for the stage ID */
    public static final String KEY_ID = "ID";

    /** The KEY_ID property is a unique stage ID,
     *  except for the main window that this ID.
     */
    public static final String ID_MAIN = "DockStage_MAIN";

    /** Singleton logo */
    private static Image logo = null;

    /** @return Logo, initialized on first call */
    private static Image getLogo()
    {
        if (logo == null)
        {
            final File custom_logo = new File(Locations.install(), "site_logo.png");
            if (custom_logo.exists())
                try
                {
                    logo = new Image(new FileInputStream(custom_logo));
                }
                catch (FileNotFoundException ex)
                {
                    logger.log(Level.WARNING, "Cannot open " + custom_logo, ex);
                }

            if (logo == null)
                logo = ImageCache.getImage(DockStage.class, "/icons/logo.png");
        }
        return logo;
    }

    /** @param what Purpose of the ID, used as prefix
     *  @return Unique ID
     */
    static String createID(final String what)
    {
        return what + "_" + UUID.randomUUID().toString().replace('-', '_');
    }

    /** Helper to configure a Stage for docking
     *
     *  <p>Adds a Scene with a BorderPane layout and a DockPane in the center
     *
     *  @param stage Stage, should be empty
     *  @param tabs Zero or more initial {@link DockItem}s
     *  @throws Exception on error
     *
     *  @return {@link DockPane} that was added to the {@link Stage}
     */
    public static DockPane configureStage(final Stage stage, final DockItem... tabs)
    {
        stage.getProperties().put(KEY_ID, createID("DockStage"));

        final DockPane pane = new DockPane(tabs);

        final BorderPane layout = new BorderPane(pane);
        pane.setDockParent(layout);

        final Scene scene = new Scene(layout, 800, 600);
        stage.setScene(scene);
        stage.setTitle(Messages.FixedTitle);
        Styles.setSceneStyle(scene);
        try
        {
            stage.getIcons().add(getLogo());
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot set application icon", ex);
        }

        // Track active pane via focus
        stage.focusedProperty().addListener((prop, old, focus) ->
        {
            // Note: Is called twice when window gains focus (true).
            //       Unclear why, but doesn't cause harm, either.
            if (focus)
                setActiveDockStage(stage);
        });

        stage.setOnCloseRequest(event ->
        {
            // Prevent closing of the stage right now
            event.consume();
            // In background, prepare to close items,
            // and on success close them
            JobManager.schedule("Close " + stage.getTitle(), monitor ->
            {
                if (prepareToCloseItems(stage))
                    Platform.runLater(() -> closeItems(stage));
            });
        });

        DockPane.setActiveDockPane(pane);

        return pane;
    }

    /** @return Unique ID of this stage */
    public static String getID(final Stage stage)
    {
        return (String) stage.getProperties().get(KEY_ID);
    }

    // Implementation detail:
    // The code in here tends to iterate over all windows, panes etc.
    // This should be OK since we expect few windows (1..5),
    // each with few panes (1..10).
    // If there are more, maps would need to be used,
    // with the added complexity of keeping them in sync with reality.

    /** @param id Unique ID of a stage
     *  @return That Stage or <code>null</code> if not found
     */
    public static Stage getDockStageByID(final String id)
    {
        for (Window window : Window.getWindows())
            // id.equals(null) is OK, will return false
            if (id.equals(window.getProperties().get(KEY_ID)))
                return (Stage) window;
        return null;
    }

    /** @return All currently open dock stages (safe copy) */
    public static List<Stage> getDockStages()
    {
        final List<Stage> dock_windows = new ArrayList<>();
        // Having a KEY_ID property implies that the Window
        // is a Stage that was configured as a DockStage
        for (Window window : Window.getWindows())
            if (window.getProperties().containsKey(KEY_ID))
                dock_windows.add((Stage) window);

        return dock_windows;
    }

    /** @param name Name of a DockPane
     *  @return That DockPane or <code>null</code> if not found
     */
    public static DockPane getDockPaneByName(final String name)
    {
        for (Stage stage : getDockStages())
            for (DockPane pane : getDockPanes(stage))
                if (pane.getName().equals(name))
                    return pane;
        return null;
    }

    /** Prepare all DockItems to be closed
     *
     *  <p>Must be called off the UI thread.
     *  Might take time if dock items need to "save" their content
     *
     *  @param stage {@link Stage} with {@link DockPane}
     *  @throws Exception on error
     */
    public static boolean prepareToCloseItems(final Stage stage) throws Exception
    {
        for (DockPane pane : getDockPanes(stage))
        {
            for (DockItem item : pane.getDockItems())
                if (! item.prepareToClose())
                    // Abort the close request
                    return false;
        }

        // All tabs either saved or don't care to save,
        // so this stage will be closed
        return true;
    }

    /** Close all DockItems of this stage
     *
     *  <p>Should be called after {@link DockStage#prepareToCloseItems}
     *
     *  @param stage {@link Stage} with {@link DockPane}
     */
    public static void closeItems(final Stage stage)
    {
        for (DockPane pane : getDockPanes(stage))
            for (DockItem item : pane.getDockItems())
                item.close();
    }

    /** @param stage Stage that supports docking
     *  @return {@link BorderPane} layout of that stage
     */
    public static BorderPane getLayout(final Stage stage)
    {
        final Parent layout = stage.getScene().getRoot();
        if (layout instanceof BorderPane)
            return (BorderPane) layout;
        throw new IllegalStateException("Expect BorderPane, got " + layout);
    }

    /** Get the top dock container
     *  @param stage Stage that supports docking
     *  @return {@link DockPane} or {@link SplitDock}
     */
    public static Node getPaneOrSplit(final Stage stage)
    {
        final Node container = getLayout(stage).getCenter();
        if (container instanceof DockPane  ||
            container instanceof SplitDock)
            return container;
        throw new IllegalStateException("Expect DockPane or SplitDock, got " + container);
    }

    /** Get all dock panes
     *  @param stage Stage that supports docking
     *  @return All {@link DockPane}s, contains at least one item
     */
    public static List<DockPane> getDockPanes(final Stage stage)
    {
        final List<DockPane> panes = new ArrayList<>();
        findDockPanes(panes, getPaneOrSplit(stage));
        return panes;
    }

    private static void findDockPanes(final List<DockPane> panes, final Node pane_or_split)
    {
        if (pane_or_split instanceof DockPane)
            panes.add((DockPane) pane_or_split);
        else if (pane_or_split instanceof SplitDock)
            for (Node sub : ((SplitDock) pane_or_split).getItems())
                findDockPanes(panes, sub);
    }

    /** @param stage Stage that supports docking which should become the active stage */
    public static void setActiveDockStage(final Stage stage)
    {
        final DockPane dock_pane = getDockPanes(stage).get(0);
        DockPane.setActiveDockPane(Objects.requireNonNull(dock_pane));
    }

    /** Locate DockItemWithInput for application and input
     *  @param application_name Application name
     *  @param input Input, must not be <code>null</code>
     *  @return {@link DockItemWithInput} or <code>null</code> if not found
     */
    public static DockItemWithInput getDockItemWithInput(final String application_name, final URI input)
    {
        Objects.requireNonNull(input);
        for (Stage stage : getDockStages())
            for (DockPane pane : getDockPanes(stage))
                for (DockItem tab : pane.getDockItems())
                    if (tab instanceof DockItemWithInput)
                    {
                        final DockItemWithInput item = (DockItemWithInput) tab;
                        if (input.equals(item.getInput()) &&
                            item.getApplication().getAppDescriptor().getName().equals(application_name))
                            return item;
                    }
        return null;
    }

    /** Locate any 'fixed' {@link DockPane}s and un-fix them
     *  @param stage Stage where to clear 'fixed' panes
     */
    public static void clearFixedPanes(final Stage stage)
    {
        clearFixedPanes(getPaneOrSplit(stage));
    }

    private static void clearFixedPanes(final Node pane_or_split)
    {
        if (pane_or_split instanceof DockPane)
        {
            final DockPane pane = (DockPane) pane_or_split;
            if (pane.isFixed())
                pane.setFixed(false);
        }
        else if (pane_or_split instanceof SplitDock)
        {
            final SplitDock split = (SplitDock) pane_or_split;
            for (Node item : split.getItems())
                clearFixedPanes(item);
        }
    }

    /** @param stage Stage for which to dump basic panel hierarchy */
    public static void dump(final StringBuilder buf, final Stage stage)
    {
        final Node node = getPaneOrSplit(stage);
        buf.append("Stage ").append(stage.getTitle()).append("\n");
        dump(buf, node, 0);
    }

    private static void dump(final StringBuilder buf, final Node node, final int level)
    {
        for (int i=0; i<level; ++i)
            buf.append("  ");
        if (node instanceof DockPane)
        {
            final DockPane pane = (DockPane) node;
            buf.append(pane).append("\n");
        }
        else if (node instanceof SplitDock)
        {
            final SplitDock split = (SplitDock) node;
            buf.append("Split").append("\n");
            dump(buf, split.getItems().get(0), level + 1);
            dump(buf, split.getItems().get(1), level + 1);
        }
        else
            buf.append(node).append("\n");
    }
}
