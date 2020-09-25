/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.docking;

import static org.phoebus.ui.docking.DockPane.logger;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.security.authorization.AuthorizationService;
import org.phoebus.ui.application.Messages;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.Styles;

import javafx.application.Platform;
import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Item for a {@link DockPane}
 *
 *  <p>Technically a {@link Tab},
 *  should be treated as a new type of node,
 *  calling only
 *  <ul>
 *  <li>the methods declared in here
 *  <li>{@link Tab#setContent(javafx.scene.Node)},
 *  <li>{@link Tab#setClosable(boolean)}
 *  </ul>
 *  to allow changes to the internals.
 *
 *  <p>Specifically,
 *  {@link Tab#setOnCloseRequest(EventHandler)}
 *  and
 *  {@link Tab#setOnClosed(EventHandler)}
 *  are used internally and should not be called.
 *
 *  <p>Instead of `setOnCloseRequest`, use {@link DockItem#addCloseCheck}.
 *  <p>Instead of `setOnClosed`, use {@link DockItem#addClosedNotification}.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DockItem extends Tab
{
    /** Property key used for the {@link AppDescriptor} */
    public static final String KEY_APPLICATION = "application";

    private final static Image info_icon = ImageCache.getImage(DockItem.class, "/icons/info.png"),
                               detach_icon = ImageCache.getImage(DockItem.class, "/icons/detach.png"),
                               split_horiz_icon = ImageCache.getImage(DockItem.class, "/icons/split_horiz.png"),
                               split_vert_icon = ImageCache.getImage(DockItem.class, "/icons/split_vert.png"),
                               close_many_icon = ImageCache.getImage(DockItem.class, "/icons/remove_multiple.png");


    /** The item that's currently being dragged
     *
     *  <p>The actual DockItem cannot be serialized
     *  for drag-and-drop,
     *  and since docking is limited to windows within
     *  the same JVM, this reference points to the item
     *  that's being dragged.
     */
    static final AtomicReference<DockItem> dragged_item = new AtomicReference<>();

    static final Border DROP_ZONE_BORDER = new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID,
                                                                       new CornerRadii(5.0), BorderStroke.MEDIUM));

    /** Drag-and-drop data format
     *
     *  Custom format to prevent dropping a tab into e.g. a text editor
     */
    private static final DataFormat DOCK_ITEM = new DataFormat("dock_item.custom");

    /** Name of the tab */
    protected String name;

    /** Label used for the Tab because Tab itself cannot participate in drag-and-drop */
    protected final Label name_tab;

    /** Called to check if OK to close the tab */
    private List<Supplier<Future<Boolean>>> close_check = new ArrayList<>();

    /** Has 'prepareToClose' been called? */
    private volatile boolean prepared_do_close = false;

    /** Called after tab was closed */
    private List<Runnable> closed_callback = null;

    /** Create dock item for instance of an application
     *  @param applicationInstance {@link AppInstance}
     *  @param content Content for this application instance
     */
    public DockItem(final AppInstance applicationInstance, final Node content)
    {
        this(applicationInstance.getAppDescriptor().getDisplayName());
        getProperties().put(KEY_APPLICATION, applicationInstance);
        setContent(content);
    }

    /** Create dock item
     *  @param label Initial label
     */
    public DockItem(final String label)
    {
        getProperties().put(DockStage.KEY_ID, DockStage.createID("DockItem"));

        // Create tab with no 'text',
        // instead using a Label for the text
        // because the label can react to drag operations
        name = label;
        name_tab = new Label(label);
        setGraphic(name_tab);

        // Clicking tab activates its dock pane,
        // which obtains this tab via the tab pane selection handler,
        // and then sets the active dock item in the phoebus app
        name_tab.setOnMouseClicked(event -> DockPane.setActiveDockPane(getDockPane()));

        // Support dragging tabs
        name_tab.setOnDragDetected(this::handleDragDetected);
        name_tab.setOnDragOver(this::handleDragOver);
        name_tab.setOnDragEntered(this::handleDragEntered);
        name_tab.setOnDragExited(this::handleDragExited);
        name_tab.setOnDragDropped(this::handleDrop);
        name_tab.setOnDragDone(this::handleDragDone);

        createContextMenu();

        setOnClosed(event -> handleClosed());
    }

    /** This tab should be in a DockPane, not a plain TabPane
     *  @return DockPane that holds this tab
     */
    public DockPane getDockPane()
    {
        final TabPane tp = getTabPane();
        if (tp == null  ||  tp instanceof DockPane)
            return (DockPane) tp;
        throw new IllegalStateException("Expected DockPane for " + this + ", got " + tp);
    }

    private void createContextMenu()
    {
        final MenuItem info = new MenuItem(Messages.DockInfo, new ImageView(info_icon));
        info.setOnAction(event -> showInfo());

        final MenuItem detach = new MenuItem(Messages.DockDetach, new ImageView(detach_icon));
        detach.setOnAction(event -> detach());

        final MenuItem split_horiz = new MenuItem(Messages.DockSplitH, new ImageView(split_horiz_icon));
        split_horiz.setOnAction(event -> split(true));

        final MenuItem split_vert = new MenuItem(Messages.DockSplitV, new ImageView(split_vert_icon));
        split_vert.setOnAction(event -> split(false));

        final MenuItem close = new MenuItem(Messages.DockClose, new ImageView(DockPane.close_icon));
        close.setOnAction(event -> close(List.of(this)));

        final MenuItem close_other = new MenuItem(Messages.DockCloseOthers, new ImageView(close_many_icon));
        close_other.setOnAction(event ->
        {
            // Close all other tabs in non-fixed panes of this window
            final Stage stage = (Stage) getDockPane().getScene().getWindow();
            final List<DockItem> tabs = new ArrayList<>();
            for (DockPane pane : DockStage.getDockPanes(stage))
                if (! pane.isFixed())
                    for (Tab tab : new ArrayList<>(pane.getTabs()))
                        if ((tab instanceof DockItem)  &&  tab != DockItem.this)
                            tabs.add((DockItem)tab);
            close(tabs);
        });

        final MenuItem close_all = new MenuItem(Messages.DockCloseAll, new ImageView(close_many_icon));
        close_all.setOnAction(event ->
        {
            // Close all tabs in non-fixed panes of this window
            final Stage stage = (Stage) getDockPane().getScene().getWindow();
            final List<DockItem> tabs = new ArrayList<>();
            for (DockPane pane : DockStage.getDockPanes(stage))
                if (! pane.isFixed())
                    for (Tab tab : new ArrayList<>(pane.getTabs()))
                        if ((tab instanceof DockItem))
                            tabs.add((DockItem)tab);
            close(tabs);
        });

        final ContextMenu menu = new ContextMenu(info);

        menu.setOnShowing(event ->
        {
            menu.getItems().setAll(info);

            final boolean may_lock = AuthorizationService.hasAuthorization("lock_ui");
            final DockPane pane = getDockPane();
            if (pane.isFixed())
            {
                if (may_lock)
                    menu.getItems().addAll(new NamePaneMenuItem(pane), new UnlockMenuItem(pane));
            }
            else
            {
                menu.getItems().addAll(new SeparatorMenuItem(),
                                       detach,
                                       split_horiz,
                                       split_vert);

                if (may_lock)
                    menu.getItems().addAll(new NamePaneMenuItem(pane), new LockMenuItem(pane));

                menu.getItems().addAll(new SeparatorMenuItem(),
                                       close,
                                       close_other,
                                       new SeparatorMenuItem(),
                                       close_all);
            }
        });

        name_tab.setContextMenu(menu);
    }

    /** @param tabs Tabs to prepare and then close */
    private static void close(final List<DockItem> tabs)
    {
        JobManager.schedule("Close", monitor ->
        {
            for (DockItem tab : tabs)
                if (! tab.prepareToClose())
                    return;

            Platform.runLater(() ->
            {
                for (DockItem tab : tabs)
                    tab.close();
            });
        });
    }

    /** @return Unique ID of this dock item */
    public String getID()
    {
        return (String) getProperties().get(DockStage.KEY_ID);
    }

    /** @return Application instance of this dock item, may be <code>null</code> */
    @SuppressWarnings("unchecked")
    public <INST extends AppInstance> INST getApplication()
    {
        return (INST) getProperties().get(KEY_APPLICATION);
    }

    /** Label of this item */
    public String getLabel()
    {
        return name;
    }

    /** @return Label node text, limited to in-package access */
    StringProperty labelTextProperty()
    {
        return name_tab.textProperty();
    }

    /** @param label Label of this item */
    public void setLabel(final String label)
    {
        name = label;
        name_tab.setText(label);
    }

    /** Show info dialog */
    private void showInfo()
    {
        final Alert dlg = new Alert(AlertType.INFORMATION);
        dlg.setTitle(Messages.DockInfo);

        // No DialogPane 'header', all info is in the 'content'
        dlg.setHeaderText("");

        final StringBuilder info = new StringBuilder();
        fillInformation(info);
        final TextArea content = new TextArea(info.toString());
        content.setEditable(false);
        content.setPrefSize(300, 100);
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        dlg.getDialogPane().setContent(content);

        DialogHelper.positionDialog(dlg, name_tab, 0, 0);
        dlg.setResizable(true);
        dlg.showAndWait();
    }

    /** @param info Multi-line information for 'Info' dialog */
    protected void fillInformation(final StringBuilder info)
    {
        if (getApplication() == null)
            info.append(Messages.DockNoApp);
        else
            info.append(Messages.DockAppName)
                .append(getApplication().getAppDescriptor().getName());
    }

    /** Allow dragging this item */
    private void handleDragDetected(final MouseEvent event)
    {
        // Disable dragging from a 'fixed' pane
        if (getDockPane().isFixed())
            return;

        final Dragboard db = name_tab.startDragAndDrop(TransferMode.MOVE);

        final ClipboardContent content = new ClipboardContent();
        content.put(DOCK_ITEM, getLabel());
        db.setContent(content);

        final DockItem previous = dragged_item.getAndSet(this);
        if (previous != null)
            logger.log(Level.WARNING, "Already dragging " + previous);

        event.consume();
    }

    /** Accept other items that are dropped onto this one */
    private void handleDragOver(final DragEvent event)
    {
        // Don't suggest dropping into a 'fixed' pane
        if (getDockPane().isFixed())
            return;

        final DockItem item = dragged_item.get();
        if (item != null  &&  item != this)
            event.acceptTransferModes(TransferMode.MOVE);
        event.consume();
    }

    /** Highlight while 'drop' is possible */
    private void handleDragEntered(final DragEvent event)
    {
        // Drop not possible into a 'fixed' pane
        if (getDockPane().isFixed())
            return;

        final DockItem item = dragged_item.get();
        if (item != null  &&  item != this)
        {
            name_tab.setBorder(DROP_ZONE_BORDER);
            name_tab.setTextFill(Color.GREEN);
        }
        event.consume();
    }

    /** Remove Highlight */
    private void handleDragExited(final DragEvent event)
    {
        name_tab.setBorder(Border.EMPTY);
        name_tab.setTextFill(Color.BLACK);
        event.consume();
    }

    /** Accept a dropped tab */
    private void handleDrop(final DragEvent event)
    {
        if (getDockPane().isFixed())
            return;

        final DockItem item = dragged_item.getAndSet(null);
        if (item == null)
            logger.log(Level.SEVERE, "Empty drop, " + event);
        else
        {
            logger.log(Level.INFO, "Somebody dropped " + item + " onto " + this);
            final DockPane old_parent = item.getDockPane();
            final DockPane new_parent = getDockPane();
            if (new_parent != old_parent)
            {
                old_parent.getTabs().remove(item);
                // Insert after the item on which it was dropped
                final int index = new_parent.getTabs().indexOf(this);
                new_parent.getTabs().add(index+1, item);
            }
            else
            {
                final int index = new_parent.getTabs().indexOf(this);
                new_parent.getTabs().remove(item);
                // If item was 'left' of this, it will be added just after this.
                // If item was 'right' of this, it'll be added just before this.
                new_parent.getTabs().add(index, item);
            }
            // Select the new item
            new_parent.getSelectionModel().select(item);
        }
        event.setDropCompleted(true);
        event.consume();
    }

    /** Handle that this tab was dragged elsewhere, or drag aborted */
    private void handleDragDone(final DragEvent event)
    {
        final DockItem item = dragged_item.getAndSet(null);
        if (item != null  &&  !event.isDropCompleted())
        {
            // Would like to position new stage where the mouse was released,
            // but event.getX(), getSceneX(), getScreenX() are all 0.0.
            // --> Using MouseInfo, which is actually AWT code
            final Stage other = item.detach();
            final PointerInfo pi = MouseInfo.getPointerInfo();
            if (pi != null)
            {
                final Point loc = pi.getLocation();
                other.setX(loc.getX());
                other.setY(loc.getY());
            }
        }
        event.consume();
    }

    private Stage detach()
    {
        // For size of new stage, approximate the
        // current size of the item, i.e. the size
        // of its DockPane, adding some extra space
        // for the window border, title bar etc.
        final DockPane old_parent = getDockPane();
        final Scene old_scene = old_parent.getScene();
        final double extra_width = Math.max(0, old_scene.getWindow().getWidth() - old_scene.getWidth());
        final double extra_height = Math.max(0, old_scene.getWindow().getHeight() - old_scene.getHeight());

        // If this tab was the last tab in the DockPane,
        // and that's in a SplitDock, the following call will
        // remove the old_parent from the scene.
        // That's why we fetched the scene info ahead of time.
        old_parent.getTabs().remove(this);

        final Stage other = new Stage();
        other.setTitle(UUID.randomUUID().toString());

        DockStage.configureStage(other, this);
        other.setWidth(old_parent.getWidth() + extra_width);
        other.setHeight(old_parent.getHeight() + extra_height);

        // Assert that styles used in old scene are still available
        for (String css : old_scene.getStylesheets())
            Styles.set(other.getScene(), css);

        other.show();

        return other;
    }

    /** Request DockPane to split
     *  @param horizontally Horizontally or vertically?
     */
    private void split(final boolean horizontally)
    {
        final DockPane pane = getDockPane();
        // Perform on next tick, outside of menu action handler.
        // This allows debugging on Linux,
        // where UI would otherwise be stuck in context menu and not
        // allow using the mouse etc.
        Platform.runLater(() -> pane.split(horizontally));
    }

    /** Select this tab, i.e. raise it in case another tab is currently visible */
    public void select()
    {
        final DockPane pane = getDockPane();
        final Window window = pane.getScene().getWindow();
        if (window instanceof Stage)
        {
            Stage stage = (Stage) window;
            if (stage.isShowing())
                stage.toFront();
        }
        pane.getSelectionModel().select(this);
    }

    /** Register check for closing the tab
     *
     *  @param ok_to_close Will be called when tab prepares to close.
     *                     Will be invoked on the UI thread, so it may
     *                     prompt for "Do you want to save?".
     *                     May return a completed future right away,
     *                     or start a background thread to for example
     *                     save the tab's content which will complete
     *                     the future when done.
     *                     Future must be <code>true</code> if OK to close,
     *                     <code>false</code> to leave the tab open.
     */
    public void addCloseCheck(final Supplier<Future<Boolean>> ok_to_close)
    {
        if (getOnCloseRequest() == null)
            setOnCloseRequest(event ->
            {
                // For now, prevent closing
                event.consume();

                // Invoke all the ok-to-close checks in background threads
                // since those that save files might take time.
                JobManager.schedule("Close " + getLabel(), monitor ->
                {
                    if (prepareToClose())
                        Platform.runLater(() -> close());
                });
            });

        close_check.add(ok_to_close);
    }

    /** Prepare dock item to close
     *
     *  Invokes all ok-to-close checks,
     *  which may start "save" threads and take some time
     *  before returning result.
     *
     *  @return <code>true</code> when OK to close
     *  @throws Exception on error
     */
    public boolean prepareToClose() throws Exception
    {
        if (Platform.isFxApplicationThread())
            logger.log(Level.SEVERE, "'prepareToClose' must not be called on UI thread because it can block/deadlock", new Exception("Stack Trace"));

        if (close_check != null)
            for (Supplier<Future<Boolean>> check : close_check)
            {
                // Invoke each actual ok-to-close check on UI thread,
                // since it may open dialogs etc. before starting a "save" thread
                final CompletableFuture<Boolean> result = new CompletableFuture<>();
                Platform.runLater(() ->
                {
                    try
                    {
                        result.complete(check.get().get());
                    }
                    catch (Exception ex)
                    {
                        result.completeExceptionally(ex);
                    }
                });
                // .. then await result of check started in UI thread
                if (! result.get())
                    return false;
            }

        prepared_do_close = true;
        return true;
    }

    /** Register for notification when tab was closed
     *
     *  @param closed Will be called after tab was closed
     */
    public void addClosedNotification(final Runnable closed)
    {
        if (closed_callback == null)
            closed_callback = new ArrayList<>();
        closed_callback.add(closed);
    }

    /** Tab has been closed
     *
     *  <p>Called via 'ON_CLOSED' event,
     *  and programmatically from close().
     *
     *  Implementation must be idempotent,
     *  since it might be called several times.
     */
    protected void handleClosed()
    {
        // If there are callbacks, invoke them
        if (closed_callback != null)
        {
            for (Runnable check : closed_callback)
                check.run();
            closed_callback = null;
        }

        // Remove content to avoid memory leaks
        // because this tab could linger in memory for a while
        setContent(null);
        // Remove "application" entry which otherwise holds on to application data model
        getProperties().remove(KEY_APPLICATION);
    }

    /** Programmatically close this tab
     *
     *  <p>Should be called after {@link #prepareToClose) has been used
     *  to allow "save".
     *
     *  @return <code>true</code> if tab closed, <code>false</code> if it remained open
     */
    public void close()
    {
        // Helper for detecting errors in the handling of prepareToClose() and close().
        //
        // Check if prepareToClose() has been called at least once
        // before close()ing the dock item.
        // Log a warning to maintainers, but otherwise continue.
        //
        // Check is imperfect.
        // Assume closing a window with many dock items, several of them 'dirty'.
        // User attempts to close the window, prepareToClose() prompts user "Do you want to save?",
        // user cancels. The "prepared_do_close" flag remains set,
        // and if we later close the panel by other means we won't really check if
        // prepareToClose() has been called again.
        if (! prepared_do_close)
            logger.log(Level.SEVERE, "Failed to call prepareToClose", new Exception("Stack Trace"));

        handleClosed();

        getDockPane().getTabs().remove(this);
    }

    @Override
    public String toString()
    {
        return "DockItem(\"" + getLabel() + "\")";
    }
}