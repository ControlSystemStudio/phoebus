package org.csstudio.trends.databrowser3.archive;

import org.csstudio.trends.databrowser3.model.ArchiveDataSource;
import org.csstudio.trends.databrowser3.model.PVItem;
import org.csstudio.trends.databrowser3.model.RequestType;
import org.csstudio.trends.databrowser3.preferences.Preferences;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.phoebus.archive.reader.ArchiveReader;
import org.phoebus.archive.reader.UnknownChannelException;
import org.phoebus.archive.reader.ValueIterator;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ArchiveFetchJobTest {

    // --- test-only subclass -------------------------------------------------

    /** Subclass that skips JobManager scheduling and injects fake readers. */
    static class TestableFetchJob extends ArchiveFetchJob {
        private final java.util.Map<String, ArchiveReader> readers = new java.util.LinkedHashMap<>();

        TestableFetchJob(PVItem item, Instant start, Instant end, ArchiveFetchJobListener listener) {
            super(item, start, end, listener, true);
        }

        void whenUrl(String url, ArchiveReader reader) {
            readers.put(url, reader);
        }

        @Override
        protected ArchiveReader openReader(String url) throws Exception {
            ArchiveReader r = readers.get(url);
            if (r == null)
                throw new Exception("No fake reader registered for URL: " + url);
            return r;
        }
    }

    // --- helpers ------------------------------------------------------------

    private static VType makeValue() {
        return VDouble.of(1.0, Alarm.none(), Time.now(), Display.none());
    }

    private static ValueIterator oneValueIterator() throws Exception {
        VType v = makeValue();
        ValueIterator it = mock(ValueIterator.class);
        when(it.hasNext()).thenReturn(true, false);
        when(it.next()).thenReturn(v);
        return it;
    }

    private static ArchiveReader readerReturning(ValueIterator valueIterator) throws Exception {
        ArchiveReader r = mock(ArchiveReader.class);
        when(r.getRawValues(any(), any(), any())).thenReturn(valueIterator);
        when(r.getOptimizedValues(any(), any(), any(), anyInt())).thenReturn(valueIterator);
        return r;
    }

    // --- tests --------------------------------------------------------------

    @Test
    @Timeout(5)
    void timeoutCancelsJob() throws Exception {
        PVItem item = new PVItem("TESTPV", 0.0);
        item.addArchiveDataSource(new ArchiveDataSource("src://slow", "Slow"));

        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        List<String> completed = Collections.synchronizedList(new ArrayList<>());
        ArchiveFetchJobListener listener = new ArchiveFetchJobListener() {
            @Override public void fetchCompleted(ArchiveFetchJob job) { completed.add("done"); }
            @Override public void archiveFetchFailed(ArchiveFetchJob job, ArchiveDataSource archive, Exception error) {
                errors.add(archive.getName());
            }
            @Override public void channelNotFound(ArchiveFetchJob job, boolean found, List<ArchiveDataSource> failed) {}
        };

        // Slow reader: blocks until released (simulates unresponsive archiver)
        CountDownLatch readerBlocking = new CountDownLatch(1);
        CountDownLatch releaseReader = new CountDownLatch(1);
        ValueIterator blockingIter = mock(ValueIterator.class);
        when(blockingIter.hasNext()).thenAnswer(inv -> {
            readerBlocking.countDown();
            releaseReader.await();
            return false;
        });
        ArchiveReader slowReader = readerReturning(blockingIter);

        try {
            TestableFetchJob job = new TestableFetchJob(item, Instant.now().minusSeconds(60), Instant.now(), listener);
            job.whenUrl("src://slow", slowReader);

            ArchiveFetchJob.WorkerThread worker = job.new WorkerThread();
            Thread t = new Thread(worker::run);
            t.start();

            readerBlocking.await();    // wait until fetch is stuck on slow source
            worker.cancel();           // simulate outer-loop timeout firing
            releaseReader.countDown(); // unblock the reader so it can exit cleanly

            t.join(3000);
            assertFalse(t.isAlive(), "WorkerThread should have exited after cancel");

            assertTrue(errors.isEmpty(), "cancel does not report a per-source error");
            assertTrue(completed.isEmpty(), "fetchCompleted is not called when job is cancelled");
        } finally {
            releaseReader.countDown(); // safety: unblock in case test fails early
        }
    }

    @Test
    @Timeout(5)
    void healthySourceCompletesWithinTimeout() throws Exception {
        PVItem item = new PVItem("TESTPV", 0.0);
        item.addArchiveDataSource(new ArchiveDataSource("src://fast", "Fast"));

        List<String> errors = new ArrayList<>();
        AtomicReference<ArchiveFetchJob> completedJob = new AtomicReference<>();
        ArchiveFetchJobListener listener = new ArchiveFetchJobListener() {
            @Override public void fetchCompleted(ArchiveFetchJob job) { completedJob.set(job); }
            @Override public void archiveFetchFailed(ArchiveFetchJob job, ArchiveDataSource archive, Exception error) {
                errors.add(error.getMessage());
            }
            @Override public void channelNotFound(ArchiveFetchJob job, boolean found, List<ArchiveDataSource> failed) {}
        };

        ValueIterator fastIter = oneValueIterator();
        ArchiveReader fastReader = readerReturning(fastIter);

        TestableFetchJob job = new TestableFetchJob(item, Instant.now().minusSeconds(60), Instant.now(), listener);
        job.whenUrl("src://fast", fastReader);

        ArchiveFetchJob.WorkerThread worker = job.new WorkerThread();
        worker.run();

        assertTrue(errors.isEmpty(), "no errors expected for healthy source, got: " + errors);
        assertNotNull(completedJob.get(), "fetchCompleted should have been called");
    }

    @Test
    @Timeout(5)
    void unknownChannelExceptionRoutesToChannelNotFound() throws Exception {
        PVItem item = new PVItem("TESTPV", 0.0);
        item.addArchiveDataSource(new ArchiveDataSource("src://unknown", "Unknown"));

        List<String> errors = new ArrayList<>();
        AtomicReference<Boolean> foundRef = new AtomicReference<>();
        ArchiveFetchJobListener listener = new ArchiveFetchJobListener() {
            @Override public void fetchCompleted(ArchiveFetchJob job) {}
            @Override public void archiveFetchFailed(ArchiveFetchJob job, ArchiveDataSource archive, Exception error) {
                errors.add(archive.getName());
            }
            @Override public void channelNotFound(ArchiveFetchJob job, boolean found, List<ArchiveDataSource> failed) {
                foundRef.set(found);
            }
        };

        ArchiveReader unknownReader = mock(ArchiveReader.class);
        when(unknownReader.getRawValues(any(), any(), any())).thenThrow(new UnknownChannelException("TESTPV"));
        when(unknownReader.getOptimizedValues(any(), any(), any(), anyInt())).thenThrow(new UnknownChannelException("TESTPV"));

        TestableFetchJob job = new TestableFetchJob(item, Instant.now().minusSeconds(60), Instant.now(), listener);
        job.whenUrl("src://unknown", unknownReader);

        job.new WorkerThread().run();

        assertNotNull(foundRef.get(), "channelNotFound should have been called");
        assertFalse(foundRef.get(), "found should be false when no source has the channel");
        assertTrue(errors.isEmpty(), "archiveFetchFailed must not be called for UnknownChannelException");
    }

    @Test
    @Timeout(5)
    void partialUnknownChannelReportsFoundTrue() throws Exception {
        PVItem item = new PVItem("TESTPV", 0.0);
        item.addArchiveDataSource(new ArchiveDataSource("src://unknown", "Unknown"));
        item.addArchiveDataSource(new ArchiveDataSource("src://ok", "Ok"));

        AtomicReference<Boolean> foundRef = new AtomicReference<>();
        AtomicReference<List<ArchiveDataSource>> failedRef = new AtomicReference<>();
        ArchiveFetchJobListener listener = new ArchiveFetchJobListener() {
            @Override public void fetchCompleted(ArchiveFetchJob job) {}
            @Override public void archiveFetchFailed(ArchiveFetchJob job, ArchiveDataSource archive, Exception error) {}
            @Override public void channelNotFound(ArchiveFetchJob job, boolean found, List<ArchiveDataSource> failed) {
                foundRef.set(found);
                failedRef.set(failed);
            }
        };

        ArchiveReader unknownReader = mock(ArchiveReader.class);
        when(unknownReader.getRawValues(any(), any(), any())).thenThrow(new UnknownChannelException("TESTPV"));
        when(unknownReader.getOptimizedValues(any(), any(), any(), anyInt())).thenThrow(new UnknownChannelException("TESTPV"));

        TestableFetchJob job = new TestableFetchJob(item, Instant.now().minusSeconds(60), Instant.now(), listener);
        job.whenUrl("src://unknown", unknownReader);
        job.whenUrl("src://ok", readerReturning(oneValueIterator()));

        job.new WorkerThread().run();

        assertNotNull(foundRef.get(), "channelNotFound should have been called");
        assertTrue(foundRef.get(), "found=true when channel exists in at least one source");
        assertEquals(1, failedRef.get().size());
        assertEquals("Unknown", failedRef.get().get(0).getName());
    }

    @Test
    @Timeout(5)
    void multipleSourcesAllSamplesComplete() throws Exception {
        PVItem item = new PVItem("TESTPV", 0.0);
        item.addArchiveDataSource(new ArchiveDataSource("src://a", "SourceA"));
        item.addArchiveDataSource(new ArchiveDataSource("src://b", "SourceB"));

        List<String> errors = new ArrayList<>();
        AtomicReference<ArchiveFetchJob> completedJob = new AtomicReference<>();
        ArchiveFetchJobListener listener = new ArchiveFetchJobListener() {
            @Override public void fetchCompleted(ArchiveFetchJob job) { completedJob.set(job); }
            @Override public void archiveFetchFailed(ArchiveFetchJob job, ArchiveDataSource archive, Exception error) {
                errors.add(error.getMessage());
            }
            @Override public void channelNotFound(ArchiveFetchJob job, boolean found, List<ArchiveDataSource> failed) {}
        };

        TestableFetchJob job = new TestableFetchJob(item, Instant.now().minusSeconds(60), Instant.now(), listener);
        job.whenUrl("src://a", readerReturning(oneValueIterator()));
        job.whenUrl("src://b", readerReturning(oneValueIterator()));

        job.new WorkerThread().run();

        assertTrue(errors.isEmpty(), "no errors expected for two healthy sources");
        assertNotNull(completedJob.get(), "fetchCompleted should be called after both sources succeed");
    }

    @Test
    @Timeout(5)
    void fetchExceptionCallsArchiveFetchFailed() throws Exception {
        PVItem item = new PVItem("TESTPV", 0.0);
        item.addArchiveDataSource(new ArchiveDataSource("src://broken", "Broken"));
        item.addArchiveDataSource(new ArchiveDataSource("src://ok", "Ok"));

        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<ArchiveFetchJob> completedJob = new AtomicReference<>();
        ArchiveFetchJobListener listener = new ArchiveFetchJobListener() {
            @Override public void fetchCompleted(ArchiveFetchJob job) { completedJob.set(job); }
            @Override public void archiveFetchFailed(ArchiveFetchJob job, ArchiveDataSource archive, Exception error) {
                errors.add(archive.getName());
            }
            @Override public void channelNotFound(ArchiveFetchJob job, boolean found, List<ArchiveDataSource> failed) {}
        };

        ArchiveReader brokenReader = mock(ArchiveReader.class);
        when(brokenReader.getRawValues(any(), any(), any())).thenThrow(new IOException("network error"));
        when(brokenReader.getOptimizedValues(any(), any(), any(), anyInt())).thenThrow(new IOException("network error"));

        TestableFetchJob job = new TestableFetchJob(item, Instant.now().minusSeconds(60), Instant.now(), listener);
        job.whenUrl("src://broken", brokenReader);
        job.whenUrl("src://ok", readerReturning(oneValueIterator()));

        job.new WorkerThread().run();

        assertEquals(List.of("Broken"), errors, "broken source should call archiveFetchFailed");
        assertNotNull(completedJob.get(), "fetch should complete after the error is reported");
    }

    @Test
    @Timeout(5)
    void cancelBeforeFirstFetchSkipsAll() throws Exception {
        PVItem item = new PVItem("TESTPV", 0.0);
        item.addArchiveDataSource(new ArchiveDataSource("src://any", "Any"));

        List<String> completed = new ArrayList<>();
        ArchiveFetchJobListener listener = new ArchiveFetchJobListener() {
            @Override public void fetchCompleted(ArchiveFetchJob job) { completed.add("done"); }
            @Override public void archiveFetchFailed(ArchiveFetchJob job, ArchiveDataSource archive, Exception error) {}
            @Override public void channelNotFound(ArchiveFetchJob job, boolean found, List<ArchiveDataSource> failed) {}
        };

        TestableFetchJob job = new TestableFetchJob(item, Instant.now().minusSeconds(60), Instant.now(), listener);
        ArchiveFetchJob.WorkerThread worker = job.new WorkerThread();
        worker.cancel(); // set cancelled=true before run() is called
        worker.run();

        assertTrue(completed.isEmpty(), "fetchCompleted must not be called when cancelled before start");
    }

    @Test
    @Timeout(5)
    void rawRequestTypeCallsGetRawValues() throws Exception {
        PVItem item = new PVItem("TESTPV", 0.0);
        item.setRequestType(RequestType.RAW);
        item.addArchiveDataSource(new ArchiveDataSource("src://any", "Any"));

        ArchiveFetchJobListener listener = new ArchiveFetchJobListener() {
            @Override public void fetchCompleted(ArchiveFetchJob job) {}
            @Override public void archiveFetchFailed(ArchiveFetchJob job, ArchiveDataSource archive, Exception error) {}
            @Override public void channelNotFound(ArchiveFetchJob job, boolean found, List<ArchiveDataSource> failed) {}
        };

        ArchiveReader mockReader = readerReturning(oneValueIterator());
        TestableFetchJob job = new TestableFetchJob(item, Instant.now().minusSeconds(60), Instant.now(), listener);
        job.whenUrl("src://any", mockReader);

        job.new WorkerThread().run();

        verify(mockReader).getRawValues(any(), any(), any());
        verify(mockReader, never()).getOptimizedValues(any(), any(), any(), anyInt());
    }

    @Test
    @Timeout(5)
    void optimizedRequestTypeCallsGetOptimizedValues() throws Exception {
        PVItem item = new PVItem("TESTPV", 0.0);
        item.setRequestType(RequestType.OPTIMIZED);
        item.addArchiveDataSource(new ArchiveDataSource("src://any", "Any"));

        ArchiveFetchJobListener listener = new ArchiveFetchJobListener() {
            @Override public void fetchCompleted(ArchiveFetchJob job) {}
            @Override public void archiveFetchFailed(ArchiveFetchJob job, ArchiveDataSource archive, Exception error) {}
            @Override public void channelNotFound(ArchiveFetchJob job, boolean found, List<ArchiveDataSource> failed) {}
        };

        ArchiveReader mockReader = readerReturning(oneValueIterator());
        TestableFetchJob job = new TestableFetchJob(item, Instant.now().minusSeconds(60), Instant.now(), listener);
        job.whenUrl("src://any", mockReader);

        job.new WorkerThread().run();

        verify(mockReader, never()).getRawValues(any(), any(), any());
        verify(mockReader).getOptimizedValues(any(), any(), any(), anyInt());
    }

    @Test
    @Timeout(5)
    void cancelInterruptsPendingFetch() throws Exception {
        PVItem item = new PVItem("TESTPV", 0.0);
        item.addArchiveDataSource(new ArchiveDataSource("src://slow", "Slow"));

        ArchiveFetchJobListener listener = new ArchiveFetchJobListener() {
            @Override public void fetchCompleted(ArchiveFetchJob job) {}
            @Override public void archiveFetchFailed(ArchiveFetchJob job, ArchiveDataSource archive, Exception error) {}
            @Override public void channelNotFound(ArchiveFetchJob job, boolean found, List<ArchiveDataSource> failed) {}
        };

        CountDownLatch readerBlocking = new CountDownLatch(1);
        CountDownLatch releaseReader = new CountDownLatch(1);
        ValueIterator blockingIter = mock(ValueIterator.class);
        when(blockingIter.hasNext()).thenAnswer(inv -> {
            readerBlocking.countDown();
            releaseReader.await();
            return false;
        });
        ArchiveReader slowReader = readerReturning(blockingIter);

        // Long timeout so cancellation (not timeout) drives the exit
        int savedTimeout = Preferences.archive_read_timeout_ms;
        Preferences.archive_read_timeout_ms = 30_000;
        try {
            TestableFetchJob job = new TestableFetchJob(item, Instant.now().minusSeconds(60), Instant.now(), listener);
            job.whenUrl("src://slow", slowReader);

            ArchiveFetchJob.WorkerThread worker = job.new WorkerThread();

            Thread t = new Thread(worker::run);
            t.start();

            readerBlocking.await(); // wait until the fetch is blocked
            worker.cancel();        // signal cancellation + cancel active reader
            releaseReader.countDown(); // unblock the carrier thread

            t.join(3000);
            assertFalse(t.isAlive(), "WorkerThread should have exited after cancel()");
        } finally {
            Preferences.archive_read_timeout_ms = savedTimeout;
            releaseReader.countDown();
        }
    }
}
