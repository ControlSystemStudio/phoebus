package org.csstudio.trends.databrowser3.ui.export;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;

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
    private final Model model;

    public ExportView(final Model model)
    {
        this.model = model;

        // * Samples To Export *
        // Start:  ___start_______________________________________________________________ [select]
        // End  :  ___end_________________________________________________________________ [x] Use start/end time of Plot
        // Source: ( ) Plot  (*) Raw Archived Data  ( ) Averaged Archived Data  __time__   ( ) Linear __linear__ {ghost}
        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);
        grid.setPadding(new Insets(5));

        grid.add(new Label(Messages.StartTimeLbl), 0, 0);
        final TextField start = new TextField();
        GridPane.setHgrow(start, Priority.ALWAYS);

        grid.add(start, 1, 0);

        final Button sel_times = new Button(Messages.StartEndDialogBtn);
        sel_times.setOnAction(event ->
        {
            System.out.println("Select time range..");
        });
        grid.add(sel_times, 3, 0);

        grid.add(new Label(Messages.EndTimeLbl), 0, 1);

        final TextField end = new TextField();
        GridPane.setHgrow(end, Priority.ALWAYS);
        grid.add(end, 1, 1);

        final CheckBox use_plot_times = new CheckBox(Messages.ExportPlotStartEnd);
        use_plot_times.setTooltip(new Tooltip(Messages.ExportPlotStartEndTT));
        grid.add(use_plot_times, 3, 1);

        use_plot_times.selectedProperty().addListener((prop, was_selected, selected) ->
        {
            if (selected  &&  ! was_selected)
            {
                // TODO
                start.setText("start from model");
                end.setText("end from model");
            }
            start.setDisable(selected);
            end.setDisable(selected);
            sel_times.setDisable(selected);
        });
        use_plot_times.setSelected(true);

        grid.add(new Label(Messages.ExportGroupSource), 0, 2);

        final ToggleGroup sources = new ToggleGroup();
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
        optimize.setTooltip(new Tooltip(Messages.ExportOptimizationTT));
        optimize.disableProperty().bind(source_opt.selectedProperty().not());

        final RadioButton source_lin = new RadioButton(Messages.ExportSource_Linear);
        source_lin.setTooltip(new Tooltip(Messages.ExportSource_LinearTT));
        source_lin.setToggleGroup(sources);

        final TextField linear = new TextField(Messages.ExportDefaultLinearInterpolation);
        linear.setTooltip(new Tooltip(Messages.ExportDefaultLinearInterpolationTT));
        linear.disableProperty().bind(source_lin.selectedProperty().not());

        final HBox source_options = new HBox(5, source_plot, source_raw, source_opt, optimize, source_lin, linear);
        source_options.setAlignment(Pos.CENTER_LEFT);
        grid.add(source_options, 1, 2);

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



        final TitledPane format = new TitledPane(Messages.ExportGroupFormat, grid);
        format.setCollapsible(false);


        // * Output *
        // Filename: ______________ [Browse] [EXPORT]



        final TitledPane output = new TitledPane(Messages.ExportGroupOutput, new Label("XXX"));
        output.setCollapsible(false);



        getChildren().setAll(source, format, output);

    }
}
