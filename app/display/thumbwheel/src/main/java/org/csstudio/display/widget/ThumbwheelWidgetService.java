package org.csstudio.display.widget;

import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.spi.WidgetsService;

import java.util.Collection;
import java.util.List;

/**
 * A widget service for which provided additional widgets
 */
public class ThumbwheelWidgetService implements WidgetsService
{
    @Override
    public Collection<WidgetDescriptor> getWidgetDescriptors()
    {
        return List.of(
                ThumbwheelWidget.WIDGET_DESCRIPTOR );
    }
}
