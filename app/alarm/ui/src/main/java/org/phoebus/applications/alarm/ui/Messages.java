/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui;

import org.phoebus.framework.nls.NLS;

public class Messages
{
    public static String error;

    public static String acknowledgeFailed;
    public static String addComponentFailed;
    public static String AlarmAreaPanel;
    public static String actionsHeaderAlarm;
    public static String actionsHeaderComponent;
    public static String actionsLabel;
    public static String actionsLabelAlarmTooltip;
    public static String actionsLabelComponentTooltip;
    public static String actionsNone;
    public static String actionsPlural;
    public static String actionsSingular;
    public static String alarmProperties;
    public static String annunciatingLabel;
    public static String annunciatingLabelTooltip;
    public static String AlarmTable;
    public static String AlarmTree;
    public static String Annunciator;
    public static String behavior;
    public static String commandsHeaderAlarm;
    public static String commandsHeaderComponent;
    public static String commandsLabel;
    public static String commandsLabelAlarmTooltip;
    public static String commandsLabelComponentTooltip;
    public static String commandsNone;
    public static String commandsPlural;
    public static String commandsSingular;
    public static String componentProperties;
    public static String configChangedOnServer;
    public static String countLabel;
    public static String countLabelTooltip;
    public static String delayLabel;
    public static String delayLabelTooltip;
    public static String descriptionLabel;
    public static String descriptionLabelTooltip;
    public static String disableAlarmFailed;
    public static String disableAlarms;
    public static String disabled;
    public static String disabledIndefinitely;
    public static String disableMenu;
    public static String disabledUntil;
    public static String disabledCommonEnableDate;
    public static String disabledVaryingEnableDate;
    public static String discardChanges;
    public static String discardChangesPrompt;
    public static String displaysHeaderAlarm;
    public static String displaysHeaderComponent;
    public static String displaysLabel;
    public static String displaysLabelAlarmTooltip;
    public static String displaysLabelComponentTooltip;
    public static String displaysNone;
    public static String displaysPlural;
    public static String displaysSingular;
    public static String enabled;
    public static String enabledLabel;
    public static String enabledLabelTooltip;
    public static String enableDatetimeLabel;
    public static String enableDatetimeLabelTooltip;
    public static String enablingFilterLabel;
    public static String enablingFilterLabelTooltip;
    public static String enableAlarmFailed;
    public static String enableAlarms;
    public static String guidanceHeaderAlarm;
    public static String guidanceHeaderComponent;
    public static String guidanceLabel;
    public static String guidanceLabelAlarmTooltip;
    public static String guidanceLabelComponentTooltip;
    public static String guidanceNone;
    public static String guidancePlural;
    public static String guidanceSingular;
    public static String headerAlreadyDisabled;
    public static String headerAlreadyEnabled;
    public static String headerConfirmDisable;
    public static String headerConfirmDisableWithEnableDate;
    public static String headerConfirmEnable;
    public static String indefinitely;
    public static String info;
    public static String itemDeletedOnServer;
    public static String latchingLabel;
    public static String latchingLabelTooltip;
    public static String loading;
    public static String MenuPath;
    public static String moveItemFailed;
    public static String nameLabel;
    public static String nameLabelTooltip;
    public static String pathLabel;
    public static String pathLabelAlarmTooltip;
    public static String pathLabelComponentTooltip;
    public static String pvLabel;
    public static String pvLabelTooltip;
    public static String removeComponentFailed;
    public static String renameItemFailed;
    public static String saveChanges;
    public static String totalPVs;
    public static String unacknowledgeFailed;
    public static String updateAlarmItemConfig;
    public static String withEnableDate;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(Messages.class);
    }

    private Messages()
    {
        // prevent instantiation
    }
}
