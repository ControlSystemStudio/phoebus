/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.pvtree;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.applications.pvtree.ui.FXTree;
import org.phoebus.applications.pvtree.ui.Messages;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.ui.dnd.DataFormats;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** PV Tree Application
 *  @author Kay Kasemir
 */
// @ProviderFor(Application.class)
@SuppressWarnings("nls")
public class PVTree
{
    public static final Logger logger = Logger.getLogger(PVTree.class.getPackageName());
    public static final String NAME = "PV Tree";

    private final TextField pv_name = new TextField();
    private final FXTree tree = new FXTree();

    public String getName()
    {
        return NAME;
    }

    public void start()
    {
        final Label label = new Label(Messages.PV_Label);
        pv_name.setOnAction(event -> setPVName(pv_name.getText()));
        pv_name.setTooltip(new Tooltip(Messages.PV_TT));

        final ToggleButton latch = new ToggleButton(null, getImageView("run.png"));
        latch.setTooltip(new Tooltip(Messages.LatchTT));
        latch.setOnAction(event ->
        {
            tree.getModel().latchOnAlarm(latch.isSelected());
            if (latch.isSelected())
                latch.setGraphic(getImageView("pause_on_alarm.png"));
            else
                latch.setGraphic(getImageView("run.png"));
        });

        final Button collapse = new Button(null, getImageView("collapse.gif"));
        collapse.setTooltip(new Tooltip(Messages.CollapseTT));
        collapse.setOnAction(event -> tree.expandAll(false));

        final Button alarms = new Button(null, getImageView("alarmtree.png"));
        alarms.setTooltip(new Tooltip(Messages.ExpandAlarmsTT));
        alarms.setOnAction(event -> tree.expandAlarms());

        final Button expand = new Button(null, getImageView("pvtree.png"));
        expand.setTooltip(new Tooltip(Messages.ExpandAllTT));
        expand.setOnAction(event -> tree.expandAll(true));

        // center vertically
        label.setMaxHeight(Double.MAX_VALUE);
        HBox.setHgrow(pv_name, Priority.ALWAYS);
        final HBox top = new HBox(5, label, pv_name, latch, collapse, alarms, expand);
        BorderPane.setMargin(top, new Insets(5, 5, 0, 5));
        BorderPane.setMargin(tree.getNode(), new Insets(5));
        final BorderPane layout = new BorderPane(tree.getNode());
        layout.setTop(top);

        hookDragDrop(layout);

        final DockItem tab = new DockItem(getName(), layout);
        DockPane.getActiveDockPane().addTab(tab);

        tab.setOnClosed(event -> stop());
    }

    private void hookDragDrop(final BorderPane layout)
    {
        // Allow dropping PV name
        layout.setOnDragOver(event ->
        {
            final Dragboard db = event.getDragboard();
            if (db.hasContent(DataFormats.ProcessVariables) ||
                db.hasString())
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            event.consume();
        });
        layout.setOnDragDropped(event ->
        {
            final Dragboard db = event.getDragboard();
            if (db.hasContent(DataFormats.ProcessVariables))
            {
                // Use last PV in case there's more than one
                @SuppressWarnings("unchecked")
                final List<ProcessVariable> pvs = (List<ProcessVariable>) db.getContent(DataFormats.ProcessVariables);
                if (pvs.size() > 0)
                    setPVName(pvs.get(pvs.size()-1).getName());
            }
            else if (db.hasString())
                setPVName(db.getString());
            event.setDropCompleted(true);
            event.consume();
        });
    }

    public void stop()
    {
        logger.log(Level.INFO, "Stopping PV Tree...");
        tree.shutdown();
        // System.out.println("Remaining PVs " + PVPool.getPVReferences());
    }

    private ImageView getImageView(final String icon)
    {
        return new ImageView(new Image(getClass().getResourceAsStream("/icons/" + icon)));
    }

    void setPVName(String name)
    {
        name = name.trim();
        pv_name.setText(name);
        tree.setPVName(name);
    }
}
