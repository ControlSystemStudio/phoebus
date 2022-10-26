package org.csstudio.display.widget;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.representation.WidgetRepresentation;
import org.csstudio.display.builder.representation.WidgetRepresentationFactory;
import org.csstudio.display.builder.representation.spi.WidgetRepresentationsService;

import java.util.Map;

import static java.util.Map.entry;

public class ThumbwheelWidgetRepresentationService implements WidgetRepresentationsService {
    @SuppressWarnings({"unchecked", "rawtypes", "nls"})
    @Override
    public <TWP, TW> Map<WidgetDescriptor, WidgetRepresentationFactory<TWP, TW>> getWidgetRepresentationFactories() {
        final WidgetDescriptor unknown_widget = new WidgetDescriptor(WidgetRepresentationFactory.UNKNOWN, null, WidgetRepresentationFactory.UNKNOWN, null, "Unknown Widget") {
            @Override
            public Widget createWidget() {
                // Cannot create instance
                return null;
            }
        };

        return Map.ofEntries(
                entry(ThumbwheelWidget.WIDGET_DESCRIPTOR, () -> (WidgetRepresentation) new ThumbwheelWidgetRepresentation()));
    }
}
