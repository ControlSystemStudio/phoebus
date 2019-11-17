/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.errlog;

import java.io.Closeable;
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
    /** Default std/err handler that ignores the recognized lines */
    private static final Consumer<String> ignore = line -> {};

    /** Handler for recognized lines sent to stdout resp. stderr */
    private static volatile Consumer<String> handle_stdout = ignore, handle_stderr = ignore;

    /** Replacement {@link ConsoleHandler} that uses interpose */
    private static Handler repl_console_handler = null;

    /** Prepare for using ErrLog
     *
     *  <p>Must be called early in the application startup process,
     *  before other libraries latch onto the current `System.out`
     *  or `System.err` streams and would thus ignore our later
     *  efforts to replace them with an interpose.
     */
    public static void prepare()
    {
        // In principle we can take a copy of the current System.out and System.err,
        // later replace them with an interpose,
        // and finally either restore the original values,
        // or use the equivalent new PrintStream(new FileOutputStream(FileDescriptor.out));
        //
        // But other libraries might latch on to System.out at any time,
        // so they would ignore our interpose replacement,
        // or keep writing to the interpose long after the ErrLog has been closed
        // and we restored the original System.out/err.
        //
        // Therefore install an interpose early on, once and for all.
        //
        // Note that these interposes will use the value of handle_stdout & handle_stderr
        // that's current whenever they're executing, i.e. those handlers
        // can change while the out and err streams remain unchanged.
        System.setOut(new PrintStream(new LineDetectingOutputInterpose(System.out, line -> handle_stdout.accept(line))));
        System.setErr(new PrintStream(new LineDetectingOutputInterpose(System.err, line -> handle_stderr.accept(line))));
    }

    /** Enable message line capture
     *  @param handle_stdout Called with each stdout line
     *  @param handle_stderr Called with each stderr line
     *  @throws Exception
     */
    public ErrLog(final Consumer<String> handle_stdout, final Consumer<String> handle_stderr) throws Exception
    {
        // Have interpose use the handlers
        ErrLog.handle_stdout = handle_stdout;
        ErrLog.handle_stderr = handle_stderr;

        // If prepare() is called before the Logger is ever used,
        // as in org.phoebus.applications.errlog.Demo,
        // then the ConsoleLogger also latches onto the prepared
        // interpose.
        // But when prepare() is called later, for example via
        // ErrLogApp.start(), the ConsoleHandler latched onto
        // the original System.err and needs to be replaced
        if (repl_console_handler == null)
        {
            Handler orig_console_handler = null;
            final Logger root_logger = Logger.getLogger("");
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
        // else: ConsoleHandler already replaced
    }

    @Override
    public void close()
    {
        // Restore original std, err streams
        handle_stdout = handle_stderr = ignore;

        // Keep ConsoleHandler replacement
    }
}
