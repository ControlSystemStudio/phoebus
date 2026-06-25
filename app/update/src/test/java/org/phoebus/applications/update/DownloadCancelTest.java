/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.update;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;
import org.phoebus.framework.jobs.BasicJobMonitor;

/** Verify the download cancellation behavior of {@link Update}.
 *
 *  <p>The download watcher used to force-cancel via the now-removed
 *  {@code Thread.stop()}. It now closes the source stream on 'cancel',
 *  which must unblock the in-progress {@code Files.copy} on both the
 *  JDK 21 target (where {@code Thread.stop()} throws at runtime) and
 *  newer JDKs (where it no longer exists at all).
 *
 *  @author Gianluca Martino
 */
@SuppressWarnings("nls")
public class DownloadCancelTest
{
    /** JobMonitor that always reports 'cancelled' */
    private static class CancelledMonitor extends BasicJobMonitor
    {
        @Override
        public boolean isCanceled()
        {
            return true;
        }
    }

    /** InputStream whose read() blocks until the stream is closed,
     *  emulating a slow network download stuck inside Files.copy.
     *  Records the name of the thread that first closed it so the
     *  test can confirm the watcher (not JUnit's timeout interrupt)
     *  performed the abort.
     */
    private static class BlockingStream extends InputStream
    {
        private final CountDownLatch closed = new CountDownLatch(1);
        private volatile String closing_thread = null;

        @Override
        public int read() throws IOException
        {
            try
            {
                closed.await();
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
            }
            throw new IOException("Stream closed");
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException
        {
            return read();
        }

        @Override
        public void close()
        {
            if (closing_thread == null)
                closing_thread = Thread.currentThread().getName();
            closed.countDown();
        }

        String getClosingThread()
        {
            return closing_thread;
        }
    }

    /** Minimal {@link Update} that streams the given bytes. */
    private static Update updateStreaming(final InputStream stream)
    {
        return new Update()
        {
            @Override
            protected Instant getVersion()
            {
                return Instant.now();
            }

            @Override
            protected Long getDownloadSize()
            {
                return 1000L;
            }

            @Override
            protected InputStream getDownloadStream()
            {
                return stream;
            }
        };
    }

    /** A cancelled download must abort instead of hanging,
     *  and the abort must be performed by the watcher thread.
     */
    @Test
    public void cancelAbortsDownload()
    {
        final BlockingStream stream = new BlockingStream();
        final Update update = updateStreaming(stream);

        // The monitor reports 'cancelled', so the watcher must close the
        // stream within its ~1s poll interval, making download() throw
        // instead of blocking forever. Generous timeout avoids CI flakiness.
        assertTimeoutPreemptively(Duration.ofSeconds(30), () ->
            assertThrows(IOException.class, () -> update.download(new CancelledMonitor())));

        // The abort must come from the watcher closing the stream, not from
        // JUnit's preemptive-timeout interrupt (which would mean it hung).
        assertEquals("Watch Download", stream.getClosingThread());
    }

    /** A normal (non-cancelled) download must complete and return the file. */
    @Test
    public void normalDownloadCompletes() throws Exception
    {
        final byte[] payload = "phoebus update payload".getBytes(StandardCharsets.UTF_8);
        final Update update = updateStreaming(new ByteArrayInputStream(payload));

        final File file = update.download(new BasicJobMonitor());
        try
        {
            assertArrayEquals(payload, Files.readAllBytes(file.toPath()));
        }
        finally
        {
            file.delete();
        }
    }
}
