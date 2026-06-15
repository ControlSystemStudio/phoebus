/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import java.time.Duration;

import javafx.scene.control.Slider;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelListener;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.TextField;

/**
 * Controller for MiscTab.fxml.
 * Wire up via FXMLLoader:
 *   FXMLLoader loader = new FXMLLoader(getClass().getResource("MiscTab.fxml"), messages);
 *   MiscTabController ctrl = new MiscTabController(model, undo);
 *   loader.setController(ctrl);
 *   Tab tab = loader.load();
 */
@SuppressWarnings({"nls", "unused"})
public class MiscTabController
{
    // -----------------------------------------------------------------------
    // FXML-injected fields — names must match fx:id attributes in the FXML
    // -----------------------------------------------------------------------


    @FXML private TextField  title;
    @FXML private TextField  updatePeriod;
    @FXML private TextField  scrollStep;
    @FXML private CheckBox   saveChanges;
    @FXML private CheckBox   showLegend;
    @FXML private ColorPicker foreground;
    @FXML private ColorPicker background;
    @FXML private FontButton  titleFont;
    @FXML private FontButton  labelFont;
    @FXML private FontButton  scaleFont;
    @FXML private FontButton  legendFont;
    @FXML private Slider opacitySlider;

    // -----------------------------------------------------------------------
    // Non-FXML state
    // -----------------------------------------------------------------------

    private final Model model;
    private final UndoableActionManager undo;

    /** Prevents recursive updates when this controller writes to the model */
    private boolean updating = false;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public MiscTabController(final Model model, final UndoableActionManager undo)
    {
        this.model = model;
        this.undo  = undo;
    }

    // -----------------------------------------------------------------------
    // Lifecycle — called automatically by FXMLLoader after injection
    // -----------------------------------------------------------------------

    @FXML
    public void initialize()
    {


        // Seed ColorPickers with current model values
        foreground.setValue(model.getPlotForeground());
        background.setValue(model.getPlotBackground());

        // Register model listener so the tab stays in sync on undo/redo
        model.addListener(modelListener);

        // Populate fields with initial model state
        modelListener.changedTitle();
        modelListener.changedColorsOrFonts();
        modelListener.changedSaveChangesBehavior(model.shouldSaveChanges());
        modelListener.changedTiming();
        modelListener.changedLayout();

        opacitySlider.valueProperty().setValue(model.getAreaOpacity());
        opacitySlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            updating = true;
            model.setAreaOpacity((int)Math.round(opacitySlider.getValue()));
            updating = false;
        });
    }

    // -----------------------------------------------------------------------
    // onAction handlers — referenced from FXML via onAction="#..."
    // -----------------------------------------------------------------------

    @FXML
    private void onTitleAction()
    {
        updating = true;
        new ChangeTitleCommand(model, undo, title.getText());
        updating = false;
    }

    @FXML
    private void onUpdatePeriodAction()
    {
        updating = true;
        final double period = Double.parseDouble(updatePeriod.getText().trim());
        new ChangeUpdatePeriodCommand(model, undo, period);
        updating = false;
    }

    @FXML
    private void onScrollStepAction()
    {
        updating = true;
        try
        {
            final Duration step = Duration.ofMillis(
                Math.round(Double.parseDouble(scrollStep.getText().trim()) * 1000.0));
            new ChangeScrollStepCommand(model, undo, step);
        }
        catch (Exception ex)
        {
            scrollStep.setText(Double.toString(model.getScrollStep().toMillis() / 1000.0));
        }
        updating = false;
    }

    @FXML
    private void onForegroundAction()
    {
        updating = true;
        new ChangePlotForegroundCommand(model, undo, foreground.getValue());
        updating = false;
    }

    @FXML
    private void onBackgroundAction()
    {
        updating = true;
        new ChangePlotBackgroundCommand(model, undo, background.getValue());
        updating = false;
    }

    @FXML
    private void onSaveChangesAction()
    {
        updating = true;
        new ChangeSaveChangesCommand(model, undo, saveChanges.isSelected());
        updating = false;
    }

    @FXML
    private void onShowLegendAction()
    {
        updating = true;
        new ChangeShowLegendCommand(model, undo, showLegend.isSelected());
        updating = false;
    }

    // -----------------------------------------------------------------------
    // Model listener — keeps the tab in sync after undo / external changes
    // -----------------------------------------------------------------------

    private final ModelListener modelListener = new ModelListener()
    {
        @Override
        public void changedSaveChangesBehavior(boolean do_save_changes)
        {
            if (!updating)
                saveChanges.setSelected(do_save_changes);
        }

        @Override
        public void changedLayout()
        {
            if (!updating)
                showLegend.setSelected(model.isLegendVisible());
        }

        @Override
        public void changedTitle()
        {
            if (!updating)
                title.setText(model.getTitle().orElse(""));
        }

        @Override
        public void changedColorsOrFonts()
        {
            if (updating)
                return;
            foreground.setValue(model.getPlotForeground());
            background.setValue(model.getPlotBackground());
            titleFont.selectFont(model.getTitleFont());
            labelFont.selectFont(model.getLabelFont());
            scaleFont.selectFont(model.getScaleFont());
            legendFont.selectFont(model.getLegendFont());
        }

        @Override
        public void changedTiming()
        {
            if (updating)
                return;
            updatePeriod.setText(Double.toString(model.getUpdatePeriod()));
            scrollStep.setText(Double.toString(model.getScrollStep().toMillis() / 1000.0));
        }
    };
}
