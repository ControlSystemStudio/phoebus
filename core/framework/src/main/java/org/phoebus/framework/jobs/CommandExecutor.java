/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.jobs;

import static org.phoebus.framework.jobs.JobManager.logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/** Execute external command
 *
 *  <p>Logs standard output of the command as INFO,
 *  error output as warning.
 *
 *  <p>For briefly running commands, it awaits the exit status
 *  and then returns the exit code.
 *
 *  <p>For longer running commands, the logging remains active
 *  but the call returns <code>null</code> since the exit code is not known.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class CommandExecutor implements Callable<Integer>
{
    /** Seconds to wait for a launched program */
    private static final int WAIT_SECONDS = 5;

    private final ProcessBuilder process_builder;
    private volatile Process process;

    public CommandExecutor(final String cmd, final File directory)
    {
        final List<String> cmd_parts = splitCmd(cmd);

        if (cmd_parts.size() > 0)
        {
            // ProcessBuilder executes commands that are on the PATH, fine.
            // When the command is in the 'directory',
            // it requires using "./the_command" to invoke a command right there.
            // Keep users from having to add "./" by doing that for them.
            final File full_path = new File(directory, cmd_parts.get(0));
            if (full_path.canExecute())
                cmd_parts.set(0, full_path.toString());
        }
        process_builder = new ProcessBuilder(cmd_parts).directory(directory);
    }

    /** Split command into items, honoring double quotes
     *  (no 'escape', no single quotes)
     *  @param cmd "cmd arg1 \"another arg\""
     *  @return [ "cmd", "arg1", "another arg" ]
     */
    public static List<String> splitCmd(final String cmd)
    {
        final List<String> items = new ArrayList<>();
        final int len = cmd.length();
        int i = 0;
        final StringBuilder line = new StringBuilder();
        while (i < len)
        {
            char c = cmd.charAt(i);
            if (c == ' '  ||  c == '\t')
            {
                items.add(line.toString());
                line.delete(0, line.length());
                do
                    ++i;
                while (i < len  &&
                       (cmd.charAt(i) == ' '  ||  cmd.charAt(i) == '\t'));
            }
            else if (c == '"')
            {
                ++i;
                while (i < len  &&  cmd.charAt(i) != '"')
                    line.append(cmd.charAt(i++));
                if (i < len  &&  cmd.charAt(i) == '"')
                    ++i;
            }
            else
            {
                line.append(c);
                ++i;
            }
        }
        if (line.length() > 0)
            items.add(line.toString());
        return items;
    }

    /** Invoke the command.
     *  @return Return code of the command, or <code>null</code> if left running
     */
    @Override
    public Integer call() throws Exception
    {
        // Get 'basename' of command
        String cmd = process_builder.command().get(0);
        final int sep = cmd.lastIndexOf('/');
        if (sep >= 0)
            cmd = cmd.substring(sep+1);

        process = process_builder.start();
        // Send stdout and error output to log
        final Thread stdout = new LogWriter(process.getInputStream(), cmd, Level.INFO);
        final Thread stderr = new LogWriter(process.getErrorStream(), cmd, Level.WARNING);
        stdout.start();
        stderr.start();

        // Wait for some time...
        if (process.waitFor(WAIT_SECONDS, TimeUnit.SECONDS))
        {   // Process completed, await exit of log watching threads
            stderr.join();
            stdout.join();
            // Check exit code
            final int status = process.exitValue();
            if (status != 0)
                logger.log(Level.WARNING, "Command {0} exited with status {1}",  new Object[] { process_builder.command(), status });
            return status;
        }

        // Java 9 would allow running something when process finally quits,
        // but LogWriter exit on their own, so no more cleanup to do?
        // process.onExit().thenRun(() -> {});

        // Leave running, continuing to log outputs, but no longer checking status
        return null;
    }

    /** @return Is the process still active? */
    public boolean isActive()
    {
        final Process p = process;
        return p != null  &&  p.isAlive();
    }

    @Override
    public String toString()
    {
        final Process p = process;
        if (p == null)
            return "CommandExecutor (idle): " +  process_builder.command().get(0);
        else if (p.isAlive())
            return "CommandExecutor (running, PID " + p.pid() + "): " +  process_builder.command().get(0);
        else
            return "CommandExecutor (" + p.exitValue() + "): " +  process_builder.command().get(0);
    }
}
