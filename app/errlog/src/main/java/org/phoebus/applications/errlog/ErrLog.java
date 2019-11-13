/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.errlog;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

/** Error log
 *
 *  <p>Interposes stdout and stderr.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ErrLog implements Closeable
{
    /** Root logger for replacing/restoring {@link ConsoleHandler} */
    private static Logger root_logger = Logger.getLogger("");

    private Handler orig_console_handler = null,
                    repl_console_handler = null;

    /** Original output stream */
    private final PrintStream stdout = new PrintStream(new FileOutputStream(FileDescriptor.out));

    /** Original error stream */
    private final PrintStream stderr = new PrintStream(new FileOutputStream(FileDescriptor.err));

    /** Interpose for stdout resp. stderr */
    // Tried to implement using PipedInputStream & PipedOutputStream,
    // but those are expected to be called by only one thread each.
    // Implementations fetches `Thread.currentThread()` when written,
    // and reader then checks at certain times if that thread is still alive.
    // When the redirected System.out is thus called from a short-term thread,
    // the pipe will report "Write end dead" or "Pipe broken" once that thread exits.
    // https://stackoverflow.com/questions/1866255/pipedinputstream-how-to-avoid-java-io-ioexception-pipe-broken/1867063#1867063
    // https://bugs.openjdk.java.net/browse/JDK-4028322
    private final LineDetectingOutputInterpose outlogger, errlogger;

    public ErrLog(final Consumer<String> handle_stdout, final Consumer<String> handle_stderr) throws Exception
    {
        outlogger = new LineDetectingOutputInterpose(stdout, handle_stdout);
        System.setOut(new PrintStream(outlogger));

        errlogger = new LineDetectingOutputInterpose(stderr, handle_stderr);
        System.setErr(new PrintStream(errlogger));

        // If there is a ConsoleHandler,
        // it's been set to the original System.err.
        for (Handler handler : root_logger.getHandlers())
            if (handler instanceof ConsoleHandler)
            {
                orig_console_handler = handler;
                break;
            }
        if (orig_console_handler != null)
        {
            // Remove that one...
            root_logger.removeHandler(orig_console_handler);
            // .. and install a new one that uses the updated System.err
            repl_console_handler = new ConsoleHandler();
            repl_console_handler.setLevel(orig_console_handler.getLevel());
            repl_console_handler.setFormatter(orig_console_handler.getFormatter());
            root_logger.addHandler(repl_console_handler);
        }
    }

    @Override
    public void close()
    {
        // Restore ConsoleHandler
        if (repl_console_handler != null)
        {
            root_logger.removeHandler(repl_console_handler);
            repl_console_handler.close();
            repl_console_handler = null;
        }
        if (orig_console_handler != null)
        {
            root_logger.addHandler(orig_console_handler);
            orig_console_handler = null;
        }

        // Restore original std, err streams
        System.setOut(stdout);
        System.setErr(stderr);
    }
}
