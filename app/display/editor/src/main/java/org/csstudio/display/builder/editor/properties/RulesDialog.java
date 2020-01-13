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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.phoebus.ui.javafx.EditCell;
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
import org.csstudio.display.builder.model.util.ModelThreadPool;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.display.builder.representation.javafx.Messages;
import org.csstudio.display.builder.representation.javafx.PVTableItem;
import org.csstudio.display.builder.representation.javafx.PVTableItem.AutoCompletedTableCell;
import org.csstudio.display.builder.representation.javafx.ScriptsDialog;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.MultiLineInputDialog;
import org.phoebus.ui.javafx.LineNumberTableCellFactory;
import org.phoebus.ui.javafx.TableHelper;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Dialog for editing {@link RuleInfo}s
 *  @author Megan Grodowitz
 *  @author Claudio Rosati
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RulesDialog extends Dialog<List<RuleInfo>>
{
    /** Expression info as property-based item for table */
    public abstract static class ExprItem<T>
    {
        final protected StringProperty boolExp = new SimpleStringProperty();
        final protected SimpleObjectProperty<Node> field = new SimpleObjectProperty<>();
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
            return new ExprInfoValue<>(boolExp.get(), internal_prop_val);
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

    /** Modifiable RuleInfo */
    public static class RuleItem
    {
        public List<ExprItem<?>> expressions;
        public List<PVTableItem> pvs;
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
                final List<PVTableItem> pvs,
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
            final List<PVTableItem> pvs = new ArrayList<>();
            info.getPVs().forEach(pv -> pvs.add(PVTableItem.forPV(pv)));
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
    private final ObservableList<PVTableItem> pv_items = FXCollections.observableArrayList();

    /** Table for PVs of currently selected rule */
    private TableView<PVTableItem> pvs_table;

    /** Data that is linked to the expressions_table */
    private final ObservableList<ExprItem<?>> expression_items = FXCollections.observableArrayList();

    /** Table for PVs of currently selected rule */
    private TableView<ExprItem<?>> expressions_table;

    /** Buttons for removing or reordering rules **/
    private Button btn_add_rule, btn_dup_rule, btn_remove_rule, btn_move_rule_up, btn_move_rule_down, btn_show_script;
    /** Buttons for adding/removing PVs and expressions from the selected rule **/
    private Button btn_add_pv, btn_rm_pv, btn_move_pv_up, btn_move_pv_down, btn_add_exp, btn_rm_exp, btn_move_exp_up, btn_move_exp_down;

    /** Currently selected rule **/
    private RuleItem selected_rule_item = null;

    /** Widget name and type for the header bar **/
    private final Widget attached_widget;
    /** Undo actions for choosing property values in expressions **/
    private final UndoableActionManager undo;

    /** Property options for target of expression **/
    private final List<PropInfo> propinfo_ls;
    private ComboBox<String> propComboBox;

    private static final int MAX_PROP_LENGTH = 40;

    /** Is the property value an expressions (i.e. user input string) **/
    private CheckBox valExpBox;

    /** The splitter used in the rule side. */
    private SplitPane ruleSplitPane;

    /** The main splitter */
    private final SplitPane content;

    /** turn this rule's property into the long string form used in the combo box **/
    public String getPropLongString(RuleItem rule)
    {
        final PropInfo pi = new PropInfo(rule.attached_widget, rule.prop_id.get());
        return pi.toString();
    }

    /** @param undo Undo support
     *  @param rules Rules to show/edit in the dialog
     *  @param attached_widget Widget
     *  @param owner The node starting this dialog
     **/
    public RulesDialog(final UndoableActionManager undo, final List<RuleInfo> rules, final Widget attached_widget, final Node owner)
    {
        this.undo = undo;
        this.attached_widget = attached_widget;
        this.propinfo_ls = RuleInfo.getTargettableProperties(attached_widget);

        setTitle(Messages.RulesDialog_Title);
        setHeaderText(Messages.RulesDialog_Info + ": " +
                      attached_widget.getType() + " " +
                      attached_widget.getName());

        rules.forEach(rule -> rule_items.add(RuleItem.forInfo(attached_widget, rule, undo)));
        fixupRules(0);

        content = createContent();
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        // use same stylesheet as ScriptsDialog, ActionsDialog
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

        DialogHelper.positionAndSize(
            this,
            owner,
            PhoebusPreferenceService.userNodeForClass(RulesDialog.class),
            prefs ->
            {
                content.setDividerPositions(prefs.getDouble("content.divider.position", 0.5));
                ruleSplitPane.setDividerPositions(prefs.getDouble("rule.content.divider.position", 0.5));
            },
            prefs ->
            {
                prefs.putDouble("content.divider.position", content.getDividerPositions()[0]);
                prefs.putDouble("rule.content.divider.position", ruleSplitPane.getDividerPositions()[0]);
            });
    }

    private SplitPane createContent()
    {
        final Node rules = createRulesTable();
        final HBox pvs = createPVsTable();
        final HBox exprs = createExpressionsTable();

        // Display PVs of currently selected rule
        rules_table.getSelectionModel().selectedItemProperty().addListener( (prop, old, selected) ->
        {
            selected_rule_item = selected;
            if (selected == null)
            {
                pvs.setDisable(true);
                exprs.setDisable(true);
                btn_remove_rule.setDisable(true);
                btn_dup_rule.setDisable(true);
                btn_move_rule_up.setDisable(true);
                btn_move_rule_down.setDisable(true);
                btn_show_script.setDisable(true);
                propComboBox.setDisable(true);
                propComboBox.getSelectionModel().select(null);
                valExpBox.setDisable(true);
                pv_items.clear();
                expression_items.clear();
            }
            else
            {
                pvs.setDisable(false);
                exprs.setDisable(false);

                final TableViewSelectionModel<RuleItem> model = rules_table.getSelectionModel();
                btn_remove_rule.setDisable(false);
                btn_dup_rule.setDisable(false);
                btn_move_rule_up.setDisable(model.getSelectedIndex() == 0);
                btn_move_rule_down.setDisable(model.getSelectedIndex() == rule_items.size() - 1);
                btn_show_script.setDisable(false);
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
        final ListChangeListener<PVTableItem> pll = change ->
        {
            final RuleItem selected = rules_table.getSelectionModel().getSelectedItem();
            if (selected != null)
                selected.pvs = new ArrayList<>(change.getList());
        };
        pv_items.addListener(pll);

        // Update buttons for currently selected PV
        pvs_table.getSelectionModel().selectedItemProperty().addListener( (prop, old, selected) ->
        {
            if (selected == null)
            {
                btn_rm_pv.setDisable(true);
                btn_move_pv_up.setDisable(true);
                btn_move_pv_down.setDisable(true);
            }
            else
            {
                final TableViewSelectionModel<PVTableItem> model = pvs_table.getSelectionModel();
                btn_rm_pv.setDisable(false);
                btn_move_pv_up.setDisable(model.getSelectedIndex() == 0);
                btn_move_pv_down.setDisable(model.getSelectedIndex() == pv_items.size() - 1);
            }
        });

        // Update Expressions of selected rule from Expressions table
        final ListChangeListener<ExprItem<?>> ell = change ->
        {
            final RuleItem selected = rules_table.getSelectionModel().getSelectedItem();
            if (selected != null)
                selected.expressions = new ArrayList<>(change.getList());
        };
        expression_items.addListener(ell);

        // Update buttons for currently selected expression
        expressions_table.getSelectionModel().selectedItemProperty().addListener( (prop, old, selected) ->
        {
            if (selected == null)
            {
                btn_rm_exp.setDisable(true);
                btn_move_exp_up.setDisable(true);
                btn_move_exp_down.setDisable(true);
            }
            else
            {
                final TableViewSelectionModel<ExprItem<?>> model = expressions_table.getSelectionModel();
                btn_rm_exp.setDisable(false);
                btn_move_exp_up.setDisable(model.getSelectedIndex() == 0);
                btn_move_exp_down.setDisable(model.getSelectedIndex() == expression_items.size() - 1);
            }
        });

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
        propComboBox = new ComboBox<>(prop_id_opts);
        propComboBox.setDisable(true);
        propComboBox.getSelectionModel().selectedIndexProperty().addListener( (p, o, index) ->
        {   // Select property info based on index within combo.
            final int idx = index.intValue();
            if (idx >= 0)
            {
                final PropInfo prop = propinfo_ls.get(idx);
                if (selected_rule_item.tryUpdatePropID(undo, prop.getPropID()))
                    expression_items.setAll(selected_rule_item.expressions);
            }
        });
        propComboBox.setMinHeight(27);
        propComboBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(propComboBox, Priority.ALWAYS);

        // TODO: change this to actually manipulate expression objects in the rule
        valExpBox = new CheckBox("Value as Expression");
        valExpBox.setDisable(true);
        valExpBox.selectedProperty().addListener( (ov, old_val, new_val) ->
        {
            if (!selected_rule_item.tryTogglePropAsExpr(undo, new_val))
                logger.log(Level.FINE, "Did not update rule property as expression flag to " + new_val);
            else
                expression_items.setAll(selected_rule_item.expressions);
        });

        final Region spring = new Region();
        HBox.setHgrow(spring, Priority.ALWAYS);

        final HBox props = new HBox(10, propLabel, propComboBox, spring, valExpBox);

        props.setAlignment(Pos.CENTER);
        pvs.setPadding(new Insets(0, 10, 0, 0));
        exprs.setPadding(new Insets(0, 0, 0, 10));

        HBox.setHgrow(pvs, Priority.ALWAYS);
        HBox.setHgrow(exprs, Priority.ALWAYS);

        ruleSplitPane = new SplitPane(pvs, exprs);
        ruleSplitPane.setOrientation(Orientation.HORIZONTAL);
        ruleSplitPane.setStyle("-fx-background-insets: 0, 0;");
        VBox.setVgrow(ruleSplitPane, Priority.ALWAYS);

        final VBox subitems = new VBox(10, props, ruleSplitPane);
        final VBox rulebox = new VBox(10, rules);
        rulebox.setPadding(new Insets(0, 10, 0, 0));
        subitems.setPadding(new Insets(0, 0, 0, 10));
        VBox.setVgrow(rules, Priority.ALWAYS);
        HBox.setHgrow(subitems, Priority.ALWAYS);

        final SplitPane splitPane = new SplitPane(rulebox, subitems);
        splitPane.setOrientation(Orientation.HORIZONTAL);

        // Select the first rule
        if (!rules_table.getItems().isEmpty())
        {
            Platform.runLater(() ->
            {
                rules_table.getSelectionModel().select(0);
                rules_table.requestFocus();
            });
        }
        else
            Platform.runLater(() -> btn_add_rule.requestFocus());

        return splitPane;
    }

    /** @return Node for UI elements that edit the rules */
    private Node createRulesTable ()
    {
        // Create table with editable rule 'name' column
        final TableColumn<RuleItem, String> name_col = new TableColumn<>(Messages.RulesDialog_ColName);

        name_col.setCellValueFactory(new PropertyValueFactory<RuleItem, String>("name"));

        name_col.setCellFactory(list -> EditCell.createStringEditCell());

        name_col.setOnEditCommit(event ->
        {
            final int row = event.getTablePosition().getRow();
            rule_items.get(row).name.set(event.getNewValue());
            fixupRules(row);
            Platform.runLater( ( ) -> btn_add_pv.requestFocus());
        });

        rules_table = new TableView<>(rule_items);
        rules_table.getColumns().add(name_col);
        rules_table.setEditable(true);
        rules_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        rules_table.setTooltip(new Tooltip(Messages.RulesDialog_RulesTT));
        rules_table.setPlaceholder(new Label(Messages.RulesDialog_NoRules));

        // Buttons
        btn_add_rule = new Button(Messages.Add, JFXUtil.getIcon("add.png"));
        btn_add_rule.setMaxWidth(Double.MAX_VALUE);
        btn_add_rule.setAlignment(Pos.CENTER_LEFT);
        btn_add_rule.setOnAction(event ->
        {
            final RuleItem newItem = new RuleItem(
                attached_widget,
                selected_rule_item == null
                ? ( ( propinfo_ls.size() == 0 ) ? "" : propinfo_ls.get(0).getPropID() )
                : selected_rule_item.prop_id.get()
            );

            rule_items.add(newItem);
            rules_table.getSelectionModel().select(newItem);

            final int newRow = rules_table.getSelectionModel().getSelectedIndex();

            ModelThreadPool.getTimer().schedule(() ->
            {
                Platform.runLater(() -> rules_table.edit(newRow, name_col));
            }, 123, TimeUnit.MILLISECONDS);
        });

        btn_remove_rule = new Button(Messages.Remove, JFXUtil.getIcon("delete.png"));
        btn_remove_rule.setMaxWidth(Double.MAX_VALUE);
        btn_remove_rule.setAlignment(Pos.CENTER_LEFT);
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
        btn_move_rule_up.setAlignment(Pos.CENTER_LEFT);
        btn_move_rule_up.setDisable(true);
        btn_move_rule_up.setOnAction(event -> TableHelper.move_item_up(rules_table, rule_items));

        btn_move_rule_down = new Button(Messages.MoveDown, JFXUtil.getIcon("down.png"));
        btn_move_rule_down.setMaxWidth(Double.MAX_VALUE);
        btn_move_rule_down.setAlignment(Pos.CENTER_LEFT);
        btn_move_rule_down.setDisable(true);
        btn_move_rule_down.setOnAction(event -> TableHelper.move_item_down(rules_table, rule_items));

        btn_dup_rule = new Button(Messages.Duplicate, JFXUtil.getIcon("file-duplicate.png"));
        btn_dup_rule.setMaxWidth(Double.MAX_VALUE);
        btn_dup_rule.setAlignment(Pos.CENTER_LEFT);
        btn_dup_rule.setDisable(true);
        btn_dup_rule.setOnAction(event ->
        {
            if (selected_rule_item != null)
            {
                final RuleItem newItem = RuleItem.forInfo(attached_widget, selected_rule_item.getRuleInfo(), undo);

                if (!newItem.nameProperty().get().endsWith(" (duplicate)"))
                    newItem.nameProperty().set(newItem.nameProperty().get() + " (duplicate)");

                rule_items.add(newItem);
                rules_table.getSelectionModel().select(newItem);

                final int newRow = rules_table.getSelectionModel().getSelectedIndex();

                ModelThreadPool.getTimer().schedule(() ->
                {
                    Platform.runLater( ( ) -> rules_table.edit(newRow, name_col));
                }, 123, TimeUnit.MILLISECONDS);
            }
        });

        btn_show_script = new Button(Messages.RulesDialog_ShowScript, JFXUtil.getIcon("file.png"));
        btn_show_script.setMaxWidth(Double.MAX_VALUE);
        btn_show_script.setMinWidth(120);
        btn_dup_rule.setAlignment(Pos.CENTER_LEFT);
        btn_show_script.setDisable(true);
        btn_show_script.setOnAction(event ->
        {
            final int sel = rules_table.getSelectionModel().getSelectedIndex();
            if (sel >= 0)
            {
                final String content = rule_items.get(sel).getRuleInfo().getTextPy(attached_widget);
                final MultiLineInputDialog dialog = new MultiLineInputDialog(btn_show_script, content);
                DialogHelper.positionDialog(dialog, btn_show_script, -200, -300);
                dialog.setTextHeight(600);
                dialog.show();
            }
        });

        final VBox buttons = new VBox(10, btn_add_rule, btn_remove_rule, btn_move_rule_up, btn_move_rule_down, new Pane(), btn_dup_rule, btn_show_script);
        final HBox content = new HBox(10, rules_table, buttons);

        HBox.setHgrow(rules_table, Priority.ALWAYS);
        HBox.setHgrow(buttons, Priority.NEVER);

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
    private HBox createExpressionsTable ()
    {
        // Create table with editable rule 'bool expression' column
        final TableColumn<ExprItem<?>, String> bool_exp_col = new TableColumn<>(Messages.RulesDialog_ColBoolExp);
        bool_exp_col.setSortable(false);
        bool_exp_col.setCellValueFactory(new PropertyValueFactory<ExprItem<?>, String>("boolExp"));
        bool_exp_col.setCellFactory(tableColumn -> EditCell.createStringEditCell());

        // Create table with editable rule 'value expression' column
        final TableColumn<ExprItem<?>, Node> val_exp_col = new TableColumn<>(Messages.RulesDialog_ColValExp);

        //  This statement requires "val_exp_col" be defined.
        bool_exp_col.setOnEditCommit(event ->
        {
            final int row = event.getTablePosition().getRow();

            expression_items.get(row).boolExpProperty().set(event.getNewValue());
            ModelThreadPool.getTimer().schedule(() ->
            {
                Platform.runLater(() -> val_exp_col.getCellData(row).requestFocus());
            }, 123, TimeUnit.MILLISECONDS);
        });

        val_exp_col.setSortable(false);
        val_exp_col.setCellValueFactory(new PropertyValueFactory<ExprItem<?>, Node>("field"));
        val_exp_col.setCellFactory(tableColumn -> new TableCell<>()
        {
            @Override
            protected void updateItem (final Node item, final boolean empty)
            {
                // calling super here is very important - don't skip this!
                super.updateItem(item, empty);
                setGraphic(item);
            }
        });
        val_exp_col.setOnEditCommit(event ->
        {
            final int row = event.getTablePosition().getRow();

            expression_items.get(row).fieldProperty().set(event.getNewValue());
            event.consume();
            ModelThreadPool.getTimer().schedule(() ->
            {
                Platform.runLater(() -> btn_add_exp.requestFocus());
            }, 1230, TimeUnit.MILLISECONDS);
        });

        expressions_table = new TableView<>(expression_items);
        expressions_table.getColumns().add(bool_exp_col);
        expressions_table.getColumns().add(val_exp_col);
        expressions_table.setEditable(true);
        expressions_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        expressions_table.setTooltip(new Tooltip(Messages.RulesDialog_ExpressionsTT));
        expressions_table.setPlaceholder(new Label(Messages.RulesDialog_NoExpressions));

        // Buttons
        btn_add_exp = new Button(Messages.Add, JFXUtil.getIcon("add.png"));
        btn_add_exp.setMaxWidth(Double.MAX_VALUE);
        btn_add_exp.setAlignment(Pos.CENTER_LEFT);
        btn_add_exp.setOnAction(event ->
        {
            selected_rule_item.addNewExpr(undo);
            expression_items.setAll(selected_rule_item.expressions);

            expressions_table.getSelectionModel().select(expression_items.size() - 1);

            final int newRow = expression_items.size() - 1;

            ModelThreadPool.getTimer().schedule(() ->
            {
                Platform.runLater(() -> expressions_table.edit(newRow, bool_exp_col));
            }, 123, TimeUnit.MILLISECONDS);
        });

        btn_rm_exp = new Button(Messages.Remove, JFXUtil.getIcon("delete.png"));
        btn_rm_exp.setMaxWidth(Double.MAX_VALUE);
        btn_rm_exp.setMinWidth(96);
        btn_rm_exp.setAlignment(Pos.CENTER_LEFT);
        btn_rm_exp.setDisable(true);
        btn_rm_exp.setOnAction(event ->
        {
            final int sel = expressions_table.getSelectionModel().getSelectedIndex();
            if (sel >= 0)
                expression_items.remove(sel);
        });

        btn_move_exp_up = new Button(Messages.MoveUp, JFXUtil.getIcon("up.png"));
        btn_move_exp_up.setMaxWidth(Double.MAX_VALUE);
        btn_move_exp_up.setAlignment(Pos.CENTER_LEFT);
        btn_move_exp_up.setDisable(true);
        btn_move_exp_up.setOnAction(event -> TableHelper.move_item_up(expressions_table, expression_items));

        btn_move_exp_down = new Button(Messages.MoveDown, JFXUtil.getIcon("down.png"));
        btn_move_exp_down.setMaxWidth(Double.MAX_VALUE);
        btn_move_exp_down.setAlignment(Pos.CENTER_LEFT);
        btn_move_exp_down.setDisable(true);
        btn_move_exp_down.setOnAction(event -> TableHelper.move_item_down(expressions_table, expression_items));

        final VBox buttons = new VBox(10, btn_add_exp, btn_rm_exp, btn_move_exp_up, btn_move_exp_down);
        final HBox content = new HBox(10, expressions_table, buttons);
        HBox.setHgrow(expressions_table, Priority.ALWAYS);
        HBox.setHgrow(buttons, Priority.NEVER);

        content.setDisable(true);

        return content;

    }

    /** @return Node for UI elements that edit the PVs of a rule */
    private HBox createPVsTable()
    {
        final TableColumn<PVTableItem, Integer> indexColumn = new TableColumn<>("#");
        indexColumn.setEditable(false);
        indexColumn.setSortable(false);
        indexColumn.setCellFactory(new LineNumberTableCellFactory<>(true));
        indexColumn.setMaxWidth(26);
        indexColumn.setMinWidth(26);

        // Create table with editable 'name' column
        final TableColumn<PVTableItem, String> name_col = new TableColumn<>(Messages.ScriptsDialog_ColPV);
        name_col.setSortable(false);
        name_col.setCellValueFactory(new PropertyValueFactory<PVTableItem, String>("name"));
        name_col.setCellFactory(col -> new AutoCompletedTableCell(btn_add_pv));
        name_col.setOnEditCommit(event ->
        {
            final int row = event.getTablePosition().getRow();
            pv_items.get(row).nameProperty().set(event.getNewValue());
            fixupPVs(row);
        });

        // Table column for 'trigger' uses CheckBoxTableCell that directly
        // modifies the Observable Property
        final TableColumn<PVTableItem, Boolean> trigger_col = new TableColumn<>(Messages.ScriptsDialog_ColTrigger);
        trigger_col.setSortable(false);
        trigger_col.setCellValueFactory(new PropertyValueFactory<PVTableItem, Boolean>("trigger"));
        trigger_col.setCellFactory(CheckBoxTableCell.<PVTableItem> forTableColumn(trigger_col));
        trigger_col.setResizable(false);
        trigger_col.setMaxWidth(70);
        trigger_col.setMinWidth(70);

        pvs_table = new TableView<>(pv_items);
        pvs_table.getColumns().add(indexColumn);
        pvs_table.getColumns().add(name_col);
        pvs_table.getColumns().add(trigger_col);
        pvs_table.setEditable(true);
        pvs_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        pvs_table.setTooltip(new Tooltip(Messages.RulesDialog_PVsTT));
        pvs_table.setPlaceholder(new Label(Messages.RulesDialog_NoPVs));

        // Buttons
        btn_add_pv = new Button(Messages.Add, JFXUtil.getIcon("add.png"));
        btn_add_pv.setMaxWidth(Double.MAX_VALUE);
        btn_add_pv.setAlignment(Pos.CENTER_LEFT);
        btn_add_pv.setOnAction(event ->
        {
            final PVTableItem newItem = new PVTableItem("new-PV", true);

            pv_items.add(newItem);
            pvs_table.getSelectionModel().select(newItem);

            final int newRow = pvs_table.getSelectionModel().getSelectedIndex();

            ModelThreadPool.getTimer().schedule(() ->
            {
                Platform.runLater(() -> pvs_table.edit(newRow, name_col));
            }, 123, TimeUnit.MILLISECONDS);

        });

        btn_rm_pv = new Button(Messages.Remove, JFXUtil.getIcon("delete.png"));
        btn_rm_pv.setMaxWidth(Double.MAX_VALUE);
        btn_rm_pv.setMinWidth(96);
        btn_rm_pv.setAlignment(Pos.CENTER_LEFT);
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
        btn_move_pv_up.setAlignment(Pos.CENTER_LEFT);
        btn_move_pv_up.setDisable(true);
        btn_move_pv_up.setOnAction(event -> TableHelper.move_item_up(pvs_table, pv_items));

        btn_move_pv_down = new Button(Messages.MoveDown, JFXUtil.getIcon("down.png"));
        btn_move_pv_down.setMaxWidth(Double.MAX_VALUE);
        btn_move_pv_down.setAlignment(Pos.CENTER_LEFT);
        btn_move_pv_down.setDisable(true);
        btn_move_pv_down.setOnAction(event -> TableHelper.move_item_down(pvs_table, pv_items));

        final VBox buttons = new VBox(10, btn_add_pv, btn_rm_pv, btn_move_pv_up, btn_move_pv_down);
        final HBox content = new HBox(10, pvs_table, buttons);
        HBox.setHgrow(pvs_table, Priority.ALWAYS);
        HBox.setHgrow(buttons, Priority.NEVER);

        content.setDisable(true);

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
            final PVTableItem item = pv_items.get(changed_row);
            if (item.nameProperty().get().trim().isEmpty())
                pv_items.remove(changed_row);
        }
    }
}
