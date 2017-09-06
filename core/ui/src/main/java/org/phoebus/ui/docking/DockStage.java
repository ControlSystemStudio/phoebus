/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.docking;

import static org.phoebus.ui.docking.DockPane.logger;

import java.util.Objects;
import java.util.logging.Level;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/** Helper for stage that uses docking
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DockStage
{
    /** Helper to configure a Stage for docking
     *
     *  <p>Adds a Scene with a BorderPane and a DockPane in the center
     *
     *  @param stage Stage, should be empty
     *  @param tabs Zero or more initial {@link DockItem}s
     *  @throws Exception on error
     *
     *  @return {@link DockPane} that was added to the {@link Stage}
     */
    public static DockPane configureStage(final Stage stage, final DockItem... tabs)
    {
        final DockPane tab_pane = new DockPane(tabs);

        final BorderPane layout = new BorderPane(tab_pane);

        final Scene scene = new Scene(layout, 800, 600);
        stage.setScene(scene);
        stage.setTitle("Phoebus");
        try
        {
            stage.getIcons().add(new Image(DockStage.class.getResourceAsStream("/icons/logo.png")));
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot set application icon", ex);
        }

        // Track active pane via focus
        stage.focusedProperty().addListener((prop, old, focus) ->
        {
            if (focus)
                DockPane.setActiveDockPane(tab_pane);
        });

        return getDockPane(stage);
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

    /** @param stage Stage that supports docking
     *  @return {@link DockPane} of that stage
     */
    public static DockPane getDockPane(final Stage stage)
    {
        final Node dock_pane = getLayout(stage).getCenter();
        if (dock_pane instanceof DockPane)
            return (DockPane) dock_pane;
        throw new IllegalStateException("Expect DockPane, got " + dock_pane);
    }

    /** @param stage Stage that supports docking which should become the active stage */
    public static void setActiveDockPane(final Stage stage)
    {
        final DockPane dock_pane = getDockPane(stage);
        DockPane.setActiveDockPane(Objects.requireNonNull(dock_pane));
    }
}
