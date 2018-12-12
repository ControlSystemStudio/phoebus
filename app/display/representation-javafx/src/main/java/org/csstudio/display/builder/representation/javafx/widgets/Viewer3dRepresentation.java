/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.io.InputStream;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.widgets.Viewer3dWidget;
import org.phoebus.applications.viewer3d.ResourceUtil;
import org.phoebus.applications.viewer3d.Viewer3d;
import org.phoebus.applications.viewer3d.Xform;
import org.phoebus.framework.jobs.JobManager;

import javafx.application.Platform;

/**
 * JFX Representation of the 3D Viewer Widget
 * @author Evan Smith
 */
@SuppressWarnings("nls")
public class Viewer3dRepresentation extends JFXBaseRepresentation<Viewer3d, Viewer3dWidget>
{
    private final DirtyFlag dirty_position = new DirtyFlag();
    private final DirtyFlag dirty_resource = new DirtyFlag();
    private final UntypedWidgetPropertyListener positionListener = this::positionChanged;
    private final UntypedWidgetPropertyListener resourceListener = this::resourceChanged;

    @Override
    protected Viewer3d createJFXNode() throws Exception
    {
       return new Viewer3d(toolkit.isEditMode());
    }

    @Override
    protected void registerListeners()
    {
        // XXX Viewer3d does not behave like other top-level nodes,
        //     cannot use basic X, Y property listener
        // super.registerListeners();
        model_widget.propVisible().addUntypedPropertyListener(positionListener);
        model_widget.propX().addUntypedPropertyListener(positionListener);
        model_widget.propY().addUntypedPropertyListener(positionListener);
        model_widget.propWidth().addUntypedPropertyListener(positionListener);
        model_widget.propHeight().addUntypedPropertyListener(positionListener);
        model_widget.propResource().addUntypedPropertyListener(resourceListener);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propVisible().removePropertyListener(positionListener);
        model_widget.propX().removePropertyListener(positionListener);
        model_widget.propY().removePropertyListener(positionListener);
        model_widget.propWidth().removePropertyListener(positionListener);
        model_widget.propHeight().removePropertyListener(positionListener);
        model_widget.propResource().removePropertyListener(resourceListener);
        // super.unregisterListeners();
    }

    private void positionChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_position.mark();
        toolkit.scheduleUpdate(this);
    }

    private void resourceChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_resource.mark();
        toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        if (dirty_position.checkAndClear())
        {
            if (model_widget.propVisible().getValue())
            {
                jfx_node.setVisible(true);
                final int x = model_widget.propX().getValue();
                final int y = model_widget.propY().getValue();
                final int w = model_widget.propWidth().getValue();
                final int h = model_widget.propHeight().getValue();
                jfx_node.setTranslateX(x);
                jfx_node.setTranslateY(y);
                jfx_node.setPrefWidth(w);
                jfx_node.setPrefHeight(h);
            }
            else
                jfx_node.setVisible(false);
        }
        if (dirty_resource.checkAndClear())
        {
            JobManager.schedule("Read 3d viewer resource", monitor ->
            {
                final String resource = model_widget.propResource().getValue();
                InputStream inputStream = null;

                try
                {
                    inputStream = ResourceUtil.openResource(resource);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Opening resource '" + resource + "' failed", ex);
                }

                if (null != inputStream)
                {
                    try
                    {
                        final Xform struct = Viewer3d.buildStructure(inputStream);
                        if (null != struct)
                            Platform.runLater(() -> jfx_node.setStructure(struct));
                    }
                    catch (Exception ex)
                    {
                        logger.log(Level.WARNING, "Building structure failed", ex);
                    }
                }
            });
        }
    }
}
