/**
 *
 */
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newDoublePropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLineColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLineWidth;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propTransparent;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.properties.WidgetColor;

/**
 * Widget that displays an arc
 * @author Megan Grodowitz
 *
 */
@SuppressWarnings("nls")
public class ArcWidget extends VisibleWidget {

    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("arc", WidgetCategory.GRAPHIC,
            "Arc",
            "/icons/arc.png",
            "An arc",
            Arrays.asList("org.csstudio.opibuilder.widgets.arc"))
    {
        @Override
        public Widget createWidget()
        {
            return new ArcWidget();
        }
    };

    //TODO: change start_anlge and total_angle to new terms. Setup input configurator to handle old terms
    private static final WidgetPropertyDescriptor<Double> propAngleStart =
            newDoublePropertyDescriptor(WidgetPropertyCategory.DISPLAY, "start_angle", Messages.WidgetProperties_AngleStart);

    private static final WidgetPropertyDescriptor<Double> propAngleSize =
            newDoublePropertyDescriptor(WidgetPropertyCategory.DISPLAY, "total_angle", Messages.WidgetProperties_AngleSize);

    // fill color
    private WidgetProperty<WidgetColor> background;
    // Do we need transparency for arc? It appears that existing displays have clear arcs, so I think yes
    private WidgetProperty<Boolean> transparent;
    // line color and width
    private WidgetProperty<WidgetColor> line_color;
    private WidgetProperty<Integer> line_width;
    // start/size degree of arc (0-365)
    private WidgetProperty<Double> arc_start;
    private WidgetProperty<Double> arc_size;


	public ArcWidget() {
		super(WIDGET_DESCRIPTOR.getType(), 100, 100);
	}

	// By default create an arc with dark blue line, light blue interior, no transparency, 90 degree angle from 0-90
    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(arc_start = propAngleStart.createProperty(this, 0.0));
        properties.add(arc_size = propAngleSize.createProperty(this, 90.0));
        properties.add(line_width = propLineWidth.createProperty(this, 3));
        properties.add(line_color = propLineColor.createProperty(this, new WidgetColor(0, 0, 255)));
        properties.add(background = propBackgroundColor.createProperty(this, new WidgetColor(30, 144, 255)));
        properties.add(transparent = propTransparent.createProperty(this, false));
    }

    /** @return 'background_color' property */
    public WidgetProperty<WidgetColor> propBackgroundColor()
    {
        return background;
    }

    /** @return 'transparent' property */
    public WidgetProperty<Boolean> propTransparent()
    {
        return transparent;
    }

    /** @return 'line_color' property */
    public WidgetProperty<WidgetColor> propLineColor()
    {
        return line_color;
    }

    /** @return 'line_width' property */
    public WidgetProperty<Integer> propLineWidth()
    {
        return line_width;
    }

    /** @return 'arc_start' property */
    public WidgetProperty<Double> propArcStart()
    {
        return arc_start;
    }

    /** @return 'arc_size' property */
    public WidgetProperty<Double> propArcSize()
    {
        return arc_size;
    }
}
