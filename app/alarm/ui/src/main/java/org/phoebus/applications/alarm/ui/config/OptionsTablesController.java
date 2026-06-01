/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.alarm.ui.config;

import javafx.fxml.FXML;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.TitleDetail;
import org.phoebus.applications.alarm.model.TitleDetailDelay;
import org.phoebus.applications.alarm.ui.Messages;

import java.util.List;

/**
 * Controller for the container of the guidance, displays... table views.
 */
public class OptionsTablesController {

    @SuppressWarnings("unused")
    @FXML
    private TitleDetailTableController guidanceViewController;
    @SuppressWarnings("unused")
    @FXML
    private TitleDetailTableController displaysViewController;
    @SuppressWarnings("unused")
    @FXML
    private TitleDetailTableController commandsViewController;
    @SuppressWarnings("unused")
    @FXML
    private TitleDetailDelayTableController actionsViewController;

    private final AlarmTreeItem<?> item;

    public OptionsTablesController(final AlarmTreeItem<?> item) {
        this.item = item;
    }

    public void initialize(){
        guidanceViewController.setTitle(Messages.guidance);
        guidanceViewController.setItems(item.getGuidance());
        displaysViewController.setTitle(Messages.displays);
        displaysViewController.setItems(item.getDisplays());
        commandsViewController.setTitle(Messages.commands);
        commandsViewController.setItems(item.getCommands());
        actionsViewController.setTitle(Messages.automatedActions);
        actionsViewController.setItems(item.getActions());
    }

    public List<TitleDetail> getGuidance(){
        return guidanceViewController.getItems().stream().map(i -> (TitleDetail)i).toList();
    }

    public List<TitleDetail> getDisplays(){
        return displaysViewController.getItems().stream().map(i -> (TitleDetail)i).toList();
    }

    public List<TitleDetail> getCommands(){
        return commandsViewController.getItems().stream().map(i -> (TitleDetail)i).toList();
    }

    public List<TitleDetailDelay> getActions(){
        return actionsViewController.getItems().stream().map(i -> (TitleDetailDelay)i).toList();
    }
}
