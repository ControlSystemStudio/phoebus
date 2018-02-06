/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.export;

import java.io.File;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.temporal.TemporalAmount;

import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.export.ExportJob;
import org.csstudio.trends.databrowser3.export.MatlabFileExportJob;
import org.csstudio.trends.databrowser3.export.MatlabScriptExportJob;
import org.csstudio.trends.databrowser3.export.PlainExportJob;
import org.csstudio.trends.databrowser3.export.Source;
import org.csstudio.trends.databrowser3.export.SpreadsheetExportJob;
import org.csstudio.trends.databrowser3.export.ValueFormatter;
import org.csstudio.trends.databrowser3.export.ValueWithInfoFormatter;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.ui.TimeRangeDialog;
import org.phoebus.archive.vtype.Style;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.dialog.SaveAsDialog;
import org.phoebus.util.time.SecondsParser;
import org.phoebus.util.time.TimeInterval;
import org.phoebus.util.time.TimeParser;
import org.phoebus.util.time.TimeRelativeInterval;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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

/** Panel for exporting data into files
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ExportView extends VBox
{
    private static final String TAG_SOURCE = "source",
                                TAG_OPTCOUNT = "optcount",
                                TAG_LININT = "linint",
                                TAG_TYPE = "type",
                                TAG_FORMAT = "format",
                                TAG_DIGITS = "digits",
                                TAG_FILE = "file";

    private final Model model;
    private final TextField start = new TextField();
    private final TextField end = new TextField();
    private final CheckBox use_plot_times = new CheckBox(Messages.ExportPlotStartEnd),
                           tabular = new CheckBox(Messages.ExportTabular),
                           min_max_col = new CheckBox(Messages.ExportMinMaxCol),
                           sev_stat = new CheckBox(Messages.ExportValueInfo);
    private final ToggleGroup sources = new ToggleGroup(),
                              table_types = new ToggleGroup(),
                              formats = new ToggleGroup();
    private final TextField optimize = new TextField(Messages.ExportDefaultOptimization),
                            linear = new TextField(Messages.ExportDefaultLinearInterpolation),
                            format_digits = new TextField(Messages.ExportDefaultDigits),
                            filename = new TextField();
    private final RadioButton source_raw = new RadioButton(Source.RAW_ARCHIVE.toString()),
                              type_matlab = new RadioButton(Messages.ExportTypeMatlab);

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
        start.setTooltip(new Tooltip(Messages.StartTimeTT));
        GridPane.setHgrow(start, Priority.ALWAYS);
        grid.add(start, 1, 0);

        final Button sel_times = new Button(Messages.StartEndDialogBtn);
        sel_times.setTooltip(new Tooltip(Messages.StartEndDialogTT));
        grid.add(sel_times, 2, 0);

        grid.add(new Label(Messages.EndTimeLbl), 0, 1);
        end.setTooltip(new Tooltip(Messages.EndTimeTT));
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

        // Order of source_* radio buttons must match the corresponding Source.* ordinal
        final RadioButton source_plot = new RadioButton(Source.PLOT.toString());
        source_plot.setTooltip(new Tooltip(Messages.ExportSource_PlotTT));
        source_plot.setToggleGroup(sources);

        source_raw.setTooltip(new Tooltip(Messages.ExportSource_RawArchiveTT));
        source_raw.setToggleGroup(sources);

        final RadioButton source_opt = new RadioButton(Source.OPTIMIZED_ARCHIVE.toString());
        source_opt.setTooltip(new Tooltip(Messages.ExportSource_OptimizedArchiveTT));
        source_opt.setToggleGroup(sources);

        optimize.setPrefColumnCount(6);
        optimize.setTooltip(new Tooltip(Messages.ExportOptimizationTT));
        optimize.disableProperty().bind(source_opt.selectedProperty().not());

        final RadioButton source_lin = new RadioButton(Source.LINEAR_INTERPOLATION.toString());
        source_lin.setTooltip(new Tooltip(Messages.ExportSource_LinearTT));
        source_lin.setToggleGroup(sources);

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

        type_matlab.setTooltip(new Tooltip(Messages.ExportTypeMatlabTT));
        type_matlab.setToggleGroup(table_types);
        grid.add(type_matlab, 1, 0);

        type_spreadsheet.setSelected(true);

        tabular.setTooltip(new Tooltip(Messages.ExportTabularTT));
        tabular.setSelected(true);
        grid.add(tabular, 0, 1);

        // Tabular only applies to spreadsheet
        tabular.disableProperty().bind(type_matlab.selectedProperty());

        min_max_col.setTooltip(new Tooltip(Messages.ExportMinMaxColTT));
        grid.add(min_max_col, 1, 1);

        sev_stat.setTooltip(new Tooltip(Messages.ExportValueInfoTT));
        grid.add(sev_stat, 2, 1);

        sev_stat.disableProperty().bind(type_matlab.selectedProperty());

        // Enable/disable min/max checkbox
        type_matlab.selectedProperty().addListener(prop -> min_max_col.setDisable(! minMaxAllowed()));
        sources.selectedToggleProperty().addListener(prop -> min_max_col.setDisable(! minMaxAllowed()));
        min_max_col.setDisable(! minMaxAllowed());

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

        format_digits.setPrefColumnCount(3);
        format_digits.setTooltip(new Tooltip(Messages.ExportDigitsTT));
        format_digits.disableProperty().bind(format_default.selectedProperty());
        grid.add(format_digits, 3, 2);

        // Formatting only applies to spreadsheet
        format_default.disableProperty().bind(type_matlab.selectedProperty());
        format_decimal.disableProperty().bind(type_matlab.selectedProperty());
        format_expo.disableProperty().bind(type_matlab.selectedProperty());
        format_digits.disableProperty().bind(type_matlab.selectedProperty());

        grid.add(new Label(Messages.ExportDigits), 4, 2);

        format_default.setSelected(true);

        final TitledPane format = new TitledPane(Messages.ExportGroupFormat, grid);
        format.setCollapsible(false);


        // * Output *
        // Filename: ______________ [Browse] [Export]
        filename.setPromptText(Messages.ExportDefaultFilename);
        filename.setTooltip(new Tooltip(Messages.ExportFilenameTT));

        final Button sel_filename = new Button(Messages.ExportBrowse);
        sel_filename.setTooltip(new Tooltip(Messages.ExportBrowseTT));
        sel_filename.setOnAction(event -> selectFilename());

        final Button export = new Button(Messages.ExportStartExport, Activator.getIcon("export"));
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

    /** @return <code>true</code> if the min/max (error) column option should be enabled */
    private boolean minMaxAllowed()
    {
        return !type_matlab.isSelected()  &&   !source_raw.isSelected();
    }

    private void selectFilename()
    {
        File file = new File(filename.getText().trim());
        file = new SaveAsDialog().promptForFile(getScene().getWindow(),
                                                Messages.Export, file, null);
        if (file != null)
            filename.setText(file.getAbsolutePath());
    }

    private void startExportJob()
    {
        try
        {
            // Determine start/end time
            TimeRelativeInterval range = null;
            if (use_plot_times.isSelected())
                range = model.getTimerange();
            else
            {
                final Object s = TimeParser.parseInstantOrTemporalAmount(start.getText());
                final Object e = TimeParser.parseInstantOrTemporalAmount(end.getText());
                if (s instanceof Instant)
                {
                    if (e instanceof Instant)
                        range = TimeRelativeInterval.of((Instant)s, (Instant)e);
                    else if (e instanceof TemporalAmount)
                        range = TimeRelativeInterval.of((Instant)s, (TemporalAmount)e);
                }
                else if (s instanceof TemporalAmount)
                {
                    if (e instanceof Instant)
                        range = TimeRelativeInterval.of((TemporalAmount) s, (Instant) e);
                    else if (e instanceof TemporalAmount)
                        range = TimeRelativeInterval.of((TemporalAmount) s, (TemporalAmount) e);
                }
            }
            if (range == null)
                throw new Exception("Invalid start..end time range");

            // Determine source: Plot, archive, ...
            final int src_index = sources.getToggles().indexOf(sources.getSelectedToggle());
            if (src_index < 0  ||  src_index >= Source.values().length)
                throw new Exception("Invalid sample source");
            final Source source = Source.values()[src_index];
            int optimize_parameter = -1;
            if (source == Source.OPTIMIZED_ARCHIVE)
            {
                try
                {
                    optimize_parameter = Integer.parseInt(optimize.getText().trim());
                }
                catch (Exception ex)
                {
                    ExceptionDetailsErrorDialog.openError(optimize, Messages.Error, Messages.ExportOptimizeCountError, new Exception(optimize.getText()));
                    Platform.runLater(optimize::requestFocus);
                    return;
                }
            }
            else if (source == Source.LINEAR_INTERPOLATION)
            {
                try
                {
                    optimize_parameter = (int) (SecondsParser.parseSeconds(linear.getText().trim()) + 0.5);
                    if (optimize_parameter < 1)
                        optimize_parameter = 1;
                }
                catch (Exception ex)
                {
                    ExceptionDetailsErrorDialog.openError(linear, Messages.Error, Messages.ExportLinearIntervalError, new Exception(linear.getText()));
                    Platform.runLater(linear::requestFocus);
                    return;
                }
            }

            // Get remaining export parameters
            final String filename = this.filename.getText().trim();
            if (filename.isEmpty())
            {
                ExceptionDetailsErrorDialog.openError(this.filename, Messages.Error, Messages.ExportEnterFilenameError, new Exception(filename));
                Platform.runLater(this.filename::requestFocus);
                return;
            }
            if (new File(filename).exists())
            {
                final Alert dialog = new Alert(AlertType.CONFIRMATION);
                dialog.setTitle(Messages.ExportFileExists);
                dialog.setHeaderText(MessageFormat.format(Messages.ExportFileExistsFmt, filename));
                DialogHelper.positionDialog(dialog, this.filename, -200, -200);
                if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
                {
                    Platform.runLater(this.filename::requestFocus);
                    return;
                }
            }


            // Construct appropriate export job
            final ExportJob export;
            final TimeInterval start_end = range.toAbsoluteInterval();
            if (type_matlab.isSelected())
            {   // Matlab file export
                if (filename.endsWith(".m"))
                    export = new MatlabScriptExportJob(model, start_end.getStart(), start_end.getEnd(), source, optimize_parameter, filename, this::handleError);
                else if (filename.endsWith(".mat"))
                    export = new MatlabFileExportJob(model, start_end.getStart(), start_end.getEnd(), source, optimize_parameter, filename, this::handleError);
                else
                {
                    ExceptionDetailsErrorDialog.openError(this.filename, Messages.Error, Messages.ExportMatlabFilenameError, new Exception(filename));
                    Platform.runLater(this.filename::requestFocus);
                    return;
                }
            }
            else
            {   // Spreadsheet file export
                Style style = Style.Default;

                // Default, decimal, exponential
                final int fmt = formats.getToggles().indexOf(formats.getSelectedToggle());
                if (fmt == 1)
                    style = Style.Decimal;
                else if (fmt == 2)
                    style = Style.Exponential;
                int precision = 0;
                if (style != Style.Default)
                {
                    try
                    {
                        precision = Integer.parseInt(format_digits.getText().trim());
                    }
                    catch (Exception ex)
                    {
                        ExceptionDetailsErrorDialog.openError(format_digits, Messages.Error, Messages.ExportDigitsError, new Exception(format_digits.getText()));
                        Platform.runLater(format_digits::requestFocus);
                        return;
                    }
                }

                final ValueFormatter formatter;
                if (sev_stat.isSelected())
                    formatter = new ValueWithInfoFormatter(style, precision);
                else
                    formatter = new ValueFormatter(style, precision);
                formatter.useMinMaxColumn(minMaxAllowed() && min_max_col.isSelected());
                if (tabular.isSelected())
                    export = new SpreadsheetExportJob(model, start_end.getStart(), start_end.getEnd(), source,
                            optimize_parameter, formatter, filename, this::handleError);
                else
                    export = new PlainExportJob(model, start_end.getStart(), start_end.getEnd(), source,
                            optimize_parameter, formatter, filename, this::handleError);
            }

            JobManager.schedule(filename, export);
        }
        catch (Exception ex)
        {
            handleError(ex);
        }
    }

    private void handleError(final Exception ex)
    {
        ExceptionDetailsErrorDialog.openError(this, Messages.Error, "Export error", ex);
    }

    public void save(final Memento memento)
    {
        memento.setNumber(TAG_SOURCE, sources.getToggles().indexOf(sources.getSelectedToggle()));
        memento.setString(TAG_OPTCOUNT, optimize.getText());
        memento.setString(TAG_LININT, linear.getText());
        memento.setNumber(TAG_TYPE, table_types.getToggles().indexOf(table_types.getSelectedToggle()));
        memento.setNumber(TAG_FORMAT, formats.getToggles().indexOf(formats.getSelectedToggle()));
        memento.setString(TAG_DIGITS, format_digits.getText());
        memento.setString(TAG_FILE, filename.getText());
    }

    public void restore(final Memento memento)
    {
        memento.getNumber(TAG_SOURCE).ifPresent(index -> sources.selectToggle(sources.getToggles().get(index.intValue())));
        memento.getString(TAG_OPTCOUNT).ifPresent(optimize::setText);
        memento.getString(TAG_LININT).ifPresent(linear::setText);
        memento.getNumber(TAG_TYPE).ifPresent(index -> table_types.selectToggle(table_types.getToggles().get(index.intValue())));
        memento.getNumber(TAG_FORMAT).ifPresent(index -> formats.selectToggle(formats.getToggles().get(index.intValue())));
        memento.getString(TAG_DIGITS).ifPresent(format_digits::setText);
        memento.getString(TAG_FILE).ifPresent(filename::setText);
    }
}
