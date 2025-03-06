/*******************************************************************************
 * Copyright (c) 2015-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.dialog;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Dialog;
import javafx.stage.Screen;
import javafx.stage.Stage;

/** Helper for dialogs
 *  @author Kay Kasemir
 *  @author Claudio Rosati
 */
@SuppressWarnings("nls")
public class DialogHelper
{
    // Prevent instantiation
    private DialogHelper()
    {
    }

    /** @param dialog Dialog that should be on top of other windows */
    public static void forceToFront(final Dialog<?> dialog)
    {
        final Stage stage  = (Stage)  dialog.getDialogPane().getScene().getWindow();
        stage.toFront();
    }


    /** Position dialog relative to another widget
     *
     *  <p>By default, dialogs seem to pop up in the center of the first monitor.
     *  .. even if the "current" window is on a different monitor.
     *
     *  <p>This helper positions the dialog relative to the center
     *  of a node.
     *
     *  @param dialog Dialog to position
     *  @param node Node relative to which dialog should be positioned
     *  @param x_offset Offset relative to center of the node
     *  @param y_offset Offset relative to center of the node
     */
    public static void positionDialog(final Dialog<?> dialog, final Node node, final int x_offset, final int y_offset)
    {
        dialog.initOwner(node.getScene().getWindow());
        // Must runLater due to dialog Width/Height are initialized after dialog shows up
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                final Bounds pos = node.localToScreen(node.getBoundsInLocal());
                if (pos == null)
                {
                    logger.log(Level.WARNING, "Cannot determine dialog position", new NullPointerException());
                    return;
                }
                final double nodeX = pos.getMinX();
                final double nodeY = pos.getMinY();

                double newX = nodeX + pos.getWidth()/2 + x_offset;
                double newY = nodeY + pos.getHeight()/2 + y_offset;

                final double dw = dialog.getWidth();
                final double dh = dialog.getHeight();

                List<Screen> scr = Screen.getScreensForRectangle(nodeX, nodeY, pos.getWidth(), pos.getHeight());
                if (scr == null || scr.size() == 0)
                {
                    logger.log(Level.WARNING, "Cannot determine dialog position (node out of screen)");
                    return;
                }
                // Take the first available screen
                Rectangle2D sb = scr.get(0).getVisualBounds();

                newX = newX < sb.getMinX() ? sb.getMinX() : newX;
                newX = newX + dw > sb.getMaxX() ? sb.getMaxX() - dw : newX;

                newY = newY < sb.getMinY() ? sb.getMinY() : newY;
                newY = newY + dh > sb.getMaxY() ? sb.getMaxY() - dh : newY;

                dialog.setX(newX);
                dialog.setY(newY);

                // Force to front
                forceToFront(dialog);
            }
        });
    }

    /**
     * Clamp a rectangle to the closest rectangle in a list of screens. This is used to prevent a dialog from going
     * off screen completely, and losing any control over the entire application.
     *
     *  @param rect The rectangle to be clamped.
     *  @param screens A list of screen regions to clamp to.
     * */
    static private Rectangle2D clampToClosest(final Rectangle2D rect, final List<Rectangle2D> screens) {
        Point2D center = new Point2D(
                rect.getMinX() + rect.getWidth() / 2,
                rect.getMinY() + rect.getHeight() / 2
        );

        Optional<Rectangle2D> closestOpt = screens.stream().min(
                Comparator.comparingDouble(screen -> {
                    // if the dialog center is inside of a screen, it will always be the closest
                    if (screen.contains(center))
                        return -1;

                    // get distance to closest edge
                    double dx = Math.max(0, Math.max(screen.getMinX() - center.getX(), center.getX() - screen.getMaxX()));
                    double dy = Math.max(0, Math.max(screen.getMinY() - center.getY(), center.getY() - screen.getMaxY()));

                    return dx * dx + dy * dy;
                })
        );

        if (closestOpt.isEmpty()) {
            // no available screens (unlikely)
            return rect;
        }

        // clamp position to screen, note that this will move the rectangle into the screen in its entirety,
        // with a preference for the top left corner
        Rectangle2D closest = closestOpt.get();
        double newMinX = Math.max(closest.getMinX(), Math.min(rect.getMinX(), closest.getMaxX() - rect.getWidth()));
        double newMinY = Math.max(closest.getMinY(), Math.min(rect.getMinY(), closest.getMaxY() - rect.getHeight()));
        return new Rectangle2D(
                newMinX, newMinY, rect.getWidth(), rect.getHeight()
        );
    }

    /** Position the given {@code dialog} initially relative to {@code owner},
     *  then it saves/restore the dialog's position and size into/from the
     *  provided {@link Preferences}.
     *
     *  <p>{@code "dialog.x"} and {@code "dialog.y"} will be the preferences names
     *  used to save and restore the dialog's location. {@code "content.width"}
     *  and {@code "content.height"} the ones used for saving the size of the
     *  dialog's pane ({@link Dialog#getDialogPane()}).
     *
     *  @param dialog The dialog to be positioned and sized.
     *  @param owner The node starting this dialog.
     *  @param prefs The {@link Preferences} used to save/restore position and size.
     */
    public static void positionAndSize(final Dialog<?> dialog, final Node owner, final Preferences prefs)
    {
        positionAndSize(dialog, owner, prefs, Double.NaN, Double.NaN, null, null);
    }

    /** Position the given {@code dialog} initially relative to {@code owner},
     *  then it saves/restore the dialog's position and size into/from the
     *  provided {@link Preferences}.
     *
     *  <p>{@code "dialog.x"} and {@code "dialog.y"} will be the preferences names
     *  used to save and restore the dialog's location. {@code "content.width"}
     *  and {@code "content.height"} the ones used for saving the size of the
     *  dialog's pane ({@link Dialog#getDialogPane()}).
     *
     *  @param dialog The dialog to be positioned and sized.
     *  @param owner The node starting this dialog.
     *  @param prefs The {@link Preferences} used to save/restore position and size.
     *  @param injector Called when preferences are read to allow client code to read additional detail.
     *                  May be <code>null</code>.
     *  @param projector Called when the dialog is about to be hidden to allow saving additional detail.
     *                   May be <code>null</code>.
     */
    public static void positionAndSize(final Dialog<?> dialog, final Node owner, final Preferences prefs,
                                       final Consumer<Preferences> injector,
                                       final Consumer<Preferences> projector)
    {
        positionAndSize(dialog, owner, prefs, Double.NaN, Double.NaN, injector, projector);
    }

    /** Position the given {@code dialog} initially relative to {@code owner},
     *  then it saves/restore the dialog's position and size into/from the
     *  provided {@link Preferences}.
     *
     *  <p>{@code "dialog.x"} and {@code "dialog.y"} will be the preferences names
     * used to save and restore the dialog's location. {@code "content.width"}
     * and {@code "content.height"} the ones used for saving the size of the
     * dialog's pane ({@link Dialog#getDialogPane()}).
     *
     *
     * @param dialog The dialog to be positioned and sized.
     * @param owner The node starting this dialog.
     * @param prefs The {@link Preferences} used to save/restore position and size.
     * @param initialWidth The (very) initial width. {@link Double#NaN} must be
     *                     used if the default, automatically computed width and height
     *                     should be used instead.
     * @param initialHeight The (very) initial height. {@link Double#NaN} must
     *                      be used if the default, automatically computed width and
     *                      height should be used instead.
     */
    public static void positionAndSize(final Dialog<?> dialog, final Node owner, final Preferences prefs,
                                       final double initialWidth, final double initialHeight)
    {
        positionAndSize(dialog, owner, prefs, initialWidth, initialHeight, null, null);
    }

    /** Position the given {@code dialog}. Saves/restores the dialog's
     *  position and size into/from the provided {@link Preferences}.
     *
     *  <p> {@code "content.width"} and {@code "content.height"} are the
     *  preference names used for saving the size of the dialog's pane
     *  ({@link Dialog#getDialogPane()}).
     *
     *  @param dialog The dialog to be positioned and sized.
     *  @param owner The node starting this dialog.
     *  @param prefs The {@link Preferences} used to save/restore size.
     *  @param initialWidth The (very) initial width. {@link Double#NaN} must be
     *                      used if the default, automatically computed width and height
     *                      should be used instead.
     * @param initialHeight The (very) initial height. {@link Double#NaN} must
     *                      be used if the default, automatically computed width and
     *                      height should be used instead.
     *  @param injector Called when preferences are read to allow client code to read additional detail.
     *                  May be <code>null</code>.
     *  @param projector Called when the dialog is about to be hidden to allow saving additional detail.
     *                   May be <code>null</code>.
     */
    public static void positionAndSize(final Dialog<?> dialog, final Node owner, final Preferences prefs,
                                       final double initialWidth, final double initialHeight,
                                       final Consumer<Preferences> injector,
                                       final Consumer<Preferences> projector) {
        Objects.requireNonNull(dialog, "Null dialog.");

        if (injector != null && prefs != null)
            injector.accept(prefs);

        if (owner != null)
            dialog.initOwner(owner.getScene().getWindow());

        final double prefWidth, prefHeight;
        if (prefs == null) {   // Use available defaults
            prefWidth = initialWidth;
            prefHeight = initialHeight;
        } else {   // Read preferences
            prefWidth = prefs.getDouble("content.width", initialWidth);
            prefHeight = prefs.getDouble("content.height", initialHeight);

            //  .. and arrange for saving location to prefs on close
            dialog.setOnHidden(event ->
            {
                prefs.putDouble("content.width", dialog.getDialogPane().getWidth());
                prefs.putDouble("content.height", dialog.getDialogPane().getHeight());

                if (projector != null)
                    projector.accept(prefs);

                // TODO Flush prefs in background thread?
                try {
                    prefs.flush();
                } catch (BackingStoreException ex) {
                    logger.log(Level.WARNING, "Unable to flush preferences", ex);
                }
            });
        }

        if (owner != null) {
            // Position relative to owner
            final Bounds pos = owner.localToScreen(owner.getBoundsInLocal());
            final Rectangle2D prefPos = new Rectangle2D(
                    pos.getMinX() - prefWidth,
                    pos.getMinY() - prefHeight/3,
                    prefWidth,
                    prefHeight
            );
            List<Screen> screens = Screen.getScreens();
            Rectangle2D clampedPos = clampToClosest(
                    prefPos, screens.stream().map(Screen::getVisualBounds).collect(Collectors.toList())
            );

            dialog.setX(clampedPos.getMinX());
            dialog.setY(clampedPos.getMinY());
        }

        if (!Double.isNaN(prefWidth) && !Double.isNaN(prefHeight))
            dialog.getDialogPane().setPrefSize(prefWidth, prefHeight);
    }
}
