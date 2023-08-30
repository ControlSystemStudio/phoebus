package org.csstudio.display.extra.widgets;

import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.spi.WidgetsService;
import org.csstudio.display.extra.widgets.linearmeter.LinearMeterWidget;

import java.util.Collection;
import java.util.List;

/**
 * A widget service for which provided additional widgets
 */
public class LinearMeterWidgetService implements WidgetsService
{
    @Override
    public Collection<WidgetDescriptor> getWidgetDescriptors()
    {
        return List.of(
                LinearMeterWidget.WIDGET_DESCRIPTOR
        );
    }
}
