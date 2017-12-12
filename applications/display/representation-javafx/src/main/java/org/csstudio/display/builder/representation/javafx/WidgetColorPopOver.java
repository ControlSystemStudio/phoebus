/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2016 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.display.builder.representation.javafx;


import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.csstudio.display.builder.model.properties.ColorWidgetProperty;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.phoebus.ui.dialog.PopOver;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;


/**
 * A factory class for popover editors.
 *
 * @author claudiorosati, European Spallation Source ERIC
 * @version 1.0.0 30 Nov 2017
 */
@SuppressWarnings("nls")
public class WidgetColorPopOver extends PopOver
{
    public WidgetColorPopOver(final ColorWidgetProperty color_prop,
                              final Consumer<WidgetColor> colorChangeConsumer)
    {
        try
        {
            URL fxml = WidgetColorPopOver.class.getResource("WidgetColorPopOver.fxml");
            InputStream iStream = WidgetColorPopOver.class.getResourceAsStream("messages.properties");
            ResourceBundle bundle = new PropertyResourceBundle(iStream);
            FXMLLoader fxmlLoader = new FXMLLoader(fxml, bundle);
            Node content = (Node) fxmlLoader.load();

            setContent(content);

            WidgetColorPopOverController controller = fxmlLoader.<WidgetColorPopOverController>getController();

            controller.setInitialConditions(this, color_prop.getValue(), color_prop.getDefaultValue(), color_prop.getDescription(), colorChangeConsumer);
        }
        catch (IOException ex)
        {
            logger.log(Level.WARNING, "Unable to edit color.", ex);
            setContent(new Label("Unable to edit color."));
        }
    }
}
