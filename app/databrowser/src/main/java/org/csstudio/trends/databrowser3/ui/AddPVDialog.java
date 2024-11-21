/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.Pair;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.preferences.Preferences;
import org.csstudio.trends.databrowser3.ui.properties.AddAxisCommand;
import org.phoebus.ui.autocomplete.PVAutocompleteMenu;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;

/** Dialog for creating a new PV or Formula Item: Get name, axis.
 *  For PV, also scan period.
 *
 *  @author Kay Kasemir
 *  @author Takashi Nakamoto changed AddPVDialog to update the error/warning message
 *                           dynamically as the user changes any value in this
 *                           dialog message, and to accept an existing name for
 *                           the name of newly created PV item.
 */
@SuppressWarnings("nls")
public class AddPVDialog extends Dialog<Boolean>
{
    private final boolean formula;
    private final List<String> existing_names;
    private final List<Pair<TextField, TextField>> nameAndDisplayNames = new ArrayList<>();
    private final List<TextField> periods = new ArrayList<>();
    private final List<CheckBox> monitors = new ArrayList<>();
    boolean useTheSameAxisForAllPVs = false;
    private ChoiceBox<String> axisForAllPVsChoiceBox = null;
    private AxisConfig axisForAllPVs = null;
    private final List<ChoiceBox<String>> axes = new ArrayList<>();
    private ObservableList<String> axis_options;

    /** @param count Number of PV names to edit
     *  @param model Model
     *  @param formula Is this for plain PVs or formula?
     */
    public AddPVDialog(final int count, final Model model, final boolean formula)
    {
        this.formula = formula;

        existing_names = model.getItems().stream().map(ModelItem::getName).collect(Collectors.toList());

        if (formula) {
            setTitle(Messages.AddFormula);
        }
        else {
            if (count == 1) {
                setTitle(Messages.AddPV);
            }
            else {
                setTitle(Messages.AddPVs);
            }
        }
        setHeaderText(formula ? Messages.AddFormulaMsg : Messages.AddPVMsg);

        getDialogPane().setContent(createContent(model, count));
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        final Button ok = (Button) getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, event ->
        {
            if (! updateAndValidate())
                event.consume();
        });

        setResultConverter(button -> button == ButtonType.OK);

        setResizable(true);

        setOnShown(event -> Platform.runLater(() ->
        {
            // Many names: Focus on OK to just confirm.
            // Otherwise focus on first name so it can be entered
            if (nameAndDisplayNames.size() > 1)
                ok.requestFocus();
            else
                nameAndDisplayNames.get(0).getKey().requestFocus();
        }));
    }

    private Node createContent(final Model model, final int count)
    {
        final GridPane gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(5);
        final ColumnConstraints stay = new ColumnConstraints();
        final ColumnConstraints fill = new ColumnConstraints();
        fill.setHgrow(Priority.ALWAYS);
        gridPane.getColumnConstraints().addAll(stay, stay, fill);

        axis_options = FXCollections.observableArrayList(model.getAxes().stream().map(AxisConfig::getName).collect(Collectors.toList()));
        axis_options.add(0, Messages.AddPV_NewOrEmptyAxis);

        int row = 0;
        if (count > 1) {
            Label label_useTheSameQuestion = new Label(Messages.UseTheSameValueAxisForAllAddedPVs);
            gridPane.add(label_useTheSameQuestion, 0, row);

            row += 1;
            ToggleGroup toggleGroup = new ToggleGroup();
            RadioButton radioButton_useTheSame = new RadioButton(Messages.YesAllPVsAreAddedToTheAxis);
            radioButton_useTheSame.setToggleGroup(toggleGroup);

            gridPane.add(radioButton_useTheSame, 0, row);

            axisForAllPVsChoiceBox = new ChoiceBox<>(axis_options);
            axisForAllPVsChoiceBox.setTooltip(new Tooltip(Messages.AddPV_AxisTT));
            axisForAllPVsChoiceBox.getSelectionModel().select(0);
            gridPane.add(axisForAllPVsChoiceBox, 1, row);

            radioButton_useTheSame.setOnAction(actionEvent -> {
                axisForAllPVsChoiceBox.setDisable(false);
                axes.forEach(axis -> axis.setDisable(true));
                useTheSameAxisForAllPVs = true;
            });

            row += 1;
            RadioButton radioButton_useDifferent = new RadioButton(Messages.NoEachPVIsAssignedAnValueAxisIndividually);
            radioButton_useDifferent.setToggleGroup(toggleGroup);
            gridPane.add(radioButton_useDifferent, 0, row);

            radioButton_useDifferent.setOnAction(actionEvent -> {
                axisForAllPVsChoiceBox.setDisable(true);
                axes.forEach(axis -> axis.setDisable(false));
                useTheSameAxisForAllPVs = false;
            });

            row += 1;
            Separator separator = new Separator();
            gridPane.add(separator, 0, row, 3, 1);

            useTheSameAxisForAllPVs = true;
            radioButton_useTheSame.setSelected(true);
            row += 1;

            if (Preferences.assign_pvs_from_clipboard_to_the_same_axis_by_default) {
                radioButton_useTheSame.setSelected(true);
                radioButton_useTheSame.getOnAction().handle(null);
            }
            else {
                radioButton_useDifferent.setSelected(true);
                radioButton_useDifferent.getOnAction().handle(null);
            }
        }

        for (int i=0; i<count; ++i)
        {
            final String nm;
            if (formula) {
                nm = Messages.Name;
            }
            else {
                nm = count == 1 ? Messages.PVName : Messages.PVName + " " + (i+1);
            }
            gridPane.add(new Label(nm), 0, row);
            final TextField name = new TextField();
            name.textProperty().addListener(event -> checkDuplicateName(name));
            name.setTooltip(new Tooltip(formula ? Messages.AddFormula_NameTT : Messages.AddPV_NameTT));
            if (! formula)
                PVAutocompleteMenu.INSTANCE.attachField(name);
            gridPane.add(name, 1, row, 2, 1);

            if (!formula) {
                row += 1;
                String displayNameLabelText = Messages.TraceDisplayName;
                Label displayNameLabel = new Label(displayNameLabelText);
                gridPane.add(displayNameLabel, 0, row);
                TextField displayNameTextField = new TextField();
                Tooltip displayNameTextFieldTooltip = new Tooltip(Messages.TraceDisplayNameTT);
                displayNameTextField.setTooltip(displayNameTextFieldTooltip);
                gridPane.add(displayNameTextField, 1, row, 2, 1);
                nameAndDisplayNames.add(new Pair(name, displayNameTextField));
            }
            else {
                nameAndDisplayNames.add(new Pair(name, name));
            }

            if (! formula)
            {
                gridPane.add(new Label(Messages.AddPV_Period), 0, ++row);
                final TextField period = new TextField(Double.toString(Preferences.scan_period));
                period.setTooltip(new Tooltip(Messages.AddPV_PeriodTT));
                periods.add(period);
                period.setDisable(true);
                gridPane.add(period, 1, row);

                final CheckBox monitor = new CheckBox(Messages.AddPV_OnChange);
                monitor.setTooltip(new Tooltip(Messages.AddPV_OnChangeTT));
                monitor.setSelected(true);
                monitors.add(monitor);
                monitor.setOnAction(event -> period.setDisable(monitor.isSelected()));
                gridPane.add(monitors.get(i), 2, row);
            }

            gridPane.add(new Label(Messages.AddPV_Axis), 0, ++row);
            final ChoiceBox<String> axis = new ChoiceBox<>(axis_options);
            axis.setTooltip(new Tooltip(Messages.AddPV_AxisTT));
            axis.getSelectionModel().select(0);
            axes.add(axis);
            gridPane.add(axes.get(i), 1, row);

            gridPane.add(new Separator(), 0, ++row, 3, 1);
            row += 1;
        }
        ScrollPane scrollPane = new ScrollPane(gridPane);
        scrollPane.setFitToWidth(true);
        axes.forEach(axis -> axis.setDisable(useTheSameAxisForAllPVs)); // When the "use the same axis for all PVs" radio button is selected, then individual axes for each PV are disabled. Otherwise, they are enabled.
        return scrollPane;
    }

    /** Set initial name. Only effective when called before dialog is opened.
     *  @param i Index
     *  @param pvName Suggested name
     */
    public void setNameAndDisplayName(final int i, final String pvName)
    {
        nameAndDisplayNames.get(i).getKey().setText(pvName);
        nameAndDisplayNames.get(i).getValue().setText("");
        nameAndDisplayNames.get(i).getValue().setPromptText(Messages.DefaultDisplayName);
    }

    /** @param i Index
     *  @return Entered PV name
     */
    public String getName(final int i)
    {
        return nameAndDisplayNames.get(i).getKey().getText().trim();
    }

    /** @param i Index
     *  @return Optionally, the display name to be associated with the PV in the Data Browser
     */
    public Optional<String> getDisplayName(final int i)
    {
        String trimmedDisplayName = nameAndDisplayNames.get(i).getValue().getText().trim();
        if (trimmedDisplayName.isEmpty()) {
            return Optional.empty();
        }
        else {
            return Optional.of(trimmedDisplayName);
        }
    }

    /** @param i Index
     *  @return Entered scan period in seconds. 0 for 'scan'
     */
    public double getScanPeriod(final int i)
    {
        if (monitors.get(i).isSelected())
            return 0.0;
        return Double.parseDouble(periods.get(i).getText());
    }

    /** @param i Index
     *  @return Index of Value Axis, or 0 for 'create new for an individual PV', or -1 for 'create one new axis to which all PVs are assigned'.
     */
    public int getAxisIndex(final int i)
    {
        if (useTheSameAxisForAllPVs) {
            int selectedIndex = axisForAllPVsChoiceBox.getSelectionModel().getSelectedIndex();
            if (selectedIndex == 0) {
                return -1;
            }
            else {
                return selectedIndex;
            }
        }
        else {
            return axes.get(i).getSelectionModel().getSelectedIndex();
        }
    }

    /** Helper for getting or creating an axis
     *  @param model {@link Model}
     *  @param undo {@link UndoableActionManager}
     *  @param axis_index Axis index from {@link AddPVDialog#getAxisIndex(int)}
     *  @return Corresponding model axis, which might have been created as necessary
     */
    public AxisConfig getOrCreateAxis(final Model model, final UndoableActionManager undo, final int axis_index)
    {
        // Did user select axis?
        if (axis_index > 0) {
            return model.getAxis(axis_index-1);
        }
        else if (axis_index == 0){
            // Use first empty axis, or create a new one
            return model.getEmptyAxis().orElseGet(() -> new AddAxisCommand(undo, model).getAxis());
        }
        else if (axis_index == -1) {
            if (axisForAllPVs != null) {
                return axisForAllPVs;
            }
            else {
                axisForAllPVs = model.getEmptyAxis().orElseGet(() -> new AddAxisCommand(undo, model).getAxis());
                return axisForAllPVs;
            }
        }
        else {
            throw new RuntimeException("Unhandled case: " + axis_index);
        }
    }

    private void checkDuplicateName(final TextField name)
    {
        final String value = name.getText().trim();
        // Model accepts multiple items with the same name,
        // there is no need to prohibit from adding a new item with the
        // existing name, but warn in case user really doesn't want the same name twice.
        if (existing_names.contains(value))
            setHeaderText(MessageFormat.format(Messages.DuplicateItemFmt, value));
        else
            setHeaderText(formula ? Messages.AddFormulaMsg : Messages.AddPVMsg);
    }

    /** Update the internal variables according to the user input and
     *  validate them.
     *  This method also updates the message shown in this dialog.
     * @return True if all the values input by the user are valid. Otherwise, false.
     */
    private boolean updateAndValidate()
    {
        for (int i = 0; i< nameAndDisplayNames.size(); ++i)
        {
            // Valid name?
            final String name = getName(i);
            if (name.isEmpty())
            {
                setHeaderText(Messages.EmptyNameError);
                nameAndDisplayNames.get(i).getKey().requestFocus();
                return false;
            }

            if (! formula)
            {
                // Valid scan period?
                try
                {
                    final double period = getScanPeriod(i);
                    if (period < 0)
                        throw new Exception();
                }
                catch (Throwable ex)
                {
                    setHeaderText(Messages.InvalidScanPeriodError);
                    periods.get(i).requestFocus();
                    return false;
                }
            }
        }
        // All OK
        setHeaderText(formula ? Messages.AddFormulaMsg : Messages.AddPVMsg);
        return true;
    }
}
