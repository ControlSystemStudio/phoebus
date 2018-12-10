/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.csstudio.trends.databrowser3.Messages;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.ui.dialog.PopOver;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

/** Button with pop-over for configuring a font
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FontButton extends Button
{
    private final ChoiceBox<String> families = new ChoiceBox<>();
    private final ComboBox<String> size = new ComboBox<>(FXCollections.observableArrayList("8", "10", "12", "14", "16", "18", "22", "24", "32"));
    private final CheckBox bold = new CheckBox(Messages.FontBtnBold),
                           italic = new CheckBox(Messages.FontBtnItalics);
    private TextField example = new TextField(Messages.FontBtnExample);

    private final PopOver popover;

    private Font font;
    private final Consumer<Font> on_font_selected;

    public FontButton(final Font initial_font, final Consumer<Font> on_font_selected)
    {
        this.font = initial_font;
        this.on_font_selected = on_font_selected;
        popover = new PopOver(createContent());
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
        // Name:  combo
        // Size:  combo
        // Style: [x] Bold [x] Italic
        //            [OK]   [Cancel]

        final GridPane layout = new GridPane();
        layout.setPadding(new Insets(5));
        layout.setHgap(5);
        layout.setVgap(5);

        layout.add(new Label(Messages.FontBtnName), 0, 0);
        layout.add(families, 1, 0, 2, 1);

        layout.add(new Label(Messages.FontBtnSize), 0, 1);
        layout.add(size, 1, 1);

        layout.add(new Label(Messages.FontBtnStyle), 0, 2);
        layout.add(bold, 1, 2);
        layout.add(italic, 2, 2);

        example.setPrefSize(200, 50);
        layout.add(example, 0, 3, 3, 1);

        final ButtonBar buttons = new ButtonBar();
        final Button ok = new Button(ButtonType.OK.getText());
        ButtonBar.setButtonData(ok, ButtonType.OK.getButtonData());
        final Button cancel = new Button(ButtonType.CANCEL.getText());
        ButtonBar.setButtonData(cancel, ButtonType.CANCEL.getButtonData());
        buttons.getButtons().addAll(ok, cancel);
        layout.add(buttons, 0, 4, 4, 1);

        JobManager.schedule(Messages.FontBtnJobName, this::getFamilies);

        size.setEditable(true);
        families.valueProperty().addListener(event -> updateFont());
        size.setOnAction(event -> updateFont());
        bold.setOnAction(event -> updateFont());
        italic.setOnAction(event -> updateFont());
        ok.setOnAction(event ->
        {
            popover.hide();
            setText(getDescription(font));
            on_font_selected.accept(font);
        });
        cancel.setOnAction(event -> popover.hide());

        return layout;
    }

    private void getFamilies(final JobMonitor monitor)
    {
        final List<String> fams = Font.getFamilies();
        Platform.runLater(() ->
        {
            families.getItems().setAll(fams);
            selectFont(font);
        });
    }

    private void updateFont()
    {
        if (size.getValue() == null)
            return;
        final double s = Double.parseDouble(size.getValue());
        font = Font.font(families.getValue(),
                         bold.isSelected() ? FontWeight.BOLD : FontWeight.NORMAL,
                         italic.isSelected() ? FontPosture.ITALIC : FontPosture.REGULAR,
                         s);
        example.setFont(font);
    }

    public void selectFont(final Font font)
    {
        Objects.requireNonNull(font);
        setText(getDescription(font));
        families.getSelectionModel().select(font.getFamily());
        size.setValue(Integer.toString((int) font.getSize()));

        // JFX Font.font() takes FontWeight and FontPosture,
        // but getStyle() returns a String, no way to get the FontWeight and FontPosture??
        bold.setSelected(font.getStyle().toLowerCase().contains("bold"));
        italic.setSelected(font.getStyle().toLowerCase().contains("italic"));

        example.setFont(font);

        this.font = font;
    }

    private String getDescription(final Font font)
    {
        final StringBuilder buf = new StringBuilder();
        buf.append(font.getFamily())
           .append(",")
           .append(font.getSize());
        if (! font.getStyle().toLowerCase().contains("regular"))
            buf.append(",").append(font.getStyle());
        return buf.toString();
    }
}