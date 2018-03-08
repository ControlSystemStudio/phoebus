/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor;

import org.csstudio.scan.ScanSystem;
import org.csstudio.scan.client.ScanClient;
import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.ui.Messages;
import org.csstudio.scan.ui.editor.properties.Properties;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.ToolbarHelper;
import org.phoebus.ui.undo.UndoButtons;
import org.phoebus.ui.undo.UndoableActionManager;

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
public class ScanEditor extends SplitPane
{
    private final Model model = new Model();
    private final UndoableActionManager undo = new UndoableActionManager(50);

    private final Button submit, simulate, pause, resume, next, abort;

    private final Label info_text = new Label();

    private final HBox buttons = new HBox(5);
    private final ToolBar toolbar;
    private final ScanCommandTree scan_tree = new ScanCommandTree(model, undo);

    public ScanEditor()
    {
        // TODO Simulate means this editor needs to submit, get a _new_ scan ID and thus maintain the ScanInfoClient
        submit = new Button();
        submit.setGraphic(ImageCache.getImageView(ScanSystem.class, "/icons/run.png"));
        submit.setTooltip(new Tooltip(Messages.scan_submit));

        simulate = new Button();
        simulate.setGraphic(ImageCache.getImageView(ScanSystem.class, "/icons/simulate.png"));
        simulate.setTooltip(new Tooltip(Messages.scan_simulate));

        pause = new Button();
        pause.setGraphic(ImageCache.getImageView(ScanSystem.class, "/icons/pause.png"));
        pause.setTooltip(new Tooltip(Messages.scan_pause));

        resume = new Button();
        resume.setGraphic(ImageCache.getImageView(ScanSystem.class, "/icons/resume.png"));
        resume.setTooltip(new Tooltip(Messages.scan_resume));

        next = new Button();
        next.setGraphic(ImageCache.getImageView(ScanSystem.class, "/icons/next.png"));
        next.setTooltip(new Tooltip(Messages.scan_next));

        abort = new Button();
        abort.setGraphic(ImageCache.getImageView(ScanSystem.class, "/icons/abort.png"));
        abort.setTooltip(new Tooltip(Messages.scan_abort));

        final Button[] undo_redo = UndoButtons.createButtons(undo);
        toolbar = new ToolBar(info_text, buttons, ToolbarHelper.createSpring(), undo_redo[0], undo_redo[1]);

        VBox.setVgrow(scan_tree, Priority.ALWAYS);
        final VBox left_stack = new VBox(toolbar, scan_tree);

        final SplitPane right_stack = new SplitPane(new Palette(model, undo), new Properties(scan_tree, undo));
        right_stack.setOrientation(Orientation.VERTICAL);

        getItems().setAll(left_stack, right_stack);
        setDividerPositions(0.6);

        updateScanInfo(null);
    }

    /** @return Model */
    public Model getModel()
    {
        return model;
    }

    /** @return UndoableActionManager */
    UndoableActionManager getUndo()
    {
        return undo;
    }

    void attachScan(final long id, final ScanClient scan_client)
    {
        System.out.println("Attach to scan #" + id);
        info_text.setText("Scan #" + id);
    }

    void updateScanInfo(final ScanInfo info)
    {
        if (info == null)
        {
            info_text.setText("");
            buttons.getChildren().setAll(submit, simulate);
        }
        else
        {
            info_text.setText(info.toString());
            switch (info.getState())
            {
            case Idle:
                buttons.getChildren().setAll(submit, simulate, abort);
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
        }
    }

    void detachScan()
    {
        System.out.println("Detach");
        // Remove scan-related toolbar items , keep info, spring, undo, redo
//        final int size = toolbar.getItems().size();
//        if (size > 4)
//            toolbar.getItems().remove(1, size-3);
    }
}
