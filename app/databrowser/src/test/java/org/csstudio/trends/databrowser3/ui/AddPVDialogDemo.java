/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui;

import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.stage.Stage;

/** Demo of the {@link AddPVDialog}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AddPVDialogDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final Model model = new Model();
        model.addAxis().setName("Axis 1");
        model.addAxis().setName("Axis 2");
        model.addItem(new PVItem("Test", 2.0));
        final AddPVDialog dlg = new AddPVDialog(2, model, false);

        if (dlg.showAndWait().orElse(false))
            for (int i=0; i<2; ++i)
                System.out.println( (i+1) + ": " + dlg.getName(i) + " @ " + dlg.getScanPeriod(i) + ", on axis " + dlg.getAxisIndex(i));
    }

    public static void main(final String[] args)
    {
        launch(AddPVDialogDemo.class, args);
    }
}
