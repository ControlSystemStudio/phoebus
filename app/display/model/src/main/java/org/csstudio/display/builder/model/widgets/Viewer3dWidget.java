/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFile;

import java.util.Collections;
import java.util.List;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;

/**
 * 3D Viewer Widget
 * @author Evan Smith
 */
public class Viewer3dWidget extends VisibleWidget
{
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("3dviewer", WidgetCategory.MISC,
                "3d Viewer",
                "/icons/viewer3d.png",
                "Embedded 3d Viewer",
                Collections.emptyList())
    {
        @Override
        public Widget createWidget()
        {
            return new Viewer3dWidget();
        }
    };
    
    /** Defines which resource to load into the viewer. */
    private volatile WidgetProperty<String> resource;
    
    public Viewer3dWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 600, 600);
    }
    
    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        
        properties.add(resource = propFile.createProperty(this, ""));
    }
    
    /** @return 'resource' property */
    public WidgetProperty<String> propResource()
    {
        return resource;
    }
}
