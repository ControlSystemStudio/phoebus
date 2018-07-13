/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.ui.write;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import org.phoebus.ui.javafx.ImageCache;

import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * View for selecting log books and tags for a log entry.
 * @author Evan Smith
 */
public class LogbooksTagsView extends VBox
{
    private final LogEntryModel model;
    
    private static final Image tag_icon = ImageCache.getImage(LogbooksTagsView.class, "/icons/add_tag.png");
    private static final Image logbook_icon = ImageCache.getImage(LogbooksTagsView.class, "/icons/logbook-16.png");
    private static final Image down_icon = ImageCache.getImage(LogbooksTagsView.class, "/icons/down_triangle.png");

    private final HBox         logbookBox, tagBox;
    private final Label        logbookLabel, tagLabel;
    private final TextField    logbookField, tagField;
    private final ContextMenu  logbookDropDown, tagDropDown;
    private final ToggleButton logbookSelector, tagSelector; // Opens context menu (dropDown).
    private final Button       addLogbook, addTag;
    
    public LogbooksTagsView(LogEntryModel model)
    {
        this.model = model;
        
        logbookBox = new HBox();
        logbookLabel = new Label("Logbooks: ");
        logbookField = new TextField();
        logbookDropDown = new ContextMenu();
        logbookSelector = new ToggleButton("", new ImageView(down_icon));
        addLogbook = new Button("", new ImageView(logbook_icon));
        
        tagBox = new HBox();
        tagLabel = new Label("Tags: ");
        tagField = new TextField();
        tagDropDown = new ContextMenu();
        tagSelector = new ToggleButton("", new ImageView(down_icon));
        addTag = new Button("", new ImageView(tag_icon));
        
        setSpacing(10);
        
        formatLogbooks();
        formatTags();
        initializeSelectors();
        
        getChildren().addAll(logbookBox, tagBox);
    }
    
    /** Format log books HBox  */
    private void formatLogbooks()
    {
        Tooltip tooltip = new Tooltip("Add logbook to the log entry.");
        addLogbook.setTooltip(tooltip);
        logbookSelector.setTooltip(tooltip);
        logbookLabel.setPrefWidth(LogEntryDialog.labelWidth);
        logbookSelector.setOnAction(actionEvent -> 
        {
            if (logbookSelector.isSelected())
                logbookDropDown.show(logbookField, Side.BOTTOM, 0, 0);
            else
                logbookDropDown.hide();
        });
        
        logbookDropDown.showingProperty().addListener((changeListener, oldVal, newVal) ->
        {
            if (newVal && tagSelector.isSelected())
                tagSelector.setSelected(false);
        });
        
        logbookSelector.focusedProperty().addListener((changeListener, oldVal, newVal) ->
        {
            if (!newVal && !tagDropDown.isShowing() && !logbookDropDown.isShowing())
                logbookSelector.setSelected(false);
        });
        
        logbookField.setEditable(false);
       
        HBox.setHgrow(logbookField, Priority.ALWAYS);
        
        final String title = "Select Logbooks";
        addLogbook.setOnAction(event ->
        {
            ListSelectionDialog select = new ListSelectionDialog(getScene().getRoot(), title, model::getLogbooks, model::getSelectedLogbooks,
                logbook ->
                {
                    try
                    {
                        return model.addSelectedLogbook(logbook);
                    } 
                    catch (Exception ex)
                    {
                        logger.log(Level.SEVERE, "Internal log selection failed.", ex);
                    }
                    return false;
                }, 
                // Function throws Exception on internal error so use lambda to catch.
                logbook ->
                {
                    try
                    {
                        return model.removeSelectedLogbook(logbook);
                    } 
                    catch (Exception ex)
                    {
                        logger.log(Level.SEVERE, "Internal log selection failed.", ex);
                    }
                    return false;
                });
            
            Optional<Boolean> result = select.showAndWait();
            if (result.isPresent() && result.get())
                setFieldText(logbookDropDown, model.getSelectedLogbooks(), logbookField);
        });
        
        logbookBox.getChildren().addAll(logbookLabel, logbookField, logbookSelector, addLogbook);
        

        logbookBox.setSpacing(5);
        logbookBox.setAlignment(Pos.CENTER);
    }
    
    /** Format tags HBox  */
    private void formatTags()
    {
        Tooltip tooltip = new Tooltip("Add tag to the log entry.");
        addTag.setTooltip(tooltip);
        tagSelector.setTooltip(tooltip);
        tagLabel.setPrefWidth(LogEntryDialog.labelWidth);
        tagSelector.setOnAction(actionEvent -> 
        {
            if (tagSelector.isSelected())
                tagDropDown.show(tagField, Side.BOTTOM, 0, 0);
            else
                tagDropDown.hide();
        });
        
        tagDropDown.showingProperty().addListener((changeListener, oldVal, newVal) ->
        {
            if (newVal && logbookSelector.isSelected())
                logbookSelector.setSelected(false);
        });
        
        tagSelector.focusedProperty().addListener((changeListener, oldVal, newVal) ->
        {
            if (!newVal && !tagDropDown.isShowing() && !logbookDropDown.isShowing())
                tagSelector.setSelected(false);
        });
        
        tagField.setEditable(false);
       
        HBox.setHgrow(tagField, Priority.ALWAYS);
        
        final String title = "Select Tags";
        addTag.setOnAction(event ->
        {
            ListSelectionDialog select = new ListSelectionDialog(getScene().getRoot(), title, model::getTags, model::getSelectedTags,
                tag ->
                {
                    try
                    {
                        return model.addSelectedTag(tag);
                    } 
                    catch (Exception ex)
                    {
                        logger.log(Level.SEVERE, "Internal tag selection failed.", ex);
                    }
                    return false;
                }, 
                // Function throws Exception on internal error so use lambda to catch.
                tag ->
                {
                    try
                    {
                        return model.removeSelectedTag(tag);
                    } 
                    catch (Exception ex)
                    {
                        logger.log(Level.SEVERE, "Internal tag selection failed.", ex);
                    }
                    return false;
                });
            
            Optional<Boolean> result = select.showAndWait();
            if (result.isPresent() && result.get())
                setFieldText(tagDropDown, model.getSelectedTags(), tagField);
        });
        
        tagBox.getChildren().addAll(tagLabel, tagField, tagSelector, addTag);
        
        tagBox.setSpacing(5);
        tagBox.setAlignment(Pos.CENTER);
    }
    
    /** Initialize the drop down context menus by adding listeners to their content lists. */
    private void initializeSelectors()
    {
        model.addLogbookListener((ListChangeListener.Change<? extends String> c) -> 
        {
            if (c.next())
                c.getAddedSubList().forEach(newLogbook -> addToLogbookDropDown(newLogbook));
        });

        model.addTagListener((ListChangeListener.Change<? extends String> c) -> 
        {
            if (c.next())
                c.getAddedSubList().forEach(newTag -> addToTagDropDown(newTag));
        });
        
        // Once the listeners are added ask the model to fetch the lists.
        // This is done on a separate thread so only start it once the listeners are in place.
        model.fetchLists();
    }
    
    /**
     * Add a new CheckMenuItem to the drop down ContextMenu.
     * @param item - Item to be added.
     */
    private void addToLogbookDropDown(String item)
    {
        CheckBox checkBox = new CheckBox(item);
        CustomMenuItem newLogbook = new CustomMenuItem(checkBox);
        newLogbook.setHideOnClick(false);
        checkBox.setOnAction(new EventHandler<ActionEvent>()
        {
            public void handle(ActionEvent e)
            {
                CheckBox source = (CheckBox) e.getSource();
                String text = source.getText();
                if (model.getSelectedLogbooks().contains(text))
                {
                    try
                    {
                        model.removeSelectedLogbook(text);
                    } 
                    catch (Exception ex)
                    {
                        logger.log(Level.SEVERE, "Internal logbook selection failed.", ex);
                    }
                    setFieldText(logbookDropDown, model.getSelectedLogbooks(), logbookField);
                }
                else
                {
                    try
                    {
                        model.addSelectedLogbook(text);
                    } 
                    catch (Exception ex)
                    {
                        logger.log(Level.SEVERE, "Internal logbook selection failed.", ex);
                    }
                    setFieldText(logbookDropDown, model.getSelectedLogbooks(), logbookField);
                }
                
            }
        });
        logbookDropDown.getItems().add(newLogbook);
    }
    
    /**
     * Add a new CheckMenuItem to the drop down ContextMenu.
     * @param item - Item to be added.
     */
    private void addToTagDropDown(String item)
    {
        CheckBox checkBox = new CheckBox(item);
        CustomMenuItem newTag = new CustomMenuItem(checkBox);
        newTag.setHideOnClick(false);
        checkBox.setOnAction(new EventHandler<ActionEvent>()
        {
            public void handle(ActionEvent e)
            {
                CheckBox source = (CheckBox) e.getSource();
                String text = source.getText();
                if (model.getSelectedTags().contains(text))
                {
                    try
                    {
                        model.removeSelectedTag(text);
                    } 
                    catch (Exception ex)
                    {
                        logger.log(Level.SEVERE, "Internal tag selection failed.", ex);
                    }
                    setFieldText(tagDropDown, model.getSelectedTags(), tagField);
                }
                else
                {
                    try
                    {
                        model.addSelectedTag(text);
                    } 
                    catch (Exception ex)
                    {
                        logger.log(Level.SEVERE, "Internal tag selection failed.", ex);
                    }
                    setFieldText(tagDropDown, model.getSelectedTags(), tagField);
                }
                
            }
        });
        tagDropDown.getItems().add(newTag);        
    }
    
    /** Sets the field's text based on the selected items list.*/
    private void setFieldText(ContextMenu dropDown, List<String> selectedItems, TextField field)
    {
        // Handle drop down menu item checking.
        for (MenuItem menuItem : dropDown.getItems())
        {
            CustomMenuItem custom = (CustomMenuItem) menuItem;
            CheckBox check = (CheckBox) custom.getContent();
            // If the item is selected make sure it is checked.
            if (selectedItems.contains(check.getText()))
            {
                if (! check.isSelected()) 
                    check.setSelected(true);
            }
            // If the item is not selected, make sure it is not checked.
            else
            {
                if (check.isSelected())
                    check.setSelected(false);
            }
        }
        
        // Build the field text string.
        String fieldText = "";
        for (String item : selectedItems)
        {
            fieldText += (fieldText.isEmpty() ? "" : ", ") + item;
        }
        
        field.setText(fieldText);
    }
}