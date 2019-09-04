/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.console;

import static org.phoebus.applications.console.Console.logger;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.phoebus.framework.jobs.CommandExecutor;

/** Wrap process that executes external command
 *
 *  <p>Captures standard and error output messages,
 *  allows sending text to the command's input.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ProcessWrapper
{
    /** Thread that reads lines from a stream */
    private class StreamReaderThread extends Thread
    {
        private final BufferedReader reader;
        private final Consumer<String> line_handler;

        /** @param stream Stream to read
         *  @param line_handler Handler to invoke with each received line
         */
        public StreamReaderThread(final InputStream stream, final Consumer<String> line_handler)
        {
            reader = new BufferedReader(new InputStreamReader(stream));
            this.line_handler = line_handler;
            setDaemon(true);
        }

        @Override
        public void run()
        {
            try
            {
                String line;
                while ((line = reader.readLine()) != null)
                    line_handler.accept(line);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Error reading process output", ex);
            }
        }
    }

    private final Consumer<String> on_output, on_error;
    private final Runnable on_exit;
    private final ProcessBuilder process_builder;
    private volatile Process process;
    private BufferedOutputStream process_input;

    public ProcessWrapper(final String cmd, final File directory,
                          final Consumer<String> on_output,
                          final Consumer<String> on_error,
                          final Runnable on_exit)
    {
        this.on_output = on_output;
        this.on_error = on_error;
        this.on_exit = on_exit;
        final List<String> cmd_parts = CommandExecutor.splitCmd(cmd);

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

    /** Start the process  */
    public void start() throws Exception
    {
        // Get 'basename' of command
        String cmd = process_builder.command().get(0);
        final int sep = cmd.lastIndexOf('/');
        if (sep >= 0)
            cmd = cmd.substring(sep+1);

        process = process_builder.start();

        // Send stdout and error output to handlers
        final Thread stdout = new StreamReaderThread(process.getInputStream(), on_output);
        final Thread stderr = new StreamReaderThread(process.getErrorStream(), on_error);
        stdout.start();
        stderr.start();

        process_input = new BufferedOutputStream(process.getOutputStream());

        // Register exit handler
        process.onExit()
               .thenRun(() ->
               {
                   try
                   {
                       process_input.close();
                   }
                   catch (IOException ex)
                   {
                       // Ignore, closing anyway
                   }
               })
               .thenRun(on_exit);
    }

    public void sendInput(final String input)
    {
        try
        {
            process_input.write(input.getBytes());
            process_input.write('\n');
            process_input.flush();
        }
        catch (IOException ex)
        {
            logger.log(Level.WARNING, "Cannot write to console process", ex);
        }
    }

    @Override
    public String toString()
    {
        final Process p = process;
        if (p == null)
            return "ProcessWrapper (idle): " +  process_builder.command().get(0);
        else if (p.isAlive())
            return "ProcessWrapper (running, PID " + p.pid() + "): " +  process_builder.command().get(0);
        else
            return "ProcessWrapper (" + p.exitValue() + "): " +  process_builder.command().get(0);
    }
}
