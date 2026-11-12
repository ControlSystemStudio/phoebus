package org.csstudio.display.builder.runtime.app;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.pv.PVPool;
import org.phoebus.ui.docking.DockStage;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import javafx.stage.Stage;

/** Loads of a display that follow each other quickly
 *
 *  <p>Each display has its own PV.
 *  Only the PV of the display that's shown in the end may remain open,
 *  and a load that was replaced must not report errors.
 */
@SuppressWarnings("nls")
class DisplayLoadSequenceUI extends ApplicationTest
{
    /** Errors: SEVERE log messages, exceptions on the UI thread */
    private final List<String> errors = new CopyOnWriteArrayList<>();

    private final Handler errorLog = new Handler()
    {
        @Override
        public void publish(final LogRecord rec)
        {
            if (rec.getLevel().intValue() >= Level.SEVERE.intValue())
                errors.add(rec.getMessage());
        }

        @Override
        public void flush()
        {
            // Nothing to flush
        }

        @Override
        public void close()
        {
            // Nothing to close
        }
    };

    private Stage stage;
    private DisplayRuntimeInstance instance;
    private boolean closed = false;

    @Override
    public void start(final Stage primaryStage)
    {
        Logger.getLogger("").addHandler(errorLog);
        Thread.currentThread().setUncaughtExceptionHandler((thread, ex) -> errors.add(ex.toString()));
        stage = new Stage();
        DockStage.configureStage(stage);
        stage.show();
        instance = new DisplayRuntimeInstance(new DisplayRuntimeApplication());
    }

    @AfterEach
    void closeStage() throws Exception
    {
        if (! closed)
            close();
        interact(stage::close);
        Logger.getLogger("").removeHandler(errorLog);
    }

    @Test
    void testBackTwiceInARow() throws Exception
    {
        open("a");
        open("b");
        open("c");

        // Two 'back' steps before the first one has been represented
        interact(() ->
        {
            instance.loadDisplayFile(instance.getNavigation().goBackward(1));
            instance.loadDisplayFile(instance.getNavigation().goBackward(1));
        });
        waitForLoads();

        assertThat(instance.getDockItem().getLabel(), equalTo("Display a"));
        assertThat(openPVs(), equalTo(List.of("load_a")));
        assertThat(errors, equalTo(List.of()));
    }

    @Test
    void testReplacedLoadFails() throws Exception
    {
        open("a");

        // A load that fails is replaced by a newer one before it fails
        final DisplayInfo missing = info("missing");
        final DisplayInfo c = info("c");
        interact(() ->
        {
            instance.loadDisplayFile(missing);
            instance.loadDisplayFile(c);
        });
        waitForLoads();

        assertThat(instance.getDockItem().getLabel(), equalTo("Display c"));
        assertThat(openPVs(), equalTo(List.of("load_c")));
        assertThat(errors, equalTo(List.of()));
    }

    @Test
    void testCloseWhileLoading() throws Exception
    {
        open("a");

        // Close the display before the load of 'b' can run
        final DisplayInfo b = info("b");
        instance.getDockItem().prepareToClose();
        interact(() ->
        {
            instance.loadDisplayFile(b);
            instance.close();
        });
        closed = true;
        waitForLoads();

        assertThat(openPVs(), equalTo(List.of()));
        assertThat(errors, equalTo(List.of()));
    }

    /** @param name Name of the test display, "a", "b", "c", or "missing" for one that doesn't exist
     *  @return {@link DisplayInfo} for that display
     */
    private DisplayInfo info(final String name) throws Exception
    {
        return DisplayInfo.forURI(getClass().getResource("/loading/load_a.bob").toURI().resolve("load_" + name + ".bob"));
    }

    /** @param name Name of the test display to load and wait for */
    private void open(final String name) throws Exception
    {
        final DisplayInfo info = info(name);
        interact(() -> instance.loadDisplayFile(info));
        waitForLoads();
        assertThat(instance.getDockItem().getLabel(), equalTo("Display " + name));
    }

    /** Close the display the same way as its tab or window would */
    private void close() throws Exception
    {
        instance.getDockItem().prepareToClose();
        interact(instance::close);
        closed = true;
    }

    private void waitForLoads() throws TimeoutException
    {
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, () -> JobManager.getJobCount() == 0);
        WaitForAsyncUtils.waitForFxEvents();
    }

    /** @return Test display PVs that are open, for example "load_a" */
    private static List<String> openPVs()
    {
        return PVPool.getPVReferences()
                     .stream()
                     .map(ref -> ref.getEntry().getName())
                     .filter(name -> name.contains("load_"))
                     .map(name -> name.substring(name.indexOf("load_")))
                     .sorted()
                     .toList();
    }
}
