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
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.model.widgets.GroupWidget.Style;
import org.csstudio.display.builder.model.widgets.TemplateInstanceWidget;
import org.csstudio.display.builder.model.widgets.TemplateInstanceWidget.InstanceProperty;
import org.csstudio.display.builder.representation.EmbeddedDisplayRepresentationUtil.DisplayAndGroup;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.macros.Macros;
import org.phoebus.framework.persistence.XMLUtil;

import javafx.geometry.Insets;
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
    private final DirtyFlag get_size_again = new DirtyFlag(false);
    private final UntypedWidgetPropertyListener sizesChangedListener = this::sizesChanged;
    private final WidgetPropertyListener<List<InstanceProperty>> instancesChangedListener = this::instancesChanged;
    private final WidgetPropertyListener<String> fileChangedListener = this::fileChanged;
    private final WidgetPropertyListener<Macros> macrosChangedListener = this::macrosChanged;

    /** Has this representation been disposed? */
    private AtomicBoolean disposed = new AtomicBoolean();

    /** The display file (and optional group inside that display) to load */
    private final AtomicReference<DisplayAndGroup> pending_template = new AtomicReference<>();

    /** Track active template, may be empty */
    private final AtomicReference<DisplayModel> active_template_model = new AtomicReference<>(new DisplayModel());

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
        final Pane pane = new Pane();
        if (toolkit.isEditMode())
            pane.setBackground(EDIT_TRANSPARENT_BACKGROUND);

        return pane;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addUntypedPropertyListener(sizesChangedListener);
        model_widget.propHeight().addUntypedPropertyListener(sizesChangedListener);
        model_widget.propGap().addUntypedPropertyListener(sizesChangedListener);
        model_widget.propHorizontal().addUntypedPropertyListener(sizesChangedListener);

        model_widget.propInstances().addPropertyListener(instancesChangedListener);

        model_widget.propFile().addPropertyListener(fileChangedListener);

        fileChanged(null, null, null);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(sizesChangedListener);
        model_widget.propHeight().removePropertyListener(sizesChangedListener);
        model_widget.propGap().removePropertyListener(sizesChangedListener);
        model_widget.propHorizontal().removePropertyListener(sizesChangedListener);

        model_widget.propInstances().removePropertyListener(instancesChangedListener);

        model_widget.propFile().removePropertyListener(fileChangedListener);

        super.unregisterListeners();
    }

    private void instancesChanged(final WidgetProperty<List<InstanceProperty>> property, final List<InstanceProperty> removed, final List<InstanceProperty> added)
    {
        // Listen to changes in macros, performing a full representation as if sizes were changed
        if (removed != null)
            for (InstanceProperty instance : removed)
                instance.macros().removePropertyListener(macrosChangedListener);
        if (added != null)
            for (InstanceProperty instance : added)
                instance.macros().addPropertyListener(macrosChangedListener);

        sizesChanged(property, removed, added);
    }

    /** When macros changed (or sizes), re-create instances */
    private void macrosChanged(final WidgetProperty<Macros> property, final Macros old_value, final Macros new_value)
    {
        scheduleInstanceUpdate();
    }

    private void sizesChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        if (resizing)
            return;

        final DisplayModel template_model = active_template_model.get();
        if (template_model.getChildren().size() > 0)
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

        if (property == model_widget.propGap() ||
            property == model_widget.propInstances() ||
            property == model_widget.propHorizontal())
            scheduleInstanceUpdate();

        dirty_sizes.mark();
        get_size_again.mark();
        toolkit.scheduleUpdate(this);
    }

    private void fileChanged(final WidgetProperty<String> property, final String old_value, final String new_value)
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

    private void scheduleInstanceUpdate()
    {
        toolkit.onRepresentationStarted();
        JobManager.schedule("Update Instances", monitor ->
        {
            try
            {
                instantiateTemplateAndRepresent();
            }
            finally
            {
                toolkit.onRepresentationFinished();
            }
        });
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
            if (disposed.get())
            {
                // System.out.println("Aborted: " + handle);
                return;
            }

            monitor.beginTask("Load " + handle);
            try
            {   // Load template's model (potentially slow)
                active_template_model.set(loadDisplayModel(model_widget, handle));
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Failed to load template " + handle, ex);
                return;
            }
            instantiateTemplateAndRepresent();
        }
        finally
        {
            toolkit.onRepresentationFinished();
        }
    }

    /** Instantiate template and represent the expanded model
     *
     *  Loading the model, duplicating it into instances, and representing the result must be separate steps
     *  Changing the file triggers all 3 steps.
     *  Changing the gap or number of instances only triggers the last 2 steps.
     */
    private void instantiateTemplateAndRepresent()
    {
        final DisplayModel template = active_template_model.get();
        try
        {
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
            // Mark new_model as _not_ being a top-level model,
            // which would cause the toolkit to create a new 'phasor' and result
            // in timeouts of the editor awaiting complete representation
            new_model.setUserData(DisplayModel.USER_DATA_EMBEDDING_WIDGET, model_widget);

            if (template.getChildren().size() > 0)
            {
                final int w = template.propWidth().getValue();
                final int h = template.propHeight().getValue();
                int x = 0, y = 0;
                for (InstanceProperty instance : model_widget.propInstances().getValue())
                {
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

                    if (model_widget.propHorizontal().getValue())
                        x += w + model_widget.propGap().getValue();
                    else
                        y += h + model_widget.propGap().getValue();
                }
            }

            // Stop (old) runtime
            // TemplateInstanceRuntime tracks this property to start/stop the embedded model's runtime
            model_widget.runtimePropEmbeddedModel().setValue(null);

            // Atomically update the 'active' model
            final DisplayModel old_model = active_content_model.getAndSet(new_model);

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
            try
            {
                final Future<Object> completion = toolkit.submit(() ->
                {
                    representContent(new_model);
                    return null;
                });
                checkCompletion(model_widget, completion, "timeout representing new content");
            }
            finally
            {
                toolkit.onRepresentationFinished();
            }

            // Allow TemplateInstanceRuntime to start the new runtime
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
            jfx_node.getChildren().clear();
            toolkit.representModel(jfx_node, content_model);
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, "Failed to represent embedded display", ex);
        }
    }

    @Override
    public void updateChanges()
    {
        // Late update after disposal?
        if (disposed.get())
            return;
        super.updateChanges();
        if (dirty_sizes.checkAndClear())
        {
            final Integer width = model_widget.propWidth().getValue();
            final Integer height = model_widget.propHeight().getValue();

            // set minimum and maximum size of jfx_node
            // to match the requested size
            jfx_node.setMinSize(width, height);
            jfx_node.setMaxSize(width, height);

            if (get_size_again.checkAndClear())
            {
                // Give the UI thread a chance to render the contents and update the width/height
                dirty_sizes.mark();
                toolkit.scheduleUpdate(this);
            }
        }
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

        if (disposed.getAndSet(true) == false  &&  em != null)
            toolkit.disposeRepresentation(em);

        super.dispose();
    }
}
