/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.ui;

import org.csstudio.apputil.formula.Formula;
import org.csstudio.apputil.formula.VariableNode;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/** Editor pane for a Formula
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FormulaPane extends VBox
{
    private final TextField  formula_txt = new TextField();
    private final Label status = new Label();
    private final ReadOnlyBooleanWrapper ok = new ReadOnlyBooleanWrapper();

    /** Location where button presses insert text into the formula.
     *
     *  <p>By default, button presses would move focus from formula_txt
     *  to button. When loosing focus, the TextField caret location
     *  is reset to 0.
     *  Trying to remember the last caret location while text field
     *  has focus, but keeping the focus, caret location and selection
     *  unchanged turned out to be difficult
     *  -> Setting buttons setFocusTraversable(false) to avoid loosing focus
     *  while within the formula pane.
     *
     *  <p>Keyboard users can thus no longer use TAB navigation to reach buttons,
     *  but then they can directly enter the formula text.
     *  Mouse users can use buttons, and the formula text retains the insert location.
     */
    private int insert_location = 0;

    private final EventHandler<ActionEvent> insert_formula_text = event ->
    {
        final String text = ((Button) event.getSource()).getText();
        // Correct invalid locations
        if (insert_location > formula_txt.getLength())
            insert_location = formula_txt.getLength();
        if (insert_location < 0)
            insert_location = 0;
        formula_txt.anchorProperty();
        // Inserting text will update the caret and thus the insert_location.
        // Note end of text we're about to insert
        final int end = insert_location + text.length();
        formula_txt.insertText(insert_location, text);
        // Assert that text field retains/gets focus
        Platform.runLater(() ->
        {
            formula_txt.requestFocus();
            // Sometimes, for example for empty text field,
            // the text inserted first will be selected
            // with caret at start, so further inserts then
            // pre-pend existing text.
            // --> Position caret at end of what was just inserted
            Platform.runLater(() -> formula_txt.selectRange(end, end));
        });
    };

    public FormulaPane()
    {
        final TitledPane form_pane = new TitledPane("Formula", formula_txt);
        form_pane.setCollapsible(false);

        final TitledPane inputs_pane = new TitledPane("Inputs", new Label("TODO"));
        inputs_pane.setCollapsible(false);
        HBox.setHgrow(inputs_pane, Priority.ALWAYS);

        final TitledPane functions_pane = new TitledPane("Functions", createFunctions());
        functions_pane.setCollapsible(false);

        final TitledPane calc_pane = new TitledPane("Calculations", createCalculations());
        calc_pane.setCollapsible(false);
        HBox.setHgrow(calc_pane, Priority.ALWAYS);

        final HBox middle_row = new HBox(5.0, inputs_pane, functions_pane, calc_pane);

        form_pane.setPadding(new Insets(5));
        middle_row.setPadding(new Insets(0, 5, 0, 5));
        status.setPadding(new Insets(5));
        getChildren().setAll(form_pane, middle_row, status);

        // Text field's caret position changes to 0 when loosing focus.
        // --> Track the last position before loosing focus
        formula_txt.caretPositionProperty().addListener((prop, old, pos) ->
        {
            if (formula_txt.isFocused())
                insert_location = pos.intValue();
        });

        formula_txt.textProperty().addListener(p -> parseFormula());

        parseFormula();
    }

    /** Property that indicates if the formula is valid */
    public ReadOnlyBooleanProperty okProperty()
    {
        return ok.getReadOnlyProperty();
    }

    private Node createFunctions()
    {
        final GridPane layout = new GridPane();
        layout.setHgap(5);
        layout.setVgap(5);

        layout.add(createButton("sin", "sin(x), sine"), 0, 0);
        layout.add(createButton("asin", "asin(x), inverse sine"), 1, 0);
        layout.add(createButton("sqrt", "sqrt(x), square root"), 2, 0);
        layout.add(createButton("^", "x^y, power"), 3, 0);

        layout.add(createButton("cos", "cos(x), cosine"), 0, 1);
        layout.add(createButton("acos", "acos(x), inverse cosine"), 1, 1);
        layout.add(createButton("log", "log(x), logarithm (base e)"), 2, 1);
        layout.add(createButton("log10", "log10(x), logarithm (base 10)"), 3, 1);

        layout.add(createButton("tan", "tan(x), tangent"), 0, 2);
        layout.add(createButton("atan", "atan(x), inverse tangent"), 1, 2);
        layout.add(createButton("atan2", "atan2(y,x), inverse tangent"), 2, 2);
        layout.add(createButton("exp", "exp(x), exponential (base e)"), 3, 2);

        layout.add(createButton("abs", "abs(x), absolute value"), 0, 3);
        layout.add(createButton("min", "min(x, y, ...), minimum"), 1, 3);
        layout.add(createButton("max", "max(x, y, ...), maximum"), 2, 3);
        layout.add(createButton("? : ", "x ? y : z, if-x-then-y-else-z"), 3, 3);

        layout.add(createButton("floor", "floor(x), round toward -\u221e"), 0, 4);
        layout.add(createButton("ceil", "ceil(x), round towards +\u221e"), 1, 4);
        layout.add(createButton("PI", "\u03C0=3.14..."), 2, 4);
        layout.add(createButton("E", "e=2.71..."), 3, 4);

        return layout;
    }

    private Node createCalculations()
    {
        final GridPane layout = new GridPane();
        layout.setHgap(5);
        layout.setVgap(5);

        layout.add(createButton("C", "Clear Formula", event ->
        {
            formula_txt.clear();
            insert_location = 0;
        }), 0, 0);
        layout.add(createButton("(", "Opening brace"), 1, 0);
        layout.add(createButton(")", "Closing brace"), 2, 0);
        layout.add(createButton("Del", "Backspace", event ->
        {
            formula_txt.deletePreviousChar();
        }), 3, 0);

        layout.add(createButton("7", "Seven"), 0, 1);
        layout.add(createButton("8", "Eight"), 1, 1);
        layout.add(createButton("9", "Nine"), 2, 1);
        layout.add(createButton("*", "Multiply"), 3, 1);

        layout.add(createButton("4", "Four"), 0, 2);
        layout.add(createButton("5", "Five"), 1, 2);
        layout.add(createButton("6", "Six"), 2, 2);
        layout.add(createButton("/", "Divide"), 3, 2);

        layout.add(createButton("1", "One"), 0, 3);
        layout.add(createButton("2", "Two"), 1, 3);
        layout.add(createButton("3", "Three"), 2, 3);
        layout.add(createButton("+", "Add"), 3, 3);

        layout.add(createButton("0", "Zero"), 0, 4, 2, 1);
        layout.add(createButton(".", "Decimal Point"), 2, 4);
        layout.add(createButton("-", "Substract"), 3, 4);

        return layout;
    }

    private Node createButton(final String text, final String tooltip)
    {
        return createButton(text, tooltip, insert_formula_text);
    }

    private Node createButton(final String text, final String tooltip, final EventHandler<ActionEvent> on_action)
    {
        final Button button = new Button(text);
        button.setFocusTraversable(false);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setTooltip(new Tooltip(tooltip));
        button.setOnAction(on_action);
        return button;
    }

    private void parseFormula()
    {
        final String form = formula_txt.getText().trim();

        if (form.isEmpty())
        {
            status.setText("Formula is empty");
            status.setTextFill(Color.DARKRED);
            ok.set(false);
            return;
        }

        // Create array of all available variables
//        final VariableNode vars[] = new VariableNode[inputs.length];
        final VariableNode vars[] = new VariableNode[0];

        // See if formula parses OK
        final Formula formula;
        try
        {
            formula = new Formula(form, vars);
        }
        catch (Exception ex)
        {
            status.setText(ex.getMessage());
            status.setTextFill(Color.RED);
            ok.set(false);
            return;
        }

        status.setText("Parsed Formula: " + formula.toString());
        status.setTextFill(Color.BLACK);
        ok.set(true);
    }
}
