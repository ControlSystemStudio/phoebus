package org.csstudio.display.builder.runtime.app;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.phoebus.pv.RefCountMap;
import org.phoebus.ui.docking.DockStage;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
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
    /** Errors: SEVERE log messages, a display runtime that could not be stopped, exceptions on the UI thread */
    private final List<String> errors = new CopyOnWriteArrayList<>();

    private final Handler errorLog = new Handler()
    {
        @Override
        public void publish(final LogRecord rec)
        {
            if (rec.getLevel().intValue() >= Level.SEVERE.intValue()  ||
                String.valueOf(rec.getMessage()).startsWith("Skipping display shutdown"))
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

    /** References to test display PVs before the test, see {@link #openPVs()} */
    private Map<String, Integer> pvsBefore;

    @Override
    public void start(final Stage primaryStage)
    {
        pvsBefore = pvReferences();
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
    void testShownDisplayIsReplacedRightAway() throws Exception
    {
        open("a");

        // As soon as a new load starts, before the file has even been read,
        // the shown display is gone and the user sees that a display is loading
        final DisplayInfo b = info("b");
        final AtomicReference<List<String>> pvsWhileLoading = new AtomicReference<>();
        final List<String> added = new CopyOnWriteArrayList<>();
        final ListChangeListener<Node> recordAdded = change ->
        {
            while (change.next())
                change.getAddedSubList().forEach(node -> added.add(describe(node)));
        };
        interact(() ->
        {
            displayArea().addListener(recordAdded);
            instance.loadDisplayFile(b);
            pvsWhileLoading.set(openPVs());
        });
        waitForDisplay("b");
        interact(() -> displayArea().removeListener(recordAdded));

        assertThat(pvsWhileLoading.get(), equalTo(List.of()));
        // First the message, then the widgets of 'b'
        assertThat(added.get(0), equalTo("Loading " + b));
        assertThat(added.get(added.size() - 1), equalTo("widget"));
        assertThat(openPVs(), equalTo(List.of("load_b")));
        assertThat(errors, equalTo(List.of()));
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
        waitForDisplay("a");

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
        waitForDisplay("c");

        assertThat(openPVs(), equalTo(List.of("load_c")));
        assertThat(errors, equalTo(List.of()));
    }

    @Test
    void testLoadWhileRuntimeStarts() throws Exception
    {
        open("a");

        // Load a display whose runtime start takes long, because its PV is slow to create
        BlockingPVFactory.block();
        final DisplayInfo slow = info("slow");
        interact(() -> instance.loadDisplayFile(slow));
        BlockingPVFactory.awaitCreation();

        // A newer load during that start doesn't have to wait for it
        final DisplayInfo b = info("b");
        final long start = System.currentTimeMillis();
        interact(() -> instance.loadDisplayFile(b));
        final long waited = System.currentTimeMillis() - start;
        BlockingPVFactory.release();
        waitForDisplay("b");

        assertThat("Milliseconds the UI waited: " + waited, waited < 2000, equalTo(true));
        // The started runtime of the 'slow' display was stopped
        assertThat(openPVs(), equalTo(List.of("load_b")));
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
        // Not reported as a failed load.
        // (The 'Loading' message that the closed display can no longer show
        //  fails the same way on master, which this test doesn't cover.)
        assertThat(errors.stream().filter(error -> error.startsWith("Cannot load model")).toList(), equalTo(List.of()));
    }

    /** @param name Name of the test display, "a", "b", "c", "slow", or "missing" for one that doesn't exist
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
        waitForDisplay(name);
    }

    /** Close the display the same way as its tab or window would */
    private void close() throws Exception
    {
        instance.getDockItem().prepareToClose();
        interact(instance::close);
        closed = true;
    }

    /** Wait until a test display is shown and no load job is left
     *  @param name Name of the test display
     */
    private void waitForDisplay(final String name) throws TimeoutException
    {
        final String label = "Display " + name;
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                  () -> label.equals(WaitForAsyncUtils.waitFor(WaitForAsyncUtils.asyncFx(() -> instance.getDockItem().getLabel()))));
        waitForLoads();
    }

    /** Wait until no load job is left.
     *  Call after interact(), by which time a job that was just scheduled is running and counted.
     */
    private static void waitForLoads() throws TimeoutException
    {
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, () -> JobManager.getJobCount() == 0);
        WaitForAsyncUtils.waitForFxEvents();
    }

    /** @return Nodes in the area of the display. Call on the UI thread */
    private ObservableList<Node> displayArea()
    {
        return JFXRepresentation.getChildren(instance.getRepresentation().getModelParent());
    }

    /** @param node Node added to the display area
     *  @return Text of the 'Loading ...' message, or "widget"
     */
    private static String describe(final Node node)
    {
        if (node instanceof Label label  &&  label.getText().startsWith("Loading"))
            return label.getText();
        return "widget";
    }

    /** @return Test display PVs that this test opened and that are still open, for example "load_a".
     *          PVs that an earlier test left open don't count.
     */
    private List<String> openPVs()
    {
        final List<String> open = new ArrayList<>();
        pvReferences().forEach((name, references) ->
        {
            for (int i = pvsBefore.getOrDefault(name, 0);  i < references;  ++i)
                open.add(name);
        });
        Collections.sort(open);
        return open;
    }

    /** @return References to each test display PV, by name like "load_a" */
    private static Map<String, Integer> pvReferences()
    {
        final Map<String, Integer> references = new HashMap<>();
        for (RefCountMap.ReferencedEntry<PV> ref : PVPool.getPVReferences())
        {
            final String name = ref.getEntry().getName();
            if (name.contains("load_"))
                references.merge(name.substring(name.indexOf("load_")), ref.getReferences(), Integer::sum);
        }
        return references;
    }
}
