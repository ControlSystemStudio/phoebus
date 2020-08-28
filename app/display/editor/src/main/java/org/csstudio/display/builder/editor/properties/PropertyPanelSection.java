/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.properties;

import static org.csstudio.display.builder.editor.Plugin.logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.editor.undo.SetMacroizedWidgetPropertyAction;
import org.csstudio.display.builder.editor.util.WidgetIcons;
import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.MacroizedWidgetProperty;
import org.csstudio.display.builder.model.ModelPlugin;
import org.csstudio.display.builder.model.StructuredWidgetProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetFactory;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.persist.WidgetClassesService;
import org.csstudio.display.builder.model.properties.ActionsWidgetProperty;
import org.csstudio.display.builder.model.properties.BooleanWidgetProperty;
import org.csstudio.display.builder.model.properties.ColorMapWidgetProperty;
import org.csstudio.display.builder.model.properties.ColorWidgetProperty;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.EnumWidgetProperty;
import org.csstudio.display.builder.model.properties.FilenameWidgetProperty;
import org.csstudio.display.builder.model.properties.FontWidgetProperty;
import org.csstudio.display.builder.model.properties.MacrosWidgetProperty;
import org.csstudio.display.builder.model.properties.PVNameWidgetProperty;
import org.csstudio.display.builder.model.properties.PointsWidgetProperty;
import org.csstudio.display.builder.model.properties.RulesWidgetProperty;
import org.csstudio.display.builder.model.properties.ScriptsWidgetProperty;
import org.csstudio.display.builder.model.properties.WidgetClassProperty;
import org.csstudio.display.builder.representation.javafx.FilenameSupport;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.ui.autocomplete.PVAutocompleteMenu;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.MultiLineInputDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;

/** Section of Property panel
 *
 *  <p>A grid with 3 columns:
 *  <ol>
 *  <li>Name of property
 *  <li>Class Indicator. May be empty or show the "C" for class-defined property.
 *      In class-editor mode it turns into a checkbox.
 *  <li>Editor for Value of Property.
 *      The editor for the value of the property may be a single text field,
 *      or a button or even a HBox with more elements.
 *  </ol>
 *
 *  <p>The {@link PropertyPanel} currently uses just one large {@link PropertyPanelSection}.
 *
 *  <p>The elements of a structure or elements of an array could be (and had in the past been)
 *  placed in their own sub-{@link PropertyPanelSection}s.
 *  That makes it easier to add/remove array elements by just updating the affected sub-section,
 *  but is would then be much harder to get all property name and editor fields to align
 *  through subsections.
 *
 *  <p>With one large grid, adding and removing array elements requires either shifting
 *  the following properties into different rows (harder to do),
 *  or to re-create the complete section (currently done).
 *  See {@link ArraySizePropertyBinding} for details.
 *
 *  @author Kay Kasemir
 *  @author Claudio Rosati
 */
@SuppressWarnings("nls")
public class PropertyPanelSection extends GridPane
{
    private static final Tooltip use_class_tooltip = new Tooltip(Messages.UseWidgetClass_TT);
    private static final Tooltip using_class_tooltip = new Tooltip(Messages.UsingWidgetClass_TT);

    private boolean class_mode = false;

    private volatile boolean has_focus = false;

    private final List<WidgetPropertyBinding<?,?>> bindings = new ArrayList<>();
    private int next_row = -1;

    //  Instance initializer.
    {

        getColumnConstraints().add(new ColumnConstraints( 6));                                                                          //  column 0 is 3 pixels wide
        getColumnConstraints().add(new ColumnConstraints( 6));                                                                          //  column 1 is 3 pixels wide
        getColumnConstraints().add(new ColumnConstraints(32, USE_COMPUTED_SIZE, Integer.MAX_VALUE, Priority.ALWAYS, HPos.LEFT, true));  //  column 2
        getColumnConstraints().add(new ColumnConstraints( 0, USE_COMPUTED_SIZE, Integer.MAX_VALUE));                                    //  column 3 is 3 pixels wide
        getColumnConstraints().add(new ColumnConstraints(32, USE_COMPUTED_SIZE, Integer.MAX_VALUE, Priority.ALWAYS, HPos.LEFT, true));  //  column 4
        getColumnConstraints().add(new ColumnConstraints( 6));                                                                          //  column 5 is 3 pixels wide
        getColumnConstraints().add(new ColumnConstraints( 6));                                                                          //  column 6 is 3 pixels wide

        getColumnConstraints().get(2).setPercentWidth(40);

    }

    public void setClassMode(final boolean class_mode)
    {
        this.class_mode = class_mode;
    }

    /**
     *  @return Whether one of the property editors has focus
     */
    public boolean hasFocus()
    {
        return has_focus;
    }

    void fill(final UndoableActionManager undo,
              final Collection<WidgetProperty<?>> properties,
              final List<Widget> other)
    {
        // Add UI items for each property
        WidgetPropertyCategory category = null;
        for (final WidgetProperty<?> property : properties)
        {
            // Skip runtime properties
            if (property.getCategory() == WidgetPropertyCategory.RUNTIME)
                continue;

            // 'class' is not used for the class definition itself,
            // it's only shown for displays where classes are then applied
            if (property instanceof WidgetClassProperty  &&  class_mode)
                continue;

            // Start of new category that needs to be shown?
            if (property.getCategory() != category)
            {
                category = property.getCategory();

                final Label header = new Label(category.getDescription());
                header.getStyleClass().add("property_category");
                header.setMaxWidth(Double.MAX_VALUE);
                add(header, 0, getNextGridRow(), 7, 1);

                final Separator separator = new Separator();
                separator.getStyleClass().add("property_separator");
                add(separator, 0, getNextGridRow(), 7, 1);
            }

            createPropertyUI(undo, property, other, 0, 0);
        }
    }

    /** @return Next row in grid layout, i.e. row that is not populated */
    private int getNextGridRow()
    {
        next_row++;
        return next_row;
    }

    /** Some 'simple' properties are handled
     *  in static method to allow use in the
     *  RulesDialog
     *  @param undo
     *  @param bindings
     *  @param property
     *  @param other
     *  @return
     */
    public static Node bindSimplePropertyField (
            final UndoableActionManager undo,
            final List<WidgetPropertyBinding<?,?>> bindings,
            final WidgetProperty<?> property,
            final List<Widget> other)
    {
        final Widget widget = property.getWidget();
        Node field = null;

        if (property.isReadonly())
        {
            //  If "Type", use a label with an icon.
            if (property.getName().equals(CommonWidgetProperties.propType.getName()))
            {
                if (widget instanceof DisplayModel)
                {   // DisplayModel is not registered as a Widget that users can add
                    field = new Label(Messages.Display, ImageCache.getImageView(ModelPlugin.class, "/icons/display.png"));
                }
                else
                {
                    final String type = widget.getType();
                    try
                    {
                        final ImageView icon = new ImageView(WidgetIcons.getIcon(type));
                        final String name = WidgetFactory.getInstance().getWidgetDescriptor(type).getName();
                        field = new Label(name, icon);
                    }
                    catch (Exception ex)
                    {
                        // Even 'unknown' widgets should have an icon,
                        // but fall back to just showing the type name
                        field = new Label(String.valueOf(property.getValue()));
                    }
                }
            }
            else
            {
                final TextField text = new TextField();
                text.setText(String.valueOf(property.getValue()));
                text.setDisable(true);
                field = text;
            }
        }
        else if (property instanceof ColorWidgetProperty)
        {
            final ColorWidgetProperty color_prop = (ColorWidgetProperty) property;
            final WidgetColorPropertyField color_field = new WidgetColorPropertyField();
            final WidgetColorPropertyBinding binding = new WidgetColorPropertyBinding(undo, color_field, color_prop, other);
            bindings.add(binding);
            binding.bind();
            field = color_field;
        }
        else if (property instanceof FontWidgetProperty)
        {
            final FontWidgetProperty font_prop = (FontWidgetProperty) property;
            final Button font_field = new Button();
            font_field.setMnemonicParsing(false);
            font_field.setMaxWidth(Double.MAX_VALUE);
            final WidgetFontPropertyBinding binding = new WidgetFontPropertyBinding(undo, font_field, font_prop, other);
            bindings.add(binding);
            binding.bind();
            field = font_field;
        }
        else if (property instanceof EnumWidgetProperty<?>)
        {
            final EnumWidgetProperty<?> enum_prop = (EnumWidgetProperty<?>) property;
            final ComboBox<String> combo = new ComboBox<>();
            combo.setPromptText(property.getDefaultValue().toString());
            combo.getItems().addAll(enum_prop.getLabels());
            combo.setMaxWidth(Double.MAX_VALUE);
            combo.setMaxHeight(Double.MAX_VALUE);

            final ToggleButton macroButton = new ToggleButton("", ImageCache.getImageView(DisplayEditor.class, "/icons/macro-edit.png"));
            macroButton.getStyleClass().add("macro_button");
            macroButton.setTooltip(new Tooltip(Messages.MacroEditButton));
            BorderPane.setMargin(macroButton, new Insets(0, 0, 0, 3));
            BorderPane.setAlignment(macroButton, Pos.CENTER);

            final EnumWidgetPropertyBinding binding = new EnumWidgetPropertyBinding(undo, combo, enum_prop, other);
            bindings.add(binding);
            binding.bind();

            final EventHandler<ActionEvent> macro_handler = event ->
            {
                final boolean use_macro = macroButton.isSelected() ||
                                          MacroHandler.containsMacros(enum_prop.getSpecification());
                combo.setEditable(use_macro);
                // Combo's text field has been set to the current value
                // while the combo was non-editable.
                // With Java 8, it that can be ignored, so set it again
                // now that the combo has become editable.
                if (use_macro  &&  combo.getEditor().getText().isEmpty())
                    binding.restore();
            };
            macroButton.setOnAction(macro_handler);
            macroButton.setSelected(MacroHandler.containsMacros(enum_prop.getSpecification()));
            macro_handler.handle(null);

            field = new BorderPane(combo, null, macroButton, null, null);
            // When used in RulesDialog, field can get focus.
            // In that case, forward focus to combo
            field.focusedProperty().addListener((ob, o, focused) ->
            {
                if (focused)
                {
                    combo.requestFocus();
                    if (combo.isEditable())
                        combo.getEditor().selectAll();
                }
            });
        }
        else if (property instanceof BooleanWidgetProperty)
        {
            final BooleanWidgetProperty bool_prop = (BooleanWidgetProperty) property;
            final ComboBox<String> combo = new ComboBox<>();
            combo.setPromptText(property.getDefaultValue().toString());
            combo.getItems().addAll("true", "false");
            combo.setMaxWidth(Double.MAX_VALUE);
            combo.setMaxHeight(Double.MAX_VALUE);
            combo.setEditable(true);

            // BooleanWidgetPropertyBinding makes either check or combo visible
            // for plain boolean vs. macro-based value
            final CheckBox check = new CheckBox();
            StackPane.setAlignment(check, Pos.CENTER_LEFT);
            final ToggleButton macroButton = new ToggleButton("", ImageCache.getImageView(DisplayEditor.class, "/icons/macro-edit.png"));
            macroButton.getStyleClass().add("macro_button");
            macroButton.setTooltip(new Tooltip(Messages.MacroEditButton));
            BorderPane.setMargin(macroButton, new Insets(0, 0, 0, 3));
            BorderPane.setAlignment(macroButton, Pos.CENTER);

            final BooleanWidgetPropertyBinding binding = new BooleanWidgetPropertyBinding(undo, check, combo, macroButton, bool_prop, other);
            bindings.add(binding);
            binding.bind();

            field = new BorderPane(new StackPane(combo, check), null, macroButton, null, null);
            // For RulesDialog, see above
            field.focusedProperty().addListener((ob, o, focused) ->
            {
                if (focused)
                {
                    if (combo.isVisible())
                    {
                        combo.requestFocus();
                        combo.getEditor().selectAll();
                    }
                    else if (check.isVisible())
                        check.requestFocus();
                }
            });
        }
        else if (property instanceof ColorMapWidgetProperty)
        {
            final ColorMapWidgetProperty colormap_prop = (ColorMapWidgetProperty) property;
            final Button map_button = new Button();
            map_button.setMnemonicParsing(false);
            map_button.setMaxWidth(Double.MAX_VALUE);
            final ColorMapPropertyBinding binding = new ColorMapPropertyBinding(undo, map_button, colormap_prop, other);
            bindings.add(binding);
            binding.bind();
            field = map_button;
        }
        else if (property instanceof WidgetClassProperty)
        {
            final WidgetClassProperty widget_class_prop = (WidgetClassProperty) property;
            final ComboBox<String> combo = new ComboBox<>();
            combo.setPromptText(property.getDefaultValue().toString());
            combo.setEditable(true);
            // List classes of this widget
            final String type = widget.getType();
            final Collection<String> classes = WidgetClassesService.getWidgetClasses().getWidgetClasses(type);
            combo.getItems().addAll(classes);
            combo.setMaxWidth(Double.MAX_VALUE);
            final WidgetClassBinding binding = new WidgetClassBinding(undo, combo, widget_class_prop, other);
            bindings.add(binding);
            binding.bind();
            field = combo;
        }
        else if (property instanceof FilenameWidgetProperty)
        {
            final FilenameWidgetProperty file_prop = (FilenameWidgetProperty)property;
            final TextField text = new TextField();
            text.setPromptText(file_prop.getDefaultValue().toString());
            text.setMaxWidth(Double.MAX_VALUE);
            final Button select_file = new Button("...");
            select_file.setOnAction(event ->
            {
                try
                {
                    final String filename = FilenameSupport.promptForRelativePath(widget, file_prop.getValue());
                    if (filename != null)
                        undo.execute(new SetMacroizedWidgetPropertyAction(file_prop, filename));
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot prompt for " + file_prop, ex);
                }
            });
            final MacroizedWidgetPropertyBinding binding = new MacroizedWidgetPropertyBinding(undo, text, file_prop, other);
            bindings.add(binding);
            binding.bind();
            field = new HBox(text, select_file);
            HBox.setHgrow(text, Priority.ALWAYS);
            // For RulesDialog, see above
            field.focusedProperty().addListener((ob, o, focused) ->
            {
                if (focused)
                    text.requestFocus();
            });
        }
        else if (property instanceof PVNameWidgetProperty)
        {
            final PVNameWidgetProperty pv_prop = (PVNameWidgetProperty)property;
            final TextField text = new TextField();
            text.setPromptText(pv_prop.getDefaultValue().toString());
            final MacroizedWidgetPropertyBinding binding = new MacroizedWidgetPropertyBinding(undo, text, pv_prop, other)
            {
                @Override
                public void bind()
                {
                    super.bind();
                    PVAutocompleteMenu.INSTANCE.attachField(text);
                }
            };
            bindings.add(binding);
            binding.bind();

            // Allow editing long PV names, including loc://text("Log text with newlines"),
            // in dialog
            final Button open_editor = new Button("...");
            open_editor.setOnAction(event ->
            {
                final MultiLineInputDialog dialog = new MultiLineInputDialog(open_editor, pv_prop.getSpecification());
                final Optional<String> result = dialog.showAndWait();
                if (!result.isPresent())
                    return;
                undo.execute(new SetMacroizedWidgetPropertyAction(pv_prop, result.get()));
                for (Widget w : other)
                {
                    final MacroizedWidgetProperty<?> other_prop = (MacroizedWidgetProperty<?>) w.getProperty(pv_prop.getName());
                    undo.execute(new SetMacroizedWidgetPropertyAction(other_prop, result.get()));
                }
            });

            field = new HBox(text, open_editor);
            HBox.setHgrow(text, Priority.ALWAYS);
            // For RulesDialog, see above
            field.focusedProperty().addListener((ob, o, focused) ->
            {
                if (focused)
                    text.requestFocus();
            });
        }
        else if (property instanceof MacroizedWidgetProperty)
        {   // MacroizedWidgetProperty needs to be checked _after_ subclasses like PVNameWidgetProperty, FilenameWidgetProperty
            final MacroizedWidgetProperty<?> macro_prop = (MacroizedWidgetProperty<?>)property;
            final TextField text = new TextField();
            text.setPromptText(macro_prop.getDefaultValue().toString());
            final MacroizedWidgetPropertyBinding binding = new MacroizedWidgetPropertyBinding(undo, text, macro_prop, other);
            bindings.add(binding);
            binding.bind();
            if (CommonWidgetProperties.propText.getName().equals(property.getName())  ||
                CommonWidgetProperties.propTooltip.getName().equals(property.getName()))
            {   // Allow editing multi-line text in dialog
                final Button open_editor = new Button("...");
                open_editor.setOnAction(event ->
                {
                    final MultiLineInputDialog dialog = new MultiLineInputDialog(open_editor, macro_prop.getSpecification());
                    DialogHelper.positionDialog(dialog, open_editor, -600, 0);
                    final Optional<String> result = dialog.showAndWait();
                    if (!result.isPresent())
                        return;
                    undo.execute(new SetMacroizedWidgetPropertyAction(macro_prop, result.get()));
                    for (Widget w : other)
                    {
                        final MacroizedWidgetProperty<?> other_prop = (MacroizedWidgetProperty<?>) w.getProperty(macro_prop.getName());
                        undo.execute(new SetMacroizedWidgetPropertyAction(other_prop, result.get()));
                    }
                });
                field = new HBox(text, open_editor);
                HBox.setHgrow(text, Priority.ALWAYS);
                // For RulesDialog, see above
                field.focusedProperty().addListener((ob, o, focused) ->
                {
                    if (focused)
                        text.requestFocus();
                });
            }
            else
                field = text;
        }
        else if (property instanceof PointsWidgetProperty)
        {
            final PointsWidgetProperty points_prop = (PointsWidgetProperty) property;
            final Button points_field = new Button();
            points_field.setMaxWidth(Double.MAX_VALUE);
            final PointsPropertyBinding binding = new PointsPropertyBinding(undo, points_field, points_prop, other);
            bindings.add(binding);
            binding.bind();
            field = points_field;
        }
        return field;
    }

    /** Add UI items for displaying or editing property
     *  @param property Property (on primary widget)
     *  @param other Zero or more additional widgets that have same type of property
     *  @param structureIndex Index of the array structure (element) being added. It is meaningful
     *                        only for properties instance of {@link StructuredWidgetProperty}.
     */
    private void createPropertyUI(
        final UndoableActionManager undo,
        final WidgetProperty<?> property,
        final List<Widget> other,
        final int structureIndex,
        final int indentationLevel
    ) {
        // Skip runtime properties
        if (property.getCategory() == WidgetPropertyCategory.RUNTIME)
            return;

        final Label label = new Label(property.getDescription());
        label.setMaxWidth(Double.MAX_VALUE);
        final String tooltip = property.getDescription() + " (" + property.getPath() + ")";
        label.setTooltip(new Tooltip(tooltip));
//        setGridLinesVisible(true); // For debugging the layout

        Node field = bindSimplePropertyField(undo, bindings, property, other);
        if (field != null)
        {
            //do nothing
        }
        else if (property instanceof MacrosWidgetProperty)
        {
            final MacrosWidgetProperty macros_prop = (MacrosWidgetProperty) property;
            final Button macros_field = new Button();
            macros_field.setMnemonicParsing(false);
            macros_field.setMaxWidth(Double.MAX_VALUE);
            final MacrosPropertyBinding binding = new MacrosPropertyBinding(undo, macros_field, macros_prop, other);
            bindings.add(binding);
            binding.bind();
            field = macros_field;
        }
        else if (property instanceof ActionsWidgetProperty)
        {
            final ActionsWidgetProperty actions_prop = (ActionsWidgetProperty) property;
            final Button actions_field = new Button();
            actions_field.setMnemonicParsing(false);
            actions_field.setMaxWidth(Double.MAX_VALUE);
            final ActionsPropertyBinding binding = new ActionsPropertyBinding(undo, actions_field, actions_prop, other);
            bindings.add(binding);
            binding.bind();
            field = actions_field;
        }
        else if (property instanceof ScriptsWidgetProperty)
        {
            final ScriptsWidgetProperty scripts_prop = (ScriptsWidgetProperty) property;
            final Button scripts_field = new Button();
            scripts_field.setMnemonicParsing(false);
            scripts_field.setMaxWidth(Double.MAX_VALUE);
            final ScriptsPropertyBinding binding = new ScriptsPropertyBinding(undo, scripts_field, scripts_prop, other);
            bindings.add(binding);
            binding.bind();
            field = scripts_field;
        }
        else if (property instanceof RulesWidgetProperty)
        {
            final RulesWidgetProperty rules_prop = (RulesWidgetProperty) property;
            final Button rules_field = new Button();
            rules_field.setMaxWidth(Double.MAX_VALUE);
            final RulesPropertyBinding binding = new RulesPropertyBinding(undo, rules_field, rules_prop, other);
            bindings.add(binding);
            binding.bind();
            field = rules_field;
        }
        else if (property instanceof StructuredWidgetProperty)
        {   // Don't allow editing structures and their elements in class mode
            if (class_mode)
                return;
            final StructuredWidgetProperty struct = (StructuredWidgetProperty) property;
            final Label header = new Label(struct.getDescription() + ( structureIndex > 0 ? " " + String.valueOf(1 + structureIndex) : ""));
            header.getStyleClass().add("structure_property_name");
            header.setMaxWidth(Double.MAX_VALUE);

            final int row = getNextGridRow();

            fillIndent(indentationLevel, row);
            add(header, 0 + indentationLevel, row, 7 - 2 * indentationLevel, 1);

            final Separator separator = new Separator();
            separator.getStyleClass().add("property_separator");
            add(separator, 0 + indentationLevel, getNextGridRow(), 7 - 2 * indentationLevel, 1);

            for (WidgetProperty<?> elem : struct.getValue())
                this.createPropertyUI(undo, elem, other, -1, indentationLevel);
            return;
        }
        else if (property instanceof ArrayWidgetProperty)
        {   // Don't allow editing arrays and their elements in class mode
            if (class_mode)
                return;
            @SuppressWarnings("unchecked")
            final ArrayWidgetProperty<WidgetProperty<?>> array = (ArrayWidgetProperty<WidgetProperty<?>>) property;

            // UI for changing array size
            final Spinner<Integer> spinner = new Spinner<>(array.getMinimumSize(), 100, 0);
            final ArraySizePropertyBinding count_binding = new ArraySizePropertyBinding(this, undo, spinner, array, other);
            bindings.add(count_binding);
            count_binding.bind();

            // set size of array
            int row = getNextGridRow();
            label.getStyleClass().add("array_property_name");
            label.setMaxWidth(Double.MAX_VALUE);
            label.setMaxHeight(Double.MAX_VALUE);
            spinner.getStyleClass().add("array_property_value");
            spinner.setMaxWidth(Double.MAX_VALUE);

            fillHeaderIndent(indentationLevel, row);
            add(label, indentationLevel, row, 4 - indentationLevel, 1);
            add(spinner, 4, row, 2 - indentationLevel, 1);

            Separator separator = new Separator();

            separator.getStyleClass().add("property_separator");
            add(separator, 1 + indentationLevel, getNextGridRow(), 5 - 2 * indentationLevel, 1);

            // array elements
            final List<WidgetProperty<?>> wpeList = array.getValue();
            for (int i = 0; i < wpeList.size(); i++)
            {
                final WidgetProperty<?> elem = wpeList.get(i);
                createPropertyUI(undo, elem, other, i, indentationLevel + 1);
            }

            // mark end of array
            row = getNextGridRow();
            final Label endlabel = new Label();
            endlabel.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(endlabel, Priority.ALWAYS);
            endlabel.getStyleClass().add("array_property_end");
            fillIndent(indentationLevel, row);
            add(endlabel,0 + indentationLevel, row, 7 - 2 * indentationLevel, 1);

            separator = new Separator();

            separator.getStyleClass().add("property_separator");
            add(separator, 0 + indentationLevel, getNextGridRow(), 7 - 2 * indentationLevel, 1);

            return;
        }
        // As new property types are added, they might need to be handled:
        // else if (property instanceof SomeNewWidgetProperty) { ... }
        else
        {   // Fallback for unknown property: read-only
            final TextField text = new TextField();
            text.setText(String.valueOf(property.getValue()));
            text.setEditable(false);
            field = text;
        }

        // Display Label, Class indicator/checkbox, field
        final int row = getNextGridRow();

        label.getStyleClass().add("property_name");
        field.getStyleClass().add("property_value");

        // Update has_focus
        final Node tmpfield;
        if (field instanceof HBox)
            tmpfield = ((HBox)field).getChildren().get(0);
        else
            tmpfield = field;

        tmpfield.focusedProperty().addListener((ob, o, focused) ->
        {
            has_focus = focused;
        });

        // Allow label to shrink (can use tooltip to see),
        // but show the value
        // GridPane.setHgrow(label, Priority.ALWAYS);
        GridPane.setHgrow(field, Priority.ALWAYS);
        fillIndent(indentationLevel, row);
        add(label, indentationLevel, row, 3 - indentationLevel, 1);

        final Widget widget = property.getWidget();
        if (! (property == widget.getProperty("type")  ||
               property == widget.getProperty("name")))
        {
            if (class_mode)
            {   // Class definition mode:
                // Check box for 'use_class'
                final CheckBox check = new CheckBox();
                check.setPadding(new Insets(0, 5, 0, 0));
                check.setTooltip(use_class_tooltip);
                final WidgetPropertyBinding<?,?> binding = new UseWidgetClassBinding(undo, check, field, property, other);
                bindings.add(binding);
                binding.bind();
                add(check, 3, row);
            }
            else
            {   // Display file mode:
                // Show if property is set by the class, not editable.
                final Label indicator = new Label();
                indicator.setPadding(new Insets(0, 5, 0, 0));
                indicator.setTooltip(using_class_tooltip);
                final WidgetPropertyBinding<?,?> binding = new ShowWidgetClassBinding(field, property, indicator);
                bindings.add(binding);
                binding.bind();
                add(indicator, 3, row);
            }
        }
        add(field, 4, row, 3 - indentationLevel, 1);

        final Separator separator = new Separator();
        separator.getStyleClass().add("property_separator");
        add(separator, 0 + indentationLevel, getNextGridRow(), 7 - 2 * indentationLevel, 1);
    }

    private void fillHeaderIndent(final int indentationLevel, final int row)
    {
        if (indentationLevel >= 0)
        {
            if (indentationLevel > 0)
            {
                final Label indent = new Label();
                indent.getStyleClass().add("array_property_filler");
                indent.setMaxWidth(Double.MAX_VALUE);
                indent.setMaxHeight(Double.MAX_VALUE);
                indent.setMinWidth(0);
                indent.setMinHeight(0);
                add(indent, 0, row, indentationLevel, 1);
            }

            Label indent = new Label();
            indent.getStyleClass().add("array_property_filler");
            indent.setMaxWidth(Double.MAX_VALUE);
            indent.setMaxHeight(Double.MAX_VALUE);
            indent.setMinWidth(0);
            indent.setMinHeight(0);
            add(indent, 7 - indentationLevel - 1, row, indentationLevel + 1, 1);

            Separator separator = new Separator();
            separator.getStyleClass().add("property_separator_filler");
            add(separator, 0, row + 1, indentationLevel + 1, 1);

            separator = new Separator();
            separator.getStyleClass().add("property_separator_filler");
            add(separator, 7 - indentationLevel - 1, row + 1, indentationLevel + 1, 1);
        }
    }

    private void fillIndent(final int indentationLevel, final int row )
    {
        if (indentationLevel > 0)
        {
            Label indent = new Label();
            indent.getStyleClass().add("array_property_filler");
            indent.setMaxWidth(Double.MAX_VALUE);
            indent.setMaxHeight(Double.MAX_VALUE);
            indent.setMinWidth(0);
            indent.setMinHeight(0);
            add(indent, 0, row, indentationLevel, 1);

            indent = new Label();
            indent.getStyleClass().add("array_property_filler");
            indent.setMaxWidth(Double.MAX_VALUE);
            indent.setMaxHeight(Double.MAX_VALUE);
            indent.setMinWidth(0);
            indent.setMinHeight(0);
            add(indent, 7 - indentationLevel, row, indentationLevel, 1);

            Separator separator = new Separator();
            separator.getStyleClass().add("property_separator_filler");
            add(separator, 0, row + 1, indentationLevel, 1);

            separator = new Separator();
            separator.getStyleClass().add("property_separator_filler");
            add(separator, 7 - indentationLevel, row + 1, indentationLevel, 1);
        }
    }

    /** Clear the property UI
     *  <P>Removes all property bindings and their UI
     */
    void clear()
    {
        bindings.forEach(WidgetPropertyBinding::unbind);
        bindings.clear();
        getChildren().clear();
    }
}
