package org.phoebus.applications.eslog;

import java.util.ArrayList;
import java.util.List;

import org.phoebus.applications.eslog.archivedjmslog.MessageSeverityPropertyFilter;
import org.phoebus.applications.eslog.archivedjmslog.PropertyFilter;
import org.phoebus.applications.eslog.archivedjmslog.StringPropertyFilter;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.GridPane;

public class FilterDialog extends Dialog<PropertyFilter[]>
{
    public static final int FILTER_COUNT = 5;

    class StringCombo extends ComboBox<String>
    {
    };

    private ButtonType applyButtonType;
    private CheckBox invertedBox[];
    private StringCombo propertyCombo[];
    private ComboBox<String> severityCombo;
    private TextField valueText[];

    /** Filter settings: Initial values; updated in <code>okPressed</code> */
    private PropertyFilter filters[];

    public FilterDialog(PropertyFilter[] filters)
    {
        this.filters = filters;

        setTitle("Set Message Log Filters");
        setHeaderText(
                "Select filter criteria:\nWhich property should contain which value?");

        this.applyButtonType = new ButtonType("Apply", ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(this.applyButtonType,
                ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // top row: severity
        grid.add(new Label("Min Severity:"), 0, 0);
        this.severityCombo = new ComboBox<String>();
        this.severityCombo.getItems().addAll(Helpers.LOG_LEVELS);
        grid.add(this.severityCombo, 1, 0);

        this.invertedBox = new CheckBox[FILTER_COUNT];
        this.propertyCombo = new StringCombo[FILTER_COUNT];
        this.valueText = new TextField[FILTER_COUNT];

        // Not: __not__ Property: ____property____ Value: ___value___
        for (int i = 0; i < FILTER_COUNT; ++i)
        {
            this.invertedBox[i] = new CheckBox();
            this.invertedBox[i].setText("NOT");
            grid.add(invertedBox[i], 0, 1 + 2 * i);

            this.propertyCombo[i] = new StringCombo();
            this.propertyCombo[i].getItems().add("(no filter)");
            this.propertyCombo[i].getItems().addAll(EsLogInstance.PROPERTIES);
            this.propertyCombo[i].getSelectionModel().select(0);
            grid.add(propertyCombo[i], 1, 1 + 2 * i);

            this.valueText[i] = new TextField();
            this.valueText[i].setPromptText("Value");
            grid.add(valueText[i], 2, 1 + 2 * i);

            /*
             * l = new Label(box, 0); l.setText(Messages.Filter_Property);
             * l.setLayoutData(new GridData()); this.property_combo[i] = new
             * Combo(box, SWT.DROP_DOWN | SWT.READ_ONLY);
             * this.property_combo[i].setToolTipText(Messages.Filter_PropertyTT)
             * ; this.property_combo[i].add(Messages.Filter_NoFilter); for
             * (String prop : this.properties) {
             * this.property_combo[i].add(prop); } this.property_combo[i]
             * .setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
             * this.property_combo[i].select(0); l = new Label(box, 0);
             * l.setText(Messages.Filter_Value); l.setLayoutData(new
             * GridData()); this.value_text[i] = new Text(box, SWT.BORDER);
             * this.value_text[i].setToolTipText(Messages.Filter_ValueTT);
             * this.value_text[i] .setLayoutData(new
             * GridData(GridData.FILL_HORIZONTAL));
             */

            // not before the first row
            if (i == 0) continue;
            grid.add(new Label("AND"), 1, 2 * i);
        }

        getDialogPane().setContent(grid);

        Platform.runLater(() -> this.severityCombo.requestFocus());

        setResultConverter(this::buttonPressed);
        displayCurrentFilterSettings();
    }

    protected PropertyFilter[] buttonPressed(ButtonType button)
    {
        if (button != this.applyButtonType)
        {
            return null;
        }

        List<PropertyFilter> valid_filters = new ArrayList<>(FILTER_COUNT);
        // Collect valid entries in result arrays
        for (int i = 0; i < FILTER_COUNT; ++i)
        { // '-1' since selection Index 0 is "no property"
            final var propValue = this.propertyCombo[i].getSelectionModel()
                    .getSelectedItem();
            if (!"(no filter)".equals(propValue))
            {
                valid_filters.add(new StringPropertyFilter(propValue,
                        this.valueText[i].getText().trim(),
                        this.invertedBox[i].isSelected()));
            }
        }

        final int min_level = this.severityCombo.getSelectionModel()
                .getSelectedIndex();
        // 0 is FINEST, i.e. effectively no filter
        if (min_level >= 1)
            valid_filters.add(new MessageSeverityPropertyFilter(min_level));

        // Turn into array
        this.filters = new PropertyFilter[valid_filters.size()];
        valid_filters.toArray(this.filters);
        return this.filters;
    }

    private void displayCurrentFilterSettings()
    {
        if (this.filters == null)
        {
            return;
        }
        int N = Math.min(this.filters.length, FILTER_COUNT);
        for (int i = 0; i < N; ++i)
        {
            final PropertyFilter f = this.filters[i];
            if (f instanceof StringPropertyFilter)
            {
                StringPropertyFilter filter = (StringPropertyFilter) f;
                // Determine index of filter property in combo box
                for (int prop_index = 0; prop_index < EsLogInstance.PROPERTIES.length; ++prop_index)
                {
                    if (EsLogInstance.PROPERTIES[prop_index]
                            .equals(filter.getProperty()))
                    { // +1 because index 0 is "no property"
                        this.propertyCombo[i].getSelectionModel()
                                .select(prop_index + 1);
                        this.valueText[i].setText(filter.getPattern());
                        this.invertedBox[i].setSelected(filter.isInverted());
                    }
                }
            }
            else if (f instanceof MessageSeverityPropertyFilter)
            {
                MessageSeverityPropertyFilter filter = (MessageSeverityPropertyFilter) f;
                this.severityCombo.getSelectionModel()
                        .select(filter.getMinLevel());
            }
        }
    }
}
