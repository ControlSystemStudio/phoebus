/*******************************************************************************
 * Copyright (c) 2018-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.annunciator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.phoebus.applications.alarm.model.SeverityLevel;

/** Controller class for an annunciator.
 *
 *  <p>Annunciates messages in order.
 *  If several messages queue up, "There are N new messages"
 *  will be spoken instead of spending time with each message.
 *
 *  <p>Messages marked as 'standout' will always be annunciated regardless of queue size.
 *
 *  <p>Previous versions re-ordered by severity, but that turned out to be confusing.
 *  Imagine "MINOR Alarm XYZ" followed by "MAJOR Alarm XYZ".
 *  If re-ordered by severity, they're annunciated out of time order
 *  which doesn't make sense to users.
 *
 *  @author Evan Smith
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AnnunciatorController
{
    /** Special message sent to request shutdown */
    private static final AnnunciatorMessage LAST_MESSAGE = new AnnunciatorMessage(false, SeverityLevel.OK, null, null);

    /** Maximum number of messages to queue before summarizing */
    private final int threshold;
    private final Consumer<AnnunciatorMessage> addToTable;

    private final BlockingQueue<AnnunciatorMessage> to_annunciate = new LinkedBlockingQueue<>();

    private final Annunciator annunciator = new Annunciator();
    private final Thread process_thread = new Thread(this::processMessages, "Annunciator");

    // Muted _IS_ read from multiple threads, so it should always be fetched from memory.
    private volatile boolean muted = false;

    /** Create AnnunciatorController.
     *  @param threshold - Integer value that the length of the queue should not exceed.
     *  @param addToTable - callback to add the message to the annunciator table.
     */
    public AnnunciatorController(final int threshold, final Consumer<AnnunciatorMessage> addToTable)
    {
        this.threshold = threshold;
        this.addToTable = addToTable;

        // The thread should exit when requested by shutdown() call, but set to daemon so it dies
        // when program closes regardless.
        process_thread.setDaemon(true);
        process_thread.start();
    }

    /** @param message Message to annunciate */
    public void annunciate(final AnnunciatorMessage message)
    {
        to_annunciate.offer(message);
    }

    /** @param val Mute the annunciator? */
    public void setMuted(final boolean val)
    {
        muted = val;
    }

    private void processMessages()
    {
        final List<AnnunciatorMessage> batch = new ArrayList<>();

        // Process new messages until receiving LAST_MESSAGE
        while (true)
        {
            // Fetch batch of pending messages
            batch.clear();
            {
                AnnunciatorMessage message;
                try
                {
                    // Wait for the first message
                    message = to_annunciate.take();
                }
                catch (InterruptedException ex)
                {
                    return;
                }

                // Gather all messages that have accumulated
                while (message != null)
                {
                    if (message == LAST_MESSAGE)
                        return;
                    batch.add(message);
                    message = to_annunciate.poll();
                }
            }

            // Simply annunciate up to threshold
            if (batch.size() <= threshold)
            {
                for (AnnunciatorMessage message : batch)
                {
                    addToTable.accept(message);
                    if (! muted)
                        annunciator.speak(message.message);
                }
            }
            else
            {   // Above threshold, annunciate only stand out messages
                int flurry = 0;
                Instant earliest = Instant.now();
                for (AnnunciatorMessage message : batch)
                {
                    if (earliest.isBefore(message.time))
                        earliest = message.time;
                    if (message.standout)
                    {   // Annunciate if marked as stand out.
                        addToTable.accept(message);
                        if (! muted)
                            annunciator.speak(message.message);
                    }
                    else
                    {   // Increment count of non stand out messages.
                        message.message += " (skipped)";
                        addToTable.accept(message);
                        flurry++;
                    }
                }
                if (flurry > 0)
                {   // Replace rest with message count
                    final AnnunciatorMessage message = new AnnunciatorMessage(false, null, earliest, "There are " + flurry + " new messages");
                    addToTable.accept(message);
                    if (! muted)
                        annunciator.speak(message.message);
                }
            }
        }
    }

    /** Shutdown the annunciator controller.
     *  @throws InterruptedException
     */
    public void shutdown() throws InterruptedException
    {
        // Send magic message that wakes annunciatorThread and causes it to exit
        to_annunciate.offer(LAST_MESSAGE);
        // The thread should shutdown
        process_thread.join(2000);

        // Deallocate the annunciator's voice.
        annunciator.shutdown();
    }
}
