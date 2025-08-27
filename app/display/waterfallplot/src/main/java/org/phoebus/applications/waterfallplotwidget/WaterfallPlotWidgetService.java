package org.phoebus.applications.waterfallplotwidget;

import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.spi.WidgetsService;

import java.util.Collection;
import java.util.List;

public class WaterfallPlotWidgetService implements WidgetsService {
    @Override
    public Collection<WidgetDescriptor> getWidgetDescriptors() {
        return List.of(
                WaterfallPlotWidget.WIDGET_DESCRIPTOR
        );
    }
}
