/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import static org.csstudio.display.builder.representation.EmbeddedDisplayRepresentationUtil.checkCompletion;
import static org.csstudio.display.builder.representation.EmbeddedDisplayRepresentationUtil.loadDisplayModel;
import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import javafx.application.Platform;
import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.widgets.EmbeddedDisplayWidget;
import org.csstudio.display.builder.model.widgets.EmbeddedDisplayWidget.Resize;
import org.csstudio.display.builder.representation.EmbeddedDisplayRepresentationUtil.DisplayAndGroup;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import org.phoebus.ui.javafx.NonCachingScrollPane;

/** Creates JavaFX item for model widget
 *
 *  <p>Different from widget representations in general,
 *  this one implements the loading of the embedded model,
 *  an operation that could be considered a runtime aspect.
 *  This was done to allow viewing the embedded content
 *  in the editor.
 *  The embedded model will be started by the EmbeddedDisplayRuntime.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EmbeddedDisplayRepresentation extends RegionBaseRepresentation<Pane, EmbeddedDisplayWidget>
{
    private static final Background TRANSPARENT_BACKGROUND = new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));
    private static final Background EDIT_TRANSPARENT_BACKGROUND = new Background(new BackgroundFill(
            new LinearGradient(
                    0, 0, 10, 10, false, CycleMethod.REPEAT,
                    new Stop(0.0, new Color(0.53, 0.52, 0.51, 0.15)),
                    new Stop(0.5, new Color(0.53, 0.52, 0.51, 0.15)),
                    new Stop(0.5, Color.TRANSPARENT),
                    new Stop(1.0, Color.TRANSPARENT)
            ), CornerRadii.EMPTY, Insets.EMPTY
    ));

    private static final Background EDIT_OVERDRAWN_BACKGROUND = new Background(new BackgroundFill(
            new LinearGradient(
                    10, 0, 0, 10, false, CycleMethod.REPEAT,
                    new Stop(0.0, Color.TRANSPARENT),
                    new Stop(0.5, Color.TRANSPARENT),
                    new Stop(0.5, new Color(0.93, 0.1, 0.1, 0.45)),
                    new Stop(1.0, new Color(0.93, 0.1, 0.1, 0.45))
            ), CornerRadii.EMPTY, Insets.EMPTY
    ));

    private final DirtyFlag dirty_sizes = new DirtyFlag();
    private final DirtyFlag dirty_background = new DirtyFlag();
    private final DirtyFlag get_size_again = new DirtyFlag();
    private final UntypedWidgetPropertyListener backgroundChangedListener = this::backgroundChanged;
    private final UntypedWidgetPropertyListener fileChangedListener = this::fileChanged;
    private final UntypedWidgetPropertyListener sizesChangedListener = this::sizesChanged;

    private volatile double zoom_factor_x = 1.0;
    private volatile double zoom_factor_y = 1.0;


    /** Inner pane that holds child widgets
     *
     *  <p>Set to null when representation is disposed,
     *  which is used as indicator to pending display updates.
     */
    private volatile Pane inner;
    private volatile Background inner_background = Background.EMPTY;

    /** Zoom for 'inner' pane */
    private Scale zoom;

    /** Optional scroll pane between 'jfx_node' Pane and 'inner'.
     *
     *  To allow scrolling, the scene graph is
     *      jfx_node -> scroll -> inner.
     *
     *  If no scrolling is desired, the scrollbars can be hidden via
     *  scroll.setHbarPolicy(ScrollBarPolicy.NEVER),
     *  but such a ScrollPane would still react to for example mouse wheel events
     *  and scroll its content. When filtering the event to work around this,
     *  one could no longer scroll the overall display while the mouse it in the embedded section.
     *
     *  The easiest way to remove the scroll bars and any of its impact on
     *  event handling is to simply remove the scrollpane from the scene graph:
     *      jfx_node-> inner.
     */
    private ScrollPane scroll;

    /** The display file (and optional group inside that display) to load */
    private final AtomicReference<DisplayAndGroup> pending_display_and_group = new AtomicReference<>();

    /** Track active model in a thread-safe way
     *  to assert that each one is represented and removed
     */
    private final AtomicReference<DisplayModel> active_content_model = new AtomicReference<>();

    /** Flag to avoid recursion when this code changes the widget size */
    private volatile boolean resizing = false;

    @Override
    protected boolean isFilteringEditModeClicks()
    {
        return true;
    }

    @Override
    public Pane createJFXNode() throws Exception
    {
        // inner.setScaleX() and setScaleY() zoom from the center
        // and not the top-left edge, requiring adjustments to
        // inner.setTranslateX() and ..Y() to compensate.
        // Using a separate Scale transformation does not have that problem.
        // See http://stackoverflow.com/questions/10707880/javafx-scale-and-translate-operation-results-in-anomaly
        inner = new Pane();
        inner.getTransforms().add(zoom = new Scale());

        scroll = new NonCachingScrollPane(inner);
        scroll.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        //  By default it seems that the minimum size is set to 36x36.
        //  This will make the border (if visible) not smaller that this minimum size
        //  even if the widget is actually smaller.
        scroll.setMinSize(1, 1);
        //  Removing 1px border around the ScrollPane's content. See https://stackoverflow.com/a/29376445
        scroll.getStyleClass().addAll("embedded_display", "edge-to-edge");
        // Panning tends to 'jerk' the content when clicked
        // scroll.setPannable(true);

        get_size_again.checkAndClear();

        return new Pane(scroll);
    }

    @Override
    protected Parent getChildParent(final Parent parent)
    {
        return inner;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addUntypedPropertyListener(sizesChangedListener);
        model_widget.propHeight().addUntypedPropertyListener(sizesChangedListener);
        model_widget.propResize().addUntypedPropertyListener(sizesChangedListener);

        model_widget.propFile().addUntypedPropertyListener(fileChangedListener);
        model_widget.propGroupName().addUntypedPropertyListener(fileChangedListener);
        model_widget.propMacros().addUntypedPropertyListener(fileChangedListener);

        model_widget.propTransparent().addUntypedPropertyListener(backgroundChangedListener);

        fileChanged(null, null, null);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(sizesChangedListener);
        model_widget.propHeight().removePropertyListener(sizesChangedListener);
        model_widget.propResize().removePropertyListener(sizesChangedListener);
        model_widget.propFile().removePropertyListener(fileChangedListener);
        model_widget.propGroupName().removePropertyListener(fileChangedListener);
        model_widget.propMacros().removePropertyListener(fileChangedListener);
        model_widget.propTransparent().removePropertyListener(backgroundChangedListener);
        super.unregisterListeners();
    }

    private void sizesChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        if (resizing)
            return;

        final int widget_width = model_widget.propWidth().getValue();
        final int widget_height = model_widget.propHeight().getValue();

        final Resize resize = model_widget.propResize().getValue();
        final DisplayModel content_model = active_content_model.get();
        if (content_model != null)
        {
            final int content_width = content_model.propWidth().getValue();
            final int content_height = content_model.propHeight().getValue();
            zoom_factor_x = zoom_factor_y = 1.0;
            if (resize == Resize.ResizeContent)
            {
                final double zoom_x = content_width  > 0 ? (double) widget_width  / content_width : 1.0;
                final double zoom_y = content_height > 0 ? (double) widget_height / content_height : 1.0;
                zoom_factor_x = zoom_factor_y = Math.min(zoom_x, zoom_y);
            }
            else if (resize == Resize.SizeToContent)
            {
                resizing = true;
                if (content_width > 0)
                    model_widget.propWidth().setValue(content_width);
                if (content_height > 0)
                    model_widget.propHeight().setValue(content_height);
                resizing = false;
            }
            else if (resize == Resize.StretchContent)
            {
                zoom_factor_x = content_width  > 0 ? (double) widget_width  / content_width : 1.0;
                zoom_factor_y = content_height > 0 ? (double) widget_height / content_height : 1.0;
            }
        }

        dirty_sizes.mark();
        get_size_again.mark();
        toolkit.scheduleUpdate(this);
    }

    private void fileChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        final DisplayAndGroup file_and_group =
                new DisplayAndGroup(model_widget.propFile().getValue(), model_widget.propGroupName().getValue());

        // System.out.println("Requested: " + file_and_group);
        final DisplayAndGroup skipped = pending_display_and_group.getAndSet(file_and_group);
        if (skipped != null)
            logger.log(Level.FINE, "Skipped: {0}", skipped);

        // Load embedded display in background thread
        toolkit.onRepresentationStarted();
        JobManager.schedule("Embedded Display", this::updatePendingDisplay);
    }

    /** Update to the next pending display
     *
     *  <p>Synchronized to serialize the background threads.
     *
     *  <p>Example: Displays A, B, C are requested in quick succession.
     *
     *  <p>pending_display_and_group=A is submitted to executor thread A.
     *
     *  <p>While handling A, pending_display_and_group=B is submitted to executor thread B.
     *  Thread B will be blocked in synchronized method.
     *
     *  <p>Then pending_display_and_group=C is submitted to executor thread C.
     *  As thread A finishes, thread B finds pending_display_and_group==C.
     *  As thread C finally continues, it finds pending_display_and_group empty.
     *  --> Showing A, then C, skipping B.
     */
    private synchronized void updatePendingDisplay(final JobMonitor monitor)
    {
        try
        {
            final DisplayAndGroup handle = pending_display_and_group.getAndSet(null);
            if (handle == null)
            {
                // System.out.println("Nothing to handle");
                return;
            }
            if (inner == null)
            {
                // System.out.println("Aborted: " + handle);
                return;
            }

            monitor.beginTask("Load " + handle);
            try
            {   // Load new model (potentially slow)
                final DisplayModel new_model = loadDisplayModel(model_widget, handle);

                // Stop (old) runtime
                // EmbeddedWidgetRuntime tracks this property to start/stop the embedded model's runtime
                model_widget.runtimePropEmbeddedModel().setValue(null);

                // Atomically update the 'active' model
                final DisplayModel old_model = active_content_model.getAndSet(new_model);
                new_model.propBackgroundColor().addUntypedPropertyListener(backgroundChangedListener);

                if (old_model != null)
                {   // Dispose old model
                    final Future<Object> completion = toolkit.submit(() ->
                    {
                        toolkit.disposeRepresentation(old_model);
                        return null;
                    });
                    checkCompletion(model_widget, completion, "timeout disposing old representation");
                }
                // Represent new model on UI thread
                toolkit.onRepresentationStarted();
                final Future<Object> completion = toolkit.submit(() ->
                {
                    representContent(new_model);
                    return null;
                });
                checkCompletion(model_widget, completion, "timeout representing new content");

                // Allow EmbeddedWidgetRuntime to start the new runtime
                model_widget.runtimePropEmbeddedModel().setValue(new_model);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Failed to handle embedded display " + handle, ex);
            }
        }
        finally
        {
            toolkit.onRepresentationFinished();
        }
    }

    /** @param content_model Model to represent */
    private void representContent(final DisplayModel content_model)
    {
        try
        {
            sizesChanged(null, null, null);

            // Set the zoom factor here so that content_model is represented with the correct size
            zoom.setX(zoom_factor_x);
            zoom.setY(zoom_factor_y);

            toolkit.representModel(inner, content_model);
            backgroundChanged(null, null, null);
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, "Failed to represent embedded display", ex);
        }
        finally
        {
            toolkit.onRepresentationFinished();
        }
    }

    private void backgroundChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        final DisplayModel content_model = active_content_model.get();
        // TODO Haven't found perfect way to set the 'background' color
        // of the embedded content.
        // Setting the 'inner' background will sometimes leave a gray section in the right and or bottom edge
        // of the embedded content if the container is (much) larger than the content
        // The scroll pane background can only be set via style,
        // and then shines through on the outside of the scrollbars
        // scroll.setStyle("-fx-control-inner-background: " + JFXUtil.webRGB(content_model.propBackgroundColor().getValue()) +
        //                  "; -fx-background: " + JFXUtil.webRGB(content_model.propBackgroundColor().getValue()));
        if (model_widget.propTransparent().getValue())
        {
            if (toolkit.isEditMode())
                inner_background = EDIT_TRANSPARENT_BACKGROUND;
            else
                inner_background = TRANSPARENT_BACKGROUND;
        }
        else
        {
            if (content_model == null)
                inner_background = Background.EMPTY;
            else
                inner_background = new Background(new BackgroundFill(JFXUtil.convert(content_model.propBackgroundColor().getValue()), CornerRadii.EMPTY, Insets.EMPTY));
        }

        dirty_background.mark();
        toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        // Late update after disposal?
        if (inner == null)
            return;
        super.updateChanges();
        if (dirty_sizes.checkAndClear())
        {
            final Integer width = model_widget.propWidth().getValue();
            final Integer height = model_widget.propHeight().getValue();

            // zoom also applies to width and height so we have to de-scale them
            final Double scaled_width = width / zoom_factor_x;
            final Double scaled_height = height / zoom_factor_y;

            // update minimum size with zoom factor applied 'in reverse'
            inner.setMinSize(scaled_width, scaled_height);

            // set minimum and maximum size of jfx_node
            // to match the requested size
            jfx_node.setMinSize(width, height);
            jfx_node.setMaxSize(width, height);

            final Resize resize = model_widget.propResize().getValue();

            zoom.setX(zoom_factor_x);
            zoom.setY(zoom_factor_y);

            if (resize == Resize.None)
            {
                // Need a scroll pane (which disables itself as needed)
                jfx_node.getChildren().setAll(scroll);
                scroll.setPrefSize(width, height);
                inner.setClip(null);
                scroll.setContent(inner);
            }
            else
            {   // Don't use a scroll pane
                scroll.setContent(null);
                jfx_node.getChildren().setAll(inner);

                // During runtime or if the resize property is set to Crop we clip inner
                // but allow 'overdrawing' in edit mode so the out-of-region widgets are visible to the user
                if (resize == Resize.Crop || !toolkit.isEditMode())
                    inner.setClip(new Rectangle(scaled_width, scaled_height));
                else
                {
                    // Check for overdrawing
                    if (get_size_again.checkAndClear())
                    {
                        // Give the UI thread a chance to render the contents and update the width/height
                        dirty_sizes.mark();
                        toolkit.scheduleUpdate(this);
                    }
                    else if (inner.getHeight() != 0.0 && inner.getWidth() != 0.0)
                    {
                        // Check if higher than allowed
                        if ((int)inner.getHeight() > scaled_height.intValue())
                        {
                            Pane rect = new Pane();
                            rect.setManaged(false);
                            rect.resizeRelocate(0, scaled_height, inner.getWidth(), inner.getHeight() - scaled_height);
                            rect.setBackground(EDIT_OVERDRAWN_BACKGROUND);
                            inner.getChildren().addAll(rect);
                        }

                        // Check if wider than allowed
                        if ((int)inner.getWidth() > scaled_width.intValue())
                        {
                            Pane rect = new Pane();
                            rect.setManaged(false);
                            rect.resizeRelocate(scaled_width, 0, inner.getWidth() - scaled_width, inner.getHeight());
                            rect.setBackground(EDIT_OVERDRAWN_BACKGROUND);
                            inner.getChildren().addAll(rect);
                        }
                    }
                }

            }
        }
        if (dirty_background.checkAndClear())
            inner.setBackground(inner_background);
    }

    @Override
    public void dispose()
    {
        Platform.runLater(() -> {
            // When the file name is changed, updatePendingDisplay()
            // will atomically update the active_content_model,
            // represent the new model, and then set runtimePropEmbeddedModel.
            //
            // Fetching the embedded model from active_content_model
            // could dispose a representation that hasn't been represented, yet.
            // Fetching the embedded model from runtimePropEmbeddedModel
            // could fail to dispose what's just now being represented.
            //
            // --> Very unlikely to happen because runtime has been stopped,
            //     so nothing is changing the file name right now.
            final DisplayModel em = active_content_model.getAndSet(null);
            model_widget.runtimePropEmbeddedModel().setValue(null);

            if (inner != null  &&  em != null)
                toolkit.disposeRepresentation(em);
            inner = null;

            super.dispose();
        });
    }
}
