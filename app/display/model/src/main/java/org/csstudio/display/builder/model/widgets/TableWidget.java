/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newBooleanPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newRuntimeValue;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newStringPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBackgroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBorderAlarmSensitive;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propFont;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propForegroundColor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propName;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propPVName;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propWidth;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.runtimePropPVValue;
import static org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.propToolbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.RuntimeWidgetProperty;
import org.csstudio.display.builder.model.StructuredWidgetProperty;
import org.csstudio.display.builder.model.StructuredWidgetProperty.Descriptor;
import org.csstudio.display.builder.model.Version;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetCategory;
import org.csstudio.display.builder.model.WidgetConfigurator;
import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.NamedWidgetFonts;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.persist.WidgetFontService;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.epics.util.array.ListDouble;
import org.epics.util.array.ListNumber;
import org.epics.vtype.VTable;
import org.epics.vtype.VType;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Widget that displays a string table
 *
 *  <p>The 'value' can be either a VTable
 *  or a <code>List&lt;List&lt;String&gt;&gt;</code>.
 *  The latter includes 2-D string arrays written
 *  by Jython scripts.
 *
 *  <B>Note:</B> this class cannot inherit from {@link PVWidget}
 *  because of the peculiar usage of "runtime value".
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TableWidget extends VisibleWidget
{
    /** Widget descriptor */
    public static final WidgetDescriptor WIDGET_DESCRIPTOR =
        new WidgetDescriptor("table", WidgetCategory.MONITOR,
            "Table",
            "/icons/table.png",
            "A table",
            Arrays.asList("org.csstudio.opibuilder.widgets.table",
                          "org.csstudio.opibuilder.widgets.VTableDisplay"))
    {
        @Override
        public Widget createWidget()
        {
            return new TableWidget();
        }
    };

    /** Structure for column configuration */
    private static final WidgetPropertyDescriptor<Boolean> propEditable =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "editable", Messages.WidgetProperties_Editable);

    private static final WidgetPropertyDescriptor<String> columnOption =
            newStringPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "option", "Option");

    private static final ArrayWidgetProperty.Descriptor<WidgetProperty<String>> propOptions =
        new ArrayWidgetProperty.Descriptor<>(WidgetPropertyCategory.DISPLAY, "options", "Options",
                                             (widget, index) ->
                                             columnOption.createProperty(widget, "Option " + (index + 1)),
                                             /* minimum size */ 0);

    private static final StructuredWidgetProperty.Descriptor propColumn =
        new Descriptor(WidgetPropertyCategory.DISPLAY, "column", "Column");

    public static class ColumnProperty extends StructuredWidgetProperty
    {
        public ColumnProperty(final Widget widget, final String name)
        {
            super(propColumn, widget,
                  Arrays.asList(propName.createProperty(widget, name),
                                propWidth.createProperty(widget, 100),
                                propEditable.createProperty(widget, true),
                                propOptions.createProperty(widget, Collections.emptyList())));
        }

        public WidgetProperty<String> name()                          { return getElement(0); }
        public WidgetProperty<Integer> width()                        { return getElement(1); }
        public WidgetProperty<Boolean> editable()                     { return getElement(2); }
        public ArrayWidgetProperty<WidgetProperty<String>> options()  { final WidgetProperty<List<WidgetProperty<String>>> prop = getElement(3);
                                                                        return (ArrayWidgetProperty<WidgetProperty<String>>)prop;
                                                                      }
    };

    /** 'columns' array */
    public static final ArrayWidgetProperty.Descriptor<ColumnProperty> propColumns =
        new ArrayWidgetProperty.Descriptor<>(WidgetPropertyCategory.DISPLAY, "columns", "Columns",
                                             (widget, index) ->
                                             new ColumnProperty(widget, "Column " + (index + 1)));


    /** Like runtimePropValue, but not limited to VType */
    private static final WidgetPropertyDescriptor<Object> runtimeValueDescriptor =
        new WidgetPropertyDescriptor<>(WidgetPropertyCategory.RUNTIME, runtimePropPVValue.getName(), Messages.WidgetProperties_Value)
        {
            @Override
            public WidgetProperty<Object> createProperty(final Widget widget, final Object value)
            {
                return new RuntimeWidgetProperty<>(this, widget, value)
                {
                    @Override
                    public void setValueFromObject(final Object value) throws Exception
                    {
                        if (value instanceof VType)
                            setValue(value);
                        else if (value instanceof List)
                            setValue(value);
                        else if (value == null)
                            setValue(null);
                        else
                            throw new Exception("Need VType or List<List<String>, got " + value);
                    }
                };
            }
        };

    /** Optional cell colors */
    private static final WidgetPropertyDescriptor<List<List<WidgetColor>>> runtimeCellColorsDescriptor =
        new WidgetPropertyDescriptor<>(WidgetPropertyCategory.RUNTIME, "cell_colors", Messages.WidgetProperties_CellColors)
        {

            @Override
            public WidgetProperty<List<List<WidgetColor>>> createProperty(final Widget widget, final List<List<WidgetColor>> default_value)
            {
                return new RuntimeWidgetProperty<>(this, widget, default_value)
                {
                    @SuppressWarnings({ "rawtypes", "unchecked" })
                    @Override
                    public void setValueFromObject(final Object value) throws Exception
                    {
                        if (value instanceof List) // Not really checking for List<List<WidgetColor>>..
                            setValue((List)value);
                        else if (value == null)
                            setValue(null);
                        else
                            throw new Exception("Need List<List<WidgetColor>, got " + value);
                    }
                };
            }
        };


    /** PV for runtime info about selection */
    private static final WidgetPropertyDescriptor<String> propSelectionPV =
        newStringPropertyDescriptor(WidgetPropertyCategory.MISC, "selection_pv", Messages.WidgetProperties_SelectionPV);

    private static final WidgetPropertyDescriptor<Boolean> propRowSelectionMode =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.MISC, "row_selection_mode", "Select Rows");

    /** Runtime info about selection */
    private static final WidgetPropertyDescriptor<VType> runtimePropSelectionInfo =
        newRuntimeValue("selection", Messages.WidgetProperties_Selection);

    /** Property to set selection */
    private static final WidgetPropertyDescriptor<List<Integer>> runtimePropSetSelection =
        new WidgetPropertyDescriptor<>(WidgetPropertyCategory.RUNTIME, "set_selection", "Set Selection")
        {
            @Override
            public WidgetProperty<List<Integer>> createProperty(final Widget widget, final List<Integer> default_value)
            {
                return new RuntimeWidgetProperty<>(this, widget, default_value)
                {
                    @SuppressWarnings({ "rawtypes", "unchecked" })
                    @Override
                    public void setValueFromObject(final Object value) throws Exception
                    {
                        if (value instanceof List) // Not really checking for List<Integer>..
                            setValue((List)value);
                        else if (value == null)
                            setValue(null);
                        else
                            throw new Exception("Need List<Integer>, got " + value);
                    }
                };
            }
        };

    /** Configurator for legacy XML files */
    private static class CustomConfigurator extends WidgetConfigurator
    {
        public CustomConfigurator(final Version xml_version)
        {
            super(xml_version);
        }

        @Override
        public boolean configureFromXML(final ModelReader model_reader, final Widget widget,
                                        final Element xml) throws Exception
        {
            if (! super.configureFromXML(model_reader, widget, xml))
                return false;

            if (xml_version.getMajor() < 2)
                configureLegacyColumns((TableWidget) widget, xml);

            return true;
        }
    };

    /** Configure column information from legacy XML
     *  @param widget
     *  @param xml
     *  @throws Exception
     */
    private static void configureLegacyColumns(final TableWidget widget, final Element xml) throws Exception
    {
        // Legacy column information:
        // <column_headers>
        //   <row>  <col>Name</col>  <col>width</col>  <col>Edit: No?</col>  <col>TEXT|CHECKBOX|DROPDOWN</col>
        final Element el = XMLUtil.getChildElement(xml, "column_headers");
        if (el == null)
            return;
        int col = -1;
        final ArrayWidgetProperty<ColumnProperty> columns = widget.propColumns();
        for (Element row : XMLUtil.getChildElements(el, "row"))
        {
            ++col;
            while (columns.size() <= col)
                columns.addElement();
            final ColumnProperty column = columns.getElement(col);

            int idx = -1;
            for (Element item : XMLUtil.getChildElements(row, "col"))
            {
                switch (++idx)
                {
                case 0: column.name().setValue(XMLUtil.getString(item));
                        break;
                case 1: final String text = XMLUtil.getString(item);
                        if (text.length() > 0)
                            try
                            {
                                column.width().setValue(Integer.parseInt(text));
                            }
                            catch (NumberFormatException ex)
                            {
                                throw new Exception("Error in legacy table column width", ex);
                            }
                        break;
                case 2: if ("No".equalsIgnoreCase(XMLUtil.getString(item)))
                            column.editable().setValue(false);
                        break;
                case 3: // Ignore editor type
                        break;
                }
            }
        }
    }

    private volatile WidgetProperty<WidgetColor> background;
    private volatile WidgetProperty<WidgetColor> foreground;
    private volatile WidgetProperty<WidgetFont> font;
    private volatile WidgetProperty<Boolean> show_toolbar;
    private volatile ArrayWidgetProperty<ColumnProperty> columns;
    private volatile WidgetProperty<String> pv_name;
    private volatile WidgetProperty<Object> value;
    private volatile WidgetProperty<List<List<WidgetColor>>> cell_colors;
    private volatile WidgetProperty<Boolean> editable;
    private volatile WidgetProperty<Boolean> row_selection_mode;
    private volatile WidgetProperty<String> selection_pv;
    private volatile WidgetProperty<VType> selection;
    private volatile WidgetProperty<List<Integer>> set_selection;

    public TableWidget()
    {
        super(WIDGET_DESCRIPTOR.getType(), 500, 300);
    }

    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);
        properties.add(propBorderAlarmSensitive.createProperty(this, true));
        properties.add(font = propFont.createProperty(this, WidgetFontService.get(NamedWidgetFonts.DEFAULT)));
        properties.add(foreground = propForegroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.TEXT)));
        properties.add(background = propBackgroundColor.createProperty(this, WidgetColorService.getColor(NamedWidgetColors.BACKGROUND)));
        properties.add(show_toolbar = propToolbar.createProperty(this,false));
        properties.add(columns = propColumns.createProperty(this, Arrays.asList(  new ColumnProperty(this, "Column 1") )));
        properties.add(pv_name = propPVName.createProperty(this, ""));
        properties.add(value = runtimeValueDescriptor.createProperty(this, null));
        properties.add(cell_colors = runtimeCellColorsDescriptor.createProperty(this, null));
        properties.add(editable = propEditable.createProperty(this, true));
        properties.add(row_selection_mode = propRowSelectionMode.createProperty(this, false));
        properties.add(selection_pv = propSelectionPV.createProperty(this, ""));
        properties.add(selection = runtimePropSelectionInfo.createProperty(this, null));
        properties.add(set_selection = runtimePropSetSelection.createProperty(this, null));
    }

    /** Allow scripts to access "value" */
    @Override
    public WidgetProperty<?> getProperty(String name)
    {
        if ("value".equals(name))
            return value;
        return super.getProperty(name);
    }

    @Override
    public WidgetConfigurator getConfigurator(final Version persisted_version) throws Exception
    {
        return new CustomConfigurator(persisted_version);
    }

    /** @return 'background_color' property */
    public WidgetProperty<WidgetColor> propBackgroundColor()
    {
        return background;
    }

    /** @return 'foreground_color' property */
    public WidgetProperty<WidgetColor> propForegroundColor()
    {
        return foreground;
    }

    /** @return 'font' property */
    public WidgetProperty<WidgetFont> propFont()
    {
        return font;
    }

    /** @return 'show_toolbar' property */
    public WidgetProperty<Boolean> propToolbar()
    {
        return show_toolbar;
    }

    /** @return 'columns' property */
    public ArrayWidgetProperty<ColumnProperty> propColumns()
    {
        return columns;
    }

    /** @return Column headers */
    public List<String> getHeaders()
    {
        return columns.getValue().stream()
                                 .map(col -> col.name().getValue())
                                 .collect(Collectors.toList());
    }

    /** Define columns based on column names
     *
     *  <p>Clears the table, i.e. sets value to empty data.
     *
     *  @param headers Names of the columns
     */
    public void setHeaders(final List<String> headers)
    {
        setValue(Collections.emptyList());

        // Create new columns, using the provided names
        final List<ColumnProperty> old_colunms = columns.getValue();
        final List<ColumnProperty> new_columns = new ArrayList<>();
        for (int i=0; i<headers.size(); ++i)
        {
            final ColumnProperty col = new ColumnProperty(this, headers.get(i));
            if (i < old_colunms.size())
            {
                // Preserving existing column widths
                col.width().setValue(old_colunms.get(i).width().getValue());
                // .. and options
                col.options().setValue(old_colunms.get(i).options().getValue());
            }
            new_columns.add(col);
        }

        columns.setValue(new_columns);
    }

    /** Set column to be editable (default) or not
     *
     *  <p>Convenience routine for <code>propColumns()</code>
     *
     *  @param column Column index, must be 0 .. <code>propColumns().size()-1</code>
     *  @param editable Should column be editable?
     */
    public void setEditable(final int column, final boolean editable)
    {
        columns.getElement(column).editable().setValue(editable);
    }

    /** Set width of a column
     *
     *  <p>Convenience routine for <code>propColumns()</code>
     *
     *  @param column Column index, must be 0 .. <code>propColumns().size()-1</code>
     *  @param width Column width
     */
    public void setColumnWidth(final int column, final int width)
    {
        columns.getElement(column).width().setValue(width);
    }

    /** Get options for a column's values
     *
     *  <p>Convenience routine for <code>propColumns()</code>
     *
     *  @param column Column index, must be 0 .. <code>propColumns().size()-1</code>
     *  @return Options that combo editor will present for the cells in that column
     */
    public  List<String> getColumnOptions(final int column)
    {
        final List<WidgetProperty<String>> options = columns.getElement(column).options().getValue();
        if (options.isEmpty())
            return Collections.emptyList();
        final List<String> values = new ArrayList<>(options.size());
        for (WidgetProperty<String> option : options)
            values.add(option.getValue());
        return values;
    }

    /** Set options for a column's values
     *
     *  <p>Convenience routine for <code>propColumns()</code>
     *
     *  @param column Column index, must be 0 .. <code>propColumns().size()-1</code>
     *  @param options Options to present in combo editor for the cells in that column
     */
    public void setColumnOptions(final int column, final List<String> options)
    {
        final ArrayWidgetProperty<WidgetProperty<String>> options_prop = columns.getElement(column).options();
        final int num = options.size();
        while (options_prop.size() > num)
            options_prop.removeElement();
        while (options_prop.size() < num)
            options_prop.addElement();
        // Received list is supposed to contain strings,
        // but script might actually send PyString, or anything else.
        // Calling toString on the object will allow anything
        // to be passed without errors like
        // "org.python.core.PyString cannot be cast to java.lang.String"
        for (int i=0; i<num; ++i)
            options_prop.getElement(i).setValue(Objects.toString(options.get(i)));
    }

    /** @return 'pv_name' property */
    public WidgetProperty<String> propPVName()
    {
        return pv_name;
    }

    /** @return Runtime 'value' */
    public WidgetProperty<Object> runtimeValue()
    {
        return value;
    }

    /** Set value, i.e. content of cells in table
     *
     *  <p>Convenience routine for <code>runtimeValue()</code>.
     *  and converting the data to a 2D list of strings.
     *
     *  <p>Accepts either a {@link List} of rows,
     *  where each row is a {@link List} of column {@link String}s,
     *  or a {@link VTable}
     *
     *  @param data {@link List} of rows, or {@link VTable}
     */
    public void setValue(final Object data)
    {
        value.setValue(data);
    }

    /** Set the text of a specific cell
     *
     *  <p>Convenience routine for updating the value property
     *
     *  @param row Table row
     *  @param column Table column
     *  @param cell_text Text for that cell.
     */
    public void setCellValue(final int row, final int column, final String cell_text)
    {
        final int cols = columns.getValue().size();
        if (column >= cols)
            throw new IndexOutOfBoundsException("Invalid column index " + column + " for table with " + cols + " columns");

        // 'value' is a deep copy of the current value, safe to modify
        final List<List<String>> value = getValue();
        while (row >= value.size())
        {
            final List<String> cells = new ArrayList<>(cols);
            for (int i=0; i<cols; ++i)
                cells.add("");
            value.add(cells);
        }
        // Assert row with enough cells
        List<String> cells = value.get(row);
        while (column >= cells.size())
            cells.add("");
        cells.set(column, cell_text);

        setValue(value);
    }

    /** Fetch value, i.e. content of cells in table
     *
     *  <p>Convenience routine for <code>runtimeValue()</code>
     *  and converting the data to a 2D list of strings.
     *
     *  @return {@link List} of rows, where each row is a {@link List} of column {@link String}s
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<List<String>> getValue()
    {
        final Object the_value = value.getValue();
        if (the_value instanceof List)
        {   // Create deep copy:
            // * Avoid changes to widget's value
            // * If a script fetches the value, modifies it, then sets that as a new value,
            //   passing out a deep copy asserts that the 'new' value is indeed recognized
            //   as new, instead of referencing the old value and thus seen as no-change OP.
            final List<List<String>> deep_copy = new ArrayList<>(((List)the_value).size());
            for (List<String> row : (List<List<String>>)the_value)
            {
                final List<String> row_copy = new ArrayList<>(row);
                deep_copy.add(row_copy);
            }
            return deep_copy;
        }
        else if (the_value instanceof VTable)
        {
            final VTable table = (VTable) the_value;
            final int rows = table.getRowCount();
            final int cols = table.getColumnCount();
            // Extract 2D string matrix for data
            final List<List<String>> data = new ArrayList<>(rows);
            for (int r=0; r<rows; ++r)
            {
                final List<String> row = new ArrayList<>(cols);
                for (int c=0; c<cols; ++c)
                {
                    final Object col_data = table.getColumnData(c);
                    if (col_data instanceof List)
                        row.add( Objects.toString(((List)col_data).get(r)) );
                    else if (col_data instanceof ListDouble)
                        row.add( Double.toString(((ListDouble)col_data).getDouble(r)) );
                    else if (col_data instanceof ListNumber)
                        row.add( Long.toString(((ListNumber)col_data).getLong(r)) );
                    else
                        row.add( Objects.toString(col_data) );
                }
                data.add(row);
            }
             return data;
        }
        else
            return Arrays.asList(Arrays.asList(Objects.toString(the_value)));
    }

    /** @return Runtime 'cell_colors' */
    public WidgetProperty<List<List<WidgetColor>>> runtimeCellColors()
    {
        return cell_colors;
    }

    /** Set the color of a specific cell
     *
     *  <p>Convenience routine for updating the 'cell_colors' property
     *
     *  @param row Table row
     *  @param column Table column
     *  @param color Color to use for that cell. <code>null</code> for default.
     */
    public void setCellColor(final int row, final int column, final WidgetColor color)
    {
        // Assert that there is a _new_ list instance,
        // because otherwise we modify the current value,
        // and the property's equality check would then suppress the update.
        List<List<WidgetColor>> colors = cell_colors.getValue();
        // Thread-safe CopyOnWriteArrayList?
        // Unnecessary when using setCellColor because all access to cell_colors' value
        // is read-only, replacing it with a new one.
        if (colors == null)
            colors = new ArrayList<>();
        else
            colors = new ArrayList<>(colors);
        // Assert table with enough (sparse) rows
        while (row >= colors.size())
            colors.add(null);
        // Assert row with enough (sparse) cell colors
        List<WidgetColor> row_colors = colors.get(row);
        if (row_colors == null)
            colors.set(row, row_colors = new ArrayList<>());
        else // Create copy to assert truly new value for equality check
            colors.set(row, row_colors = new ArrayList<>(row_colors));
        while (column >= row_colors.size())
            row_colors.add(null);
        // Set cell color
        row_colors.set(column, color);

//        System.out.println("Setting " + row + ", " + column + " to " + str(color));
//        dumpColors(colors);
        cell_colors.setValue(colors);
    }

//    private void dumpColors(List<List<WidgetColor>> colors)
//    {
//        if (colors == null)
//        {
//            System.out.println("None");
//            return;
//        }
//        for (List<WidgetColor> row : colors)
//            if (row == null)
//                System.out.println("-- null row --");
//            else
//            {
//                for (WidgetColor col : row)
//                    System.out.print(str(col));
//                System.out.println();
//            }
//    }
//
//    private String str(WidgetColor col)
//    {
//        if (col == null)
//            return " null        ";
//        return String.format("%3d,%3d,%3d  ",  col.getRed(), col.getGreen(), col.getBlue());
//    }

    /** Select specific cells
     *
     *  <p>Selects based on a flattened list of (row, col) pairs.
     *  Even in "row selection mode" where completed rows are
     *  selected instead of individual cells, the list must contain
     *  (row, col) data, and col may always be 0.
     *
     *  @param rows_cols List of row, col, row, col, ..
     */
    public void setSelection(final List<Integer> rows_cols)
    {
        // If there's no change, force update because
        // current selection of table on screen could be anything,
        // and script wants to re-set a previously requested selection
        if (Objects.equals(rows_cols, set_selection.getValue()))
            set_selection.setValue(null);
        set_selection.setValue(rows_cols);
    }

    /** @return 'editable' property */
    public WidgetProperty<Boolean> propEditable()
    {
        return editable;
    }

    /** @return Misc. 'row_selection_mode' */
    public WidgetProperty<Boolean> propRowSelectionMode()
    {
        return row_selection_mode;
    }

    /** @return Misc. 'selection_pv' */
    public WidgetProperty<String> propSelectionPV()
    {
        return selection_pv;
    }

    /** @return Runtime 'selection' property */
    public WidgetProperty<VType> runtimePropSelection()
    {
        return selection;
    }

    /** @return Runtime 'set_selection' property */
    public WidgetProperty<List<Integer>> runtimePropSetSelection()
    {
        return set_selection;
    }
}
