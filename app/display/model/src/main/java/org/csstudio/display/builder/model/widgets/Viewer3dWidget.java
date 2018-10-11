package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFile;

import java.util.Collections;
import java.util.List;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;

public class Viewer3dWidget extends VisibleWidget
{
    /** Widget descriptor */
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
