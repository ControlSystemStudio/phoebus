/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm.server.actions;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.logging.Level;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreeLeaf;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.model.TitleDetailDelay;
import org.phoebus.framework.jobs.NamedThreadFactory;

/** Handler of automated actions
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AutomatedActions
{
    /** Timer shared by all automated actions */
    private static final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("AutomatedActions"));

    /** Item for which to handle automated actions */
    private final AlarmTreeItem<?> item;

    /** Severity level that was notified */
    private final AtomicReference<SeverityLevel> notified_severity;

    /** Actions that have been scheduled with the timer */
    private final ConcurrentHashMap<TitleDetailDelay, ScheduledFuture<?>> scheduled_actions = new ConcurrentHashMap<>(1);

    /** Will be invoked to actually perform one of the item's actions */
    private final BiConsumer<AlarmTreeItem<?>, TitleDetailDelay> perform_action;

    /** Actions that have been performed and need a follow-up when the alarm clears
     *  SYNC on access because of atomic get-all-and-clear
     */
    private final List<TitleDetailDelay> performed_actions = new ArrayList<>();

    /** Handle automated actions for one item
     *  @param item Item for which automated actions should be handled
     *  @param initial_severity Initial alarm severity (which is ignored)
     *  @param perform_action Will be invoked to actually perform the action for an item
     */
    public AutomatedActions(final AlarmTreeItem<?> item,
                            final SeverityLevel initial_severity,
                            final BiConsumer<AlarmTreeItem<?>, TitleDetailDelay> perform_action)
    {
        this.item = item;
        this.notified_severity = new AtomicReference<>(initial_severity);
        this.perform_action = perform_action;

        // If item is PV, and not enabled, this should never be called
        if (item instanceof AlarmTreeLeaf   &&  ! ((AlarmTreeLeaf) item).isEnabled())
            throw new IllegalStateException();
    }

    /** Handle a severity update
     *
     *  <p>Check if actions need to be scheduled/triggered/cancelled
     *
     *  @param severity Most recent alarm severity of the item
     */
    public void handleSeverityUpdate(final SeverityLevel severity)
    {
        final boolean is_active = severity.isActive();

        if (is_active)
        {
            // Is this a change to a higher alarm?
            final SeverityLevel was = notified_severity.getAndUpdate(prev ->
            {
                if (severity.ordinal() > prev.ordinal())
                    return severity;
                return prev;
            });

            if (severity == was)
            {
                logger.log(Level.FINE, () -> item.getPathName() + " no change in auto action from " + was + " to " + severity);
                return;
            }
            logger.log(Level.FINE, () -> item.getPathName() + " auto action triggered by going from " + was + " to " + severity);
        }
        else
        {
            if (notified_severity.getAndSet(SeverityLevel.OK) == SeverityLevel.OK)
            {
                logger.log(Level.FINE, () -> item.getPathName() + " no change in auto action for " + severity);
                return;
            }
        }

        if (is_active)
        {
            for (TitleDetailDelay action : item.getActions())
            {
                if (action.detail.startsWith(TitleDetailDelay.SEVRPV))
                    continue;

                // Schedule action to be executed unless already scheduled
                scheduled_actions.computeIfAbsent(action, a ->
                {
                    final Runnable trigger_action = () ->
                    {
                        // Cancelled, i.e. removed from scheduled_actions, just before we 'run'?
                        if (scheduled_actions.remove(a) == null)
                            logger.log(Level.INFO, item.getPathName() + ": Aborting execution of cancelled action " + a);
                        else
                        {
                            // Perform the action
                            perform_action.accept(item, a);

                            // Is follow up is requested for this type of action?
                            for (String followup : AlarmSystem.automated_action_followup)
                                if (action.detail.startsWith(followup))
                                {
                                    synchronized (performed_actions)
                                    {
                                        performed_actions.add(action);
                                    }
                                    logger.log(Level.FINE, () -> "Will follow up on " + action + " when alarm clears");
                                    break;
                                }
                        }
                    };
                    logger.log(Level.INFO, item.getPathName() + ": Schedule " + a.title + " in " + a.delay + " s");
                    return timer.schedule(trigger_action, a.delay, TimeUnit.SECONDS);
                });
            }
        }
        else
        {
            // Cancel all scheduled actions
            cancelScheduled();

            logger.log(Level.FINE, () -> item.getPathName() + ": Clearing auto actions");

            // Follow up on actions that have been executed and now need an "It's OK"
            final List<TitleDetailDelay> follow_up;
            synchronized (performed_actions)
            {
                // Exit ASAP if nothing to do
                if (performed_actions.isEmpty())
                    return;
                // Atomically get-and-clear the actions on which to follow up
                follow_up = new ArrayList<>(performed_actions);
                performed_actions.clear();
            }
            // Invokes the action again. Action will notice that the item is right now OK
            // and can act accordingly by for example creating a differently worded email.
            for (TitleDetailDelay action : follow_up)
            {
                logger.log(Level.INFO, item.getPathName() + ": Follow up since alarm no longer active");
                perform_action.accept(item, action);
            }
        }
    }

    /** Cancel/clear all scheduled actions */
    private void cancelScheduled()
    {
        // Cancel/clear all scheduled actions
        scheduled_actions.forEach((action, scheduled) ->
        {
            scheduled.cancel(false);
            logger.log(Level.INFO, item.getPathName() + ": Cancelled");
            scheduled_actions.remove(action);
        });
    }

    /** Cancel all scheduled actions and forget about follow-up */
    public void cancel()
    {
        cancelScheduled();
        synchronized (performed_actions)
        {
            performed_actions.clear();
        }
    }
}
