/*******************************************************************************
 * Copyright (c) 2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import static org.csstudio.display.builder.representation.EmbeddedDisplayRepresentationUtil.checkCompletion;
import static org.csstudio.display.builder.representation.EmbeddedDisplayRepresentationUtil.loadDisplayModel;
import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.io.ByteArrayOutputStream;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.model.widgets.GroupWidget.Style;
import org.csstudio.display.builder.model.widgets.LabelWidget;
import org.csstudio.display.builder.model.widgets.TemplateInstanceWidget;
import org.csstudio.display.builder.model.widgets.TemplateInstanceWidget.InstanceProperty;
import org.csstudio.display.builder.representation.EmbeddedDisplayRepresentationUtil.DisplayAndGroup;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.persistence.XMLUtil;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

/** Creates JavaFX item for model widget
 *
 *  <p>Different from widget representations in general,
 *  this one implements the loading of the embedded model,
 *  an operation that could be considered a runtime aspect.
 *  This was done to allow viewing the embedded content
 *  in the editor.
 *  The embedded model will be started by the TemplateInstanceRuntime.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TemplateInstanceRepresentation extends RegionBaseRepresentation<Pane, TemplateInstanceWidget>
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

    private final DirtyFlag dirty_sizes = new DirtyFlag();
    private final DirtyFlag dirty_background = new DirtyFlag();
    private final DirtyFlag get_size_again = new DirtyFlag();
    private final UntypedWidgetPropertyListener backgroundChangedListener = this::backgroundChanged;
    private final UntypedWidgetPropertyListener fileChangedListener = this::fileChanged;
    private final UntypedWidgetPropertyListener sizesChangedListener = this::sizesChanged;

    /** Inner pane that holds child widgets
     *
     *  <p>Set to null when representation is disposed,
     *  which is used as indicator to pending display updates.
     */
    private volatile Pane inner;
    private volatile Background inner_background = Background.EMPTY;

    /** The display file (and optional group inside that display) to load */
    private final AtomicReference<DisplayAndGroup> pending_template = new AtomicReference<>();

    /** Track active template's XML */
    private final AtomicReference<DisplayModel> active_template_model = new AtomicReference<>();

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
        inner = new Pane();

        get_size_again.checkAndClear();

        return new Pane(inner); // TODO Just use 'inner' or this pane, not both?
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
        model_widget.propInstances().addUntypedPropertyListener(sizesChangedListener);
        model_widget.propGap().addUntypedPropertyListener(sizesChangedListener);

        model_widget.propFile().addUntypedPropertyListener(fileChangedListener);

        fileChanged(null, null, null);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(sizesChangedListener);
        model_widget.propHeight().removePropertyListener(sizesChangedListener);
        model_widget.propInstances().removePropertyListener(sizesChangedListener);
        model_widget.propGap().removePropertyListener(sizesChangedListener);

        model_widget.propFile().removePropertyListener(fileChangedListener);

        super.unregisterListeners();
    }

    private void sizesChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        if (resizing)
            return;

        final DisplayModel template_model = active_template_model.get();
        if (template_model != null)
        {
            // Size to content
            final int content_width = template_model.propWidth().getValue();
            final int content_height = template_model.propHeight().getValue();
            final int count = model_widget.propInstances().size();
            final int gap = model_widget.propGap().getValue();
            final boolean horiz = model_widget.propHorizontal().getValue();

            resizing = true;

            if (content_width > 0)
                model_widget.propWidth().setValue(horiz
                                                  ? content_width * count + gap * (count-1)
                                                  : content_width);
            if (content_height > 0)
                model_widget.propHeight().setValue(horiz
                                                   ? content_height
                                                   : content_height * count + gap * (count-1));
            resizing = false;
        }

        dirty_sizes.mark();
        get_size_again.mark();
        toolkit.scheduleUpdate(this);
    }

    private void fileChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        final DisplayAndGroup file_and_group =
            new DisplayAndGroup(model_widget.propFile().getValue(), "");

        // System.out.println("Requested: " + file_and_group);
        final DisplayAndGroup skipped = pending_template.getAndSet(file_and_group);
        if (skipped != null)
            logger.log(Level.FINE, "Skipped: {0}", skipped);

        // Load embedded display in background thread
        toolkit.onRepresentationStarted();
        JobManager.schedule("Load Template", this::loadTemplate);
    }

    /** Update to the next pending display
     *
     *  <p>Synchronized to serialize the background threads.
     *
     *  <p>Example: Displays A, B, C are requested in quick succession.
     *
     *  <p>pending_template=A is submitted to executor thread A.
     *
     *  <p>While handling A, pending_template=B is submitted to executor thread B.
     *  Thread B will be blocked in synchronized method.
     *
     *  <p>Then pending_template=C is submitted to executor thread C.
     *  As thread A finishes, thread B finds pending_template==C.
     *  As thread C finally continues, it finds pending_template empty.
     *  --> Showing A, then C, skipping B.
     */
    private synchronized void loadTemplate(final JobMonitor monitor)
    {
        try
        {
            final DisplayAndGroup handle = pending_template.getAndSet(null);
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
            final DisplayModel template;
            try
            {   // Load template's model (potentially slow)
                template = loadDisplayModel(model_widget, handle);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Failed to load template " + handle, ex);
                return;
            }
            instantiateTemplateAndRepresent(template);
        }
        finally
        {
            toolkit.onRepresentationFinished();
        }
    }

    private void instantiateTemplateAndRepresent(final DisplayModel template)
    {
        System.out.println("instantiateTemplateAndRepresent on " + Thread.currentThread());
        try
        {

            active_template_model.set(template);

            // TODO Loading the model, duplicating it into instances, and representing the result must be separate steps
            // Changing the file triggers all 3 steps.
            // Changing the gap or number of instances only triggers the last 2 steps.
            // Each must be reverted as appropriate when changing the file, gap or number of instances
            final ByteArrayOutputStream xml = new ByteArrayOutputStream();
            try
            (
                final ModelWriter writer = new ModelWriter(xml)
            )
            {
                writer.writeModel(template);
            }
            final String template_xml = xml.toString(XMLUtil.ENCODING);

            // Copy template into new model
            final DisplayModel new_model = new DisplayModel();

            final int w = template.propWidth().getValue();
            final int h = template.propHeight().getValue();
            int x = 0, y = 0;
            for (InstanceProperty instance : model_widget.propInstances().getValue())
            {
                if (toolkit.isEditMode())
                {   // Show hint for each instance
                    final LabelWidget hint = new LabelWidget();
                    hint.propText().setValue(Objects.toString(instance.macros().getValue()));
                    hint.propBackgroundColor().setValue(new WidgetColor(0, 0, 255, 50));
                    hint.propTransparent().setValue(false);
                    hint.propX().setValue(x);
                    hint.propY().setValue(y);
                    hint.propWidth().setValue(w);
                    hint.propHeight().setValue(h);
                    new_model.runtimeChildren().addChild(hint);
                }
                else
                {   // Duplicate model for each instance, wrapped in Group to set macros
                    final DisplayModel inst = ModelReader.parseXML(template_xml);
                    final GroupWidget wrapper = new GroupWidget();
                    wrapper.propStyle().setValue(Style.NONE);
                    wrapper.propX().setValue(x);
                    wrapper.propY().setValue(y);
                    wrapper.propWidth().setValue(w);
                    wrapper.propHeight().setValue(h);

                    for (Widget widget : inst.getChildren())
                        wrapper.runtimeChildren().addChild(widget);
                    wrapper.propMacros().setValue(instance.macros().getValue());

                    new_model.runtimeChildren().addChild(wrapper);
                }

                if (model_widget.propHorizontal().getValue())
                    x += w + model_widget.propGap().getValue();
                else
                    y += h + model_widget.propGap().getValue();
            }

            // Stop (old) runtime
            // TODO EmbeddedWidgetRuntime tracks this property to start/stop the embedded model's runtime
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
            logger.log(Level.WARNING, "Failed to instantiate template " + model_widget.propFile().getValue(), ex);
        }
    }

    /** @param content_model Model to represent */
    private void representContent(final DisplayModel content_model)
    {
        try
        {
            sizesChanged(null, null, null);
            toolkit.representModel(inner, content_model);
            backgroundChanged(null, null, null);
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, "Failed to represent embedded display", ex);
        }
    }

    private void backgroundChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        if (toolkit.isEditMode())
            inner_background = EDIT_TRANSPARENT_BACKGROUND;
        else
            inner_background = TRANSPARENT_BACKGROUND;

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

            // set minimum and maximum size of jfx_node
            // to match the requested size
            inner.setMinSize(width, height);
            jfx_node.setMinSize(width, height);
            jfx_node.setMaxSize(width, height);

            jfx_node.getChildren().setAll(inner);

            // Check for overdrawing
            if (get_size_again.checkAndClear())
            {
                // Give the UI thread a chance to render the contents and update the width/height
                dirty_sizes.mark();
                toolkit.scheduleUpdate(this);
            }
        }
        if (dirty_background.checkAndClear())
            inner.setBackground(inner_background);
    }

    @Override
    public void dispose()
    {
        // When the file name is changed, loadTemplate()
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
    }
}
