/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.console;

import org.phoebus.applications.console.Console.LineType;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

/** Console app instance
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class ConsoleInstance implements AppInstance
{
    private ConsoleApp app;

    ConsoleInstance(final ConsoleApp app)
    {
        this.app = app;

        try
        {
            final Console console = new Console();

            final DockItem dock_item = new DockItem(this, console.getNode());
            DockPane.getActiveDockPane().addTab(dock_item);

            final ProcessWrapper process = new ProcessWrapper(Console.shell, Console.directory,
                output -> console.addLine(output, LineType.OUTPUT),
                error  -> console.addLine(error, LineType.ERROR),
                ()     -> console.disable());

            console.setOnInput(process::sendInput);

            process.start();
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError("Error", "Cannot create console", ex);
        }
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }
}
