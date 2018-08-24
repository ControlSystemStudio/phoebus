/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.util.shell;

import java.util.concurrent.CountDownLatch;

/** Demo of the {@link CommandShell}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class CommandShellDemo
{
    private static final String HELP =
            "help - Show help\n" +
            "exit - Exit";

    private static final CountDownLatch done = new CountDownLatch(1);

    private static boolean handleCommand(final String[] args)
    {
        if (args == null  ||  (args.length == 1  &&  args[0].startsWith("ex")))
        {
            done.countDown();
            return true;
        }
        // .. read example would handle many more commands..

        return false;
    }


    public static void main(String[] args) throws Exception
    {
        final CommandShell shell = new CommandShell(HELP,
                                                    CommandShellDemo::handleCommand);
        shell.start();
        done.await();
        shell.stop();

        System.out.println("\nDone.");
    }
}
