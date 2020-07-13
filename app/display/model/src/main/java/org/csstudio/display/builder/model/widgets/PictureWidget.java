/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newBooleanPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newDoublePropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFile;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.Version;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/** Widget that displays an image loaded from a file
 *  @author Megan Grodowitz
 */
@SuppressWarnings("nls")
public class PictureWidget extends MacroWidget
{
    public final static String default_pic = "examples:/icons/default_picture.png";

    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
            new WidgetDescriptor("picture", WidgetCategory.GRAPHIC,
                    "Picture",
                    "/icons/picture.png",
                    "Display a picture from a file",
                    Arrays.asList("org.csstudio.opibuilder.widgets.Image"))
    {
        @Override
        public Widget createWidget()
        {
            return new PictureWidget();
        }
    };

    /** Custom configurator to read legacy *.opi files */
    private static class CustomConfigurator extends WidgetConfigurator
    {
        public CustomConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget, final Element widget_xml)
                throws Exception
        {
            Element xml = XMLUtil.getChildElement(widget_xml, "image_file");
            if (xml != null)
            {
                final Document doc = widget_xml.getOwnerDocument();
                Element fname = doc.createElement(propFile.getName());

                if (xml.getFirstChild() != null)
                {
                    fname.appendChild(xml.getFirstChild().cloneNode(true));
                }
                else
                {
                    Text fpath = doc.createTextNode(default_pic);
                    fname.appendChild(fpath);
                }
                widget_xml.appendChild(fname);
            }

            if (xml_version.getMajor() < 2)
            {
                final PictureWidget picture = (PictureWidget) widget;
                MacroWidget.importPVName(model_reader, widget, widget_xml);
                XMLUtil.getChildBoolean(widget_xml, "stretch_to_fit")
                       .ifPresent(stretch -> picture.propStretch().setValue(stretch));
            }

            // Parse updated XML
            return super.configureFromXML(model_reader, widget, widget_xml);
        }
    }
    /** 'rotation' property: What is the rotation of the picture */
    public static final WidgetPropertyDescriptor<Double> propRotation =
            newDoublePropertyDescriptor(WidgetPropertyCategory.DISPLAY, "rotation", Messages.WidgetProperties_Rotation);

    public static final WidgetPropertyDescriptor<Boolean> propStretch =
            newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "stretch_image", Messages.WidgetProperties_StretchToFit);

    private volatile WidgetProperty<String> filename;
    private volatile WidgetProperty<Boolean> stretch_image;
    private volatile WidgetProperty<Double> rotation;

    public PictureWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 150, 100);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(filename = propFile.createProperty(this, default_pic));
        properties.add(stretch_image = propStretch.createProperty(this, false));
        properties.add(rotation = propRotation.createProperty(this, 0.0));
    }

    /** @return 'rotation' property */
    public WidgetProperty<Double> propRotation()
    {
        return rotation;
    }

    /** @return 'text' property */
    public WidgetProperty<String> propFile()
    {
        return filename;
    }

    /** @return 'stretch' property */
    public WidgetProperty<Boolean> propStretch()
    {
        return stretch_image;
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version)
            throws Exception
    {
        return new CustomConfigurator(persisted_version);
    }

}
