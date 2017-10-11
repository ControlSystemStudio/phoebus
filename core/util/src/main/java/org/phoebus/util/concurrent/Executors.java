/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * Factory and utility methods to for the Executor framework.
 *
 * @author carcassi
 */
public class Executors {

    private static final Logger log = Logger.getLogger(Executors.class.getName());

    /**
     * Executes tasks on the Swing Event Dispatch Thread using
     * SwingUtilities.invokeLater().
     *
     * @return an executor that posts events on the EDT
     */
    public static Executor swingEDT() {
        return SWING_EXECUTOR;
    }

    /**
     * Executes tasks on the current thread.
     *
     * @return an object that runs tasks on the current thread
     */
    public static Executor localThread() {
        return CURRENT_EXECUTOR;
    }

    private static final Executor SWING_EXECUTOR = new Executor() {

        @Override
        public void execute(Runnable command) {
            SwingUtilities.invokeLater(command);
        }
    };

    private static final Executor CURRENT_EXECUTOR = new Executor() {

        @Override
        public void execute(Runnable command) {
            try {
                command.run();
            } catch (Exception ex) {
                log.log(Level.WARNING, "Exception on the timer thread caused by a ValueListener", ex);
            } catch (AssertionError ex) {
                log.log(Level.WARNING, "Assertion failed on the timer thread", ex);
            }
        }
    };


    /**
     * A thread factory where each new thread starts with the given name. The
     * name of the thread will be poolname + number. This can be used instead
     * of {@link java.util.concurrent.Executors#defaultThreadFactory()},
     * which unfortunately
     * only use generic names for the thread, which makes it harder to debug.
     *
     * @param poolName name of the pool
     * @return a new factory
     */
    public static ThreadFactory namedPool(String poolName) {
        return new DefaultThreadFactory(poolName);
    }

    /**
     * Taken from {@link Executors#defaultThreadFactory() }.
     */
    static class DefaultThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory(String poolName) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null)? s.getThreadGroup() :
                                 Thread.currentThread().getThreadGroup();
            namePrefix = poolName;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                                  namePrefix + threadNumber.getAndIncrement(),
                                  0);
            if (!t.isDaemon())
                t.setDaemon(true);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}
