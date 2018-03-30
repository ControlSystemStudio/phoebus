/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.monitor;

import static org.csstudio.scan.ScanSystem.logger;

import java.util.logging.Level;

import org.csstudio.scan.ScanSystem;
import org.csstudio.scan.client.ScanClient;
import org.csstudio.scan.info.ScanState;
import org.csstudio.scan.ui.Messages;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

/** Table cell for {@link ScanState}
 *
 *  <p>
 *  Shows current state, color coded, with actions based on the current state
 */
@SuppressWarnings("nls")
class StateCell extends TableCell<ScanInfoProxy, ScanState>
{
    private final ScanClient scan_client;
    private final Label text = new Label();
    private Button pause, resume, next, abort, remove;
    private final HBox graphics = new HBox(5, text);

    /** Perform action on a scan */
    private static interface ScanAction
    {
        /** @param id ID of scan
         *  @throws Exception on error
         */
        void perform(long id) throws Exception;
    }

    StateCell(final ScanClient scan_client)
    {
        this.scan_client = scan_client;
        text.setPrefWidth(90);
    }

    private Button createButton(final String icon, final String tooltip, final ScanAction action)
    {
        final Button button = new Button();
        button.setMinSize(ButtonBase.USE_PREF_SIZE, ButtonBase.USE_PREF_SIZE);
        button.setPrefHeight(20);
        button.setGraphic(ImageCache.getImageView(ScanSystem.class, icon));
        button.setTooltip(new Tooltip(tooltip));
        button.setOnAction(event ->
        {
            try
            {
                action.perform(getTableRow().getItem().id.get());
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Failed: " + tooltip, ex);
            }
        });
        return button;
    }

    private Button getNext()
    {
        if (next == null)
            next = createButton("/icons/next.png", Messages.scan_next, id -> scan_client.nextCommand(id));
        return next;
    }

    private Button getPause()
    {
        if (pause == null)
            pause = createButton("/icons/pause.png", Messages.scan_pause, id -> scan_client.pauseScan(id));
        return pause;
    }

    private Button getResume()
    {
        if (resume == null)
            resume = createButton("/icons/resume.png", Messages.scan_resume, id -> scan_client.resumeScan(id));
        return resume;
    }

    private Button getAbort()
    {
        if (abort == null)
            abort = createButton("/icons/abort.png", Messages.scan_abort, id -> scan_client.abortScan(id));
        return abort;
    }

    private Button getRemove()
    {
        if (remove == null)
            remove = createButton("/icons/remove.png", Messages.scan_remove, id -> scan_client.removeScan(id));
        return remove;
    }

    private void show(final Button button)
    {
        graphics.getChildren().add(button);
    }

    @Override
    protected void updateItem(final ScanState state, final boolean empty)
    {
        super.updateItem(state, empty);
        if (empty)
            setGraphic(null);
        else
        {
            text.setText(state.toString());
            text.setTextFill(getStateColor(state));

            // Remove all but the label
            int i = graphics.getChildren().size();
            while (i > 1)
                graphics.getChildren().remove(--i);

            // Add suitable buttons
            switch (state)
            {
            case Idle:
                show(getAbort());
                break;
            case Running:
                show(getPause());
                show(getNext());
                show(getAbort());
                break;
            case Paused:
                show(getResume());
                show(getAbort());
                break;
            case Aborted:
            case Failed:
            case Finished:
            case Logged:
                show(getRemove());
                break;
            }

            setGraphic(graphics);
        }
    }

    static Color getStateColor(final ScanState state)
    {
        switch (state)
        {
        case Idle:      return Color.DARKBLUE;
        case Aborted:   return Color.DARKGOLDENROD;
        case Failed:    return Color.RED;
        case Finished:  return Color.DARKGREEN;
        case Paused:    return Color.GRAY;
        case Running:   return Color.GREEN;
        default:        return Color.BLACK;
        }
    }
}
