/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.properties;

import static org.csstudio.display.builder.editor.Plugin.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.RulesWidgetProperty;
import org.csstudio.display.builder.model.properties.ScriptPV;
import org.csstudio.display.builder.model.rules.RuleInfo;
import org.csstudio.display.builder.model.rules.RuleInfo.ExprInfoString;
import org.csstudio.display.builder.model.rules.RuleInfo.ExprInfoValue;
import org.csstudio.display.builder.model.rules.RuleInfo.ExpressionInfo;
import org.csstudio.display.builder.model.rules.RuleInfo.PropInfo;
import org.csstudio.display.builder.representation.javafx.AutocompleteMenu;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.display.builder.representation.javafx.Messages;
import org.csstudio.display.builder.representation.javafx.ScriptsDialog;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.MultiLineInputDialog;
import org.phoebus.ui.javafx.TableHelper;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

/** Dialog for editing {@link RuleInfo}s
 *  @author Megan Grodowitz
 */
@SuppressWarnings("nls")
public class RulesDialog extends Dialog<List<RuleInfo>>
{
    /** ScriptPV info as property-based item for table */
    public static class PVItem
    {
        private final StringProperty name = new SimpleStringProperty();
        private final BooleanProperty trigger = new SimpleBooleanProperty(true);

        public PVItem(final String name, final boolean trigger)
        {
            this.name.set(name);
            this.trigger.set(trigger);
        }

        public static PVItem forPV(final ScriptPV info)
        {
            return new PVItem(info.getName(), info.isTrigger());
        }

        public ScriptPV toScriptPV()
        {
            return new ScriptPV(name.get(), trigger.get());
        }

        public StringProperty nameProperty()
        {
            return name;
        }

        public BooleanProperty triggerProperty()
        {
            return trigger;
        }
    };

    /** Expression info as property-based item for table */
    public abstract static class ExprItem<T>
    {
        final protected StringProperty boolExp = new SimpleStringProperty();
        final protected SimpleObjectProperty<Node> field = new SimpleObjectProperty<Node>();
        final protected List<WidgetPropertyBinding<?,?>> bindings = new ArrayList<>();

        public ExprItem(final String boolE, final T valE, final UndoableActionManager undo)
        {
            this.boolExp.set(boolE);
        }

        public SimpleObjectProperty<Node> fieldProperty()
        {
            return field;
        }

        public StringProperty boolExpProperty()
        {
            return boolExp;
        }

        abstract boolean isWidgetProperty();
        abstract public ExpressionInfo<T> toExprInfo();
        abstract public T getPropVal();
    };


    public static class ExprItemString extends ExprItem<String>
    {
        final protected Widget widget = new Widget("ExprItemString");
        final protected WidgetProperty<String> string_prop;
        protected String internal_prop_val;

        public ExprItemString(String bool_exp, String prop_val, UndoableActionManager undo)
        {
            super(bool_exp, prop_val, undo);
            internal_prop_val = prop_val;
            string_prop = CommonWidgetProperties.propText.createProperty(widget, prop_val);
            field.setValue(PropertyPanelSection.
                    bindSimplePropertyField(undo, bindings, string_prop, new ArrayList<Widget>()));
        }

        @Override
        boolean isWidgetProperty()
        {
            return false;
        }

        @Override
        public String getPropVal()
        {
            internal_prop_val = string_prop.getValue();
            return internal_prop_val;
        }

        @Override
        public ExprInfoString toExprInfo()
        {
            return new ExprInfoString(boolExp.get(), getPropVal());
        }
    };

    public static class ExprItemValue<T> extends ExprItem< WidgetProperty<T> >
    {
        protected final WidgetProperty<T> internal_prop_val;

        public ExprItemValue(String bool_exp, WidgetProperty<T> prop_val, UndoableActionManager undo)
        {
            super(bool_exp, prop_val, undo);
            internal_prop_val = prop_val;
            field.setValue(PropertyPanelSection.
                    bindSimplePropertyField(undo, bindings, prop_val, new ArrayList<Widget>()));
        }

        @Override
        public ExprInfoValue<T> toExprInfo()
        {
            return new ExprInfoValue<T>(boolExp.get(), internal_prop_val);
        }

        @Override
        boolean isWidgetProperty()
        {
            return true;
        }

        @Override
        public WidgetProperty<T> getPropVal()
        {
            return internal_prop_val;
        }
    };

    public static class ExprItemFactory
    {
        public static <T> ExprItem<?> InfoToItem(
                final ExpressionInfo<T> info,
                final UndoableActionManager undo) throws Exception
        {
            if (info.getPropVal() instanceof String)
                return new ExprItemString(info.getBoolExp(), (String)info.getPropVal(), undo);
            if (info.getPropVal() instanceof WidgetProperty<?>)
                return new ExprItemValue<>(info.getBoolExp(), (WidgetProperty<?>)info.getPropVal(), undo);

            logger.log(Level.WARNING, "Tried to make new Expression from info with property not of type String or WidgetProperty: "
                    + info.getPropVal().getClass().getName());

            throw new Exception("Invalid info property type");
        }

        public static <T> ExprItem<?> makeNew(
                final T property,
                final UndoableActionManager undo) throws Exception
        {
            if (property instanceof String)
                return new ExprItemString("new expr", (String)property, undo);
            if (property instanceof WidgetProperty<?>)
                return new ExprItemValue<>("new exp", (WidgetProperty<?>)property, undo);

            logger.log(Level.WARNING, "Tried to make new Expression from property not of type String or WidgetProperty: "
                    + property.getClass().getName());

            throw new Exception("Invalid property type");
        }

        public static <T> ExprItem<?> makeNewFromOld(
                final T property,
                final ExprItem<?> old_exp,
                final UndoableActionManager undo) throws Exception
        {
            if (property instanceof String)
                return new ExprItemString(old_exp.boolExpProperty().get(), (String)property, undo);
            if (property instanceof WidgetProperty<?>)
                return new ExprItemValue<>(old_exp.boolExpProperty().get(), (WidgetProperty<?>)property, undo);

            logger.log(Level.WARNING,"Tried to make new Expression from property not of type String or WidgetProperty: "
                    + property.getClass().getName());

            throw new Exception("Invalid property type");
        }
    }

    public class ValueFormatCell extends TableCell<ExprItem<?>, Node>
    {
        @Override protected void updateItem(Node item, boolean empty)
        {
            // calling super here is very important - don't skip this!
            super.updateItem(item, empty);
            setGraphic(item);
        }
    }

    /** Modifiable RuleInfo */
    public static class RuleItem
    {
        public List<ExprItem<?>> expressions;
        public List<PVItem> pvs;
        protected StringProperty name = new SimpleStringProperty();
        protected StringProperty prop_id = new SimpleStringProperty();
        public BooleanProperty prop_as_expr = new SimpleBooleanProperty(false);
        protected Widget attached_widget = null;


        public RuleItem(final Widget attached_widget, final String prop_id)
        {
            this(attached_widget, new ArrayList<>(), new ArrayList<>(),
                 Messages.RulesDialog_DefaultRuleName, prop_id, false);
        }

        public RuleItem(final Widget attached_widget,
                final List<ExprItem<?>> exprs,
                final List<PVItem> pvs,
                final String name,
                final String prop_id,
                final boolean prop_as_exp)
        {
            this.attached_widget = attached_widget;
            this.expressions = exprs;
            this.pvs = pvs;
            this.name.set(name);
            this.prop_id.set(prop_id);
            this.prop_as_expr.set(prop_as_exp);
        }

        public static RuleItem forInfo(final Widget attached_widget, final RuleInfo info, final UndoableActionManager undo)
        {
            final List<PVItem> pvs = new ArrayList<>();
            info.getPVs().forEach(pv -> pvs.add(PVItem.forPV(pv)));
            final List<ExprItem<?>> exprs = new ArrayList<>();
            info.getExpressions().forEach(expr ->
            {
                try
                {
                    exprs.add(ExprItemFactory.InfoToItem(expr, undo));
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Error converting " + expr, ex);
                }
            });

            return new RuleItem(attached_widget, exprs, pvs, info.getName(), info.getPropID(), info.getPropAsExprFlag());
        }

        public RuleInfo getRuleInfo()
        {
            final List<ScriptPV> spvs = new ArrayList<>();
            pvs.forEach(pv -> spvs.add(pv.toScriptPV()));
            final List<ExpressionInfo<?>> exps = new ArrayList<>();
            expressions.forEach(exp -> exps.add(exp.toExprInfo()));
            return new RuleInfo(name.get(), prop_id.get(), prop_as_expr.get(), exps, spvs);
        }

        public StringProperty nameProperty()
        {
            return name;
        }

        public StringProperty propIDProperty()
        {
            return prop_id;
        }

        public static ExprItem<?> addNewExpr(
                final UndoableActionManager undo,
                final ExprItem<?> old_exp,
                final Widget attached_widget,
                List<ExprItem<?>> expls,
                final String prop_id,
                final boolean prop_as_expr)
        {
            final Object new_prop;
            if (prop_as_expr)
                new_prop = prop_id + " value";
            else
                new_prop = RulesWidgetProperty.propIDToNewProp(attached_widget, prop_id, "");

            ExprItem<?> new_exp = null;
            try
            {
                if (old_exp != null)
                    new_exp = ExprItemFactory.makeNewFromOld(new_prop, old_exp, undo);
                else
                    new_exp = ExprItemFactory.makeNew(new_prop, undo);
                expls.add(new_exp);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Rule expression error", ex);
            }
            return new_exp;
        }

        public ExprItem<?> addNewExpr(final UndoableActionManager undo)
        {
            return addNewExpr(undo, null, attached_widget, expressions, prop_id.get(), prop_as_expr.get());
        }

        public boolean tryTogglePropAsExpr(final UndoableActionManager undo, boolean new_val)
        {
            if (prop_as_expr.get() == new_val)
                return false;

            List<ExprItem<?>> new_expr = new ArrayList<>();
            expressions.forEach(expr -> addNewExpr(undo, expr, attached_widget, new_expr, prop_id.get(), new_val));
            prop_as_expr.set(new_val);
            expressions = new_expr;
            return true;
        }

        public boolean tryUpdatePropID(final UndoableActionManager undo, String new_prop_id)
        {
            if (new_prop_id.equals(prop_id.get()))
                return false;

            prop_id.set(new_prop_id);

            // If just an output expression string. No need to change objects
            if (prop_as_expr.get())
                return true;

            final List<ExprItem<?>> new_exps = new ArrayList<>();
            for (final ExprItem<?> exp : expressions)
            {
                WidgetProperty<?> new_prop =
                        RulesWidgetProperty.propIDToNewProp(attached_widget, prop_id.get(), "");
                try
                {
                    new_exps.add(ExprItemFactory.makeNewFromOld(new_prop, exp, undo));
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Rule error", ex);
                }
            }
            expressions = new_exps;

            return true;
        }
    };

    /** Data that is linked to the rules_table */
    private final ObservableList<RuleItem> rule_items = FXCollections.observableArrayList();

    /** Table for all rules */
    private TableView<RuleItem> rules_table;

    /** Data that is linked to the pvs_table */
    private final ObservableList<PVItem> pv_items = FXCollections.observableArrayList();

    /** Table for PVs of currently selected rule */
    private TableView<PVItem> pvs_table;

    /** Data that is linked to the expressions_table */
    private final ObservableList<ExprItem<?>> expression_items = FXCollections.observableArrayList();

    /** Table for PVs of currently selected rule */
    private TableView<ExprItem<?>> expressions_table;

    /** Buttons for removing or reordering rules **/
    private Button btn_dup_rule, btn_remove_rule, btn_move_rule_up, btn_move_rule_down, btn_show_script;
    /** Buttons for adding/removing PVs and expressions from the selected rule **/
    private Button btn_add_pv, btn_rm_pv, btn_move_pv_up, btn_move_pv_down, btn_add_exp, btn_rm_exp, btn_move_exp_up, btn_move_exp_down;

    /** Currently selected rule **/
    private RuleItem selected_rule_item = null;

    /** Widget name and type for the header bar **/
    private final Widget attached_widget;
    /** Undo actions for choosing property values in expressions **/
    private final UndoableActionManager undo;
    /** Autocomplete menu for pv names */
    private final AutocompleteMenu menu;

    /** Property options for target of expression **/
    private final List<PropInfo> propinfo_ls;
    private ComboBox<String> propComboBox;

    private static final int MAX_PROP_LENGTH = 40;

    /** Is the property value an expressions (i.e. user input string) **/
    private CheckBox valExpBox;

    /** turn this rule's property into the long string form used in the combo box **/
    public String getPropLongString(RuleItem rule)
    {
        final PropInfo pi = new PropInfo(rule.attached_widget, rule.prop_id.get());
        return pi.toString();
    }

    /** @param rules Rules to show/edit in the dialog */
    public RulesDialog(final UndoableActionManager undo, final List<RuleInfo> rules, final Widget attached_widget, final AutocompleteMenu menu)
    {
        setTitle(Messages.RulesDialog_Title);
        this.undo = undo;
        this.attached_widget = attached_widget;
        this.menu = menu;

        propinfo_ls = RuleInfo.getTargettableProperties(attached_widget);
        setHeaderText(Messages.RulesDialog_Info + ": " +
                      attached_widget.getType() + " " +
                      attached_widget.getName());


        rules.forEach(rule -> rule_items.add(RuleItem.forInfo(attached_widget, rule, undo)));
        fixupRules(0);

        getDialogPane().setContent(createContent());
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        //use same stylesheet as ScriptsDialog, ActionsDialog
        getDialogPane().getStylesheets().add(ScriptsDialog.class.getResource("opibuilder.css").toExternalForm());
        setResizable(true);

        setResultConverter(button ->
        {
            if (button != ButtonType.OK)
                return null;
            return rule_items.stream()
                    .filter(item -> ! item.name.get().isEmpty())
                    .map(RuleItem::getRuleInfo)
                    .collect(Collectors.toList());
        });
    }

    private Node createContent()
    {
        final Node rules = createRulesTable();
        final Node pvs = createPVsTable();
        final Node exprs = createExpressionsTable();

        // Display PVs of currently selected rule
        rules_table.getSelectionModel().selectedItemProperty().addListener((prop, old, selected) ->
        {
            selected_rule_item = selected;
            if (selected == null)
            {
                btn_remove_rule.setDisable(true);
                btn_dup_rule.setDisable(true);
                btn_move_rule_up.setDisable(true);
                btn_move_rule_down.setDisable(true);
                btn_show_script.setDisable(true);
                btn_add_pv.setDisable(true);
                btn_rm_pv.setDisable(true);
                btn_move_pv_up.setDisable(true);
                btn_move_pv_down.setDisable(true);
                btn_add_exp.setDisable(true);
                btn_rm_exp.setDisable(true);
                btn_move_exp_up.setDisable(true);
                btn_move_exp_down.setDisable(true);
                propComboBox.setDisable(true);
                propComboBox.getSelectionModel().select(null);
                valExpBox.setDisable(true);
                pv_items.clear();
                expression_items.clear();
            }
            else
            {
                btn_remove_rule.setDisable(false);
                btn_dup_rule.setDisable(false);
                btn_move_rule_up.setDisable(false);
                btn_move_rule_down.setDisable(false);
                btn_show_script.setDisable(false);
                btn_add_pv.setDisable(false);
                btn_rm_pv.setDisable(false);
                btn_move_pv_up.setDisable(false);
                btn_move_pv_down.setDisable(false);
                btn_add_exp.setDisable(false);
                btn_rm_exp.setDisable(false);
                btn_move_exp_up.setDisable(false);
                btn_move_exp_down.setDisable(false);
                propComboBox.setDisable(false);
                propComboBox.getSelectionModel().select(getPropLongString(selected));
                valExpBox.setDisable(false);
                valExpBox.selectedProperty().set(selected.prop_as_expr.get());
                pv_items.setAll(selected.pvs);
                expression_items.setAll(selected.expressions);
                fixupPVs(0);
            }
        });
        // Update PVs of selected rule from PVs table
        final ListChangeListener<PVItem> pll = change ->
        {
            final RuleItem selected = rules_table.getSelectionModel().getSelectedItem();
            if (selected != null)
                selected.pvs = new ArrayList<>(change.getList());
        };
        pv_items.addListener(pll);

        // Update Expressions of selected rule from Expressions table
        final ListChangeListener<ExprItem<?>> ell = change ->
        {
            final RuleItem selected = rules_table.getSelectionModel().getSelectedItem();
            if (selected != null)
                selected.expressions = new ArrayList<>(change.getList());
        };
        expression_items.addListener(ell);

        // What is the property id option we are using?
        final Label propLabel = new Label("Property ID:");
        // Show each property with current value
        final ObservableList<String> prop_id_opts = FXCollections.observableArrayList();
        for (PropInfo pi : propinfo_ls)
        {   // Property _value_ can be long, ex. points of a polyline
            // Truncate the value that's shown in the combo box
            // to prevent combo from using all screen width.
            String prop_opt = pi.toString();
            if (prop_opt.length() > MAX_PROP_LENGTH)
                prop_opt = prop_opt.substring(0, MAX_PROP_LENGTH) + "...";
            prop_id_opts.add(prop_opt);
        }
        propComboBox = new ComboBox<String>(prop_id_opts);
        propComboBox.setDisable(true);
        // Select property info based on index within combo
        propComboBox.getSelectionModel().selectedIndexProperty().addListener( (p, o, index) ->
        {
            final PropInfo prop = propinfo_ls.get(index.intValue());
            if (selected_rule_item.tryUpdatePropID(undo, prop.getPropID()))
                expression_items.setAll(selected_rule_item.expressions);
        });

        // TODO: change this to actually manipulate expression objects in the rule
        valExpBox = new CheckBox("Value as Expression");
        valExpBox.setDisable(true);
        valExpBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) {
                if (!selected_rule_item.tryTogglePropAsExpr(undo, new_val))
                {
                    logger.log(Level.FINE, "Did not update rule property as expression flag to " + new_val);
                }
                else
                {
                    expression_items.setAll(selected_rule_item.expressions);
                }
            }
        });

        final HBox props = new HBox(10, valExpBox, new Separator(Orientation.VERTICAL), propLabel, propComboBox);
        final HBox subtabs = new HBox(10, pvs, exprs);
        final VBox subitems = new VBox(10, props, new Separator(Orientation.HORIZONTAL), subtabs);
        final VBox rulebox = new VBox(10, rules);
        final HBox box = new HBox(10, rulebox, new Separator(Orientation.VERTICAL), subitems);
        VBox.setVgrow(rules, Priority.ALWAYS);
        HBox.setHgrow(pvs, Priority.ALWAYS);
        HBox.setHgrow(exprs, Priority.ALWAYS);
        HBox.setHgrow(subitems, Priority.ALWAYS);
        VBox.setVgrow(subtabs, Priority.ALWAYS);

        return box;
    }

    /** @param attached_widget
     * @return Node for UI elements that edit the rules */
    private Node createRulesTable()
    {
        // Create table with editable rule 'name' column
        final TableColumn<RuleItem, String> name_col = new TableColumn<>(Messages.RulesDialog_ColName);
        name_col.setCellValueFactory(new PropertyValueFactory<RuleItem, String>("name"));
        name_col.setCellFactory(TextFieldTableCell.<RuleItem>forTableColumn());
        name_col.setOnEditCommit(event ->
        {
            final int row = event.getTablePosition().getRow();
            rule_items.get(row).name.set(event.getNewValue());
            fixupRules(row);
        });

        rules_table = new TableView<>(rule_items);
        rules_table.getColumns().add(name_col);
        rules_table.setEditable(true);
        rules_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        rules_table.setTooltip(new Tooltip(Messages.RulesDialog_RulesTT));

        // Buttons
        final Button add = new Button(Messages.Add, JFXUtil.getIcon("add.png"));
        add.setMaxWidth(Double.MAX_VALUE);
        add.setOnAction(event ->
        {
            rule_items.add( new RuleItem(
                    attached_widget,
                    ( (selected_rule_item == null) ?
                            ( (propinfo_ls.size() == 0) ? "" : propinfo_ls.get(0).getPropID() )
                            :
                                selected_rule_item.prop_id.get() )
                    ));
        });

        btn_dup_rule = new Button(Messages.Copy, JFXUtil.getIcon("embedded_script.png"));
        btn_dup_rule.setMaxWidth(Double.MAX_VALUE);
        btn_dup_rule.setDisable(true);
        btn_dup_rule.setOnAction(event ->
        {
            if (selected_rule_item != null) {
                rule_items.add( RuleItem.forInfo(attached_widget,
                        selected_rule_item.getRuleInfo(),
                        undo) );
            }
        });

        btn_remove_rule = new Button(Messages.Remove, JFXUtil.getIcon("delete.png"));
        btn_remove_rule.setMaxWidth(Double.MAX_VALUE);
        btn_remove_rule.setDisable(true);
        btn_remove_rule.setOnAction(event ->
        {
            final int sel = rules_table.getSelectionModel().getSelectedIndex();
            if (sel >= 0)
            {
                rule_items.remove(sel);
                fixupRules(sel);
            }
        });

        btn_move_rule_up = new Button(Messages.MoveUp, JFXUtil.getIcon("up.png"));
        btn_move_rule_up.setMaxWidth(Double.MAX_VALUE);
        btn_move_rule_up.setDisable(true);
        btn_move_rule_up.setOnAction(event -> TableHelper.move_item_up(rules_table, rule_items));

        btn_move_rule_down = new Button(Messages.MoveDown, JFXUtil.getIcon("down.png"));
        btn_move_rule_down.setMaxWidth(Double.MAX_VALUE);
        btn_move_rule_down.setDisable(true);
        btn_move_rule_down.setOnAction(event -> TableHelper.move_item_down(rules_table, rule_items));

        btn_show_script = new Button(Messages.RulesDialog_ShowScript, JFXUtil.getIcon("embedded_script.png"));
        btn_show_script.setMaxWidth(Double.MAX_VALUE);
        btn_show_script.setAlignment(Pos.BOTTOM_CENTER);
        btn_show_script.setDisable(true);
        btn_show_script.setOnAction(event ->
        {
            final int sel = rules_table.getSelectionModel().getSelectedIndex();
            if (sel >= 0)
            {
                final String content = rule_items.get(sel).getRuleInfo().getTextPy(attached_widget);
                final MultiLineInputDialog dialog = new MultiLineInputDialog(content);
                DialogHelper.positionDialog(dialog, btn_show_script, -200, -300);
                dialog.setTextHeight(600);
                dialog.show();
            }
        });

        final VBox buttons = new VBox(10, add,
                new Separator(Orientation.HORIZONTAL),
                btn_dup_rule,
                btn_remove_rule,
                btn_move_rule_up,
                btn_move_rule_down,
                new Separator(Orientation.HORIZONTAL),
                btn_show_script);

        final HBox content = new HBox(10, rules_table, buttons);
        HBox.setHgrow(rules_table, Priority.ALWAYS);
        return content;
    }

    /** Fix rules data: Delete empty rows in middle
     *  @param changed_row Row to check, and remove if it's empty
     */
    private void fixupRules(final int changed_row)
    {
        // Check if edited row is now empty and should be deleted
        if (changed_row < rule_items.size())
        {
            final RuleItem item = rule_items.get(changed_row);
            if (item.nameProperty().get().trim().isEmpty())
                rule_items.remove(changed_row);
        }
    }

    /** @return Node for UI elements that edit the expressions */
    private Node createExpressionsTable()
    {
        // Create table with editable rule 'bool expression' column
        final TableColumn<ExprItem<?>, String> bool_exp_col = new TableColumn<>(Messages.RulesDialog_ColBoolExp);
        bool_exp_col.setCellValueFactory(new PropertyValueFactory<ExprItem<?>, String>("boolExp"));
        bool_exp_col.setCellFactory(TextFieldTableCell.forTableColumn());
        bool_exp_col.setOnEditCommit(event ->
        {
            final int row = event.getTablePosition().getRow();
            expression_items.get(row).boolExpProperty().set(event.getNewValue());
        });

        // Create table with editable rule 'value expression' column
        final TableColumn<ExprItem<?>, Node> val_exp_col = new TableColumn<>(Messages.RulesDialog_ColValExp);
        val_exp_col.setCellValueFactory(new PropertyValueFactory<ExprItem<?>, Node>("field"));
        val_exp_col.setCellFactory(new Callback<TableColumn<ExprItem<?>, Node>, TableCell<ExprItem<?>, Node>>()
        {
            @Override public TableCell<ExprItem<?>, Node> call(TableColumn<ExprItem<?>, Node> t)
            {
                return new ValueFormatCell();
            }
        });
        val_exp_col.setOnEditCommit(event ->
        {
            final int row = event.getTablePosition().getRow();
            expression_items.get(row).fieldProperty().set(event.getNewValue());
        });


        expressions_table = new TableView<>(expression_items);
        expressions_table.getColumns().add(bool_exp_col);
        expressions_table.getColumns().add(val_exp_col);
        expressions_table.setEditable(true);
        expressions_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        expressions_table.setTooltip(new Tooltip(Messages.RulesDialog_ExpressionsTT));
        expressions_table.setPlaceholder(new Label(Messages.RulesDialog_SelectRule));


        btn_add_exp = new Button(Messages.Add, JFXUtil.getIcon("add.png"));
        btn_add_exp.setMaxWidth(Double.MAX_VALUE);
        btn_add_exp.setDisable(true);
        btn_add_exp.setOnAction(event ->
        {
            selected_rule_item.addNewExpr(undo);
            expression_items.setAll(selected_rule_item.expressions);
        });

        btn_rm_exp = new Button(Messages.Remove, JFXUtil.getIcon("delete.png"));
        btn_rm_exp.setMaxWidth(Double.MAX_VALUE);
        btn_rm_exp.setDisable(true);
        btn_rm_exp.setOnAction(event ->
        {
            final int sel = expressions_table.getSelectionModel().getSelectedIndex();
            if (sel >= 0)
                expression_items.remove(sel);
        });

        btn_move_exp_up = new Button(Messages.MoveUp, JFXUtil.getIcon("up.png"));
        btn_move_exp_up.setMaxWidth(Double.MAX_VALUE);
        btn_move_exp_up.setDisable(true);
        btn_move_exp_up.setOnAction(event -> TableHelper.move_item_up(expressions_table, expression_items));

        btn_move_exp_down = new Button(Messages.MoveDown, JFXUtil.getIcon("down.png"));
        btn_move_exp_down.setMaxWidth(Double.MAX_VALUE);
        btn_move_exp_down.setDisable(true);
        btn_move_exp_down.setOnAction(event -> TableHelper.move_item_down(expressions_table, expression_items));

        final VBox buttons = new VBox(10, btn_add_exp, btn_rm_exp, btn_move_exp_up, btn_move_exp_down);
        final HBox content = new HBox(10, expressions_table, buttons);
        HBox.setHgrow(expressions_table, Priority.ALWAYS);
        return content;
    }

    /**
     * {@link PVItem} {@link TableCell} with {@link AutocompleteMenu}
     *
     * @author Amanda Carpenter
     */
    private class AutoCompletedTableCell extends TableCell<PVItem, String>
    {
        //TODO: make autocomplete menu styling consistent with other menus
        //(stylesheet in representation.javafx package is not applied)
        private TextField textField;
        private final AutocompleteMenu menu;

        public AutoCompletedTableCell(final AutocompleteMenu menu)
        {
            this.menu = menu;
        }

        @Override
        public void startEdit()
        {
            if (!isEmpty())
            {
                super.startEdit();
                createTextField();
                setText(null);
                setGraphic(textField);
                menu.attachField(textField);
                textField.selectAll();
            }
        }

        @Override
        public void cancelEdit()
        {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
        }

        @Override
        public void updateItem(String item, boolean empty)
        {
            super.updateItem(item, empty);
            if (empty)
            {
                setText(null);
                setGraphic(null);
            } else if (isEditing())
            {
                if (textField != null)
                    textField.setText(getItem() == null ? "" : getItem());
                setText(null);
                setGraphic(textField);
            } else
            {
                setText(getItem() == null ? "" : getItem());
                setGraphic(null);
                if (textField != null)
                    menu.removeField(textField);
            }
        }

        private void createTextField()
        {
            if (textField == null)
            {
                textField = new TextField(getItem() == null ? "" : getItem());
                textField.setOnAction((event) ->
                {
                    commitEdit(textField.getText());
                });
            } else
                textField.setText(getItem() == null ? "" : getItem());
            textField.setMinWidth(getWidth() - getGraphicTextGap() * 2);
        }
    }

    /** @return Node for UI elements that edit the PVs of a rule */
    private Node createPVsTable()
    {
        // Create table with editable 'name' column
        final TableColumn<PVItem, String> name_col = new TableColumn<>(Messages.ScriptsDialog_ColPV);
        name_col.setCellValueFactory(new PropertyValueFactory<PVItem, String>("name"));
        name_col.setCellFactory((col) -> new AutoCompletedTableCell(menu));
        name_col.setOnEditCommit(event ->
        {
            final int row = event.getTablePosition().getRow();
            pv_items.get(row).nameProperty().set(event.getNewValue());
            fixupPVs(row);
        });

        // Table column for 'trigger' uses CheckBoxTableCell that directly modifies the Observable Property
        final TableColumn<PVItem, Boolean> trigger_col = new TableColumn<>(Messages.ScriptsDialog_ColTrigger);
        trigger_col.setCellValueFactory(new PropertyValueFactory<PVItem, Boolean>("trigger"));
        trigger_col.setCellFactory(CheckBoxTableCell.<PVItem>forTableColumn(trigger_col));

        pvs_table = new TableView<>(pv_items);
        pvs_table.getColumns().add(name_col);
        pvs_table.getColumns().add(trigger_col);

        pvs_table.setEditable(true);
        pvs_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        pvs_table.setTooltip(new Tooltip(Messages.RulesDialog_PVsTT));
        pvs_table.setPlaceholder(new Label(Messages.RulesDialog_SelectRule));

        //name_col.prefWidthProperty().bind(pvs_table.widthProperty().multiply(0.8));
        //trigger_col.prefWidthProperty().bind(pvs_table.widthProperty().multiply(0.2));

        // Buttons
        btn_add_pv = new Button(Messages.Add, JFXUtil.getIcon("add.png"));
        btn_add_pv.setMaxWidth(Double.MAX_VALUE);
        btn_add_pv.setDisable(true);
        btn_add_pv.setOnAction(event ->
        {
            pv_items.add(new PVItem("newpv", true));
        });

        btn_rm_pv = new Button(Messages.Remove, JFXUtil.getIcon("delete.png"));
        btn_rm_pv.setMaxWidth(Double.MAX_VALUE);
        btn_rm_pv.setDisable(true);
        btn_rm_pv.setOnAction(event ->
        {
            final int sel = pvs_table.getSelectionModel().getSelectedIndex();
            if (sel >= 0)
            {
                pv_items.remove(sel);
                fixupPVs(sel);
            }
        });

        btn_move_pv_up = new Button(Messages.MoveUp, JFXUtil.getIcon("up.png"));
        btn_move_pv_up.setMaxWidth(Double.MAX_VALUE);
        btn_move_pv_up.setDisable(true);
        btn_move_pv_up.setOnAction(event -> TableHelper.move_item_up(pvs_table, pv_items));

        btn_move_pv_down = new Button(Messages.MoveDown, JFXUtil.getIcon("down.png"));
        btn_move_pv_down.setMaxWidth(Double.MAX_VALUE);
        btn_move_pv_down.setDisable(true);
        btn_move_pv_down.setOnAction(event -> TableHelper.move_item_down(pvs_table, pv_items));

        final VBox buttons = new VBox(10, btn_add_pv, btn_rm_pv, btn_move_pv_up, btn_move_pv_down);
        final HBox content = new HBox(10, pvs_table, buttons);
        HBox.setHgrow(pvs_table, Priority.ALWAYS);
        return content;
    }

    /** Fix PVs data: Delete empty rows in middle
     *  @param changed_row Row to check, and remove if it's empty
     */
    private void fixupPVs(final int changed_row)
    {
        // Check if edited row is now empty and should be deleted
        if (changed_row < pv_items.size())
        {
            final PVItem item = pv_items.get(changed_row);
            if (item.nameProperty().get().trim().isEmpty())
                pv_items.remove(changed_row);
        }
    }
}
