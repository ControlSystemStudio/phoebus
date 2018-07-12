/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.ui;

import java.util.ArrayList;
import java.util.List;

import org.csstudio.apputil.formula.Formula;
import org.csstudio.apputil.formula.VariableNode;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

/** Editor pane for a Formula
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FormulaPane extends GridPane
{
    private final TextField formula_txt = new TextField();

    private final ObservableList<InputItem> inputs = FXCollections.observableArrayList();
    private final TableView<InputItem> input_table = new TableView<>(inputs);

    private final Label status = new Label();
    private final ReadOnlyBooleanWrapper ok = new ReadOnlyBooleanWrapper();

    private InputItem used_inputs[] = new InputItem[0];

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
        insert(text);
    };

    /** @param initial_formula Initial formula text
     *  @param inputs Possible inputs, i.e. PV names and suggested variable names.
     *                Variable names will be edited within the {@link FormulaPane}!
     */
    public FormulaPane(final String initial_formula, final List<InputItem> inputs)
    {
        this.inputs.setAll(inputs);

        setHgap(5);
        setVgap(5);


        add(formula_txt, 0, 0, 3, 1);

        final TableView<InputItem> input_pane = createInputs();
        add(input_pane, 0, 1);

        final TitledPane functions_pane = new TitledPane("Functions", createFunctions());
        functions_pane.setCollapsible(false);
        add(functions_pane, 1, 1);

        final TitledPane calc_pane = new TitledPane("Calculations", createCalculations());
        calc_pane.setCollapsible(false);
        add(calc_pane, 2, 1);

        input_pane.prefHeightProperty().bind(functions_pane.heightProperty());

        add(status, 0, 2, 3, 1);

        // Text field's caret position changes to 0 when loosing focus.
        // --> Track the last position before loosing focus
        formula_txt.caretPositionProperty().addListener((prop, old, pos) ->
        {
            if (formula_txt.isFocused())
                insert_location = pos.intValue();
        });

        formula_txt.textProperty().addListener(p -> parseFormula());

        // Set initial text (which triggers parsing)
        formula_txt.setText(initial_formula);
        Platform.runLater(() ->
        {
            // Move caret to end of initial text
            insert_location = initial_formula.length();
            formula_txt.selectRange(insert_location, insert_location);
            formula_txt.requestFocus();
        });
    }

    private void insert(final String text)
    {
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
    }

    /** Property that indicates if the formula is valid */
    public ReadOnlyBooleanProperty okProperty()
    {
        return ok.getReadOnlyProperty();
    }

    /** @return Formula. Only valid when OK
     *  @see #okProperty()
     */
    public String getFormula()
    {
        return formula_txt.getText();
    }

    /** @return {@link InputItem}s used in the formula. Only valid when OK
     *  @see #okProperty()
     */
    public InputItem[] getInputs()
    {
        return used_inputs;
    }

    private TableView<InputItem> createInputs()
    {
        TableColumn<InputItem, String> col = new TableColumn<>("Input");
        col.setCellValueFactory(c ->  c.getValue().input_name);
        input_table.getColumns().add(col);

        col = new TableColumn<>("Variable");
        col.setCellValueFactory(c ->  c.getValue().variable_name);
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        // When variable is renamed, re-evaluate formula
        col.setOnEditCommit(event -> parseFormula());
        col.setEditable(true);
        input_table.getColumns().add(col);

        input_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        input_table.setTooltip(new Tooltip("Double-click input to add to formula, or edit variable name"));
        input_table.setEditable(true);
        // Double-click (on input column) adds that variable name to formula
        input_table.addEventHandler(MouseEvent.MOUSE_PRESSED, event ->
        {
            if (event.getClickCount() == 2)
                insert(input_table.getSelectionModel().getSelectedItem().variable_name.get());
        });

        return input_table;
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
        final VariableNode[] vars = new VariableNode[inputs.size()];
        for (int i = 0; i < vars.length; ++i)
            vars[i] = new VariableNode(inputs.get(i).variable_name.get());

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

        // Create array of all variables actually found inside the formula
        final List<InputItem> used = new ArrayList<>();
        for (InputItem input : inputs)
            if (formula.hasSubnode(input.variable_name.get()))
                used.add(input);
        // Convert to array
        used_inputs = used.toArray(new InputItem[used.size()]);

        ok.set(true);
    }
}
