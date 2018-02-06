package org.csstudio.trends.databrowser3.ui.export;

import java.io.File;
import java.time.Instant;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.ui.TimeRangeDialog;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.dialog.SaveAsDialog;
import org.phoebus.util.time.TimeInterval;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ExportView extends VBox
{
    private static final String TAG_SOURCE = "source",
                                TAG_TYPE = "type",
                                TAG_FORMAT = "format",
                                TAG_FILE = "file";

    private final Model model;
    private final TextField start = new TextField();
    private final TextField end = new TextField();
    private final CheckBox use_plot_times = new CheckBox(Messages.ExportPlotStartEnd);
    private final ToggleGroup sources = new ToggleGroup(),
                              table_types = new ToggleGroup(),
                              formats = new ToggleGroup();
    private final TextField filename = new TextField();

    public ExportView(final Model model)
    {
        this.model = model;

        // * Samples To Export *
        // Start:  ___start_______________________________________________________________ [select]
        // End  :  ___end_________________________________________________________________ [x] Use start/end time of Plot
        // Source: ( ) Plot  (*) Raw Archived Data  ( ) Averaged Archived Data  __time__   ( ) Linear __linear__
        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);
        grid.setPadding(new Insets(5));

        grid.add(new Label(Messages.StartTimeLbl), 0, 0);
        GridPane.setHgrow(start, Priority.ALWAYS);
        grid.add(start, 1, 0);

        final Button sel_times = new Button(Messages.StartEndDialogBtn);
        grid.add(sel_times, 2, 0);

        grid.add(new Label(Messages.EndTimeLbl), 0, 1);
        GridPane.setHgrow(end, Priority.ALWAYS);
        grid.add(end, 1, 1);

        sel_times.setOnAction(event ->
        {
            final TimeRangeDialog dlg = new TimeRangeDialog(model.getTimerange());
            DialogHelper.positionDialog(dlg, this, -200, -200);
            dlg.showAndWait().ifPresent(interval ->
            {
                final String[] range = Model.getTimerangeText(interval);
                start.setText(range[0]);
                end.setText(range[1]);
            });
        });

        use_plot_times.setTooltip(new Tooltip(Messages.ExportPlotStartEndTT));
        grid.add(use_plot_times, 2, 1);

        use_plot_times.selectedProperty().addListener((prop, was_selected, selected) ->
        {
            if (selected  &&  ! was_selected)
            {
                final String[] range = model.getTimerangeText();
                start.setText(range[0]);
                end.setText(range[1]);
            }
            start.setDisable(selected);
            end.setDisable(selected);
            sel_times.setDisable(selected);
        });
        use_plot_times.setSelected(true);

        grid.add(new Label(Messages.ExportGroupSource), 0, 2);

        final RadioButton source_plot = new RadioButton(Messages.ExportSource_Plot);
        source_plot.setTooltip(new Tooltip(Messages.ExportSource_PlotTT));
        source_plot.setToggleGroup(sources);

        final RadioButton source_raw = new RadioButton(Messages.ExportSource_RawArchive);
        source_raw.setTooltip(new Tooltip(Messages.ExportSource_RawArchiveTT));
        source_raw.setToggleGroup(sources);

        final RadioButton source_opt = new RadioButton(Messages.ExportSource_OptimizedArchive);
        source_opt.setTooltip(new Tooltip(Messages.ExportSource_OptimizedArchiveTT));
        source_opt.setToggleGroup(sources);

        final TextField optimize = new TextField(Messages.ExportDefaultOptimization);
        optimize.setPrefColumnCount(6);
        optimize.setTooltip(new Tooltip(Messages.ExportOptimizationTT));
        optimize.disableProperty().bind(source_opt.selectedProperty().not());

        final RadioButton source_lin = new RadioButton(Messages.ExportSource_Linear);
        source_lin.setTooltip(new Tooltip(Messages.ExportSource_LinearTT));
        source_lin.setToggleGroup(sources);

        final TextField linear = new TextField(Messages.ExportDefaultLinearInterpolation);
        linear.setPrefColumnCount(8);
        linear.setTooltip(new Tooltip(Messages.ExportDefaultLinearInterpolationTT));
        linear.disableProperty().bind(source_lin.selectedProperty().not());

        final HBox source_options = new HBox(5, source_plot, source_raw, source_opt, optimize, source_lin, linear);
        source_options.setAlignment(Pos.CENTER_LEFT);
        grid.add(source_options, 1, 2, 2, 1);

        source_raw.setSelected(true);

        final TitledPane source = new TitledPane(Messages.ExportGroupSource, grid);
        source.setCollapsible(false);


        // * Format *
        // (*) Spreadsheet ( ) Matlab
        // [x] Tabular [x] ... with min/max column [x] ... with Severity/Status
        // (*) Default format  ( ) decimal notation  ( ) exponential notation _digits_ fractional digits
        grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);
        grid.setPadding(new Insets(5));

        final RadioButton type_spreadsheet = new RadioButton(Messages.ExportTypeSpreadsheet);
        type_spreadsheet.setTooltip(new Tooltip(Messages.ExportTypeSpreadsheetTT));
        type_spreadsheet.setToggleGroup(table_types);
        grid.add(type_spreadsheet, 0, 0);

        final RadioButton type_matlab = new RadioButton(Messages.ExportTypeMatlab);
        type_matlab.setTooltip(new Tooltip(Messages.ExportTypeMatlabTT));
        type_matlab.setToggleGroup(table_types);
        grid.add(type_matlab, 1, 0);

        type_spreadsheet.setSelected(true);

        final CheckBox tabular = new CheckBox(Messages.ExportTabular);
        tabular.setTooltip(new Tooltip(Messages.ExportTabularTT));
        tabular.setSelected(true);
        grid.add(tabular, 0, 1);

        final CheckBox min_max_col = new CheckBox(Messages.ExportMinMaxCol);
        min_max_col.setTooltip(new Tooltip(Messages.ExportMinMaxColTT));
        grid.add(min_max_col, 1, 1);

        final CheckBox sev_stat = new CheckBox(Messages.ExportValueInfo);
        sev_stat.setTooltip(new Tooltip(Messages.ExportValueInfoTT));
        grid.add(sev_stat, 2, 1);


        final RadioButton format_default = new RadioButton(Messages.Format_Default);
        format_default.setTooltip(new Tooltip(Messages.ExportFormat_DefaultTT));
        format_default.setToggleGroup(formats);
        grid.add(format_default, 0, 2);

        final RadioButton format_decimal = new RadioButton(Messages.Format_Decimal);
        format_decimal.setTooltip(new Tooltip(Messages.ExportFormat_DecimalTT));
        format_decimal.setToggleGroup(formats);
        grid.add(format_decimal, 1, 2);

        final RadioButton format_expo = new RadioButton(Messages.Format_Exponential);
        format_expo.setTooltip(new Tooltip(Messages.ExportFormat_ExponentialTT));
        format_expo.setToggleGroup(formats);
        grid.add(format_expo, 2, 2);

        final TextField format_digits = new TextField(Messages.ExportDefaultDigits);
        format_digits.setPrefColumnCount(3);
        format_digits.setTooltip(new Tooltip(Messages.ExportDigitsTT));
        format_digits.disableProperty().bind(format_default.selectedProperty());
        grid.add(format_digits, 3, 2);

        grid.add(new Label(Messages.ExportDigits), 4, 2);

        format_default.setSelected(true);

        final TitledPane format = new TitledPane(Messages.ExportGroupFormat, grid);
        format.setCollapsible(false);


        filename.setPromptText(Messages.ExportDefaultFilename);
        filename.setTooltip(new Tooltip(Messages.ExportFilenameTT));

        final Button sel_filename = new Button(Messages.ExportBrowse);
        sel_filename.setTooltip(new Tooltip(Messages.ExportBrowseTT));
        sel_filename.setOnAction(event -> selectFilename());

        final Button export = new Button(Messages.ExportStartExport);
        export.setOnAction(event -> startExportJob());

        final HBox outputs = new HBox(5, new Label(Messages.ExportFilename), filename, sel_filename, export);
        outputs.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(filename, Priority.ALWAYS);

        final TitledPane output = new TitledPane(Messages.ExportGroupOutput, outputs);
        output.setCollapsible(false);

        getChildren().setAll(source, format, output);


        // Enter in filename suggests to next start export
        filename.setOnAction(event -> export.requestFocus());
    }

    private void selectFilename()
    {
        File file = new File(filename.getText().trim());
        file = new SaveAsDialog().promptForFile(getScene().getWindow(),
                                                "Select output file", file, null);
        if (file != null)
            filename.setText(file.getAbsolutePath());
    }

    private void startExportJob()
    {
        // Determine start/end time
        Instant start_time, end_time;
        if (use_plot_times.isSelected())
        {
            final TimeInterval abs = model.getTimerange().toAbsoluteInterval();
            start_time = abs.getStart();
            end_time = abs.getEnd();
        }
        else
        {
            try
            {
                // TODO
//                start_time = XMLPersistence.parseInstant(start.getText());
//
//                end_time = XMLPersistence.parseInstant(end.getText());
            }
            catch (Exception ex)
            {
                handleError(ex);
            }
        }
    }

    private void handleError(final Exception ex)
    {
        ExceptionDetailsErrorDialog.openError(Messages.Error, "Export error", ex);
    }

    public void save(final Memento memento)
    {
        // XXX Could save & restore more items
        memento.setNumber(TAG_SOURCE, sources.getToggles().indexOf(sources.getSelectedToggle()));
        memento.setNumber(TAG_TYPE, table_types.getToggles().indexOf(table_types.getSelectedToggle()));
        memento.setNumber(TAG_FORMAT, formats.getToggles().indexOf(formats.getSelectedToggle()));
        memento.setString(TAG_FILE, filename.getText());
    }

    public void restore(final Memento memento)
    {
        memento.getNumber(TAG_SOURCE).ifPresent(index -> sources.selectToggle(sources.getToggles().get(index.intValue())));
        memento.getNumber(TAG_TYPE).ifPresent(index -> table_types.selectToggle(table_types.getToggles().get(index.intValue())));
        memento.getNumber(TAG_FORMAT).ifPresent(index -> formats.selectToggle(formats.getToggles().get(index.intValue())));
        memento.getString(TAG_FILE).ifPresent(filename::setText);
    }
}
