/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.javafx;

import java.text.MessageFormat;

import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;

/** Tracker is a 'rubberband' type rectangle with handles to move or resize.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Tracker extends Group
{
    private static final int HANDLE_SIZE = 10;

    private final Rectangle2D restriction;

    private TrackerListener listener;

    /** Main rectangle of tracker */
    protected final Rectangle tracker = new Rectangle();

    /** Handles at corners and edges of tracker */
    private final Rectangle handle_top_left, handle_top, handle_top_right,
                            handle_right, handle_bottom_right, handle_bottom,
                            handle_bottom_left, handle_left;

    /** Indicators for location and size of tracker */
    private final Label locationLabel, sizeLabel;

    /** Mouse position at start of drag. -1 used to indicate 'not active' */
    private double start_x = -1, start_y = -1;

    /** Tracker position at start of drag */
    private Rectangle2D orig;

    /** Show the location and size? */
    private boolean showLocationAndSize = true;

    /** Enable changes? */
    protected boolean enable_changes = true;

    protected int bigDeltaX = 10;
    protected int bigDeltaY = 10;

    /** Create tracker */
    public Tracker()
    {
        this(null);
    }

    /** Create tracker with restricted position range
     *  @param restriction Bounds within which tracker will stay
     */
    public Tracker(final Rectangle2D restriction)
    {
        this.restriction = restriction;

        tracker.getStyleClass().add("tracker");

        handle_top_left = createHandle();
        handle_top = createHandle();
        handle_top_right = createHandle();
        handle_right = createHandle();
        handle_bottom_right = createHandle();
        handle_bottom = createHandle();
        handle_bottom_left = createHandle();
        handle_left = createHandle();

        locationLabel = createLabel(TextAlignment.LEFT, Pos.TOP_LEFT);
        sizeLabel = createLabel(TextAlignment.RIGHT, Pos.BOTTOM_RIGHT);

        getChildren().addAll(tracker, handle_top_left, handle_top, handle_top_right,
                handle_right, handle_bottom_right,
                handle_bottom, handle_bottom_left, handle_left, locationLabel, sizeLabel);

        hookEvents();
    }

    /** @param enable_changes Allow resizes and moves? Otherwise just show selection */
    public void enableChanges(final boolean enable_changes)
    {
        this.enable_changes = enable_changes;
    }

    /** @param listener Listener to notify of tracker changes */
    public void setListener(final TrackerListener listener)
    {
        this.listener = listener;
    }

    /** @param show Show location and size indicators? */
    public void showLocationAndSize(final boolean show)
    {
        this.showLocationAndSize = show;
        setPosition(tracker.getX(), tracker.getY(), tracker.getWidth(), tracker.getHeight());
    }

    /** @param show Show location and size indicator / initial from prefs */
    public void setShowLocationAndSize(final boolean show)
    {
        this.showLocationAndSize = show;
    }

    /** @return Show location and size indicator */
    public boolean getShowLocationAndSize()
    {
        return this.showLocationAndSize;
    }

    /** @return 'Handle' type rectangle */
    private Rectangle createHandle()
    {
        final Rectangle handle = new Rectangle(HANDLE_SIZE, HANDLE_SIZE);
        handle.getStyleClass().add("tracker_handle");
        handle.setOnMousePressed(this::startDrag);
        handle.setOnMouseReleased(this::endMouseDrag);
        return handle;
    }

    private Label createLabel(final TextAlignment talign, final Pos align)
    {
        // Initial text is used to determine size
        final Label lbl = new Label("\u00A0\u00A000, 00\u00A0\u00A0");

        lbl.getStyleClass().add("location_size");
        lbl.setTextAlignment(talign);
        lbl.setAlignment(align);
        // When clicking/dragging the tracker,
        // don't allow the label to capture mouse clicks.
        lbl.setMouseTransparent(true);

        return lbl;
    }

    void hookEvents()
    {
        tracker.setCursor(Cursor.MOVE);
        tracker.addEventHandler(MouseEvent.MOUSE_PRESSED, this::mousePressed);
        tracker.setOnMouseDragged((MouseEvent event) ->
        {
            // When Control (Mac: Alt) is pressed, this might be start of copy-D&D,
            // so abort moving tracker
            if (!enable_changes ||
                start_x < 0 ||
                (PlatformInfo.is_mac_os_x
                 ? event.isAltDown()
                 : event.isControlDown()))
                return;
            double dx = event.getX() - start_x,  dy = event.getY() - start_y;
            if (event.isShiftDown())
            {   // Restrict to horizontal or vertical move
                if (Math.abs(dx) > Math.abs(dy))
                    dy = 0;
                else
                    dx = 0;
            }
            final Point2D pos = constrain(orig.getMinX() + dx, orig.getMinY() + dy);
            setPosition(pos.getX(), pos.getY(), orig.getWidth(), orig.getHeight());
        });
        tracker.addEventHandler(MouseEvent.MOUSE_RELEASED, this::mouseReleased);

        tracker.setOnKeyPressed(this::handleKeyEvent);

        // Keep the keyboard focus to actually get key events.
        // The RTImagePlot will also listen to mouse moves and try to keep the focus,
        // so the active tracker uses an event filter to have higher priority
        tracker.addEventFilter(MouseEvent.MOUSE_CLICKED, event ->
        {
            event.consume();
            tracker.requestFocus();
        });

        handle_top_left.setCursor(Cursor.NW_RESIZE);
        handle_top_left.setOnMouseDragged((MouseEvent event) ->
        {
            if (start_x < 0)
                return;
            final double dx = event.getX() - start_x,  dy = event.getY() - start_y;
            final Point2D tl = constrain(orig.getMinX() + dx, orig.getMinY() + dy);
            setPosition(tl.getX(), tl.getY(),
                        orig.getWidth() - (tl.getX() - orig.getMinX()),
                        orig.getHeight() - (tl.getY() - orig.getMinY()));
        });
        handle_top.setCursor(Cursor.N_RESIZE);
        handle_top.setOnMouseDragged((MouseEvent event) ->
        {
            if (start_x < 0)
                return;
            final double dy = event.getY() - start_y;
            final Point2D t = constrain(orig.getMinX(), orig.getMinY() + dy);
            setPosition(t.getX(), t.getY(),
                        orig.getWidth() - (t.getX() - orig.getMinX()),
                        orig.getHeight() - (t.getY() - orig.getMinY()));
        });
        handle_top_right.setCursor(Cursor.NE_RESIZE);
        handle_top_right.setOnMouseDragged((MouseEvent event) ->
        {
            if (start_x < 0)
                return;
            final double dx = event.getX() - start_x,  dy = event.getY() - start_y;
            final Point2D tr = constrain(orig.getMinX() + orig.getWidth() + dx, orig.getMinY() + dy);
            setPosition(orig.getMinX(), tr.getY(),
                        tr.getX() - orig.getMinX(), orig.getHeight() - (tr.getY() - orig.getMinY()));
        });
        handle_right.setCursor(Cursor.E_RESIZE);
        handle_right.setOnMouseDragged((MouseEvent event) ->
        {
            if (start_x < 0)
                return;
            final double dx = event.getX() - start_x;
            final Point2D r = constrain(orig.getMinX() + orig.getWidth() + dx, orig.getMinY());
            setPosition(orig.getMinX(), orig.getMinY(), r.getX() - orig.getMinX(), orig.getHeight());
        });
        handle_bottom_right.setCursor(Cursor.SE_RESIZE);
        handle_bottom_right.setOnMouseDragged((MouseEvent event) ->
        {
            if (start_x < 0)
                return;
            final double dx = event.getX() - start_x,  dy = event.getY() - start_y;
            final Point2D br = constrain(orig.getMinX() + orig.getWidth() + dx, orig.getMinY() + orig.getHeight() + dy);
            setPosition(orig.getMinX(), orig.getMinY(), br.getX() - orig.getMinX(), br.getY() - orig.getMinY());
        });
        handle_bottom.setCursor(Cursor.S_RESIZE);
        handle_bottom.setOnMouseDragged((MouseEvent event) ->
        {
            if (start_x < 0)
                return;
            final double dy = event.getY() - start_y;
            final Point2D b = constrain(orig.getMinX(), orig.getMinY() + orig.getHeight() + dy);
            setPosition(orig.getMinX(), orig.getMinY(), orig.getWidth(), b.getY() - orig.getMinY());
        });
        handle_bottom_left.setCursor(Cursor.SW_RESIZE);
        handle_bottom_left.setOnMouseDragged((MouseEvent event) ->
        {
            if (start_x < 0)
                return;
            final double dx = event.getX() - start_x,  dy = event.getY() - start_y;
            final Point2D bl = constrain(orig.getMinX() + dx, orig.getMinY() + orig.getHeight() + dy);
            setPosition(bl.getX(), orig.getMinY(),
                        orig.getWidth() - (bl.getX() - orig.getMinX()),
                        bl.getY() - orig.getMinY());
        });
        handle_left.setCursor(Cursor.W_RESIZE);
        handle_left.setOnMouseDragged((MouseEvent event) ->
        {
            if (start_x < 0)
                return;
            final double dx = event.getX() - start_x;
            final Point2D l = constrain(orig.getMinX() + dx, orig.getMinY());
            setPosition(l.getX(), orig.getMinY(), orig.getWidth() - (l.getX() - orig.getMinX()), orig.getHeight());
        });
    }

    /** Allow derived class to constrain positions
     *  @param x Requested X position
     *  @param y Requested Y position
     *  @return Actual position
     */
    protected Point2D constrain(final double x, final double y)
    {
        return new Point2D(x, y);
    }

    /** @param event {@link MouseEvent} */
    protected void mousePressed(final MouseEvent event)
    {
        startDrag(event);
    }

    /** @param event {@link MouseEvent} */
    protected void startDrag(final MouseEvent event)
    {
        // Take snapshot of current positions
        if ( event == null )
        {
            start_x = -1;
            start_y = -1;
        }
        else
        {
            event.consume();

            start_x = event.getX();
            start_y = event.getY();
        }

        orig = new Rectangle2D(tracker.getX(), tracker.getY(), tracker.getWidth(), tracker.getHeight());
    }

    /** @param event {@link MouseEvent} */
    protected void mouseReleased(final MouseEvent event)
    {
        endMouseDrag(event);
    }

    /** @param event {@link MouseEvent} */
    protected void endMouseDrag(final MouseEvent event)
    {
        if ( start_x < 0 )
            return;

        //  Don't consume the event, otherwise the
        //  AutoScrollHandler will not work properly.

        notifyListenerOfChange();

        start_x = -1;
        start_y = -1;

        // Prepare for another move via keyboard
        orig = new Rectangle2D(tracker.getX(), tracker.getY(), tracker.getWidth(), tracker.getHeight());
    }

    /** Allow move/resize with cursor keys, and abort Drag & Drop operations with ESC key.
     *  <p>
     *  Shift: Resize
     *
     *  @param event {@link KeyEvent}
     */
    protected void handleKeyEvent(final KeyEvent event)
    {
        if (!enable_changes)
            return;

        // Consume handled event to keep the key focus,
        // which is otherwise lost to the 'tab-order' traversal
        final KeyCode code = event.getCode();
        boolean notify = false;

        int delta_x = 1;
        int delta_y = 1;
        if (event.isShortcutDown()) {
            delta_x = this.bigDeltaX;
            delta_y = this.bigDeltaY;
        }

        switch (code)
        {
        case UP:
            if (event.isShiftDown())
                setPosition(tracker.getX(), tracker.getY(), tracker.getWidth(), tracker.getHeight() - delta_y);
            else
                setPosition(tracker.getX(), tracker.getY() - delta_y, tracker.getWidth(), tracker.getHeight());
            notify = true;
            break;
        case DOWN:
            if (event.isShiftDown())
                setPosition(tracker.getX(), tracker.getY(), tracker.getWidth(), tracker.getHeight() + delta_y);
            else
                setPosition(tracker.getX(), tracker.getY() + delta_y, tracker.getWidth(), tracker.getHeight());
            notify = true;
            break;
        case LEFT:
            if (event.isShiftDown())
                setPosition(tracker.getX(), tracker.getY(), tracker.getWidth() - delta_x, tracker.getHeight());
            else
                setPosition(tracker.getX() - delta_x, tracker.getY(), tracker.getWidth(), tracker.getHeight());
            notify = true;
            break;
        case RIGHT:
            if (event.isShiftDown())
                setPosition(tracker.getX(), tracker.getY(), tracker.getWidth() + delta_x, tracker.getHeight());
            else
                setPosition(tracker.getX() + delta_x, tracker.getY(), tracker.getWidth(), tracker.getHeight());
            notify = true;
            break;
        case ESCAPE:
            if (start_x >= 0)
            {
                setPosition(orig);
                endMouseDrag(null);
                notify = true;
            }
            break;
        default:
            return;
        }

        event.consume();

        if (notify)
            notifyListenerOfChange();

        if (code != KeyCode.ESCAPE)
        {
            // Reset tracker as if we started at this position.
            // That way, a sequence of cursor key moves turns into individual
            // undo-able actions.
            orig = new Rectangle2D(tracker.getX(), tracker.getY(), tracker.getWidth(), tracker.getHeight());
        }
    }

    /** Update location and size of tracker
     *  @param position
     */
    public final void setPosition(final Rectangle2D position)
    {
        setPosition(position.getMinX(), position.getMinY(), position.getWidth(), position.getHeight());
        orig = position;
    }

    /** Update location and size of tracker
     *  @param x
     *  @param y
     *  @param width
     *  @param height
     */
    public void setPosition(double x, double y, double width, double height)
    {
        // Enforce valid position
        if (x < 0)
            x = 0;
        if (y < 0)
            y = 0;
        if (width < 0)
            width = 0;
        if (height < 0)
            height = 0;
        if (restriction != null)
        {
            if (x < restriction.getMinX())
                x = restriction.getMinX();
            if (y < restriction.getMinY())
                y = restriction.getMinY();
            if (x + width > restriction.getMaxX())
                width = restriction.getMaxX() - x;
            if (y + height > restriction.getMaxY())
                height = restriction.getMaxY() - y;
        }

        // relocate() will _not_ update Rectangle.x, y!
        tracker.setX(x);
        tracker.setY(y);
        tracker.setWidth(width);
        tracker.setHeight(height);

        handle_top_left.setVisible(enable_changes);
        handle_top_left.setX(x - HANDLE_SIZE);
        handle_top_left.setY(y - HANDLE_SIZE);

        handle_top.setVisible(enable_changes  &&  width > HANDLE_SIZE);
        handle_top.setX(x + (width - HANDLE_SIZE) / 2);
        handle_top.setY(y - HANDLE_SIZE);

        handle_top_right.setVisible(enable_changes);
        handle_top_right.setX(x + width);
        handle_top_right.setY(y - HANDLE_SIZE);

        handle_right.setVisible(enable_changes  &&  height > HANDLE_SIZE);
        handle_right.setX(x + width);
        handle_right.setY(y + (height - HANDLE_SIZE)/2);

        handle_bottom_right.setVisible(enable_changes);
        handle_bottom_right.setX(x + width);
        handle_bottom_right.setY(y + height);

        handle_bottom.setVisible(enable_changes  &&  width > HANDLE_SIZE);
        handle_bottom.setX(x + (width - HANDLE_SIZE)/2);
        handle_bottom.setY(y + height);

        handle_bottom_left.setVisible(enable_changes);
        handle_bottom_left.setX(x - HANDLE_SIZE);
        handle_bottom_left.setY(y + height);

        handle_left.setVisible(enable_changes  &&  height > HANDLE_SIZE);
        handle_left.setX(x - HANDLE_SIZE);
        handle_left.setY(y + (height - HANDLE_SIZE)/2);

        locationLabel.setText(MessageFormat.format("\u00A0\u00A0{0,number,###0}, {1,number,###0}\u00A0\u00A0", x, y));
        locationLabel.setVisible(showLocationAndSize && ( ((width >= 80) || (height >= 30)) && ( width >= 40 && height >= 20 )));
        locationLabel.relocate(x + 3, y + 3);

        sizeLabel.setText(MessageFormat.format("\u00A0\u00A0{0,number,###0}, {1,number,###0}\u00A0\u00A0", width, height));
        sizeLabel.setVisible(showLocationAndSize && ( width >= 40 && height >= 20 ));
        // Slight issue:
        // The text was just set, layout may not have happened, so getWidth() is wrong until the next update
        sizeLabel.relocate(x + width - sizeLabel.getWidth() - 3, y + height - sizeLabel.getHeight() - 3);
    }

    private void notifyListenerOfChange()
    {
        final Rectangle2D current = new Rectangle2D(tracker.getX(), tracker.getY(), tracker.getWidth(), tracker.getHeight());
        if (! current.equals(orig))
            listener.trackerChanged(orig, current);
    }
}
