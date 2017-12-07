/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.test;

import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.representation.javafx.WidgetColorDialog;

import javafx.application.Application;
import javafx.stage.Stage;

/** Demo of {@link WidgetColorDialog}
 *  @author Kay Kasemir
 */
public class JFXColorDialogDemo  extends Application
{
    public static void main(final String[] args)
    {
        launch(args);
    }

    @Override
    public void start(final Stage stage)
    {
        final WidgetColorDialog dialog = new WidgetColorDialog(WidgetColorService.getColor(NamedWidgetColors.BACKGROUND), "Background", new WidgetColor(255, 255, 255));
        System.out.println(dialog.showAndWait());
    }
}
