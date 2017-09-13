/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.internal;

import org.phoebus.framework.persistence.MementoTree;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.docking.DockStage;

import javafx.stage.Stage;

/** Helper for persisting UI to/from memento
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MementoHelper
{
    private static final String FULLSCREEN = "fullscreen";
    private static final String HEIGHT = "height";
    private static final String MAXIMIZED = "maximized";
    private static final String MINIMIZED = "minimized";
    private static final String WIDTH = "width";
    private static final String X = "x";
    private static final String Y = "y";

    /** Save state of Stage to memento
     *  @param memento
     *  @param stage
     */
    public static void saveStage(final MementoTree memento, final Stage stage)
    {
        final MementoTree stage_memento = memento.getChild(DockStage.getID(stage));
        stage_memento.setNumber(X, stage.getX());
        stage_memento.setNumber(Y, stage.getY());
        stage_memento.setNumber(WIDTH, stage.getWidth());
        stage_memento.setNumber(HEIGHT, stage.getHeight());
        if (stage.isFullScreen())
            stage_memento.setBoolean(FULLSCREEN, true);
        else if (stage.isMaximized())
            stage_memento.setBoolean(MAXIMIZED, true);
        else if (stage.isIconified())
            stage_memento.setBoolean(MINIMIZED, true);

        for (DockItem item : DockStage.getDockPane(stage).getDockItems())
            saveDockItem(stage_memento, item);
    }

    private static void saveDockItem(final MementoTree memento, final DockItem item)
    {
        final MementoTree item_memento = memento.getChild(item.getID());
        // TODO Don't save item's label.
        //      Get application to save its information,
        //      so that app can later be asked to re-open.
        item_memento.setString("dummy_label", item.getLabel());
    }

    /** Restore state of Stage from memento
     *  @param memento
     *  @param stage
     */
    public static void restoreStage(final MementoTree stage_memento, final Stage stage)
    {
        stage_memento.getNumber(X).ifPresent(num -> stage.setX(num.doubleValue()));
        stage_memento.getNumber(Y).ifPresent(num -> stage.setY(num.doubleValue()));
        stage_memento.getNumber(WIDTH).ifPresent(num -> stage.setWidth(num.doubleValue()));
        stage_memento.getNumber(HEIGHT).ifPresent(num -> stage.setHeight(num.doubleValue()));
        stage_memento.getBoolean(FULLSCREEN).ifPresent(flag -> stage.setFullScreen(flag));
        stage_memento.getBoolean(MAXIMIZED).ifPresent(flag -> stage.setMaximized(flag));
        stage_memento.getBoolean(MINIMIZED).ifPresent(flag -> stage.setIconified(flag));

        final DockPane pane = DockStage.getDockPane(stage);
        for (MementoTree item_memento : stage_memento.getChildren())
            restoreDockItem(item_memento, pane);
    }

    private static void restoreDockItem(MementoTree item_memento, DockPane pane)
    {
        System.out.println("Need to restore " + item_memento.getName());
        // TODO Do _not_ create a DockItem,
        //      but open the original application instance
        //      which will then open a dock item.
        final DockItem dummy = new DockItem(item_memento.getString("dummy_label").orElse("??"));
        pane.addTab(dummy);
    }
}
