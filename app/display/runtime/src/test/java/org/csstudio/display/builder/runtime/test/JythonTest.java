/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;
import org.python.core.PyCode;
import org.python.core.PySystemState;
import org.python.core.RegistryKey;
import org.python.util.PythonInterpreter;

/** Jython Demo
 *
 *  <p>This test only functions if it's the one that
 *  initializes the {@link PythonInterpreter}.
 *  If started in a VM where the interpreter has already
 *  been initialized, the "/tmp/always" entry will be
 *  missing from the python.path.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class JythonTest
{
    private final static int RUNTIME_SECONDS = 5;

    // Meant to check for https://github.com/ControlSystemStudio/cs-studio/issues/1687,
    // where presumably the following happened:
    //
    // 1) Jython is initialized, once
    // 2) When a Jython interpreter instance is created, it's configured with additional path elements
    // 3) When that Jython interpreter is later used, occasionally it seems to revert to the
    //    basic path from step 1, ignoring its configuration from step 2.
    //
    // The problem appears related to usage of ThreadLocal in org.python.core.ThreadStateMapping,
    // also mentioned in https://bugs.jython.org/issue2505 "PySystemState is lost".
    // Setting all path elements once in step 1 seems to fix the problem,
    // but it would be nice to demonstrate the original issue in a unit test.
    //
    // Unfortunately, very hard to duplicate. Only rarely see the test fail:
    //  Expected: a string containing "special"
    //  but: was "['/tmp/always', '/usr/local/hudson/config/jobs/CSS_display.builder/workspace/org.csstudio.display.builder.runtime.test/target/work/plugins/org.python.jython_2.7.0.release/Lib', '__classpath__', '__pyclasspath__/']"
    //   at org.hamcrest.MatcherAssert.assertThat(MatcherAssert.java:20)
    //   at org.junit.Assert.assertThat(Assert.java:956)
    //   at org.junit.Assert.assertThat(Assert.java:923)
    //   at org.csstudio.display.builder.runtime.test.JythonTest.testInterpreter(JythonTest.java:115) // old line number
    //   at org.csstudio.display.builder.runtime.test.JythonTest.lambda$2(JythonTest.java:136)        // old line number
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private void init()
    {
        // System.out.println("Initializing on " + Thread.currentThread().getName());
        final Properties pre_props = System.getProperties();
        final Properties props = new Properties();

        String home = PythonInterpreter.class
                                       .getProtectionDomain().getCodeSource().getLocation().toString();
        System.out.println("Jython home: " + home);
        assertThat(home, not(nullValue()));

        if (home.contains(".jar"))
            System.out.println("Jython provided as JAR");
        home = home.replace("file:", "");

        props.setProperty("python.home", home);
        // props.setProperty("python.executable", "None");
        props.setProperty(RegistryKey.PYTHON_CACHEDIR_SKIP, "true");
        props.setProperty("python.import.site", "false");
        // props.setProperty("python.console.encoding", "UTF-8");

        props.setProperty("python.path", "/tmp/always");
        // props.setProperty("python.verbose", "debug");
        // Options.verbose = Py.DEBUG;
        PythonInterpreter.initialize(pre_props, props, new String[0]);
    }

    private PythonInterpreter createInterpreter()
    {
        // System.out.println("Creating interpreter on " + Thread.currentThread().getName());

        final PySystemState state = new PySystemState();
        // Check for default settings
        if (! state.path.toString().contains("always"))
        {
            System.out.println("Not running in fresh VM, missing /tmp/always in " + state.path);
            state.path.add(0, "/tmp/always");
        }
        assertThat(state.path.toString(), not(containsString("special")));
        // Add settings specific to this interpreter
        state.path.add(0, "/tmp/special");
        return new PythonInterpreter(null, state);
    }

    private void testInterpreter(final PythonInterpreter python) throws Exception
    {
        // Run on new threads to get fresh thread-locals
        final ExecutorService new_executor = Executors.newCachedThreadPool();

        final String script = "import sys\nresult = sys.path";
        final Callable<String> test_run = () ->
        {
            // System.out.println("Executing on " + Thread.currentThread().getName());
            final PyCode code = python.compile(script);
            python.exec(code);
            return python.get("result").toString();
        };

        final List<Future<String>> futures = new ArrayList<>();
        final long end = System.currentTimeMillis() + 1000L;
        while (System.currentTimeMillis() < end)
        {
            for (int i=0; i<50; ++i)
                futures.add(new_executor.submit(test_run));
            for (Future<String> future : futures)
            {
                final String result = future.get();
                //System.out.println(result);
                assertThat(result, containsString("always"));
                assertThat(result, containsString("special"));
            }
            futures.clear();
        }
        new_executor.shutdown();
    }

    @Test
    public void testPathWithThreads() throws Exception
    {
        // Initialize on some other thread
        executor.submit(() -> init()).get();

        for (int i=0; i<RUNTIME_SECONDS; ++i)
        {
            final ExecutorService new_executor = Executors.newSingleThreadExecutor();
            new_executor.submit(() ->
            {
                final PythonInterpreter python = createInterpreter();
                try
                {
                    testInterpreter(python);
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
                python.close();
            }).get();
            new_executor.shutdown();
        }
    }
}
