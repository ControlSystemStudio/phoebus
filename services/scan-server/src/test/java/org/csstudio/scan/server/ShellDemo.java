/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SourceCodeAnalysis.CompletionInfo;

/** Demo of the Java 9 shell, {@link JShell}
 *
 *  <p>In principle, would be a nice shell for the 'console'
 *  of a terminal application, but not possible.
 *
 *  <p>As a pratical matter, the code for command completion etc.
 *  is in jdk.internal.jshell.tool.*, an inaccessible package.
 *  Only the basic JShell is accessible, need to implement
 *  the line reader etc.
 *
 *  <p>Most important, the {@link JShell} runs out of process,
 *  with its own class loader, and objects in the 'main' program
 *  are separate from objects in the shell,
 *  as this demo shows where the main program creates several
 *  {@link Demo} instances, but when the shell creates more,
 *  those start counting back up from 1 because the shell is
 *  a separate process.
 *
 *  <p>The {@link JShell} is thus not useable as a console
 *  for interacting with a terminal tool
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ShellDemo
{
    // Demo for a custom class
    public static class Demo
    {
        private static final AtomicInteger count = new AtomicInteger();

        private final int instance;

        public Demo()
        {
            instance = count.incrementAndGet();
            System.out.println("Created instance " + instance + " of 'Demo'");
        }

        @Override
        public String toString()
        {
            return "Demo class #" + instance;
        }
    }

    public static void main(String[] args) throws Exception
    {
        final AtomicBoolean run = new AtomicBoolean(true);

        // This code can of course access the custom class
        System.out.println("Creating some 'Demo' instances:");
        new Demo();
        new Demo();
        new Demo();

        final JShell shell = JShell.create();
        shell.onSnippetEvent(event ->
        {
            // System.out.println("Event: " + event);
            if (event.value() != null)
                System.out.println("==> " + event.value());

            if (event.status() == Snippet.Status.REJECTED)
            {
                System.err.println(event.status() + ":");
                System.err.println(event.snippet());
            }
            if (event.exception() != null)
            {
                System.err.println("Exception:");
                System.err.println(event.exception());
                event.exception().printStackTrace(System.err);
            }
        });
        shell.onShutdown(sh ->
        {
            System.out.println("Shell shut down");
            run.set(false);
        });

        final String path = ShellDemo.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        System.out.println("Adding this path to shell so it can locate the 'Demo':");
        System.out.println(path);
        shell.addToClasspath(path);

        System.out.println("Created shell");
        shell.eval("2+3");
        shell.eval("System.out.println(\"hi!\")");

        System.out.println("* Interactive loop");
        System.out.println("* Examples to try:");
        System.out.println();
        System.out.println("   2 + 4");
        System.out.println();
        System.out.println("   2 + ");
        System.out.println("   5 ");
        System.out.println();
        System.out.println("   import org.csstudio.scan.server.ShellDemo.Demo");
        System.out.println("   Demo d = new Demo()");
        System.out.println("   d");
        System.out.println();
        System.out.println("Note that these demo instances will count back up from 1,");
        System.out.println("because the JShell runs as its own process.");
        System.out.println();
        System.out.println("   System.exit(2)");
        System.out.println();

        String text = "";
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (run.get())
        {
            if (text.isEmpty())
                System.out.print("jshell> ");
            else
                System.out.print("   ...> ");
            final String line = reader.readLine();
            if (line == null)
                break;

            if (text.isEmpty())
                text = line;
            else
                text = text + "\n" + line;

            final CompletionInfo info = shell.sourceCodeAnalysis().analyzeCompletion(text);
            if (info.completeness().isComplete())
            {
                shell.eval(info.source());
                text = info.remaining();
            }
        }
        shell.close();
        System.out.println("Done");
    }
}
