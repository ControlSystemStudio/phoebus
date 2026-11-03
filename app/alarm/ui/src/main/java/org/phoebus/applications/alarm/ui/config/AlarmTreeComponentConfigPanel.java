package org.phoebus.applications.alarm.ui.config;

import javafx.geometry.HPos;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.ui.Messages;


public class AlarmTreeComponentConfigPanel extends AlarmTreeConfigPanel
{
    /** The path of the node that this panel is for. Does not account for newName */
    private final String pathName;

    /** Detatched node storing the item's configuration when the panel was created */
    private final AlarmClientNode initialConfig;

    /** Detached node storing the item's configuration including the changes the user has made */
    private final AlarmClientNode newConfig;

    public AlarmTreeComponentConfigPanel(final AlarmClientNode component)
    {
        super();

        pathName = component.getPathName();
        initialConfig = component.createDetachedCopy();
        newConfig = component.createDetachedCopy();

        getColumnConstraints().add(new ColumnConstraints(32, USE_COMPUTED_SIZE, Integer.MAX_VALUE, Priority.ALWAYS, HPos.LEFT, true));
        getColumnConstraints().add(new ColumnConstraints(32, USE_COMPUTED_SIZE, Integer.MAX_VALUE, Priority.ALWAYS, HPos.LEFT, true));
        getColumnConstraints().get(0).setPercentWidth(40);

        addCategory(Messages.componentProperties);
        addDisabledTextRow(Messages.pathLabel, Messages.pathLabelComponentTooltip, component.getPathName());
        addDisabledTextRow(Messages.nameLabel, Messages.nameLabelTooltip, initialConfig.getName());

        addSubcategory(Messages.info);
        addTitleDetailListBox(
                Messages.guidanceLabel,
                Messages.guidanceLabelComponentTooltip,
                Messages.guidanceHeaderComponent,
                Messages.guidanceNone,
                Messages.guidanceSingular,
                Messages.guidancePlural,
                initialConfig.getGuidance(),
                newConfig::getGuidance,
                newConfig::setGuidance
        );
        addTitleDetailListBox(
                Messages.displaysLabel,
                Messages.displaysLabelComponentTooltip,
                Messages.displaysHeaderComponent,
                Messages.displaysNone,
                Messages.displaysSingular,
                Messages.displaysPlural,
                initialConfig.getDisplays(),
                newConfig::getDisplays,
                newConfig::setDisplays
        );
        addTitleDetailListBox(
                Messages.commandsLabel,
                Messages.commandsLabelComponentTooltip,
                Messages.commandsHeaderComponent,
                Messages.commandsNone,
                Messages.commandsSingular,
                Messages.commandsPlural,
                initialConfig.getCommands(),
                newConfig::getCommands,
                newConfig::setCommands
        );

        addSubcategory(Messages.behavior);
        addTitleDetailDelayListBox(
                Messages.actionsLabel,
                Messages.actionsLabelComponentTooltip,
                Messages.actionsHeaderComponent,
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
