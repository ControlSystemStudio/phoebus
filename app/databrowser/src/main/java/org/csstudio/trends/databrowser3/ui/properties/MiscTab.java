/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import java.time.Duration;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelListener;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;

/** Property tab for misc. items
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MiscTab extends Tab
{
    private final Model model;

    /** Flag to prevent recursion when this tab updates the model and thus triggers the model_listener */
    private boolean updating = false;

    private final TextField title = new TextField(), update_period = new TextField(), scroll_step = new TextField();
    private CheckBox save_changes = new CheckBox(), show_legend = new CheckBox();
    private ColorPicker foreground = new ColorPicker(), background = new ColorPicker();
    private FontButton title_font, label_font, scale_font, legend_font;

    /** Update Tab when model changes (undo, ...) */
    private ModelListener model_listener = new ModelListener()
    {
        @Override
        public void changedSaveChangesBehavior(boolean do_save_changes)
        {
            if (updating)
                return;
            save_changes.setSelected(do_save_changes);
        }

        @Override
        public void changedLayout()
        {
            if (updating)
                return;
            show_legend.setSelected(model.isLegendVisible());
        }

        @Override
        public void changedTitle()
        {
            if (updating)
                return;
            title.setText(model.getTitle().orElse(""));
        }

        @Override
        public void changedColorsOrFonts()
        {
            if (updating)
                return;
            foreground.setValue(model.getPlotForeground());
            background.setValue(model.getPlotBackground());
            title_font.selectFont(model.getTitleFont());
            label_font.selectFont(model.getLabelFont());
            scale_font.selectFont(model.getScaleFont());
            legend_font.selectFont(model.getLegendFont());
        }

        @Override
        public void changedTiming()
        {
            if (updating)
                return;
            update_period.setText(Double.toString(model.getUpdatePeriod()));
            scroll_step.setText(Double.toString(model.getScrollStep().toMillis() / 1000.0));
        }
    };


    MiscTab(final Model model, final UndoableActionManager undo)
    {
        super(Messages.Miscellaneous);
        this.model = model;

        final GridPane layout = new GridPane();
        layout.setHgap(5);
        layout.setVgap(5);
        layout.setPadding(new Insets(5));
        // layout.setGridLinesVisible(true); // Debug layout


        layout.add(new Label(Messages.TitleLbl), 0, 0);
        title.setTooltip(new Tooltip(Messages.TitleTT));
        title.setOnAction(event ->
        {
            updating = true;
            new ChangeTitleCommand(model, undo, title.getText());
            updating = false;
        });
        layout.add(title, 1, 0);

        layout.add(new Label(Messages.UpdatePeriodLbl), 0, 1);
        layout.add(update_period, 1, 1);
        update_period.setOnAction(event ->
        {
            updating = true;
            final double period = Double.parseDouble(update_period.getText().trim());
            new ChangeUpdatePeriodCommand(model, undo, period);
            updating = false;
        });

        layout.add(new Label(Messages.ScrollStepLbl), 0, 2);
        layout.add(scroll_step, 1, 2);
        scroll_step.setOnAction(event ->
        {
            updating = true;
            try
            {
                final Duration step = Duration.ofMillis(
                        Math.round( Double.parseDouble(scroll_step.getText().trim()) * 1000.0 ) );
                new ChangeScrollStepCommand(model, undo, step);
            }
            catch (Exception ex)
            {
                scroll_step.setText(Double.toString(model.getScrollStep().toMillis() / 1000.0));
            }
            updating = false;
        });

        layout.add(new Label(Messages.ForegroundColorLbl), 0, 3);
        foreground.setStyle("-fx-color-label-visible: false ;");
        layout.add(foreground, 1, 3);
        foreground.setValue(model.getPlotForeground());
        foreground.setOnAction(event ->
        {
            updating = true;
            new ChangePlotForegroundCommand(model, undo, foreground.getValue());
            updating = false;
        });

        layout.add(new Label(Messages.BackgroundColorLbl), 0, 4);
        background.setStyle("-fx-color-label-visible: false ;");
        layout.add(background, 1, 4);
        background.setValue(model.getPlotBackground());
        background.setOnAction(event ->
        {
            updating = true;
            new ChangePlotBackgroundCommand(model, undo, background.getValue());
            updating = false;
        });

        layout.add(new Label(Messages.SaveChangesLbl), 0, 5);
        save_changes.setTooltip(new Tooltip(Messages.SaveChangesTT));
        save_changes.setOnAction(event ->
        {
            updating = true;
            new ChangeSaveChangesCommand(model, undo, save_changes.isSelected());
            updating = false;
        });
        layout.add(save_changes, 1, 5);

        layout.add(new Label(Messages.TitleFontLbl), 2, 0);
        title_font = new FontButton(model.getTitleFont(),
            font -> new ChangeFontCommand(model, undo, model.getTitleFont(), font, (m, f) -> m.setTitleFont(f)));
        title_font.setMaxWidth(Double.MAX_VALUE);
        layout.add(title_font, 3, 0);

        layout.add(new Label(Messages.LabelFontLbl), 2, 1);
        label_font = new FontButton(model.getLabelFont(),
            font -> new ChangeFontCommand(model, undo, model.getLabelFont(), font, (m, f) -> m.setLabelFont(f)));
        label_font.setMaxWidth(Double.MAX_VALUE);
        layout.add(label_font, 3, 1);

        layout.add(new Label(Messages.ScaleFontLbl), 2, 2);
        scale_font = new FontButton(model.getScaleFont(),
            font -> new ChangeFontCommand(model, undo, model.getScaleFont(), font, (m, f) -> m.setScaleFont(f)));
        scale_font.setMaxWidth(Double.MAX_VALUE);
        layout.add(scale_font, 3, 2);

        layout.add(new Label(Messages.LegendFontLbl), 2, 3);
        legend_font = new FontButton(model.getLegendFont(),
            font -> new ChangeFontCommand(model, undo, model.getLegendFont(), font, (m, f) -> m.setLegendFont(f)));
        legend_font.setMaxWidth(Double.MAX_VALUE);
        layout.add(legend_font, 3, 3);

        layout.add(new Label(Messages.LegendLbl), 2, 4);
        show_legend.setOnAction(event ->
        {
            updating = true;
            new ChangeShowLegendCommand(model, undo, show_legend.isSelected());
            updating = false;
        });
        layout.add(show_legend, 3, 4);

        setContent(layout);

        model.addListener(model_listener);

        // Initial values
        model_listener.changedTitle();
        model_listener.changedColorsOrFonts();
        model_listener.changedSaveChangesBehavior(model.shouldSaveChanges());
        model_listener.changedTiming();
        model_listener.changedLayout();
    }
}
