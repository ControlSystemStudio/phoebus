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

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.model.ModelListener;
import org.csstudio.trends.databrowser3.model.PlotSample;
import org.csstudio.trends.databrowser3.model.PlotSamples;
import org.phoebus.framework.jobs.Job;
import org.phoebus.util.time.TimeRelativeInterval;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class StatisticsTabController {
    private Model model;

    private static Color border = Color.color(0.71, 0.71, 0.71);

    @FXML
    private Button refreshAll;

    @FXML
    private TableView<ModelItem> tracesTable;
    @FXML
    private TableColumn<ModelItem, ColorIndicator> indicatorColumn;
    @FXML
    private TableColumn<ModelItem, String> displayNameColumn;
    @FXML
    private TableColumn<ModelItem, String> countColumn;
    @FXML
    private TableColumn<ModelItem, String> meanColumn;
    @FXML
    private TableColumn<ModelItem, String> medianColumn;
    @FXML
    private TableColumn<ModelItem, String> stdDevColumn;
    @FXML
    private TableColumn<ModelItem, String> minColumn;
    @FXML
    private TableColumn<ModelItem, String> maxColumn;
    @FXML
    private TableColumn<ModelItem, String> sumColumn;

    private HashMap<String, ItemStatistics> itemStatistics = new HashMap<>();

    public StatisticsTabController(Model model){
        this.model = model;
    }


    @FXML
    public void initialize() {

        refreshAll.setText(Messages.Refresh);
        refreshAll.setOnAction(e -> refreshAll());
        tracesTable.setPlaceholder(new Label(Messages.TraceTableEmpty));
        createTable();
        model.addListener(new ModelListener() {
            @Override
            public void itemAdded(ModelItem item) {
                ItemStatistics statistics = new ItemStatistics();
                itemStatistics.put(item.getResolvedName(), statistics);
                tracesTable.getItems().setAll(model.getItems());
            }

            @Override
            public void changedItemLook(ModelItem item){
                tracesTable.refresh();
            }
        });
    }

    @FXML
    public void refreshAll(){
        model.getItems().stream().forEach(item -> refresh(item));
    }

    private void createTable(){
        displayNameColumn.setText(Messages.TraceDisplayName);
        displayNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDisplayName()));
        displayNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        indicatorColumn.setCellValueFactory(column ->
             new SimpleObjectProperty<>(new ColorIndicator(column.getValue().getPaintColor()))
        );

        indicatorColumn.setCellFactory(column -> new TableCell<>(){
            @Override
            public void updateItem(final ColorIndicator colorIndicator, final boolean empty){
                super.updateItem(colorIndicator, empty);
                setGraphic(empty ? null : colorIndicator);
                setStyle( "-fx-alignment: CENTER-LEFT;");
            }
        });

        countColumn.setText(Messages.StatisticsSampleCount);
        countColumn.setCellValueFactory(cell ->
                itemStatistics.get(cell.getValue().getResolvedName()).getCount());
        meanColumn.setText(Messages.StatisticsMean);
        meanColumn.setCellValueFactory(cell ->
            itemStatistics.get(cell.getValue().getResolvedName()).getMean());
        meanColumn.setText(Messages.StatisticsMedian);
        medianColumn.setCellValueFactory(cell ->
                itemStatistics.get(cell.getValue().getResolvedName()).getMedian());
        stdDevColumn.setText(Messages.StatisticsStdDev);
        stdDevColumn.setCellValueFactory(cell ->
                itemStatistics.get(cell.getValue().getResolvedName()).getStdDev());
        minColumn.setText(Messages.StatisticsMin);
        minColumn.setCellValueFactory(cell ->
                itemStatistics.get(cell.getValue().getResolvedName()).getMin());
        maxColumn.setText(Messages.StatisticsMax);
        maxColumn.setCellValueFactory(cell ->
                itemStatistics.get(cell.getValue().getResolvedName()).getMax());
        sumColumn.setText(Messages.StatisticsSum);
        sumColumn.setCellValueFactory(cell ->
                itemStatistics.get(cell.getValue().getResolvedName()).getSum());
    }

    private void refresh(ModelItem modelItem){
        itemStatistics.get(modelItem.getResolvedName()).update(modelItem);
    }

    private class ColorIndicator extends Rectangle {
        public ColorIndicator(Color color){
            super();
            setX(0);
            setY(0);
            setWidth(10);
            setHeight(10);
            setFill(color);
            setStroke(border);
        }
    }

    private class ItemStatistics{
        private SimpleStringProperty count = new SimpleStringProperty();
        private SimpleStringProperty mean = new SimpleStringProperty();
        private SimpleStringProperty median = new SimpleStringProperty();
        private SimpleStringProperty stdDev = new SimpleStringProperty();
        private SimpleStringProperty min = new SimpleStringProperty();
        private SimpleStringProperty max = new SimpleStringProperty();
        private SimpleStringProperty sum = new SimpleStringProperty();

        public void update(ModelItem modelItem){
            DescriptiveStatistics statistics = new DescriptiveStatistics();

            PlotSamples plotSamples = modelItem.getSamples();
            plotSamples.getLock().lock();
            TimeRelativeInterval timeRelativeInterval = model.getTimerange();
            long start;
            long end;
            if(timeRelativeInterval.getAbsoluteEnd().isPresent()){
                start = 1000 * timeRelativeInterval.getAbsoluteStart().get().getEpochSecond();
                end = 1000 *  timeRelativeInterval.getAbsoluteEnd().get().getEpochSecond();
            }
            else{
                long now = System.currentTimeMillis();
                start = now - 1000 * timeRelativeInterval.getRelativeStart().get().get(ChronoUnit.SECONDS);
                end = now - 1000 * timeRelativeInterval.getRelativeEnd().get().get(ChronoUnit.SECONDS);
            }
            int length = modelItem.getSamples().size();
            int counter = 0;
            for(int i = 0; i < length; i++){

                if(plotSamples.get(i).getPosition().isBefore(Instant.ofEpochMilli(start))){
                    continue;
                }
                else if(plotSamples.get(i).getPosition().isAfter(Instant.ofEpochMilli(end))){
                    break;
                }
                counter++;
                statistics.addValue(plotSamples.get(i).getValue());
            }
            plotSamples.getLock().unlock();
            count.set(String.valueOf(counter));

            mean.set(String.valueOf(statistics.getMean()));
            median.set(String.valueOf(statistics.getPercentile(50)));
            stdDev.set(String.valueOf(statistics.getStandardDeviation()));
            min.set(String.valueOf(statistics.getMin()));
            max.set(String.valueOf(statistics.getMax()));
            sum.set(String.valueOf(statistics.getSum()));
        }

        public SimpleStringProperty getCount() {
            return count;
        }

        public SimpleStringProperty getStdDev() {
            return stdDev;
        }

        public SimpleStringProperty getMean() {
            return mean;
        }

        public SimpleStringProperty getMedian() {
            return median;
        }

        public SimpleStringProperty getMin() {
            return min;
        }

        public SimpleStringProperty getMax() {
            return max;
        }

        public SimpleStringProperty getSum() {
            return sum;
        }
    }
}
