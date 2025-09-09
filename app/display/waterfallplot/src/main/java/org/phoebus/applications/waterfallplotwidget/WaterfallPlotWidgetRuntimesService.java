package org.phoebus.applications.waterfallplotwidget;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.runtime.WidgetRuntime;
import org.csstudio.display.builder.runtime.spi.WidgetRuntimesService;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.phoebus.applications.waterfallplotwidget.WaterfallPlotWidget.WIDGET_DESCRIPTOR;

public class WaterfallPlotWidgetRuntimesService implements WidgetRuntimesService {
    @Override
    public Map<String, Supplier<WidgetRuntime<? extends Widget>>> getWidgetRuntimeFactories() {
        Map<String, Supplier<WidgetRuntime<? extends Widget>>> widgetRuntimeFactories = new HashMap<>();
        widgetRuntimeFactories.put(WIDGET_DESCRIPTOR.getType(), () -> new WaterfallPlotRuntime());
        return widgetRuntimeFactories;
    }
}
