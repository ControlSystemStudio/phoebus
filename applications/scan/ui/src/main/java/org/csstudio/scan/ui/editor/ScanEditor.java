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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.csstudio.scan.ScanSystem;
import org.csstudio.scan.client.Preferences;
import org.csstudio.scan.client.ScanClient;
import org.csstudio.scan.client.ScanInfoModel;
import org.csstudio.scan.client.ScanInfoModelListener;
import org.csstudio.scan.command.XMLCommandWriter;
import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.ui.Messages;
import org.csstudio.scan.ui.editor.properties.Properties;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.ToolbarHelper;
import org.phoebus.ui.undo.UndoButtons;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
    private final Model model = new Model();
    private final UndoableActionManager undo = new UndoableActionManager(50);

    private final Button submit, simulate, pause, resume, next, abort;

    private final Label info_text = new Label();

    private final HBox buttons = new HBox(5);
    private final ToolBar toolbar;
    private final ScanCommandTree scan_tree = new ScanCommandTree(model, undo);

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
                    Platform.runLater(() -> updateScanInfo(info));
                    return;
                }
            // No info about the active scan
            Platform.runLater(() -> updateScanInfo(null));
        }

        @Override
        public void connectionError()
        {
            Platform.runLater(() -> updateScanInfo(null));
        }
    };

    private volatile String scan_name = "<not saved to file>";

    public ScanEditor()
    {
        submit = new Button();
        submit.setGraphic(ImageCache.getImageView(ScanSystem.class, "/icons/run.png"));
        submit.setTooltip(new Tooltip(Messages.scan_submit));
        submit.setOnAction(event ->
        {
            JobManager.schedule(Messages.scan_submit, monitor ->
            {
                final String xml_commands = XMLCommandWriter.toXMLString(model.getCommands());
                final ScanClient scan_client = new ScanClient(Preferences.host, Preferences.port);
                final long id = scan_client.submitScan(scan_name, xml_commands, false);
                attachScan(id);
            });
        });
        // TODO Submit without queuing?


        simulate = new Button();
        simulate.setGraphic(ImageCache.getImageView(ScanSystem.class, "/icons/simulate.png"));
        simulate.setTooltip(new Tooltip(Messages.scan_simulate));
        // TODO Simulate

        pause = new Button();
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

        resume = new Button();
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

        next = new Button();
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

        abort = new Button();
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
        toolbar = new ToolBar(info_text, ToolbarHelper.createStrut(), buttons, ToolbarHelper.createSpring(), undo_redo[0], undo_redo[1]);

        VBox.setVgrow(scan_tree, Priority.ALWAYS);
        final VBox left_stack = new VBox(toolbar, scan_tree);

        final SplitPane right_stack = new SplitPane(new Palette(model, undo), new Properties(scan_tree, undo));
        right_stack.setOrientation(Orientation.VERTICAL);

        getItems().setAll(left_stack, right_stack);
        setDividerPositions(0.6);

        updateScanInfo(null);
    }

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

    void updateScanInfo(final ScanInfo info)
    {
        if (info == null)
        {
            // Leave info_text on the last known state?
            buttons.getChildren().setAll(submit, simulate);
        }
        else
        {
            info_text.setText(info.toString());
            switch (info.getState())
            {
            case Idle:
                buttons.getChildren().setAll(simulate, abort);
                break;
            case Aborted:
            case Failed:
            case Finished:
                buttons.getChildren().setAll(submit, simulate);
                break;
            case Running:
                buttons.getChildren().setAll(pause, next, abort);
                break;
            case Paused:
                buttons.getChildren().setAll(resume, abort);
                break;
            case Logged:
                buttons.getChildren().clear();
                break;
            }
            // TODO mark active command
            System.out.println("Current address: " + info.getCurrentAddress());
        }
    }

    /** If currently monitoring a scan, detach */
    void detachFromScan()
    {
        final ScanInfoModel infos = scan_info_model.getAndSet(null);
        if (infos != null)
        {
            infos.removeListener(scan_info_listener);
            Platform.runLater(() -> updateScanInfo(null));
            infos.release();
        }
    }
}
