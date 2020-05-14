/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ScriptInfo;
import org.csstudio.display.builder.model.properties.ScriptPV;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.stage.Stage;

/** Demo of {@link ScriptsDialog}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScriptsDialogDemo extends ApplicationWrapper
{
    public static void main(final String[] args)
    {
        launch(ScriptsDialogDemo.class, args);
    }

    @Override
    public void start(final Stage stage)
    {
        final List<ScriptInfo> scripts = new ArrayList<>();
        scripts.add(new ScriptInfo("/tmp/demo1.py", true, new ScriptPV("pv1")));
        scripts.add(new ScriptInfo("/tmp/demo2.py", false, new ScriptPV("pv1"), new ScriptPV("pv2", false)));

        final ScriptsDialog dialog = new ScriptsDialog(new Widget("demo"), scripts, null);
        final Optional<List<ScriptInfo>> result = dialog.showAndWait();
        if (result.isPresent())
        {
            for (ScriptInfo info : result.get())
                System.out.println(info + ", embedded text: " + info.getText());
        }
        else
            System.out.println("Cancelled");
    }
}
