package org.phoebus.applications.alarm.ui.config;

import javafx.geometry.HPos;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.ui.Messages;


public class AlarmTreeAlarmConfigPanel extends AlarmTreeConfigPanel
{
    /** The path of the node that this panel is for. Does not account for newName */
    private final String pathName;

    /** Detatched node storing the item's configuration when the panel was created */
    private final AlarmClientLeaf initialConfig;

    /** Detached node storing the item's configuration including the changes the user has made */
    private final AlarmClientLeaf newConfig;

    public AlarmTreeAlarmConfigPanel(final AlarmClientLeaf alarm)
    {
        super();

        pathName = alarm.getPathName();
        initialConfig = alarm.createDetachedCopy();
        newConfig = alarm.createDetachedCopy();

        getColumnConstraints().add(new ColumnConstraints(32, USE_COMPUTED_SIZE, Integer.MAX_VALUE, Priority.ALWAYS, HPos.LEFT, true));
        getColumnConstraints().add(new ColumnConstraints(32, USE_COMPUTED_SIZE, Integer.MAX_VALUE, Priority.ALWAYS, HPos.LEFT, true));
        getColumnConstraints().get(0).setPercentWidth(40);

        addCategory(Messages.alarmProperties);
        addDisabledTextRow(Messages.pathLabel, Messages.pathLabelAlarmTooltip, alarm.getPathName());
        addDisabledTextRow(Messages.pvLabel, Messages.pvLabelTooltip, initialConfig.getName());

        addSubcategory(Messages.info);
        addTextRow(Messages.descriptionLabel, Messages.descriptionLabelTooltip, initialConfig.getDescription(), newConfig::setDescription);
        addTitleDetailListBox(
                Messages.guidanceLabel,
                Messages.guidanceLabelAlarmTooltip,
                Messages.guidanceHeaderAlarm,
                Messages.guidanceNone,
                Messages.guidanceSingular,
                Messages.guidancePlural,
                initialConfig.getGuidance(),
                newConfig::getGuidance,
                newConfig::setGuidance
        );
        addTitleDetailListBox(
                Messages.displaysLabel,
                Messages.displaysLabelAlarmTooltip,
                Messages.displaysHeaderAlarm,
                Messages.displaysNone,
                Messages.displaysSingular,
                Messages.displaysPlural,
                initialConfig.getDisplays(),
                newConfig::getDisplays,
                newConfig::setDisplays
        );
        addTitleDetailListBox(
                Messages.commandsLabel,
                Messages.commandsLabelAlarmTooltip,
                Messages.commandsHeaderAlarm,
                Messages.commandsNone,
                Messages.commandsSingular,
                Messages.commandsPlural,
                initialConfig.getCommands(),
                newConfig::getCommands,
                newConfig::setCommands
        );

        addSubcategory(Messages.enabled);
        final CheckBox enabledCheckBox = addCheckboxRow(Messages.enabledLabel, Messages.enabledLabelTooltip, initialConfig.isEnabled(), newConfig::setEnabled);
        final DateTimePicker enableDateTimePicker = addDateTimeRow(Messages.enableDatetimeLabel, Messages.enableDatetimeLabelTooltip, initialConfig.getEnabledDate(), newConfig::setEnabledDate);

        enableDateTimePicker.setDisable(enabledCheckBox.isSelected());
        enabledCheckBox.selectedProperty().addListener(_ -> {
            enableDateTimePicker.setDisable(enabledCheckBox.isSelected());
            if (enabledCheckBox.isSelected()) {
                enableDateTimePicker.setDateTimeValue(null);
            }
        });

        addTextRow(Messages.enablingFilterLabel, Messages.enablingFilterLabelTooltip, initialConfig.getFilter(), newConfig::setFilter);

        addSubcategory(Messages.behavior);
        addCheckboxRow(Messages.latchingLabel, Messages.latchingLabelTooltip, initialConfig.isLatching(), newConfig::setLatching);
        addCheckboxRow(Messages.annunciatingLabel, Messages.annunciatingLabelTooltip, initialConfig.isAnnunciating(), newConfig::setAnnunciating);
        addIntSpinnerRow(Messages.delayLabel, Messages.delayLabelTooltip, initialConfig.getDelay(), newConfig::setDelay);
        addIntSpinnerRow(Messages.countLabel, Messages.countLabelTooltip, initialConfig.getCount(), newConfig::setCount);
        addTitleDetailDelayListBox(
                Messages.actionsLabel,
                Messages.actionsLabelAlarmTooltip,
                Messages.actionsHeaderAlarm,
                Messages.actionsNone,
                Messages.actionsSingular,
                Messages.actionsPlural,
                initialConfig.getActions(),
                newConfig::getActions,
                newConfig::setActions
        );

        add(new HBox(saveButton, discardButton), 0, getRowCount() + 1, 2, 1);
    }

    @Override
    public String getPathName()
    {
        return pathName;
    }

    @Override
    public AlarmTreeItem<?> getInitialConfig()
    {
        return initialConfig;
    }

    @Override
    public AlarmTreeItem<?> getNewConfig()
    {
        return newConfig;
    }
}
