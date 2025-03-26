/*******************************************************************************
 * Copyright (c) 2015-2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import javafx.scene.Node;
import javafx.scene.image.Image;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.csstudio.display.builder.model.properties.ActionInfos;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.csstudio.display.builder.model.widgets.ActionButtonWidget;
import org.phoebus.framework.macros.Macros;
import org.phoebus.ui.javafx.ImageCache;
import org.w3c.dom.Element;

import javax.xml.stream.XMLStreamWriter;
import java.io.FileOutputStream;
import java.util.Arrays;

/**
 * Tool that creates demo file with all widget types and all properties
 * <p>
 * Invoke from IDE or via `ant all_widgets`
 *
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AllWidgetsAllProperties {
    public static void main(String[] args) throws Exception {
        if (args.length != 1)
            throw new Exception("Usage: AllWidgetsAllProperties /path/to/all_widgets.bob");

        final String filename = args[0];
        System.out.println("Writing " + filename + " ...");

        final DisplayModel model = new DisplayModel();
        for (final WidgetDescriptor widget_type : WidgetFactory.getInstance().getWidgetDescriptions()) {
            final Widget widget = widget_type.createWidget();
            widget.setPropertyValue("name", widget_type.getName() + " 1");

            // For some widgets, adjust default values
            if (widget_type == ActionButtonWidget.WIDGET_DESCRIPTOR) {   // Action Button: Add open-display example
                ActionButtonWidget button = (ActionButtonWidget) widget;
                final Macros macros = new Macros();
                macros.add("S", "Test");
                macros.add("N", "2");
                ActionInfo actionInfo = new ActionInfo() {
                    @Override
                    public String getType() {
                        return "open_display";
                    }

                    @Override
                    public Image getImage() {
                        return ImageCache.getImage(ActionInfo.class, "/icons/open_display.png");
                    }

                    @Override
                    public String getDescription() {
                        return "Open Display";
                    }

                    @Override
                    public void setDescription(String description) {

                    }

                    @Override
                    public void readFromXML(ModelReader modelReader, Element actionXml)  {
                    }

                    @Override
                    public void writeToXML(ModelWriter modelWriter, XMLStreamWriter writer) throws Exception {
                        writer.writeAttribute(XMLTags.TYPE, "open_display");
                        writer.writeStartElement(XMLTags.DESCRIPTION);
                        writer.writeCharacters("Open Display");
                        writer.writeEndElement();
                        writer.writeStartElement(XMLTags.SCRIPT);
                        writer.writeEndElement();
                    }
                };
                button.propActions().setValue(new ActionInfos(Arrays.asList(actionInfo)));
            }

            model.runtimeChildren().addChild(widget);
        }
        ModelWriter.with_comments = true;
        ModelWriter.skip_defaults = false;
        ModelWriter.enable_saved_on_comment = true;
        try {
            final ModelWriter writer = new ModelWriter(new FileOutputStream(filename));
            writer.writeModel(model);
            writer.close();
        } finally {
            ModelWriter.with_comments = false;
            ModelWriter.skip_defaults = true;
        }
        System.out.println("Done.");
    }
}
