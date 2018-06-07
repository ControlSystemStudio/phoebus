/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.jobs;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.phoebus.framework.jobs.JobManager.logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.junit.Before;
import org.junit.Test;

/** JUnit test of the CommandExecutor
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class CommandExecutorTest
{
    /** Example shell scripts are only functional on Linux and Mac OS X.
     *  Skip tests on Windows.
     */
    private static final boolean is_windows = System.getProperty("os.name").startsWith("Windows");

    // Capture log messages
    private ByteArrayOutputStream log_buf;
    private StreamHandler handler;

    // Examples directory
    private File examples_dir;

    @Before
    public void setup()
    {
        // Setup Logger
        // 1-date, 2-source, 3-logger, 4-level, 5-message, 6-thrown
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
        log_buf = new ByteArrayOutputStream();
        handler = new StreamHandler(log_buf, new SimpleFormatter());
        logger.addHandler(handler);

        // Locate examples via class
        examples_dir = new File(CommandExecutorTest.class.getResource("/cmds").getPath().replace("file:", ""));

        // Change either framework/target/display-runtime*.jar!/cmds
        // or            framework/target/classes/cmds
        // into the source location
        examples_dir = new File(examples_dir.getParentFile().getParentFile().getParentFile(), "src/test/resources/cmds");

        System.out.println("Examples directory: " + examples_dir);
    }

    public String getLoggedMessages()
    {
        handler.flush();
        return log_buf.toString();
    }

    @Test
    public void testCommandSplit() throws Exception
    {
        List<String> cmd = CommandExecutor.splitCmd("path/cmd");
        System.out.println(cmd);
        assertThat(cmd, equalTo(Arrays.asList("path/cmd")));

        cmd = CommandExecutor.splitCmd("path/cmd arg1");
        System.out.println(cmd);
        assertThat(cmd, equalTo(Arrays.asList("path/cmd", "arg1")));

        cmd = CommandExecutor.splitCmd("path/cmd arg1 arg2");
        System.out.println(cmd);
        assertThat(cmd, equalTo(Arrays.asList("path/cmd", "arg1", "arg2")));

        cmd = CommandExecutor.splitCmd("path/cmd \"one arg\"");
        System.out.println(cmd);
        assertThat(cmd, equalTo(Arrays.asList("path/cmd", "one arg")));

        cmd = CommandExecutor.splitCmd("path/cmd \"one arg\" arg2 arg3");
        System.out.println(cmd);
        assertThat(cmd, equalTo(Arrays.asList("path/cmd", "one arg", "arg2", "arg3")));
    }

    @Test
    public void testShortCommand() throws Exception
    {
        if (is_windows)
            return;

        final String cmd = "./cmd_short.sh \"With one arg\" another_arg";
        final Integer status = new CommandExecutor(cmd, examples_dir).call();

        final String log = getLoggedMessages();
        assertThat(log, containsString("Example warning"));
        assertThat(log, containsString("2 arguments"));
        assertThat(log, containsString("Finished OK"));
        assertThat(status, equalTo(0));
    }

    @Test
    public void testErrorCommand() throws Exception
    {
        if (is_windows)
            return;

        // Start one directory 'up' to generate error
        final Integer status = new CommandExecutor("cmds/cmd_short.sh", examples_dir.getParentFile()).call();

        final String log = getLoggedMessages();
        assertThat(log, containsString("Wrong directory"));
        assertThat(log, containsString("exited with status 2"));
        assertThat(status, equalTo(2));
    }

    private static int countLogWriters()
    {
        int log_writers = 0;
        final int count = Thread.activeCount();
        System.out.println("Checking " + count + " active threads");
        Thread[] threads = new Thread[2*count];
        Thread.enumerate(threads);
        for (Thread thread : threads)
            if (thread != null  &&  thread.getName().contains("LogWriter"))
            {
                System.out.println("Found " + thread.getName());
                ++log_writers;
            }
        return log_writers;
    }

    @Test
    public void testLongCommand() throws Exception
    {
        if (is_windows)
            return;

        final CommandExecutor executor = new CommandExecutor("./cmd_long.sh", examples_dir);
        System.out.println(executor);
        assertThat(executor.toString(), containsString("(idle)"));
        final Integer status = executor.call();

        // No exit status since process remains running
        System.out.println(executor + " call() returned " + status);
        assertThat(executor.toString(), containsString("(running"));
        assertThat(status, nullValue());

        // Messages continue to be logged
        assertThat(countLogWriters(), equalTo(2));

        // Wait for external process to end
        String log = getLoggedMessages();
        int wait = 20;
        while (! log.contains("Finished OK"))
        {
            TimeUnit.SECONDS.sleep(1);
            log = getLoggedMessages();
            if (--wait < 0)
                throw new TimeoutException();
        }
        // After the long command printed "Finished OK",
        // the process might still need a little time to quit.
        // Without this wait, the next assert would sometimes fail in unit tests.
        wait = 5;
        while (executor.isActive())
        {
            TimeUnit.SECONDS.sleep(1);
            if (--wait < 0)
                throw new TimeoutException();
        }
        System.out.println(executor);
        assertThat(executor.toString(), containsString("(0)"));

        // Wait a little longer to allow checking in debugger
        // that the LogWriter threads exit
        TimeUnit.SECONDS.sleep(5);

        assertThat(countLogWriters(), equalTo(0));
    }
}
