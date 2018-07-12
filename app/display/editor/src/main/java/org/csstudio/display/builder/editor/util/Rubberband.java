/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.util;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;

/** Rubber band type selector
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Rubberband
{
    /** Handler for rubberband selection of region */
    @FunctionalInterface
    public static interface RubberbandHandler
    {
        /** User selected region on screen with rubberband
         *  @param region Screen region
         *  @param update_existing Was 'Crtl' held to update existing selection?
         */
        public void handleSelectedRegion(Rectangle2D region, boolean update_existing);
    }

    private final Group parent;
    private final RubberbandHandler handler;
    private final Rectangle rect;
    private boolean active = false;
    private double x0, y0, x1, y1;

    /** Create rubber
     *  @param event_source       Node that will react to mouse click/drag/release,
     *                            where the user will be able to 'start' a rubber band selection
     *  @param parent             Parent in which rubber band is displayed
     *  @param rubberband_handler Handler that will be invoked with the selected region
     *
     */
    public Rubberband(final Node event_source, final Group parent, final RubberbandHandler rubberband_handler)
    {
        this.parent = parent;
        this.handler = rubberband_handler;
        event_source.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleStart);
        event_source.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleDrag);
        event_source.addEventFilter(MouseEvent.MOUSE_RELEASED, this::handleStop);
        rect = new Rectangle(0, 0, 0, 0);
        rect.setArcWidth(5);
        rect.setArcHeight(5);
        rect.getStyleClass().add("rubberband");
    }

    private void handleStart(final MouseEvent event)
    {
        if (! event.isPrimaryButtonDown())
            return;
        active = true;

        // Event originates from a node that allows 'clicking' beyond the
        // model elements, i.e. the size of the 'parent'.
        // The 'parent', however, may be scrolled, and the rubber band needs
        // to show up in the 'parent', so convert coordinates
        // from event to the parent:
        final Point2D in_parent = parent.sceneToLocal(event.getSceneX(), event.getSceneY());
        x0 = in_parent.getX();
        y0 = in_parent.getY();
        rect.setX(x0);
        rect.setY(y0);
        rect.setWidth(1);
        rect.setHeight(1);
        parent.getChildren().add(rect);
        event.consume();
    }

    private void handleDrag(final MouseEvent event)
    {
        if (! active)
            return;
        final Point2D in_parent = parent.sceneToLocal(event.getSceneX(), event.getSceneY());
        x1 = in_parent.getX();
        y1 = in_parent.getY();
        rect.setX(Math.min(x0, x1));
        rect.setY(Math.min(y0, y1));
        rect.setWidth(Math.abs(x1 - x0));
        rect.setHeight(Math.abs(y1 - y0));
        event.consume();
    }

    private void handleStop(final MouseEvent event)
    {
        if (! active)
            return;
        final Point2D in_parent = parent.sceneToLocal(event.getSceneX(), event.getSceneY());
        x1 = in_parent.getX();
        y1 = in_parent.getY();
        parent.getChildren().remove(rect);
        active = false;

        handler.handleSelectedRegion(new Rectangle2D(Math.min(x0, x1), Math.min(y0, y1),
                                                     Math.abs(x1 - x0), Math.abs(y1 - y0)),
                                     event.isShortcutDown());
        event.consume();
    }
}
