/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.ActionInfos;
import org.csstudio.display.builder.model.properties.ExecuteScriptActionInfo;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo.Target;
import org.csstudio.display.builder.model.properties.ScriptInfo;
import org.csstudio.display.builder.model.properties.WritePVActionInfo;
import org.phoebus.framework.macros.Macros;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.stage.Stage;

/** Demo of {@link ActionsDialog}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ActionsDialogDemo extends ApplicationWrapper
{
    public static void main(final String[] args)
    {
        launch(ActionsDialogDemo.class, args);
    }

    @Override
    public void start(final Stage stage)
    {
        final Widget widget = new Widget("demo");
        final Macros macros = new Macros();
        macros.add("S", "Test");
        macros.add("N", "17");
        final ActionInfos actions = new ActionInfos(Arrays.asList(
                new OpenDisplayActionInfo("Related Display", "../opi/some_file.opi", macros, Target.TAB, "Side"),
                new WritePVActionInfo("Reset", "Test:CS:Reset", "1"),
                new ExecuteScriptActionInfo("Script", new ScriptInfo(ScriptInfo.EMBEDDED_PYTHON, "print 'hi'", false, Collections.emptyList())))
                );
        final ActionsDialog dialog = new ActionsDialog(widget, actions, null);
        final Optional<ActionInfos> result = dialog.showAndWait();
        if (result.isPresent())
        {
            if (result.get().isExecutedAsOne())
                System.out.println("Execute all commands as one:");
            for (ActionInfo info : result.get().getActions())
            {
                if (info instanceof ExecuteScriptActionInfo)
                {
                    final ExecuteScriptActionInfo action = (ExecuteScriptActionInfo)info;
                    System.out.println("Execute " + action.getDescription() + ", " + action.getInfo() + ": " + action.getInfo().getText());
                }
                else
                    System.out.println(info);
            }
        }
        else
            System.out.println("Cancelled");
    }
}
