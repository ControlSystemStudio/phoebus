/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.csstudio.trends.databrowser3.ui.properties;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.csstudio.javafx.rtplot.data.PlotDataSearch;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.model.ModelListener;
import org.csstudio.trends.databrowser3.model.PlotSample;
import org.csstudio.trends.databrowser3.model.PlotSamples;
import org.epics.vtype.VString;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.jobs.JobRunnable;
import org.phoebus.util.time.TimeInterval;
import org.phoebus.util.time.TimeRelativeInterval;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Tab showing statistics data for traces. User needs to actively request computation of the statistical data.
 * Only samples visible in the plot are considered in the calculation. For instance, if the time axis changes
 * to show a subset of loaded samples, the calculation will not consider non-visible portions of the trace.
 */
public class StatisticsTabController implements ModelListener{
    private Model model;

    private static Color colorIndicatorBorderColor =
            Color.color(0.71, 0.71, 0.71);

    @FXML
    private Button refreshAll;

    @FXML
    private TableView<ModelItemStatistics> tracesTable;
    @FXML
    private TableColumn<ModelItemStatistics, ColorIndicator> indicatorColumn;
    @FXML
    private TableColumn<ModelItemStatistics, String> displayNameColumn;
    @FXML
    private TableColumn<ModelItemStatistics, Long> countColumn;
    @FXML
    private TableColumn<ModelItemStatistics, Double> meanColumn;
    @FXML
    private TableColumn<ModelItemStatistics, Double> medianColumn;
    @FXML
    private TableColumn<ModelItemStatistics, Double> stdDevColumn;
    @FXML
    private TableColumn<ModelItemStatistics, Double> minColumn;
    @FXML
    private TableColumn<ModelItemStatistics, Double> maxColumn;
    @FXML
    private TableColumn<ModelItemStatistics, Double> sumColumn;

    /** @param model Model */
    public StatisticsTabController(Model model){
        this.model = model;
    }

    private SimpleBooleanProperty refreshDisabled = new SimpleBooleanProperty(false);

    /** Init */
    @FXML
    public void initialize() {

        refreshAll.setText(Messages.Refresh);
        refreshAll.setOnAction(e -> refreshAll());
        tracesTable.setPlaceholder(new Label(Messages.TraceTableEmpty));
        model.addListener(this);
        createTable();
        refreshAll.disableProperty().bind(refreshDisabled);
    }

    /** @param modelItem Model item */
    @Override
    public void itemAdded(ModelItem modelItem) {
        ModelItemStatistics statistics = new ModelItemStatistics(modelItem);
        tracesTable.getItems().add(statistics);
    }

    /** @param modelItem Model item */
    @Override
    public void changedItemLook(ModelItem modelItem){
        ModelItemStatistics statistics =
                tracesTable.getItems().stream().filter(item -> item.getUniqueItemId().equals(modelItem.getUniqueId())).findAny().orElse(null);
        if(statistics != null){
            statistics.setTraceName(modelItem.getResolvedDisplayName());
            statistics.setColorIndicator(modelItem.getPaintColor());
        }
    }

    /** @param modelItem Model item */
    @Override
    public void itemRemoved(ModelItem modelItem) {
        ModelItemStatistics statistics =
                tracesTable.getItems().stream().filter(item -> item.getUniqueItemId().equals(modelItem.getUniqueId())).findAny().orElse(null);
        if(statistics != null){
            tracesTable.getItems().remove(statistics);
        }
    }

    /** Refresh all */
    @FXML
    public void refreshAll(){
        JobManager.schedule("Compute trace statistics", new JobRunnable() {
            @Override
            public void run(JobMonitor monitor) {
                try {
                    refreshDisabled.set(true);
                    tracesTable.getItems().stream().forEach(item -> item.update(model.getItemByUniqueId(item.getUniqueItemId())));
                }
                finally {
                    refreshDisabled.set(false);
                }
            }
        });
    }

    private void createTable(){
        displayNameColumn.setText(Messages.TraceDisplayName);
        displayNameColumn.setCellValueFactory(cell -> cell.getValue().getTraceName());
        displayNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        indicatorColumn.setCellValueFactory(column -> column.getValue().getColorIndicator());

        indicatorColumn.setCellFactory(column -> new TableCell<>(){
            @Override
            public void updateItem(final ColorIndicator colorIndicator, final boolean empty){
                super.updateItem(colorIndicator, empty);
                setGraphic(empty ? null : colorIndicator);
                setStyle( "-fx-alignment: CENTER-LEFT;");
            }
        });

        countColumn.setText(Messages.StatisticsSampleCount);
        countColumn.setCellValueFactory(cell -> cell.getValue().getCount().asObject());
        meanColumn.setText(Messages.StatisticsMean);
        meanColumn.setCellValueFactory(cell -> cell.getValue().getMean().asObject());
        medianColumn.setText(Messages.StatisticsMedian);
        medianColumn.setCellValueFactory(cell -> cell.getValue().getMedian().asObject());
        stdDevColumn.setText(Messages.StatisticsStdDev);
        stdDevColumn.setCellValueFactory(cell -> cell.getValue().getStdDev().asObject());
        minColumn.setText(Messages.StatisticsMin);
        minColumn.setCellValueFactory(cell -> cell.getValue().getMin().asObject());
        maxColumn.setText(Messages.StatisticsMax);
        maxColumn.setCellValueFactory(cell -> cell.getValue().getMax().asObject());
        sumColumn.setText(Messages.StatisticsSum);
        sumColumn.setCellValueFactory(cell -> cell.getValue().getSum().asObject());
    }

    /** Remove listener */
    public void removeModelListener(){
        model.removeListener(this);
    }

    /**
     * Simple color indicator used to identify the trace in the table.
     */
    private class ColorIndicator extends Rectangle {
        public ColorIndicator(Color color){
            super();
            setX(0);
            setY(0);
            setWidth(12);
            setHeight(12);
            setFill(color);
            setStroke(colorIndicatorBorderColor);
        }
    }

    /**
     * Wraps statistical data plus name and color indicator for a trace. This is
     * the data model for the table.
     */
    private class ModelItemStatistics {
        private SimpleLongProperty count = new SimpleLongProperty();
        private SimpleDoubleProperty mean = new SimpleDoubleProperty(Double.NaN);
        private SimpleDoubleProperty median = new SimpleDoubleProperty(Double.NaN);
        private SimpleDoubleProperty stdDev = new SimpleDoubleProperty(Double.NaN);
        private SimpleDoubleProperty min = new SimpleDoubleProperty(Double.NaN);
        private SimpleDoubleProperty max = new SimpleDoubleProperty(Double.NaN);
        private SimpleDoubleProperty sum = new SimpleDoubleProperty(Double.NaN);
        private SimpleObjectProperty colorIndicator = new SimpleObjectProperty();
        private SimpleStringProperty traceName = new SimpleStringProperty();

        private String uniqueItemId;

        public ModelItemStatistics(ModelItem modelItem){
            uniqueItemId = modelItem.getUniqueId();
            setColorIndicator(modelItem.getPaintColor());
            setTraceName(modelItem.getResolvedDisplayName());
        }

        /**
         * Clears all statistics fields, can be used to indicate that there
         * is nothing to show (e.g. no plot samples in range)
         */
        private void clear(){
            Platform.runLater(() -> {
                count.set(0);
                mean.set(Double.NaN);
                median.set(Double.NaN);
                stdDev.set(Double.NaN);
                min.set(Double.NaN);
                max.set(Double.NaN);
                sum.set(Double.NaN);
            });
        }

        private void set(DescriptiveStatistics statistics){
            Platform.runLater(() -> {
                count.set(statistics.getN());
                mean.set(statistics.getMean());
                median.set(statistics.getPercentile(50));
                stdDev.set(statistics.getStandardDeviation());
                min.set(statistics.getMin());
                max.set(statistics.getMax());
                sum.set(statistics.getSum());
            });
        }

        /**
         * Updates the statistics data using background {@link org.phoebus.framework.jobs.Job}.
         * @param modelItem
         */
        public void update(final ModelItem modelItem){
            if(modelItem == null){ // Should not happen...?
                return;
            }
            DescriptiveStatistics statistics = new DescriptiveStatistics();
            PlotSamples plotSamples = modelItem.getSamples();

            TimeRelativeInterval timeRelativeInterval = model.getTimerange();
            TimeInterval timeInterval = timeRelativeInterval.toAbsoluteInterval();
            int startIndex;
            int endIndex;
            PlotDataSearch plotDataSearch = new PlotDataSearch();
            plotSamples.getLock().lock();
            startIndex = plotDataSearch.findSampleLessOrEqual(plotSamples, timeInterval.getStart());
            endIndex = plotDataSearch.findSampleGreaterOrEqual(plotSamples, timeInterval.getEnd());
            if(startIndex == -1){ // First sample after start time...
                startIndex = 0;
                if(endIndex == -1){//...and last sample after end time
                    plotSamples.getLock().unlock();
                    clear();
                    return; // No samples found in time range
                }
            }
            for(int i = startIndex; i <= endIndex; i++){
                PlotSample plotSample = plotSamples.get(i);
                // Exclude VString samples, e.g. due to disconnected PV
                if(plotSample.getVType() instanceof VString){
                    continue;
                }
                statistics.addValue(plotSample.getValue());
            }
            plotSamples.getLock().unlock();
            set(statistics);
        }

        public void setTraceName(String traceName){
            this.traceName.set(traceName);
        }

        public void setColorIndicator(Color color){
            colorIndicator.set(new ColorIndicator(color));
        }

        public SimpleLongProperty getCount() {
            return count;
        }

        public SimpleDoubleProperty getStdDev() {
            return stdDev;
        }

        public SimpleDoubleProperty getMean() {
            return mean;
        }

        public SimpleDoubleProperty getMedian() {
            return median;
        }

        public SimpleDoubleProperty getMin() {
            return min;
        }

        public SimpleDoubleProperty getMax() {
            return max;
        }

        public SimpleDoubleProperty getSum() {
            return sum;
        }

        public SimpleObjectProperty getColorIndicator() {
            return colorIndicator;
        }

        public SimpleStringProperty getTraceName() {
            return traceName;
        }

        public String getUniqueItemId(){
            return uniqueItemId;
        }

        @Override
        public boolean equals(Object other){
            if(!(other instanceof ModelItemStatistics)){
                return false;
            }
            return uniqueItemId.equals(((ModelItemStatistics)other).getUniqueItemId());
        }

        @Override
        public int hashCode(){
            return uniqueItemId.hashCode();
        }
    }
}
