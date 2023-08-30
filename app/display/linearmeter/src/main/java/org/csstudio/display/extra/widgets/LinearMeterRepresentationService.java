package org.csstudio.display.extra.widgets;

import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.representation.WidgetRepresentation;
import org.csstudio.display.builder.representation.WidgetRepresentationFactory;
import org.csstudio.display.builder.representation.spi.WidgetRepresentationsService;
import org.csstudio.display.extra.widgets.linearmeter.LinearMeterRepresentation;
import org.csstudio.display.extra.widgets.linearmeter.LinearMeterWidget;

import java.util.Map;

import static java.util.Map.entry;

public class LinearMeterRepresentationService implements WidgetRepresentationsService {
    @SuppressWarnings({"unchecked", "rawtypes", "nls"})
    @Override
    public <TWP, TW> Map<WidgetDescriptor, WidgetRepresentationFactory<TWP, TW>> getWidgetRepresentationFactories() {
        return Map.ofEntries(
                entry(LinearMeterWidget.WIDGET_DESCRIPTOR, () -> (WidgetRepresentation) new LinearMeterRepresentation()));
    }
}
