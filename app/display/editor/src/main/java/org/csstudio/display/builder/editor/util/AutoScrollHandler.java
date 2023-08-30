/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2016 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.display.builder.editor.util;


import java.awt.MouseInfo;
import java.awt.Point;
import java.util.concurrent.atomic.AtomicReference;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;


/**
 * Provide the support for auto-scroll in {@link ScrollPane}, when a drag
 * operation
 * is going outside the pane borders.
 * <P>
 * The {@link ScrollPane} is listened for {@link DragEvent#DRAG_EXITED} and
 * {@link MouseEvent#MOUSE_EXITED} events that will start a {@link Timeline}
 * scrolling the pane every 250ms of an amount proportional to the distance
 * of the cursor from the pane borders.
 * <P>
 * {@link DragEvent#DRAG_ENTERED} and {@link MouseEvent#MOUSE_ENTERED} events,
 * and mouse up condition are monitored to stop the {@link Timeline}.
 *
 * @author Claudio Rosati, European Spallation Source ERIC
 * @version 1.0.0 25 Oct 2016
 */
public class AutoScrollHandler {

    private enum Edge {
        LEFT, RIGHT, TOP, BOTTOM
    }

    private AtomicReference<Timeline> autoScrollTimeline = new AtomicReference<>();
    private final ScrollPane scrollPane;
    private volatile boolean enabled = true;

    /**
     * @param scrollPane The {@link ScrollPane} instance for which auto-scroll must be managed.
     */
    public AutoScrollHandler ( ScrollPane scrollPane ) {

        this.scrollPane = scrollPane;

        scrollPane.setOnDragDone(event -> canceTimeline());
        scrollPane.setOnDragEntered(event -> canceTimeline());
        scrollPane.setOnDragExited(event ->
        {
            if (enabled)
                autoScrollTimeline.compareAndSet(null, createAndStartTimeline(getEdge(scrollPane.sceneToLocal(event.getSceneX(), event.getSceneY()))));
        });
        scrollPane.setOnMouseEntered(event -> canceTimeline());
        scrollPane.setOnMouseExited(event -> {
            if ( enabled  &&  event.isPrimaryButtonDown() ) {
                autoScrollTimeline.compareAndSet(null, createAndStartTimeline(getEdge(scrollPane.sceneToLocal(event.getSceneX(), event.getSceneY()))));
            }
        });
        scrollPane.setOnMouseReleased(event -> canceTimeline());

    }

    /** @param enabled Enable auto-scroll? */
    public void enable(final boolean enabled)
    {
        this.enabled = enabled;
        // When disabled, stop any ongoing 'scroll'
        if (! enabled)
            canceTimeline();
    }

    /**
     * Stop and clear the {@link Timeline} object.
     */
    public void canceTimeline() {

        Timeline timeline = autoScrollTimeline.getAndSet(null);

        if ( timeline != null ) {
            timeline.stop();
        }

    }

    /**
     * Creates a new {@link Timeline} and start it. The timeline is started to scroll
     * in the direction specified by the given {@code edge} parameter.
     *
     * @param edge The scrolling side.
     * @return A newly created (and started) {@link Timeline} object, or {@code null}
     *         if {@code edge} is {@code null}.
     */
    private Timeline createAndStartTimeline ( final Edge edge ) {

        if ( edge != null ) {

            Timeline timeline = new Timeline(new KeyFrame(Duration.millis(123), event -> scroll(edge)));

            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.play();

            return timeline;

        } else {
            return null;
        }

    }

    /**
     * Return the edge of the {@link ScrollPane} where the cursor exited.
     *
     * @param point The current cursor location relative to the {@link ScrollPane}.
     * @return A {@link Edge} object, {code null} if the cursor is actually inside the {@link ScrollPane}.
     */
    private Edge getEdge ( Point2D point ) {
        return getEdge(point.getX(), point.getY());
    }

    /**
     * Return the edge of the {@link ScrollPane} where the cursor exited.
     *
     * @param x The current cursor x position relative to the {@link ScrollPane}.
     * @param y The current cursor y position relative to the {@link ScrollPane}.
     * @return A {@link Edge} object, {code null} if the cursor is actually inside the {@link ScrollPane}.
     */
    private Edge getEdge ( final double x, final double y ) {

        Bounds bounds = scrollPane.getBoundsInLocal();

        if ( x <= bounds.getMinX() ) {
            return Edge.LEFT;
        } else if ( x >= bounds.getMaxX() ) {
            return Edge.RIGHT;
        } else if ( y <= bounds.getMinY() ) {
            return Edge.TOP;
        } else if ( y >= bounds.getMaxY() ) {
            return Edge.BOTTOM;
        } else {
            // Inside
            return null;
        }

    }

    /**
     * Scrolls the {@link ScrollPane} along the given {@code edge}.
     *
     * @param edge The scrolling side.
     */
    private void scroll ( Edge edge ) {

        if ( edge == null ) {
            return;
        }

        Point screenLocation = MouseInfo.getPointerInfo().getLocation();
        Point2D localLocation = scrollPane.screenToLocal(screenLocation.getX(), screenLocation.getY());

        switch ( edge ) {
            case LEFT:
                scrollPane.setHvalue(scrollPane.getHvalue() + Math.min(1.0, localLocation.getX() / 2.0) / scrollPane.getWidth());
                break;
            case RIGHT:
                scrollPane.setHvalue(scrollPane.getHvalue() + Math.max(1.0, ( localLocation.getX() - scrollPane.getWidth() ) / 2.0) / scrollPane.getWidth());
                break;
            case TOP:
                scrollPane.setVvalue(scrollPane.getVvalue() + Math.min(1.0, localLocation.getY() / 2.0) / scrollPane.getHeight());
                break;
            case BOTTOM:
                scrollPane.setVvalue(scrollPane.getVvalue() + Math.max(1.0, ( localLocation.getY() - scrollPane.getHeight() ) / 2.0) / scrollPane.getHeight());
                break;
        }

    }

}
