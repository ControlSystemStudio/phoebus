/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.util.shell;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Helper for creating a command shell
 *
 *  <p>Executes in its own thread,
 *  reads from {@link System#in}
 *  and passes received commands to a handler.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class CommandShell
{
    public interface CommandHandler
    {
        /** Invoked when user entered a command
         *  @param args Entered command, split at spaces, or <code>null</code> when user entered 'Ctrl-D' to close shell
         *  @return <code>true</code> if command was handled, <code>false</code> to show help
         *  @throws Exception on error
         */
        public boolean handle(String[] args) throws Throwable;
    }

    private final Thread thread;
    private final String help;
    private final CommandHandler handler;
    private String prompt = "";

    /** Create shell
     *  @param help Text that is displayed initially
     *              and whenever a command isn't handled
     *  @param handler {@link CommandHandler}
     */
    public CommandShell(final String help,
                        final CommandHandler handler)
    {
        this.help = help;
        this.handler = handler;
        thread = new Thread(this::run, "CommandShell");
        thread.setDaemon(true);
    }

    /** Start the command loop thread */
    public void start()
    {
        thread.start();
    }

    /** Stop the command loop thread */
    public void stop()
    {
        thread.interrupt();
    }

    /** Get the prompt string. */
    public String getPrompt()
    {
        return prompt;
    }

    /** Set the prompt string. */
    public void setPrompt(final String newPrompt)
    {
        this.prompt = newPrompt;
    }

    private void run()
    {
        System.out.println(help);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try
        {
            while (! thread.isInterrupted())
            {
                System.out.print(prompt + " > ");
                String line = reader.readLine();
                if (line == null)
                {
                    System.out.println("\nEnd of input, exiting command shell.");
                    try
                    {
                        handler.handle(null);
                    }
                    catch (Throwable ex)
                    {
                        ex.printStackTrace();
                    }
                    return;
                }
                line = line.trim();
                if (line.isEmpty())
                    continue;
                try
                {
                    if (! handler.handle(line.split("[ \t]")))
                        System.out.println(help);
                }
                catch (Throwable ex)
                {
                    ex.printStackTrace();
                }
            }
        }
        catch (Exception ex)
        {
            Logger.getLogger(getClass().getPackageName())
                  .log(Level.SEVERE, "Quitting CommandShell", ex);
        }
    }
}
