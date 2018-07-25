/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.dialog;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import javafx.stage.Window;

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

    /** Ensure the dialog stays always on top of his {@link Stage}.
     *  @param dialog Dialog to position whose modality must be changed.
     */
    public static void hackModality (final Dialog<?> dialog)
    {
        final Window window = dialog.getDialogPane().getScene().getWindow();
        if (window instanceof Stage)
            ((Stage) window).setAlwaysOnTop(true);
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
        hackModality(dialog);

        final Bounds pos = node.localToScreen(node.getBoundsInLocal());
        dialog.setX(pos.getMinX() + pos.getWidth()/2 + x_offset);
        dialog.setY(pos.getMinY() + pos.getHeight()/2 + y_offset);
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
                                       final Consumer<Preferences> projector)
    {
        Objects.requireNonNull(dialog, "Null dialog.");

        hackModality(dialog);

        if (injector != null  &&  prefs != null)
            injector.accept(prefs);

        if (owner != null)
            dialog.initOwner(owner.getScene().getWindow());

        final double prefX, prefY, prefWidth, prefHeight;
        if (prefs == null)
        {   // Use available defaults
            prefX = Double.NaN;
            prefY = Double.NaN;
            prefWidth = initialWidth;
            prefHeight = initialHeight;
        }
        else
        {   // Read preferences
            prefX = prefs.getDouble("dialog.x", Double.NaN);
            prefY = prefs.getDouble("dialog.y", Double.NaN);
            prefWidth = prefs.getDouble("content.width", initialWidth);
            prefHeight = prefs.getDouble("content.height", initialHeight);

            //  .. and arrange for saving location to prefs on close
            dialog.setOnHidden(event ->
            {
                prefs.putDouble("dialog.x", dialog.getX());
                prefs.putDouble("dialog.y", dialog.getY());
                prefs.putDouble("content.width", dialog.getDialogPane().getWidth());
                prefs.putDouble("content.height", dialog.getDialogPane().getHeight());

                if (projector != null)
                    projector.accept(prefs);

                // TODO Flush prefs in background thread?
                try
                {
                    prefs.flush();
                }
                catch (BackingStoreException ex)
                {
                    logger.log(Level.WARNING, "Unable to flush preferences", ex);
                }
            });
        }

        if (!Double.isNaN(prefX)  &&  !Double.isNaN(prefY))
        {
            dialog.setX(prefX);
            dialog.setY(prefY);
        }
        else if (owner != null)
        {
            // Position relative to owner
            final Bounds pos = owner.localToScreen(owner.getBoundsInLocal());

            dialog.setX(pos.getMinX());
            dialog.setY(pos.getMinY() + pos.getHeight()/3);
        }

        if (!Double.isNaN(prefWidth)  &&  !Double.isNaN(prefHeight))
            dialog.getDialogPane().setPrefSize(prefWidth, prefHeight);
    }
}
