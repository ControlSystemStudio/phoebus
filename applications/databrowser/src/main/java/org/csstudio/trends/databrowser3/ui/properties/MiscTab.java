/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelListener;
import org.phoebus.ui.dialog.PopOver;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;

/** Property tab for misc. items
 *  @author Kay Kasemir
 */
public class MiscTab extends Tab
{
    private final Model model;

    private final UndoableActionManager undo;


    /** Flag to prevent recursion when this tab updates the model and thus triggers the model_listener */
    private boolean updating = false;

    /** Update Tab when model changes (undo, ...) */
    private ModelListener model_listener = new ModelListener()
    {
     };

    private TextField title;


    MiscTab(final Model model, final UndoableActionManager undo)
    {
        super(Messages.Miscellaneous);
        this.model = model;
        this.undo = undo;

        final GridPane layout = new GridPane();
        layout.setHgap(5);
        layout.setVgap(5);
        layout.setPadding(new Insets(5));
        // layout.setGridLinesVisible(true); // Debug layout


        layout.add(new Label(Messages.TitleLbl), 0, 0);
        title = new TextField();
        title.setTooltip(new Tooltip(Messages.TitleTT));
        title.setOnAction(event ->  new ChangeTitleCommand(model, undo, title.getText()));
        layout.add(title, 1, 0);

        layout.add(new Label(Messages.UpdatePeriodLbl), 0, 1);

        layout.add(new Label(Messages.ScrollStepLbl), 0, 2);


        layout.add(new Label(Messages.BackgroundColorLbl), 0, 4);


        layout.add(new Label(Messages.SaveChangesLbl), 0, 5);
        layout.add(new CheckBox(), 1, 5);


        layout.add(new Label(Messages.TitleFontLbl), 2, 0);
        final Button title_font = new FontButton(Messages.TitleFontLbl);
        layout.add(title_font, 3, 0);

        layout.add(new Label(Messages.LabelFontLbl), 2, 1);

        layout.add(new Label(Messages.ScaleFontLbl), 2, 2);

        layout.add(new Label(Messages.LegendFontLbl), 2, 3);

        setContent(layout);
    }

    // TODO Extract
    static class FontButton extends Button
    {
        public FontButton(final String label)
        {
            super(label);

            final Node content = createContent();
            final PopOver popover = new PopOver(content);
            setOnAction(event ->
            {
                if (popover.isShowing())
                    popover.hide();
                else
                    popover.show(this);
            });
        }

        private Node createContent()
        {
            // TODO Name: combo
            //      Size: spinner
            //      Style: [ ] Bold [ ] Italic
            //
            //             OK   Cancel

            final GridPane layout = new GridPane();
            layout.setPadding(new Insets(5));
            layout.setHgap(5);
            layout.setVgap(5);

            layout.add(new Label("Font Name:"), 0, 0);
            layout.add(new ComboBox<String>(), 1, 0, 2, 1);

            layout.add(new Label("Size:"), 0, 1);
            layout.add(new Spinner<>(), 1, 1);

            layout.add(new Label("Style:"), 0, 2);
            layout.add(new CheckBox("Bold"), 1, 2);
            layout.add(new CheckBox("Italics"), 2, 2);

            layout.add(new Label("Example Text"), 0, 3, 3, 1);

            final ButtonBar buttons = new ButtonBar();
            Button yesButton = new Button("Ok");
            ButtonBar.setButtonData(yesButton, ButtonData.OK_DONE);
            Button noButton = new Button("Cancel");
            ButtonBar.setButtonData(noButton, ButtonData.CANCEL_CLOSE);
            buttons.getButtons().addAll(yesButton, noButton);
            layout.add(buttons, 0, 4, 4, 1);

            return layout;
        }
    }
}
