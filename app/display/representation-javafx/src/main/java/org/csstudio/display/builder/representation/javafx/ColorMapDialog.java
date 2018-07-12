/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import org.csstudio.display.builder.model.properties.ColorMap;
import org.csstudio.display.builder.model.properties.PredefinedColorMaps;
import org.csstudio.javafx.rtplot.ColorMappingFunction;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.ui.dialog.DialogHelper;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/** Dialog for editing {@link ColorMap}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ColorMapDialog extends Dialog<ColorMap>
{
    private static final int COLOR_BAR_HEIGHT = 50;

    /** One section of the color map: Value for slot and color */
    private static class ColorSection
    {
        final int value;
        final Color color;
        ColorSection(final int value, final Color color)
        {
            this.value = value;
            this.color = color;
        }
    }

    /** Table for picking predefined color */
    private TableView<PredefinedColorMaps.Predefined> predefined_table;

    /** Table for editing <code>color_sections</code> */
    private TableView<ColorSection> sections_table;

    /** Add, remove entry in <code>color_sections</code> */
    private Button add, remove;

    /** Sections of current map */
    private final ObservableList<ColorSection> color_sections = FXCollections.observableArrayList();

    /** Preview of <code>map</code> */
    private Region color_bar;

    /** Current 'value' of the dialog */
    private ColorMap map;

    /** @param map {@link ColorMap} show/edit in the dialog
     *  @param owner Node that invoked this dialog
     */
    public ColorMapDialog(final ColorMap map, final Node owner)
    {
        setTitle(Messages.ColorMapDialog_Title);
        setHeaderText(Messages.ColorMapDialog_Info);

        getDialogPane().setContent(createContent());
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        setResizable(true);

        updateUIfromMap(map);

        hookListeners();

        setResultConverter(button -> (button == ButtonType.OK) ? this.map : null);

        DialogHelper.positionAndSize(this, owner,
                                     PhoebusPreferenceService.userNodeForClass(ColorMapDialog.class));
    }

    /** Table cell that shows ColorPicker for ColorSection */
    private static class ColorTableCell extends TableCell<ColorSection, ColorPicker>
    {   // The color_column's CellValueFactory already turned the ColorSection into ColorPicker
        // Show place the picker in the cell
        @Override
        protected void updateItem(final ColorPicker picker, final boolean empty)
        {
            super.updateItem(picker, empty);
            setGraphic(empty ? null : picker);
        }
    }

    private Node createContent()
    {
        // Table for selecting a predefined color map
        predefined_table = new TableView<>();
        final TableColumn<PredefinedColorMaps.Predefined, String> name_column = new TableColumn<>(Messages.ColorMapDialog_PredefinedMap);
        name_column.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getDescription()));
        predefined_table.getColumns().add(name_column);
        predefined_table.getItems().addAll(PredefinedColorMaps.PREDEFINED);
        predefined_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Table for viewing/editing color sections
        sections_table = new TableView<>();
        // Value of color section
        final TableColumn<ColorSection, String> value_column = new TableColumn<>(Messages.ColorMapDialog_Value);
        value_column.setCellValueFactory(param -> new SimpleStringProperty(Integer.toString(param.getValue().value)));
        value_column.setCellFactory(TextFieldTableCell.forTableColumn());
        value_column.setOnEditCommit(event ->
        {
            final int index = event.getTablePosition().getRow();
            try
            {
                final int value = Math.max(0, Math.min(255, Integer.parseInt(event.getNewValue().trim())));
                color_sections.set(index, new ColorSection(value, color_sections.get(index).color));
                color_sections.sort((sec1, sec2) -> sec1.value - sec2.value);
                updateMapFromSections();
            }
            catch (NumberFormatException ex)
            {
                // Ignore, field will reset to original value
            }
        });

        // Color of color section
        final TableColumn<ColorSection, ColorPicker> color_column = new TableColumn<>(Messages.ColorMapDialog_Color);
        color_column.setCellValueFactory(param ->
        {
            final ColorSection segment = param.getValue();
            return new SimpleObjectProperty<>(createColorPicker(segment));
        });
        color_column.setCellFactory(column -> new ColorTableCell());

        sections_table.getColumns().add(value_column);
        sections_table.getColumns().add(color_column);
        sections_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        sections_table.setItems(color_sections);
        sections_table.setEditable(true);

        add = new Button(Messages.ColorMapDialog_Add, JFXUtil.getIcon("add.png"));
        add.setMaxWidth(Double.MAX_VALUE);
        remove = new Button(Messages.ColorMapDialog_Remove, JFXUtil.getIcon("delete.png"));
        remove.setMaxWidth(Double.MAX_VALUE);
        final VBox buttons = new VBox(10, add, remove);

        // Upper section with tables
        final HBox tables_and_buttons = new HBox(10, predefined_table, sections_table, buttons);
        HBox.setHgrow(predefined_table, Priority.ALWAYS);
        HBox.setHgrow(sections_table, Priority.ALWAYS);

        // Lower section with resulting color map
        final Region fill1 = new Region(), fill2 = new Region(), fill3 = new Region();
        HBox.setHgrow(fill1, Priority.ALWAYS);
        HBox.setHgrow(fill2, Priority.ALWAYS);
        HBox.setHgrow(fill3, Priority.ALWAYS);
        final HBox color_title = new HBox(fill1, new Label(Messages.ColorMapDialog_Result), fill2);

        color_bar = new Region();
        color_bar.setMinHeight(COLOR_BAR_HEIGHT);
        color_bar.setMaxHeight(COLOR_BAR_HEIGHT);
        color_bar.setPrefHeight(COLOR_BAR_HEIGHT);

        final HBox color_legend = new HBox(new Label("0"), fill3, new Label("255"));

        final VBox box = new VBox(10, tables_and_buttons, new Separator(), color_title, color_bar, color_legend);
        VBox.setVgrow(tables_and_buttons, Priority.ALWAYS);

        return box;
    }

    /** @param section Segment of the color map
     *  @return ColorPicker that updates this segment in #color_sections
     */
    private ColorPicker createColorPicker(final ColorSection section)
    {
        final Color color = section.color;
        final int index = color_sections.indexOf(section);
        if (index < 0)
            throw new IllegalArgumentException("Cannot locate color section " + section);
        final ColorPicker picker = new ColorPicker(color);
        picker.setOnAction(event ->
        {
            color_sections.set(index, new ColorSection(section.value, picker.getValue()));
            updateMapFromSections();
        });
        return picker;
    }

    private void hookListeners()
    {
        predefined_table.getSelectionModel().selectedItemProperty().addListener(
        (p, old, selected_map) ->
        {
            if (selected_map != null)
                updateUIfromMap(selected_map);
        });

        // Assert that at least 2 sections are left
        remove.setOnAction(event ->
        {
            if (color_sections.size() <= 2)
                return;

            final int index = sections_table.getSelectionModel().getSelectedIndex();
            if (index < 0)
                return;
            color_sections.remove(index);
            updateMapFromSections();
        });

        final InvalidationListener check_sections = observable -> remove.setDisable(color_sections.size() < 3);
        color_sections.addListener(check_sections);
        check_sections.invalidated(color_sections);

        // Try to interpolate new section between existing sections
        add.setOnAction(event ->
        {
            final int size = color_sections.size();
            final int index = Math.max(0, sections_table.getSelectionModel().getSelectedIndex());
            final ColorSection before = color_sections.get(index);
            if (index + 1  <  size)
            {   // Add new color in 'middle'
                final ColorSection after = (index + 1) < size ? color_sections.get(index+1) : null;
                final int mid_value = (before.value + after.value) / 2;
                final Color color_mix = before.color.interpolate(after.color, 0.5);
                color_sections.add(index+1, new ColorSection(mid_value, color_mix));
            }
            else
                color_sections.add(new ColorSection(255, before.color));

            updateMapFromSections();
        });
    }

    private void updateUIfromMap(final ColorMap map)
    {
        this.map = map;

        if (map instanceof PredefinedColorMaps.Predefined)
            predefined_table.getSelectionModel().select((PredefinedColorMaps.Predefined) map);
        else
            predefined_table.getSelectionModel().clearSelection();

        color_sections.clear();
        final int[][] sections = map.getSections();
        for (int[] section : sections)
            color_sections.add(new ColorSection(section[0], Color.rgb(section[1], section[2], section[3])));

        updateColorBar();
    }

    /** Update 'map' from 'color_sections' */
    private void updateMapFromSections()
    {
        final int num = color_sections.size();
        // Assert sections start .. end with 0 .. 255
        if (color_sections.get(0).value != 0)
            color_sections.set(0, new ColorSection(0, color_sections.get(0).color));
        if (color_sections.get(num-1).value != 255)
            color_sections.set(num-1, new ColorSection(255, color_sections.get(num-1).color));

        // Create ColorMap from sections
        final int[][] sections = new int[num][4];
        for (int i=0; i<num; ++i)
        {
            sections[i][0] = color_sections.get(i).value;
            sections[i][1] = (int) Math.round(color_sections.get(i).color.getRed()*255);
            sections[i][2] = (int) Math.round(color_sections.get(i).color.getGreen()*255);
            sections[i][3] = (int) Math.round(color_sections.get(i).color.getBlue()*255);
        }
        map = new ColorMap(sections);
        // Custom color map, not based on any predefined map
        predefined_table.getSelectionModel().clearSelection();

        updateColorBar();
    }

    /** Update color bar in UI from current 'map' */
    private void updateColorBar()
    {
        // On Mac OS X it was OK to create an image sized 256 x 1:
        // 256 wide to easily set the 256 colors,
        // 1 pixel height which is then stretched via the BackgroundSize().
        // On Linux, the result was garbled unless the image height matched the
        // actual height, so it's now fixed to COLOR_BAR_HEIGHT
        final WritableImage colors = new WritableImage(256, COLOR_BAR_HEIGHT);
        final PixelWriter writer = colors.getPixelWriter();
        for (int x=0; x<256; ++x)
        {
            final int arfb = ColorMappingFunction.getRGB(map.getColor(x));
            for (int y=0; y<COLOR_BAR_HEIGHT; ++y)
                writer.setArgb(x, y, arfb);
        }
        // Stretch image to fill color_bar
        color_bar.setBackground(new Background(
                new BackgroundImage(colors, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                                    BackgroundPosition.DEFAULT,
                                    new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, true, true, true, true))));
    }
}
