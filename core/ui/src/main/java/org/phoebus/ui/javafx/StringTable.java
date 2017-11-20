/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.javafx;

import static org.phoebus.ui.javafx.JFXUtil.logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.Cell;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Callback;
import javafx.util.converter.DefaultStringConverter;

/** Table of strings
 *
 *  <p>Table that shows String data based on a list
 *  of headers and a String matrix (List of Lists).
 *
 *  <p>Data can be changed at runtime, columns will
 *  then be re-created.

 *  <p>User can edit the cells.
 *  While inefficient, the table creates a deep copy
 *  of the data submitted to it for display, so changes
 *  in the table will not affect the original data.
 *
 *  <p>Toolbar and key shortcuts can be used to add/remove
 *  rows or columns:
 *  <ul>
 *  <li>t - Show/hide toolbar
 *  </ul>
 *
 *  <p>Class is implemented as {@link BorderPane}, but should
 *  be treated as a {@link Region}.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class StringTable extends BorderPane
{
    /** Value used for the last row
     *
     *  <p>This exact value is placed in the last row.
     *  It's not considered part of the data,
     *  but a marker that allows user to start a new row
     *  by entering values.
     *
     *  <p>Table data is compared as exact identity (== MAGIC_LAST_ROW).
     */
    private static final List<String> MAGIC_LAST_ROW = Arrays.asList(Messages.MagicLastRow);

    /** Value used to temporarily detach the 'data' from table */
    private static final ObservableList<List<String>> NO_DATA = FXCollections.observableArrayList();

    /** Data shown in the table, includes MAGIC_LAST_ROW */
    private final ObservableList<List<String>> data = FXCollections.observableArrayList();

    /** Optional cell coloring, does not include MAGIC_LAST_ROW */
    private volatile List<List<Color>> cell_colors = null;

    /** Cell factory for displaying the text
     *
     *  <p>special coloring of MAGIC_LAST_ROW which only has one column
     */
    private static final Callback<CellDataFeatures<List<String>, String>, ObservableValue<String>> CELL_FACTORY = param ->
    {
        final TableView<List<String>> table = param.getTableView();
        final int col_index = table.getColumns().indexOf(param.getTableColumn());
        final List<String> value = param.getValue();
        final String text;
        if (value == MAGIC_LAST_ROW)
            text = col_index == 0 ? MAGIC_LAST_ROW.get(0): "";
        else if (col_index < value.size())
            text = value.get(col_index);
        else
        	text = "<col " + col_index + "?>";
        return new SimpleStringProperty(text);
    };

    private final boolean editable;

    private Color background_color = Color.WHITE;

    private Color text_color = Color.BLACK;

    private Color last_row_color = text_color.deriveColor(0, 0, 0, 0.5);

    private Font font = Font.font(12);

    /** Table cell that displays a String,
     *  with special coloring of the MAGIC_LAST_ROW
     */
    private class StringTextCell extends TextFieldTableCell<List<String>, String>
    {
        public StringTextCell()
        {
            super(new DefaultStringConverter());
        }

        @Override
        public void updateItem(final String item, final boolean empty)
        {
            super.updateItem(item, empty);
            if (empty)
                return;
            final int row = getIndex();
            final int col = getTableView().getColumns().indexOf(getTableColumn());
            setTextFill(data.get(row) == MAGIC_LAST_ROW ? last_row_color : text_color);
            setCellStyle(this, row, col);
        }
    }

    /** Column options used for {@link BooleanCell} */
    public static final List<String> BOOLEAN_OPTIONS = Arrays.asList("false", "true");

    /** Cell with checkbox, sets data to "true"/"false" */
    private class BooleanCell extends TableCell<List<String>, String>
    {
        private final CheckBox checkbox = new CheckBox();

        public BooleanCell()
        {
            getStyleClass().add("check-box-table-cell");

            checkbox.setOnAction(event ->
            {
                final int row = getIndex();
                final int col = getTableView().getColumns().indexOf(getTableColumn());
                final String value = Boolean.toString(checkbox.isSelected());
                data.get(row).set(col, value);
                fireDataChanged();
            });
        }

        @Override
        protected void updateItem(final String item, final boolean empty)
        {
            super.updateItem(item, empty);

            final int row = getIndex();
            if (empty)
                setGraphic(null);
            else
            {
                if (data.get(row) == MAGIC_LAST_ROW)
                {
                    setText(item);
                    setGraphic(null);
                }
                else
                {
                    setText(null);
                    setGraphic(checkbox);
                    checkbox.setSelected(item.equalsIgnoreCase("true"));
                    final int col = getTableView().getColumns().indexOf(getTableColumn());
                    setCellStyle(this, row, col);
                }
            }
        }
    };

    /** Cell that allows selecting options from a combo */
    private class ComboCell extends ComboBoxTableCell<List<String>, String>
    {
        public ComboCell(final List<String> options)
        {
            super(FXCollections.observableArrayList(options));
            setComboBoxEditable(true);
        }

        @Override
        public void updateItem(final String item, final boolean empty)
        {
            super.updateItem(item, empty);
            if (empty)
                return;
            final int row = getIndex();
            final int col = getTableView().getColumns().indexOf(getTableColumn());
            setCellStyle(this, row, col);
        }
    };


    private final ToolBar toolbar = new ToolBar();

    private final TableView<List<String>> table = new TableView<>(data);

    /** Currently editing a cell? */
    private boolean editing = false;

    private volatile StringTableListener listener = null;


    /** Constructor
     *  @param editable Allow user interaction (toolbar, edit), or just display data?
     */
    public StringTable(final boolean editable)
    {
        this.editable = editable;
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setCellSelectionEnabled(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.getSelectionModel().getSelectedIndices().addListener(this::selectionChanged);
        table.setPlaceholder(new Label());

        if (editable)
        {
            table.setEditable(true);
            // Check for keys in both toolbar and table
            setOnKeyPressed(this::handleKey);
        }
        updateStyle();
        fillToolbar();
        setTop(toolbar);

        // Scroll if table is larger than its screen space
        final ScrollPane scroll = new ScrollPane(table);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        setCenter(scroll);

        setData(Arrays.asList(Arrays.asList()));
    }

    /** @param select_rows Select complete rows, or individual cells? */
    public void setRowSelectionMode(final boolean select_rows)
    {
    	if (select_rows)
            table.getSelectionModel().setCellSelectionEnabled(false);
    	else
            table.getSelectionModel().setCellSelectionEnabled(true);
    }

    /** @param listener Listener to notify of changes */
    public void setListener(final StringTableListener listener)
    {
        this.listener = listener;
    }

    private void fillToolbar()
    {
        toolbar.getItems().addAll(
            createToolbarButton("add_row", Messages.AddRow, event -> addRow()),
            createToolbarButton("remove_row", Messages.RemoveRow, event -> deleteRow()),
            createToolbarButton("row_up", Messages.MoveRowUp, event -> moveRowUp()),
            createToolbarButton("row_down", Messages.MoveRowDown, event -> moveRowDown()),
            createToolbarButton("rename_col", Messages.RenameColumn, event -> renameColumn()),
            createToolbarButton("add_col", Messages.AddColumn, event -> addColumn()),
            createToolbarButton("remove_col", Messages.RemoveColumn, event -> deleteColumn()),
            createToolbarButton("col_left", Messages.MoveColumnLeft, event -> moveColumnLeft()),
            createToolbarButton("col_right", Messages.MoveColumnRight, event -> moveColumnRight()));
        toolbar.layout();
    }

    private Button createToolbarButton(final String id, final String tool_tip, final EventHandler<ActionEvent> handler)
    {
        final Button button = new Button();
        try
        {
            // TODO Icons are not centered inside the button until the
            // button is once pressed, or at least focused via "tab"
            button.setGraphic(ImageCache.getImageView(getClass(), "/icons/" + id + ".png"));

            // Using the image as a background like this centers the image,
            // but replaces the complete characteristic button outline with just the icon.
            // button.setBackground(new Background(new BackgroundImage(new Image(Activator.getIcon(id)),
            //                      BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
            //                      BackgroundPosition.CENTER,
            //                      new BackgroundSize(16, 16, false, false, false, false))));
            button.setTooltip(new Tooltip(tool_tip));
            // Without defining the button size, the buttons may start out zero-sized
            // until they're first pressed/tabbed
            button.setMinSize(35, 25);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot load icon for " + id, ex);
            button.setText(tool_tip);
        }
        button.setOnAction(handler);

        return button;
    }

    /** @return <code>true</code> if toolbar is visible */
    public boolean isToolbarVisible()
    {
        return getTop() != null;
    }

    /** @param show <code>true</code> if toolbar should be displayed */
    public void showToolbar(final boolean show)
    {
        if (isToolbarVisible() == show)
            return;
        if (show)
            setTop(toolbar);
        else
            setTop(null);

        // Force layout to reclaim space used by hidden toolbar,
        // or make room for the visible toolbar
        layoutChildren();
        // XX Hack: Toolbar is garbled, all icons in pile at left end,
        // when shown the first time, i.e. it was hidden when the plot
        // was first shown.
        // Manual fix is to hide and show again.
        // Workaround is to force another layout a little later
        if (show)
            ForkJoinPool.commonPool().submit(() ->
            {
                Thread.sleep(1000);
                Platform.runLater(() -> layoutChildren() );
                return null;
            });
    }

    /** @param color Background color */
    public void setBackgroundColor(final Color color)
    {
        background_color = color;
        updateStyle();
    }

    /** @param color Text color */
    public void setTextColor(final Color color)
    {
        text_color = color;
        last_row_color = color.deriveColor(0, 0, 0, 0.5);
        updateStyle();
    }

    /** @param font Font */
    public void setFont(final Font font)
    {
        this.font = font;
        updateStyle();
    }

    /** Update style for colors and font */
    private void updateStyle()
    {
        table.setStyle("-fx-base: " + JFXUtil.webRGB(background_color) + "; " +
                       "-fx-text-background-color: " + JFXUtil.webRGB(text_color) + "; " +
                       "-fx-font-family: \"" + font.getFamily() + "\"; " +
                       "-fx-font-size: " + font.getSize()/12 + "em");
    }

    /** Set or update headers, i.e. define the columns
     *  @param headers Header labels
     */
    public void setHeaders(final List<String> headers)
    {
        // Remove all data
        table.setItems(NO_DATA);

        // Forcing this refresh avoided https://github.com/kasemir/org.csstudio.display.builder/issues/245,
        // an IndexOutOfBoundsException somewhere in CSS updates that uses the wrong table row count
        // Doesn't seem necessary any more?
        table.refresh();

        cell_colors = null;
        // Remove table columns, create new ones
        table.getColumns().clear();
        for (String header : headers)
            createTableColumn(-1, header);

        // Start over with no data, since table columns changed
        data.clear();
        if (editable)
            data.add(MAGIC_LAST_ROW);
        table.setItems(data);
    }

    /** Set (minimum and preferred) column width
     *
     *  @param column Column index, 0 .. <code>getHeaders().size()-1</code>
     *  @param width Width
     */
    public void setColumnWidth(final int column, final int width)
    {
        table.getColumns().get(column).setMinWidth(width);
        table.getColumns().get(column).setPrefWidth(width);
    }

    /** Allow editing a column
     *
     *  <p>By default, all columns of an 'active' table
     *  are editable, but this method can change it.
     *
     *  @param column Column index, 0 .. <code>getHeaders().size()-1</code>
     *  @param editable
     */
    public void setColumnEditable(final int column, final boolean editable)
    {
        table.getColumns().get(column).setEditable(editable);
    }

    /** Configure column options.
     *
     *  <p>If the list of options is empty,
     *  the cells in the column will offer a generic text field
     *  for entering values.
     *
     *  <p>If there are options, the column will use a drop-down
     *  list (combo box) for selecting one of the options.
     *
     *  @param column Column index, 0 .. <code>getHeaders().size()-1</code>
     *  @param options Options, may be <code>null</code>
     */
    public void setColumnOptions(final int column, final List<String> options)
    {
        @SuppressWarnings("unchecked")
        final TableColumn<List<String>, String> table_column = (TableColumn<List<String>, String>) table.getColumns().get(column);
        final Callback<TableColumn<List<String>, String>, TableCell<List<String>, String>> factory;

        if (options == null || options.isEmpty())
            factory = list -> new StringTextCell();
        else if (optionsAreBoolean(options))
            factory = list -> new BooleanCell();
        else
            factory = list -> new ComboCell(options);
        table_column.setCellFactory(factory);
    }

    /** Get options of a column
     *
     *  @param column Column index, 0 .. <code>getHeaders().size()-1</code>
     *  @return Options for boolean or combo-box cells, or empty list for plain string cell
     */
    public List<String> getColumnOptions(final int column)
    {
        final TableCell<List<String>, ?> cell = table.getColumns().get(column).getCellFactory().call(null);
        if (cell instanceof ComboCell)
            return ((ComboCell)cell).getItems();
        else if (cell instanceof BooleanCell)
            return BOOLEAN_OPTIONS;
        return Collections.emptyList();
    }

    /** Check if list of options suggest a boolean value
     *
     *  @param options Possible values of a column
     *  @return <code>true</code> if options are some variation of "true", "false"
     */
    private boolean optionsAreBoolean(final List<String> options)
    {
        return options.size() == 2   &&
               options.containsAll(BOOLEAN_OPTIONS);
    }

    /** @param index Column index, -1 to add to end
     *  @param header Header text
     */
    private void createTableColumn(final int index, final String header)
    {
        final TableColumn<List<String>, String> table_column = new TableColumn<>(header);
        table_column.setCellValueFactory(CELL_FACTORY);
        // Prevent column re-ordering
        // (handled via moveColumn which also re-orders the data)
        table_column.setReorderable(false);

        // By default, use text field editor. setColumnOptions() can replace
        table_column.setCellFactory(list -> new StringTextCell());
        table_column.setOnEditStart(event -> editing = true);
        table_column.setOnEditCommit(event ->
        {
            editing = false;
            final int col = event.getTablePosition().getColumn();
            List<String> row = event.getRowValue();
            if (row == MAGIC_LAST_ROW)
            {
                // Entered in last row? Create new row
                row = createEmptyRow();
                final List<List<String>> data = table.getItems();
                data.add(data.size()-1, row);
            }
            row.set(col, event.getNewValue());
            fireDataChanged();
        });
        table_column.setOnEditCancel(event -> editing = false);
        table_column.setSortable(false);

        if (index >= 0)
            table.getColumns().add(index, table_column);
        else
            table.getColumns().add(table_column);
    }

    /** @return Header labels */
    public List<String> getHeaders()
    {
        return table.getColumns().stream().map(col -> col.getText()).collect(Collectors.toList());
    }

    private List<String> createEmptyRow()
    {
        final int size = getColumnCount();
        final List<String> row = new ArrayList<>(size);
        for (int i=0; i<size; ++i)
            row.add("");
        return row;
    }

    private int getColumnCount()
    {
        return table.getColumns().size();
    }

    /** Set or update data
     *
     *  @param new_data Rows of data,
     *                  where each row must contain the same number
     *                  of elements as the column headers
     */
    public void setData(final List<List<String>> new_data)
    {
        final int columns = getColumnCount();
        cell_colors = null;
        data.clear();
        for (List<String> new_row : new_data)
        {
            final ArrayList<String> row;
            if (new_row instanceof ArrayList)
                row = (ArrayList<String>)new_row;
            else
                row = new ArrayList<>(new_row);
            if (row.size() < columns)
            {
                logger.log(Level.WARNING, "Table needs " + columns +
                           " columns but got row with just " + row.size());
                for (int i=row.size(); i<columns; ++i)
                    row.add("");
            }
            data.add(row);
        }

        if (editable)
            data.add(MAGIC_LAST_ROW);
        // Don't fire, since external source changed data, not user
        // fireDataChanged();
    }

    /** Get complete table content
     *  @return List of rows, where each row contains the list of cell strings
     */
    public List<List<String>> getData()
    {
        final List<List<String>> data = new ArrayList<>(table.getItems());
        while (data.size() > 0  &&  data.get(data.size()-1) == MAGIC_LAST_ROW)
            data.remove(data.size()-1);
        return data;
    }

    /** Get data of one table cell
     *  @param row Table row
     *  @param col Table column
     *  @return Value of that cell or "" for invalid row, column
     */
    public String getCell(final int row, final int col)
    {
        try
        {
            final List<String> row_data = table.getItems().get(row);
            if (row_data == MAGIC_LAST_ROW)
                return "";
            return row_data.get(col);
        }
        catch (IndexOutOfBoundsException ex)
        {
            return "";
        }
    }

    /** Set background color for specific cells
     *
     *  <p>Expects a list of rows,
     *  where each row contains a list of colors for each cell
     *  in that row.
     *  The list may be sparse, i.e. the list for a certain row
     *  may be <code>null</code>, or contain <code>null</code>
     *  instead of a color for a specific cell.
     *
     *  <p><b>Note:</b> The cell colors are used as provided,
     *  no deep copy is created!
     *  Caller needs to either provide a static or a thread-safe
     *  table of colors.
     *
     *  @param row Table row
     *  @param col Table column
     *  @param color Color of that cell, <code>null</code> for default
     */
    public void setCellColors(final List<List<Color>> colors)
    {
        cell_colors = colors;
        table.refresh();
    }

    /** Get background color for a specific cell
     *  @param row Table row
     *  @param col Table column
     *  @return Color of that cell, <code>null</code> for default
     */
    private Color getCellColor(final int row, final int col)
    {
        final List<List<Color>> colors = cell_colors;
        if (colors != null  &&  row < colors.size())
        {
            final List<Color> row_colors = colors.get(row);
            if (row_colors != null  &&  col < row_colors.size())
                return row_colors.get(col);
        }
        return null;
    }

    /** Set style of table cell to reflect optional background color
     * @param cell
     *  @param row Table row
     *  @param col Table column
     */
    private void setCellStyle(Cell<String> cell, final int row, final int col)
    {
        final Color color = getCellColor(row, col);
        if (color == null)
            cell.setStyle(null);
        else
        {   // Based on modena.css
            // .table-cell has no -fx-background-color to see overall background,
            // but .table-cell:selected uses this to get border with an inset color
            cell.setStyle("-fx-background-color: -fx-table-cell-border-color, " + JFXUtil.webRGB(color) +
                          ";-fx-background-insets: 0, 0 0 1 0;");
        }
    }

    /** Handle key pressed on the table
     *
     *  <p>Ignores keystrokes while editing a cell.
     *
     *  @param event Key pressed
     */
    private void handleKey(final KeyEvent event)
    {
        if (editing)
            return;
        switch (event.getCode())
        {
        case T:
            showToolbar(! isToolbarVisible());
            break;
        default:
        }
    }

    /** Add a row above the selected column,
     *  or on the very bottom if nothing selected
     */
    private void addRow()
    {
        cell_colors = null;
        int row = table.getSelectionModel().getSelectedIndex();
        final List<List<String>> data = table.getItems();
        final int len = data.size();
        if (row < 0  ||  row > len-1)
            row = len-1;
        data.add(row, createEmptyRow());
        fireDataChanged();
    }

    /** Move selected row up  */
    private void moveRowUp()
    {
        int row = table.getSelectionModel().getSelectedIndex();
        final int num = data.size() - 1;
        if (row < 0 || num < 1)
            return;
        moveRow(row, (row - 1 + num) % num);
    }

    /** Move selected row down  */
    private void moveRowDown()
    {
        int row = table.getSelectionModel().getSelectedIndex();
        final int num = data.size() - 1;
        if (row < 0 || num < 1)
            return;
        moveRow(row, (row + 1) % num);
    }

    /** Move a row up/down
     *  @param row Row to move
     *  @param target Desired location
     */
    private void moveRow(final int row, final int target)
    {
        cell_colors = null;
        final int column = getSelectedColumn();
        final List<String> line = data.remove(row);
        data.add(target, line);
        table.getSelectionModel().clearAndSelect(target, table.getColumns().get(column));
        fireDataChanged();
    }

    /** Delete currently selected row */
    private void deleteRow()
    {
        cell_colors = null;
        int row = table.getSelectionModel().getSelectedIndex();
        final List<List<String>> data = table.getItems();
        final int len = data.size();
        if (row < 0  ||  row >= len-1)
            return;
        data.remove(row);
        fireDataChanged();
    }

    /** Listener to table selection */
    private void selectionChanged(final Observable what)
    {
        final StringTableListener copy = listener;
        if (copy == null)
            return;

        @SuppressWarnings("rawtypes")
        final ObservableList<TablePosition> cells = table.getSelectionModel().getSelectedCells();
        int num = cells.size();
        // Don't select the magic last row
        if (num > 0  &&  data.get(cells.get(num-1).getRow()) == MAGIC_LAST_ROW)
            --num;
        final int[] rows = new int[num], cols = new int[num];
        for (int i=0; i<num; ++i)
        {
            rows[i] = cells.get(i).getRow();
            cols[i] = cells.get(i).getColumn();
        }
        copy.selectionChanged(this, rows, cols);
    }

    /** @return Currently selected table column or -1 */
    private int getSelectedColumn()
    {
        @SuppressWarnings("rawtypes")
        final ObservableList<TablePosition> cells = table.getSelectionModel().getSelectedCells();
        if (cells.isEmpty())
            return -1;
        return cells.get(0).getColumn();
    }

    /** Prompt for column name
     *  @param name Suggested name
     *  @return Name entered by user or <code>null</code>
     */
    private String getColumnName(final String name)
    {
        final TextInputDialog dialog = new TextInputDialog(name);
        // Position dialog near table
        final Bounds absolute = localToScreen(getBoundsInLocal());
        dialog.setX(absolute.getMinX() + 10);
        dialog.setY(absolute.getMinY() + 10);
        dialog.setTitle(Messages.RenameColumnTitle);
        dialog.setHeaderText(Messages.RenameColumnInfo);
        return dialog.showAndWait().orElse(null);
    }

    /** Renames the currently selected column */
    private void renameColumn()
    {
        final int column = getSelectedColumn();
        if (column < 0)
            return;
        final TableColumn<List<String>, ?> table_col = table.getColumns().get(column);
        final String name = getColumnName(table_col.getText());
        if (name == null)
            return;
        table_col.setText(name);
        fireTableChanged();
    }

    /** Add a column to the left of the selected column,
     *  or on the very right if nothing selected
     */
    private void addColumn()
    {
        int column = getSelectedColumn();
        final String name = getColumnName(Messages.DefaultNewColumnName);
        if (name == null)
            return;
        cell_colors = null;
        if (column < 0)
            column = table.getColumns().size();

        // Cannot update data and table concurrently, so detach data from table:
        table.setItems(NO_DATA);
        // Add new column
        createTableColumn(column, name);
        // Add empty col. to data
        for (List<String> row : data)
            if (row != MAGIC_LAST_ROW)
                row.add(column, "");
        // Show the updated data
        table.setItems(data);
        table.refresh();

        fireTableChanged();
    }

    /** Move selected column to the left */
    private void moveColumnLeft()
    {
        final int column = getSelectedColumn();
        final int num = table.getColumns().size();
        if (column < 0 || num < 1)
            return;
        moveColumn(column, (column - 1 + num) % num);
    }

    /** Move selected column to the right */
    private void moveColumnRight()
    {
        final int column = getSelectedColumn();
        final int num = table.getColumns().size();
        if (column < 0 || num < 1)
            return;
        moveColumn(column, (column + 1) % num);
    }

    /** Move a column left/right
     *  @param column Column to move
     *  @param target Desired location
     */
    private void moveColumn(final int column, final int target)
    {
        cell_colors = null;
        int row = table.getSelectionModel().getSelectedIndex();

        // Some table columns have special cell factories to
        // represent boolean column data as a checkbox etc.
        // In principle, need to update both the data and the table columns
        // concurrently because otherwise a checkbox cell would briefly try to
        // represent non-boolean data, resulting in a long stack trace.
        // Cannot update data and table concurrently, so detach data from table:
        table.setItems(NO_DATA);

        // Move table column
        final TableColumn<List<String>, ?> col = table.getColumns().remove(column);
        table.getColumns().add(target, col);

        // Move column in data
        for (List<String> data_row : data)
            if (data_row != MAGIC_LAST_ROW)
            {
                final String cell = data_row.remove(column);
                data_row.add(target, cell);
            }

        // Re-attach data to table
        table.setItems(data);

        // Select the moved cell
        table.getSelectionModel().clearAndSelect(row, table.getColumns().get(target));
        fireTableChanged();
    }

    /** Delete currently selected column */
    private void deleteColumn()
    {
        cell_colors = null;
        final int column = getSelectedColumn();
        if (column < 0)
            return;
        // Detach data from table
        table.setItems(NO_DATA);
        // Update table columns
        table.getColumns().remove(column);
        // Remove that column from data
        for (List<String> row : data)
            if (row != MAGIC_LAST_ROW  &&  column < row.size())
                row.remove(column);
        // Re-attach data to table
        table.setItems(data);
        fireTableChanged();
    }

    private void fireTableChanged()
    {
        final StringTableListener copy = listener;
        if (copy != null)
            copy.tableChanged(this);
    }

    private void fireDataChanged()
    {
        final StringTableListener copy = listener;
        if (copy != null)
            copy.dataChanged(this);
    }
}

