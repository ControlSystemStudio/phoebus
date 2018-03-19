/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor;

import static org.csstudio.scan.ScanSystem.logger;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.csstudio.scan.ScanSystem;
import org.csstudio.scan.client.Preferences;
import org.csstudio.scan.client.ScanClient;
import org.csstudio.scan.client.ScanInfoModel;
import org.csstudio.scan.client.ScanInfoModelListener;
import org.csstudio.scan.command.XMLCommandWriter;
import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.info.ScanState;
import org.csstudio.scan.info.SimulationResult;
import org.csstudio.scan.ui.Messages;
import org.csstudio.scan.ui.editor.properties.Properties;
import org.csstudio.scan.ui.simulation.SimulationDisplay;
import org.csstudio.scan.ui.simulation.SimulationDisplayApplication;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.ToolbarHelper;
import org.phoebus.ui.undo.UndoButtons;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Scan editor: Tree of scan, palette of commands
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanEditor extends SplitPane
{
    /** Memento tags */
    private static final String HDIV = "hdiv",
                                VDIV = "vdiv";

    private final Model model = new Model();
    private final UndoableActionManager undo = new UndoableActionManager(50);

    private final Button pause = new Button(), resume = new Button(), next = new Button(), abort = new Button();

    private final Label info_text = new Label();

    private final HBox buttons = new HBox(5);
    private final ToolBar toolbar;
    private final ScanCommandTree scan_tree = new ScanCommandTree(model, undo);
    private final SplitPane right_stack;


    /** Scan that's monitored, -1 for none */
    private volatile long active_scan = -1;

    /** {@link ScanInfoModel}, set while monitoring scan */
    private final AtomicReference<ScanInfoModel> scan_info_model = new AtomicReference<>();

    /** Track update of {@link ScanEditorInstance#active_scan} */
    private final ScanInfoModelListener scan_info_listener = new ScanInfoModelListener()
    {
        @Override
        public void scanUpdate(final List<ScanInfo> infos)
        {
            for (ScanInfo info : infos)
                if (info.getId() == active_scan)
                {
                    if (info.getState().isDone())
                        detachFromScan();
                    updateScanInfo(info);
                    return;
                }
            // No info about the active scan
            updateScanInfo(null);
        }

        @Override
        public void connectionError()
        {
            updateScanInfo(null);
        }
    };

    private volatile String scan_name = "<not saved to file>";

    public ScanEditor()
    {
        //  toolbar    |  palette
        //  ---------  |
        //  scan_tree  |  ----------
        //             |
        //             |  properties

        toolbar = createToolbar();
        VBox.setVgrow(scan_tree, Priority.ALWAYS);
        final VBox left_stack = new VBox(toolbar, scan_tree);

        right_stack = new SplitPane(new Palette(model, undo), new Properties(scan_tree, undo));
        right_stack.setOrientation(Orientation.VERTICAL);

        getItems().setAll(left_stack, right_stack);
        setDividerPositions(0.6);

        updateScanInfo(null);

        createContextMenu();
    }

    void restore(final Memento memento)
    {
        // Has no effect when run right now?
        Platform.runLater(() ->
        {
            memento.getNumber(HDIV).ifPresent(div -> setDividerPositions(div.doubleValue()));
            memento.getNumber(VDIV).ifPresent(div -> right_stack.setDividerPositions(div.doubleValue()));
        });
    }

    void save(final Memento memento)
    {
        memento.setNumber(HDIV, getDividerPositions()[0]);
        memento.setNumber(VDIV, right_stack.getDividerPositions()[0]);
    }

    private ToolBar createToolbar()
    {
        pause.setGraphic(ImageCache.getImageView(ScanSystem.class, "/icons/pause.png"));
        pause.setTooltip(new Tooltip(Messages.scan_pause));
        pause.setOnAction(event ->
        {
            try
            {
                scan_info_model.get().getScanClient().pauseScan(active_scan);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot pause scan " + active_scan, ex);
            }
        });

        resume.setGraphic(ImageCache.getImageView(ScanSystem.class, "/icons/resume.png"));
        resume.setTooltip(new Tooltip(Messages.scan_resume));
        resume.setOnAction(event ->
        {
            try
            {
                scan_info_model.get().getScanClient().resumeScan(active_scan);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot resume scan " + active_scan, ex);
            }
        });

        next.setGraphic(ImageCache.getImageView(ScanSystem.class, "/icons/next.png"));
        next.setTooltip(new Tooltip(Messages.scan_next));
        next.setOnAction(event ->
        {
            try
            {
                scan_info_model.get().getScanClient().nextCommand(active_scan);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot move scan " + active_scan + " to next command", ex);
            }
        });

        abort.setGraphic(ImageCache.getImageView(ScanSystem.class, "/icons/abort.png"));
        abort.setTooltip(new Tooltip(Messages.scan_abort));
        abort.setOnAction(event ->
        {
            try
            {
                scan_info_model.get().getScanClient().abortScan(active_scan);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot abort scan " + active_scan, ex);
            }
        });

        final Button[] undo_redo = UndoButtons.createButtons(undo);
        return new ToolBar(info_text, ToolbarHelper.createStrut(), buttons, ToolbarHelper.createSpring(), undo_redo[0], undo_redo[1]);
    }

    private void createContextMenu()
    {
        final MenuItem copy = new MenuItem("Copy",
                                           ImageCache.getImageView(ImageCache.class, "/icons/copy.png"));
        copy.setOnAction(event -> scan_tree.copyToClipboard());

        final MenuItem paste = new MenuItem("Paste",
                                            ImageCache.getImageView(ImageCache.class, "/icons/paste.png"));
        paste.setOnAction(event -> scan_tree.pasteFromClipboard());

        final MenuItem delete = new MenuItem("Delete",
                                             ImageCache.getImageView(ImageCache.class, "/icons/delete.png"));
        delete.setOnAction(event -> scan_tree.cutToClipboard());


        final MenuItem simulate = new MenuItem(Messages.scan_simulate,
                ImageCache.getImageView(ScanSystem.class, "/icons/simulate.png"));
        simulate.setOnAction(event -> submitOrSimulate(null));

        final MenuItem submit = new MenuItem(Messages.scan_submit,
                                             ImageCache.getImageView(ScanSystem.class, "/icons/run.png"));
        submit.setOnAction(event -> submitOrSimulate(true));

        final MenuItem submit_unqueued = new MenuItem(Messages.scan_submit_unqueued,
                                                      ImageCache.getImageView(ScanSystem.class, "/icons/run.png"));
        submit_unqueued.setOnAction(event -> submitOrSimulate(false));

        final ContextMenu menu = new ContextMenu(copy, paste, delete,
                                                 new SeparatorMenuItem(),
                                                 simulate, submit, submit_unqueued);
        setOnContextMenuRequested(event ->
        {
            menu.show(getScene().getWindow(), event.getScreenX(), event.getScreenY());
        });
    }

    /** @param how true/false to submit queue/un-queued, <code>null</code> to simulate */
    private void submitOrSimulate(final Boolean how)
    {
        JobManager.schedule(how == null ? Messages.scan_simulate : Messages.scan_submit, monitor ->
        {
            // In case of simulation, open SimulationDisplay right away to show we're reacting
            final CompletableFuture<SimulationDisplay> display = new CompletableFuture<>();
            if (how == null)
                Platform.runLater(() ->
                    display.complete(ApplicationService.createInstance(SimulationDisplayApplication.NAME)));

            // In background thread, format commands and submit
            final String xml_commands = XMLCommandWriter.toXMLString(model.getCommands());
            final ScanClient scan_client = new ScanClient(Preferences.host, Preferences.port);
            if (how == null)
            {
                monitor.beginTask("Awaiting simulation results");
                final SimulationResult simulation = scan_client.simulateScan(xml_commands);
                display.get().show(simulation);
            }
            else
            {
                final long id = scan_client.submitScan(scan_name, xml_commands, how);
                attachScan(id);
            }
        });
    }

    /** Set name of scan based on file
     *  @param file File that contains the scan commands
     */
    void setScanName(final File file)
    {
        scan_name = file.getName();
        final int sep = scan_name.lastIndexOf('.');
        if (sep >= 0)
            scan_name = scan_name.substring(0, sep);
    }

    /** @return Model */
    Model getModel()
    {
        return model;
    }

    /** @return UndoableActionManager */
    UndoableActionManager getUndo()
    {
        return undo;
    }

    /** Attach to scan, i.e. allow control of its progress
     *
     *  <p>Will be called off the UI thread.
     *
     *  @param id Scan ID
     *  @throws Exception on error
     */
    void attachScan(final long id) throws Exception
    {
        active_scan = id;

        final ScanInfoModel infos = ScanInfoModel.getInstance();
        final ScanInfoModel previous = scan_info_model.getAndSet(infos);
        if (previous != null)
        {
            previous.removeListener(scan_info_listener);
            previous.release();
        }
        infos.addListener(scan_info_listener);
    }

    /** @param info Info about scan, <code>null</code> if no info available or scan completed */
    void updateScanInfo(final ScanInfo info)
    {
        final List<Button> desired;
        final String text;

        if (info == null)
        {
            // Leave info_text on the last known state?
            text = null;
            desired = Collections.emptyList();

            this.scan_tree.setActiveCommand(-1);
        }
        else
        {
            text = info.toString();
            final ScanState state = info.getState();
            switch (state)
            {
            case Idle:
                desired = List.of(abort);
                break;
            case Running:
                desired = List.of(pause, next, abort);
                break;
            case Paused:
                desired = List.of(resume, abort);
                break;
            default:
                desired = Collections.emptyList();
            }

            this.scan_tree.setActiveCommand(info.getCurrentAddress());
        }

        // XXX Optimize, skip the UI call when buttons didn't change?

        Platform.runLater(() ->
        {
            buttons.getChildren().setAll(desired);
            if (text != null)
                info_text.setText(text);
        });
    }

    /** If currently monitoring a scan, detach */
    void detachFromScan()
    {
        final ScanInfoModel infos = scan_info_model.getAndSet(null);
        if (infos != null)
        {
            infos.removeListener(scan_info_listener);
            updateScanInfo(null);
            infos.release();
        }
    }
}
