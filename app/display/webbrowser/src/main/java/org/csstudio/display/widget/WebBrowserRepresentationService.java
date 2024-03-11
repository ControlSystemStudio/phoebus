package org.csstudio.display.widget;

import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.representation.WidgetRepresentation;
import org.csstudio.display.builder.representation.WidgetRepresentationFactory;
import org.csstudio.display.builder.representation.spi.WidgetRepresentationsService;

import java.util.Map;

import static java.util.Map.entry;

public class WebBrowserRepresentationService implements WidgetRepresentationsService {
    @SuppressWarnings({"unchecked", "rawtypes", "nls"})
    @Override
    public <TWP, TW> Map<WidgetDescriptor, WidgetRepresentationFactory<TWP, TW>> getWidgetRepresentationFactories() {
        return Map.ofEntries(entry(WebBrowserWidget.WIDGET_DESCRIPTOR, () -> (WidgetRepresentation) new WebBrowserRepresentation()));
    }
}
