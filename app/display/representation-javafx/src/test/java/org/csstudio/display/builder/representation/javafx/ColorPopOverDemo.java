/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.properties.ColorWidgetProperty;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/** Demo of {@link WidgetColorPopOver}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")

public class ColorPopOverDemo extends ApplicationWrapper
{
    public static void main(final String[] args)
    {
        launch(ColorPopOverDemo.class, args);
    }

    @Override
    public void start(final Stage stage)
    {
        final Widget widget = new Widget("test");
        final ColorWidgetProperty prop = (ColorWidgetProperty) CommonWidgetProperties.propForegroundColor.createProperty(widget, WidgetColorService.getColor(NamedWidgetColors.TEXT));
        final WidgetColorPopOver popover = new WidgetColorPopOver(prop, color -> System.out.println("Selected " + color));

        final Button toggle_popup = new Button("Color");
        toggle_popup.setOnAction(event ->
        {
            if (popover.isShowing())
                popover.hide();
            else
                popover.show(toggle_popup);
        });

        final BorderPane layout = new BorderPane(toggle_popup);
        stage.setScene(new Scene(layout, 400, 300));
        stage.show();
    }
}
