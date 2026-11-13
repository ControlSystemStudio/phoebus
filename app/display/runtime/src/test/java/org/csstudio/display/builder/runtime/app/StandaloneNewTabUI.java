package org.csstudio.display.builder.runtime.app;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.docking.DockStage;
import org.phoebus.ui.docking.Geometry;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import javafx.stage.Stage;

/** A display whose action button opens another display in a new tab
 *
 *  <p>A standalone window shows no tabs,
 *  so the new tab must not go to the standalone window and hide the display.
 */
@SuppressWarnings("nls")
class StandaloneNewTabUI extends ApplicationTest
{
    private DockPane mainPane;
    private DockPane pane;

    @Override
    public void start(final Stage primaryStage)
    {
        final Stage mainStage = new Stage();
        mainPane = DockStage.configureStage(mainStage);
        mainStage.getProperties().put(DockStage.KEY_ID, DockStage.ID_MAIN);
        mainStage.show();
    }

    @AfterEach
    void closeWindows() throws Exception
    {
        // Close the displays the same way as their tabs would
        final List<DockItem> tabs = onUiThread(StandaloneNewTabUI::allTabs);
        for (DockItem tab : tabs)
            tab.prepareToClose();
        interact(() ->
        {
            tabs.forEach(DockItem::close);
            DockStage.getDockStages().forEach(Stage::close);
        });
    }

    @Test
    void testNewTabFromStandaloneWindowOpensInMainWindow() throws Exception
    {
        openDisplayInNewWindow(true);
        openNewTab();

        assertThat(labels(pane), equalTo(List.of("Display a")));
        assertThat(labels(mainPane), equalTo(List.of("Display b")));
    }

    @Test
    void testNewTabFromNormalWindowStaysInThatWindow() throws Exception
    {
        openDisplayInNewWindow(false);
        openNewTab();

        assertThat(labels(pane), equalTo(List.of("Display a", "Display b")));
        assertThat(labels(mainPane), equalTo(List.of()));
    }

    @Test
    void testNewTabFromStandaloneWindowWithLockedMainWindowOpensNewWindow() throws Exception
    {
        interact(() -> mainPane.setFixed(true));
        openDisplayInNewWindow(true);
        openNewTab();

        assertThat(labels(pane), equalTo(List.of("Display a")));
        assertThat(labels(mainPane), equalTo(List.of()));
        assertThat(onUiThread(() -> DockStage.getDockStages().size()), equalTo(3));
    }

    /** Open 'Display a' in a new window, whose pane becomes the 'pane' of the test
     *  @param standalone Open a standalone window, or a normal one?
     */
    private void openDisplayInNewWindow(final boolean standalone) throws Exception
    {
        final DisplayInfo info = DisplayInfo.forURI(getClass().getResource("/standalone/tab_a.bob").toURI());
        interact(() ->
        {
            final Stage stage = new Stage();
            pane = DockStage.configureStage(stage, new Geometry(null), standalone);
            stage.show();
            final DisplayRuntimeInstance instance = new DisplayRuntimeInstance(new DisplayRuntimeApplication());
            instance.loadDisplayFile(info);
        });
        waitForLoads();
    }

    /** Click the button of 'Display a' that opens 'Display b' in a new tab, wait for 'Display b' */
    private void openNewTab() throws Exception
    {
        clickOn("Open b");
        // A tab gets the display name once the display is represented
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS,
                                  () -> onUiThread(() -> allTabs().stream()
                                                                  .map(DockItem::getLabel)
                                                                  .anyMatch("Display b"::equals)));
        waitForLoads();
    }

    /** Wait until no display is loading and the UI has handled all pending events */
    private static void waitForLoads() throws TimeoutException
    {
        WaitForAsyncUtils.waitFor(10, TimeUnit.SECONDS, () -> JobManager.getJobCount() == 0);
        WaitForAsyncUtils.waitForFxEvents();
    }

    /** @return Tabs of all windows. Call on the UI thread */
    private static List<DockItem> allTabs()
    {
        return DockStage.getDockStages()
                        .stream()
                        .flatMap(stage -> DockStage.getDockPanes(stage).stream())
                        .flatMap(dockPane -> dockPane.getDockItems().stream())
                        .toList();
    }

    /** @param dockPane {@link DockPane}
     *  @return Labels of its tabs
     */
    private static List<String> labels(final DockPane dockPane)
    {
        return onUiThread(() -> dockPane.getDockItems().stream().map(DockItem::getLabel).toList());
    }

    /** @param <T> Result type
     *  @param callable Code that reads the UI
     *  @return Its result, read on the UI thread
     */
    private static <T> T onUiThread(final Callable<T> callable)
    {
        return WaitForAsyncUtils.waitFor(WaitForAsyncUtils.asyncFx(callable));
    }
}
