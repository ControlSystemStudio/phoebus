/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;

/** Dialog for editing {@link AlarmTreeItem}
 *
 *  <p>When pressing "OK", dialog sends updated
 *  configuration.
 */
@SuppressWarnings("nls")
class ItemConfigDialog extends Dialog<Boolean>
{
    private TextField description;
    private CheckBox enabled, latching, annunciating;
    private Spinner<Integer> delay, count;
    private TextField filter;
    private final TitleDetailTable guidance, displays, commands, actions;

    public ItemConfigDialog(final AlarmClient model, final AlarmTreeItem<?> item)
    {
        // Allow multiple instances
        initModality(Modality.NONE);
        setTitle("Configure " + item.getPathName());

        final GridPane layout = new GridPane();
        // layout.setGridLinesVisible(true); // Debug layout
        layout.setHgap(5);
        layout.setVgap(5);

        int row = 0;
        if (item instanceof AlarmClientLeaf)
        {
            final AlarmClientLeaf leaf = (AlarmClientLeaf) item;

            layout.add(new Label("Description:"), 0, row);
            description = new TextField(leaf.getDescription());
            description.setTooltip(new Tooltip("Alarm description, also used for annunciation"));
            layout.add(description, 1, row++);
            GridPane.setHgrow(description, Priority.ALWAYS);

            layout.add(new Label("Behavior:"), 0, row);
            enabled = new CheckBox("Enabled");
            enabled.setTooltip(new Tooltip("Enable alarms? See also filter expression"));
            enabled.setSelected(leaf.isEnabled());

            latching = new CheckBox("Latch");
            latching.setTooltip(new Tooltip("Latch alarm until acknowledged?"));
            latching.setSelected(leaf.isLatching());

            annunciating = new CheckBox("Annunciate");
            annunciating.setTooltip(new Tooltip("Request audible alarm annunciation (using the description)?"));
            annunciating.setSelected(leaf.isAnnunciating());

            layout.add(new HBox(10, enabled, latching, annunciating), 1, row++);

            layout.add(new Label("Alarm Delay [seconds]:"), 0, row);
            delay = new Spinner<>(0, Integer.MAX_VALUE, leaf.getDelay());
            delay.setTooltip(new Tooltip("Alarms are indicated when they persist for at least this long"));
            delay.setEditable(true);
            delay.setPrefWidth(80);
            layout.add(delay, 1, row++);

            layout.add(new Label("Alarm Count [within delay]:"), 0, row);
            count = new Spinner<>(0, Integer.MAX_VALUE, leaf.getCount());
            count.setTooltip(new Tooltip("Alarms are indicated when they occur this often within the delay"));
            count.setEditable(true);
            count.setPrefWidth(80);
            layout.add(count, 1, row++);

            layout.add(new Label("Enabling Filter"), 0, row);
            filter = new TextField(leaf.getFilter());
            filter.setTooltip(new Tooltip("Optional expression for enabling the alarm"));
            layout.add(filter, 1, row++);

            // Disable most of the detail when PV not enabled
            final ChangeListener<? super Boolean> enablement = (p, old, enable) ->
            {
                latching.setDisable(!enable);
                annunciating.setDisable(!enable);
                delay.setDisable(!enable);
                count.setDisable(!enable);
                filter.setDisable(!enable);
            };
            enabled.selectedProperty().addListener(enablement);
            enablement.changed(null, null, leaf.isEnabled());


            // Initial focus on description
            Platform.runLater(() -> description.requestFocus());
        }

        // Layout has two column
        // The PV-specific items above use two columns.
        // If there's no PV,
        // the following items use one column or span two columns.
        // There must be _something_ in the second column with Hgrow=Always
        // to cause the layout to fill its parent area.
        // 'dummy' is used for that.

        // Guidance:
        layout.add(new Label("Guidance:"), 0, row);
        final Label dummy = new Label("");
        GridPane.setHgrow(dummy, Priority.ALWAYS);
        layout.add(dummy, 1, row++);
        guidance = new TitleDetailTable(item.getGuidance());
        guidance.setPrefHeight(100);
        layout.add(guidance, 0, row++, 2, 1);

        // Displays:
        layout.add(new Label("Displays:"), 0, row++);
        displays = new TitleDetailTable(item.getDisplays());
        displays.setPrefHeight(100);
        layout.add(displays, 0, row++, 2, 1);

        // Commands:
        layout.add(new Label("Commands:"), 0, row++);
        commands = new TitleDetailTable(item.getCommands());
        commands.setPrefHeight(100);
        layout.add(commands, 0, row++, 2, 1);

        // Automated Actions:
        layout.add(new Label("Automated Actions:"), 0, row++);
        actions = new TitleDetailTable(item.getActions());
        actions.setPrefHeight(100);
        layout.add(actions, 0, row++, 2, 1);

        // Dialog is quite high; allow scroll
        final ScrollPane scroll = new ScrollPane(layout);

        // Scroll pane stops the content from resizing,
        // so tell content to use the widths of the scroll pane,
        // minus some border, and suggest minimum width
        scroll.widthProperty().addListener((p, old, width) -> layout.setPrefWidth(Math.max(width.doubleValue()-25, 450)));

        getDialogPane().setContent(scroll);
        setResizable(true);

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        final Button ok = (Button) getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, event ->
        {
             if (!validateAndStore(model, item))
                 event.consume();
        });

        setResultConverter(button -> button == ButtonType.OK);
    }

    /** Send requested configuration
     *  @param model {@link AlarmClient}
     *  @param item Original item
     *  @return <code>true</code> on success
     */
    private boolean validateAndStore(final AlarmClient model, final AlarmTreeItem<?> item)
    {
        final AlarmTreeItem<?> config;

        if (item instanceof AlarmClientLeaf)
        {
            final AlarmClientLeaf pv = new AlarmClientLeaf(null, item.getName());
            pv.setDescription(description.getText().trim());
            pv.setEnabled(enabled.isSelected());
            pv.setLatching(latching.isSelected());
            pv.setAnnunciating(annunciating.isSelected());
            pv.setDelay(delay.getValue());
            pv.setCount(count.getValue());
            // TODO Check filter expression
            pv.setFilter(filter.getText().trim());
            config = pv;
        }
        else
            config = new AlarmClientNode(null, item.getName());
        config.setGuidance(guidance.getItems());
        config.setDisplays(displays.getItems());
        config.setCommands(commands.getItems());
        config.setActions(actions.getItems());

        try
        {
            model.sendItemConfigurationUpdate(item.getPathName(), config);
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError("Error", "Cannot update item", ex);
            return false;
        }

        return true;
    }
}