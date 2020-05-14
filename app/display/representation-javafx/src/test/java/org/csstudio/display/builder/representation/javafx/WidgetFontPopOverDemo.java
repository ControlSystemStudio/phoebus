/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.FontWidgetProperty;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/** Demo of {@link WidgetFontPopOver}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WidgetFontPopOverDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage)
    {
        final WidgetFontPopOver popover = new WidgetFontPopOver(
                (FontWidgetProperty) CommonWidgetProperties.propFont.createProperty(null, WidgetFontService.get(NamedWidgetFonts.DEFAULT_BOLD)),
                font -> System.out.println("Selected " + font)
                );

        final Button toggle_popup = new Button("Font");
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

    public static void main(final String[] args)
    {
        launch(WidgetFontPopOverDemo.class, args);
    }
}
