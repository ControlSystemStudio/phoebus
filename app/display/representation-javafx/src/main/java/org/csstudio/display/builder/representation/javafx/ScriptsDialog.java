/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ScriptInfo;
import org.csstudio.display.builder.model.properties.ScriptPV;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.model.util.ModelThreadPool;
import org.csstudio.display.builder.representation.javafx.PVTableItem.AutoCompletedTableCell;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.ui.application.ApplicationLauncherService;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.MultiLineInputDialog;
import org.phoebus.ui.javafx.EditCell;
import org.phoebus.ui.javafx.LineNumberTableCellFactory;
import org.phoebus.ui.javafx.TableHelper;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Dialog for editing {@link ScriptInfo}s
 *  @author Kay Kasemir
 *  @author Claudio Rosati
 */
@SuppressWarnings("nls")
public class ScriptsDialog extends Dialog<List<ScriptInfo>>
{
    private final Widget widget;

    /** Modifiable ScriptInfo */
    public static class ScriptItem
    {
        public StringProperty file = new SimpleStringProperty();
        public String text;
        public boolean check_connections;
        public List<PVTableItem> pvs;

        public ScriptItem()
        {
            this(Messages.ScriptsDialog_DefaultScriptFile, ScriptInfo.EXAMPLE_PYTHON, true, new ArrayList<>());
        }

        public ScriptItem(final String file, final String text, final boolean check_connections, final List<PVTableItem> pvs)
        {
            this.file.set(file);
            this.text = text;
            this.check_connections = check_connections;
            this.pvs = pvs;
        }

        public static ScriptItem forInfo(final ScriptInfo info)
        {
            final List<PVTableItem> pvs = new ArrayList<>();
            info.getPVs().forEach(pv -> pvs.add(PVTableItem.forPV(pv)));
            return new ScriptItem(info.getPath(), info.getText(), info.getCheckConnections(), pvs);
        }

        public ScriptInfo getScriptInfo()
        {
            final List<ScriptPV> spvs = new ArrayList<>();
            pvs.forEach(pv -> spvs.add(pv.toScriptPV()));

            // 'text' is kept while the dialog is open.
            // This allows user to enter embedded text,
            // then select a script file,
            // then go back to editing the embedded text.
            // -> text is not lost when selecting a file.
            // Once the dialog is closed, however,
            // the embedded text is deleted when a file
            // was selected because otherwise
            // the display file is larger (embedded text that's not used)
            // or the runtime would be confused and actually execute the
            // embedded text.
            // #249
            if (ScriptInfo.isEmbedded(file.get()))
                return new ScriptInfo(file.get(), text, check_connections, spvs);
            else
                return new ScriptInfo(file.get(), null, check_connections, spvs);
        }

        public StringProperty fileProperty()
        {
            return file;
        }
    };

    /** Data that is linked to the scripts_table */
    private final ObservableList<ScriptItem> script_items = FXCollections.observableArrayList();

    /** Table for all scripts */
    private TableView<ScriptItem> scripts_table;
    private TableColumn<ScriptItem, String> scripts_name_col;
    private TableColumn<ScriptItem, ImageView> scripts_icon_col;
    private MenuItem convertToFileMenuItem = new MenuItem(Messages.ConvertToScriptFile, JFXUtil.getIcon("file.png"))
    {
        {
           setOnAction(e -> convertToScriptFile());
        }
    };
    private MenuItem convertToEmbeddedPythonMenuItem = new MenuItem(Messages.ConvertToEmbeddedPython, JFXUtil.getIcon("python.png"))
    {
        {
            setOnAction(e -> convertToEmbeddedPython());
        }
    };
    private MenuItem convertToEmbeddedJavaScriptMenuItem = new MenuItem(Messages.ConvertToEmbeddedJavaScript, JFXUtil.getIcon("javascript.png"))
    {
        {
            setOnAction(e -> convertToEmbeddedJavaScript());
        }
    };
    private MenuItem openInExternalEditorMenuItem = new MenuItem(Messages.OpenInExternalEditor, JFXUtil.getIcon("file.png"))
    {
        {
           setOnAction(e -> openInExternalEditor());
        }
    };

    /** Data that is linked to the pvs_table */
    private final ObservableList<PVTableItem> pv_items = FXCollections.observableArrayList();

    /** Table for PVs of currently selected script */
    private TableView<PVTableItem> pvs_table;
    private TableColumn<PVTableItem, String> pvs_name_col;
    private TableColumn<PVTableItem, Boolean> pvs_trigger_col;

    private MenuButton addMenuButton;
    private SplitMenuButton btn_edit;
    private Button btn_script_remove;
    private Button btn_pv_add, btn_pv_remove, btn_pv_up, btn_py_down;

    private CheckBox btn_check_connections;

    /** The main splitter */
    private final SplitPane content;

    private ScriptItem selected_script_item = null;

    /** @param widget Widget
     *  @param scripts Scripts to show/edit in the dialog
     *  @param owner The node starting this dialog.
     */
    public ScriptsDialog (final Widget widget, final List<ScriptInfo> scripts, final Node owner)
    {
        this.widget = widget;

        setTitle(Messages.ScriptsDialog_Title);
        setHeaderText(Messages.ScriptsDialog_Info);

        scripts.forEach(script -> script_items.add(ScriptItem.forInfo(script)));
        fixupScripts(0);

        content = createContent();

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().getStylesheets().add(getClass().getResource("opibuilder.css").toExternalForm());
        setResizable(true);

        setResultConverter(button ->
        {
            if (button != ButtonType.OK)
                return null;
            return script_items.stream()
					           .filter(item -> ! item.file.get().isEmpty())
					           .map(ScriptItem::getScriptInfo)
					           .collect(Collectors.toList());
        });

        DialogHelper.positionAndSize(
            this,
            owner,
            PhoebusPreferenceService.userNodeForClass(ScriptsDialog.class),
            prefs -> content.setDividerPositions(prefs.getDouble("content.divider.position", 0.5)),
            prefs -> prefs.putDouble("content.divider.position", content.getDividerPositions()[0]));
    }

    private SplitPane createContent()
    {
        final Region scripts = createScriptsTable();
        final Region pvs = createPVsTable();

        // Display PVs of currently selected script
        scripts_table.getSelectionModel().selectedItemProperty().addListener((prop, old, selected) ->
        {
            selected_script_item = selected;
            if (selected == null)
            {
                btn_script_remove.setDisable(true);
                btn_edit.setDisable(true);
                pvs.setDisable(true);
                pv_items.clear();
            }
            else
            {
                btn_script_remove.setDisable(false);
                btn_edit.setDisable(false);
                pvs.setDisable(false);
                btn_check_connections.setSelected(selected.check_connections);
                pv_items.setAll(selected.pvs);

                fixupPVs(0);

                if (ScriptInfo.isEmbedded(selected.getScriptInfo().getPath()))
                {
                    scripts_table.setEditable(false);
                    btn_edit.setText(Messages.Edit);
                    btn_edit.setGraphic(JFXUtil.getIcon("edit.png"));

                    if (ScriptInfo.isJython(selected.getScriptInfo().getPath()))
                    {
                        convertToFileMenuItem.setDisable(false);
                        convertToEmbeddedPythonMenuItem.setDisable(true);
                        convertToEmbeddedJavaScriptMenuItem.setDisable(false);
                        openInExternalEditorMenuItem.setDisable(true);
                    }
                    else if (ScriptInfo.isJavaScript(selected.getScriptInfo().getPath()))
                    {
                        convertToFileMenuItem.setDisable(false);
                        convertToEmbeddedPythonMenuItem.setDisable(false);
                        convertToEmbeddedJavaScriptMenuItem.setDisable(true);
                        openInExternalEditorMenuItem.setDisable(true);
                    }
                    else
                    {
                        convertToFileMenuItem.setDisable(true);
                        convertToEmbeddedPythonMenuItem.setDisable(true);
                        convertToEmbeddedJavaScriptMenuItem.setDisable(true);
                        openInExternalEditorMenuItem.setDisable(true);
                    }
                }
                else
                {
                    scripts_table.setEditable(true);
                    btn_edit.setText(Messages.Select);
                    btn_edit.setGraphic(JFXUtil.getIcon("select-file.png"));
                    convertToFileMenuItem.setDisable(true);
                    convertToEmbeddedPythonMenuItem.setDisable(false);
                    convertToEmbeddedJavaScriptMenuItem.setDisable(false);
                    openInExternalEditorMenuItem.setDisable(!externalEditorExists());
                }
            }
        });

		// Update PVs of selected script from PVs table
        final ListChangeListener<PVTableItem> ll = change ->
        {
            final ScriptItem selected = scripts_table.getSelectionModel().getSelectedItem();
            if (selected != null)
                selected.pvs = new ArrayList<>(change.getList());
        };
        pv_items.addListener(ll);

        // Update buttons for currently selected PV
        pvs_table.getSelectionModel().selectedItemProperty().addListener( ( prop, old, selected ) ->
        {
            if (selected == null)
            {
                btn_pv_remove.setDisable(true);
                btn_pv_up.setDisable(true);
                btn_py_down.setDisable(true);
            }
            else
            {
                final TableViewSelectionModel<PVTableItem> model = pvs_table.getSelectionModel();
                btn_pv_remove.setDisable(false);
                btn_pv_up.setDisable(model.getSelectedIndex() == 0);
                btn_py_down.setDisable(model.getSelectedIndex() == pv_items.size() - 1);
            }
        });

        scripts.setPadding(new Insets(0, 10, 0, 0));
        pvs.setPadding(new Insets(0, 0, 0, 10));

        final SplitPane splitPane = new SplitPane(scripts, pvs);

        // Select the first script
        if ( !scripts_table.getItems().isEmpty())
        {
            Platform.runLater(() ->
            {
                scripts_table.getSelectionModel().select(0);
                scripts_table.requestFocus();
            });
        }
        else
            Platform.runLater(() -> addMenuButton.requestFocus());

        return splitPane;
    }

    /** @return Node for UI elements that edit the scripts */
    private Region createScriptsTable()
    {
        scripts_icon_col = new TableColumn<>();
        scripts_icon_col.setCellValueFactory(cdf-> new SimpleObjectProperty<>(getScriptImage(cdf.getValue()))
        {
            {
                bind(Bindings.createObjectBinding(() -> getScriptImage(cdf.getValue()), cdf.getValue().fileProperty()));
            }
        });
        scripts_icon_col.setCellFactory(col -> new TableCell<>()
        {
            /* Instance initializer. */
            {
                setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(final ImageView item, final boolean empty)
            {
                super.updateItem(item, empty);
                super.setGraphic(item);
            }
        });
        scripts_icon_col.setEditable(false);
        scripts_icon_col.setSortable(false);
        scripts_icon_col.setMaxWidth(25);
        scripts_icon_col.setMinWidth(25);

        // Create table with editable script 'file' column
        scripts_name_col = new TableColumn<>(Messages.ScriptsDialog_ColScript);
        scripts_name_col.setCellValueFactory(new PropertyValueFactory<ScriptItem, String>("file"));
        scripts_name_col.setCellFactory(list -> EditCell.createStringEditCell());

        scripts_name_col.setOnEditCommit(event ->
        {
            final int row = event.getTablePosition().getRow();
            script_items.get(row).file.set(event.getNewValue());
            fixupScripts(row);
        });

        scripts_table = new TableView<>(script_items);
        scripts_table.getColumns().add(scripts_icon_col);
        scripts_table.getColumns().add(scripts_name_col);
        scripts_table.setEditable(true);
        scripts_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        scripts_table.setTooltip(new Tooltip(Messages.ScriptsDialog_ScriptsTT));
        scripts_table.setPlaceholder(new Label(Messages.ScriptsDialog_NoScripts));

        // Buttons
        addMenuButton = new MenuButton(
           Messages.Add,
           JFXUtil.getIcon("add.png"),
           new MenuItem(Messages.AddPythonFile, JFXUtil.getIcon("file-python.png"))
           {
               {
                   setOnAction(e -> addPythonFile());
               }
           },
           new MenuItem(Messages.AddJavaScriptFile, JFXUtil.getIcon("file-javascript.png"))
           {
               {
                   setOnAction(e -> addJavaScriptFile());
               }
           },
           new SeparatorMenuItem(),
           new MenuItem(Messages.AddEmbeddedPython, JFXUtil.getIcon("python.png"))
           {
               {
                   setOnAction(e -> addEmbeddedJython());
               }
           },
           new MenuItem(Messages.AddEmbeddedJavaScript, JFXUtil.getIcon("javascript.png"))
           {
               {
                   setOnAction(e -> addEmbeddedJavaScript());
               }
           });
        addMenuButton.setMaxWidth(Double.MAX_VALUE);
        addMenuButton.setAlignment(Pos.CENTER_LEFT);

        btn_script_remove = new Button(Messages.Remove, JFXUtil.getIcon("delete.png"));
        btn_script_remove.setMaxWidth(Double.MAX_VALUE);
        btn_script_remove.setAlignment(Pos.CENTER_LEFT);
        btn_script_remove.setDisable(true);
        btn_script_remove.setOnAction(event ->
        {
            final int sel = scripts_table.getSelectionModel().getSelectedIndex();
            if (sel >= 0)
            {
                script_items.remove(sel);
                fixupScripts(sel);
            }
        });

        btn_edit = new SplitMenuButton(
            convertToFileMenuItem,
            new SeparatorMenuItem(),
            convertToEmbeddedPythonMenuItem,
            convertToEmbeddedJavaScriptMenuItem,
            new SeparatorMenuItem(),
            openInExternalEditorMenuItem);
        btn_edit.setText(Messages.Select);
        btn_edit.setGraphic(JFXUtil.getIcon("select-file.png"));
        btn_edit.setMaxWidth(Double.MAX_VALUE);
        btn_edit.setMinWidth(120);
        btn_edit.setAlignment(Pos.CENTER_LEFT);
        btn_edit.setDisable(true);
        btn_edit.setOnAction(e -> editOrSelect());

        final VBox buttons = new VBox(10, addMenuButton, btn_script_remove, btn_edit);
        final HBox content = new HBox(10, scripts_table, buttons);
        HBox.setHgrow(scripts_table, Priority.ALWAYS);
        HBox.setHgrow(buttons, Priority.NEVER);
        return content;
    }

    /** Fix scripts data: Delete empty rows in middle
     *  @param changed_row Row to check, and remove if it's empty
     */
    private void fixupScripts(final int changed_row)
    {
        // Check if edited row is now empty and should be deleted
        if (changed_row < script_items.size())
        {
            final ScriptItem item = script_items.get(changed_row);
            if (item.fileProperty().get().trim().isEmpty())
                script_items.remove(changed_row);
        }
    }

    /** @return Node for UI elements that edit the PVs of a script */
    private Region createPVsTable()
    {
        final TableColumn<PVTableItem, Integer> indexColumn = new TableColumn<>("#");
        indexColumn.setEditable(false);
        indexColumn.setSortable(false);
        indexColumn.setCellFactory(new LineNumberTableCellFactory<>(true));
        indexColumn.setMaxWidth(26);
        indexColumn.setMinWidth(26);

        // Create table with editable 'name' column
        pvs_name_col = new TableColumn<>(Messages.ScriptsDialog_ColPV);
        pvs_name_col.setSortable(false);
        pvs_name_col.setCellValueFactory(new PropertyValueFactory<PVTableItem, String>("name"));
        pvs_name_col.setCellFactory((col) -> new AutoCompletedTableCell(btn_pv_add));
        pvs_name_col.setOnEditCommit(event ->
        {
            final int row = event.getTablePosition().getRow();
            pv_items.get(row).nameProperty().set(event.getNewValue());
            fixupPVs(row);
        });

        pvs_trigger_col = new TableColumn<>(Messages.ScriptsDialog_ColTrigger);
        pvs_trigger_col.setSortable(false);
        pvs_trigger_col.setCellValueFactory(new PropertyValueFactory<PVTableItem, Boolean>("trigger"));
        pvs_trigger_col.setCellFactory(CheckBoxTableCell.<PVTableItem>forTableColumn(pvs_trigger_col));
        pvs_trigger_col.setResizable(false);
        pvs_trigger_col.setMaxWidth(70);
        pvs_trigger_col.setMinWidth(70);

        // Table column for 'trigger' uses CheckBoxTableCell that directly modifies the Observable Property
        pvs_table = new TableView<>(pv_items);
        pvs_table.getColumns().add(indexColumn);
        pvs_table.getColumns().add(pvs_name_col);
        pvs_table.getColumns().add(pvs_trigger_col);
        pvs_table.setEditable(true);
        pvs_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        pvs_table.setTooltip(new Tooltip(Messages.ScriptsDialog_PVsTT));
        pvs_table.setPlaceholder(new Label(Messages.ScriptsDialog_NoPVs));

        // Buttons
        btn_pv_add = new Button(Messages.Add, JFXUtil.getIcon("add.png"));
        btn_pv_add.setMaxWidth(Double.MAX_VALUE);
        btn_pv_add.setAlignment(Pos.CENTER_LEFT);
        btn_pv_add.setOnAction(event ->
        {
            final PVTableItem newItem = new PVTableItem("new-PV", true);
            pv_items.add(newItem);
            pvs_table.getSelectionModel().select(newItem);

            final int newRow = pvs_table.getSelectionModel().getSelectedIndex();
            ModelThreadPool.getTimer().schedule(() ->
            {
                Platform.runLater(() -> pvs_table.edit(newRow, pvs_name_col));
            }, 123, TimeUnit.MILLISECONDS);
        });

        btn_pv_remove = new Button(Messages.Remove, JFXUtil.getIcon("delete.png"));
        btn_pv_remove.setMaxWidth(Double.MAX_VALUE);
        btn_pv_remove.setMinWidth(96);
        btn_pv_remove.setAlignment(Pos.CENTER_LEFT);
        btn_pv_remove.setDisable(true);
        btn_pv_remove.setOnAction(event ->
        {
            final int sel = pvs_table.getSelectionModel().getSelectedIndex();
            if (sel >= 0)
            {
                pv_items.remove(sel);
                fixupPVs(sel);
            }
        });

        btn_pv_up = new Button(Messages.MoveUp, JFXUtil.getIcon("up.png"));
        btn_pv_up.setMaxWidth(Double.MAX_VALUE);
        btn_pv_up.setAlignment(Pos.CENTER_LEFT);
        btn_pv_up.setDisable(true);
        btn_pv_up.setOnAction(event -> TableHelper.move_item_up(pvs_table, pv_items));

        btn_py_down = new Button(Messages.MoveDown, JFXUtil.getIcon("down.png"));
        btn_py_down.setMaxWidth(Double.MAX_VALUE);
        btn_py_down.setAlignment(Pos.CENTER_LEFT);
        btn_py_down.setDisable(true);
        btn_py_down.setOnAction(event -> TableHelper.move_item_down(pvs_table, pv_items));

        btn_check_connections = new CheckBox(Messages.ScriptsDialog_CheckConnections);
        btn_check_connections.setSelected(true);
        btn_check_connections.setOnAction(event ->
        {
            selected_script_item.check_connections = btn_check_connections.isSelected();
        });

        final VBox buttons = new VBox(10, btn_pv_add, btn_pv_remove, btn_pv_up, btn_py_down);
        final HBox pvs_buttons = new HBox(10, pvs_table, buttons);
        HBox.setHgrow(pvs_table, Priority.ALWAYS);
        HBox.setHgrow(buttons, Priority.NEVER);

        final VBox content = new VBox(10, pvs_buttons, btn_check_connections);
        VBox.setVgrow(pvs_buttons, Priority.ALWAYS);

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

    private void add(final String file, final String text)
    {
        final ScriptItem newItem = new ScriptItem(file, text, true, new ArrayList<>());

        script_items.add(newItem);
        scripts_table.getSelectionModel().select(newItem);

        final int newRow = scripts_table.getSelectionModel().getSelectedIndex();

        ModelThreadPool.getTimer().schedule(() ->
        {
            if (ScriptInfo.isEmbedded(file))
            {
                Platform.runLater(() ->
                {
                    final MultiLineInputDialog dlg = new MultiLineInputDialog(scripts_table, selected_script_item.text);
                    dlg.showAndWait().ifPresent(result -> selected_script_item.text = result);
                });
            }
            else
                Platform.runLater(() -> scripts_table.edit(newRow, scripts_name_col));
        }, 123, TimeUnit.MILLISECONDS);

    }

    private void addEmbeddedJavaScript()
    {
        add(ScriptInfo.EMBEDDED_JAVASCRIPT, ScriptInfo.EXAMPLE_JAVASCRIPT);
    }

    private void addEmbeddedJython()
    {
        add(ScriptInfo.EMBEDDED_PYTHON, ScriptInfo.EXAMPLE_PYTHON);
    }

    private void addJavaScriptFile()
    {
        add(Messages.ScriptsDialog_JavaScriptScriptFile, ScriptInfo.EXAMPLE_JAVASCRIPT);
    }

    private void addPythonFile()
    {
        add(Messages.ScriptsDialog_PythonScriptFile, ScriptInfo.EXAMPLE_PYTHON);
    }

    private void convertToEmbeddedJavaScript()
    {
        if ( selected_script_item.text == null
          || selected_script_item.text.trim().isEmpty()
          || selected_script_item.text.trim().equals(ScriptInfo.EXAMPLE_PYTHON) )
        {
            selected_script_item.text = ScriptInfo.EXAMPLE_JAVASCRIPT;
        }

        selected_script_item.file.set(ScriptInfo.EMBEDDED_JAVASCRIPT);
        btn_edit.setText(Messages.Edit);
        btn_edit.setGraphic(JFXUtil.getIcon("edit.png"));
        convertToFileMenuItem.setDisable(false);
        convertToEmbeddedPythonMenuItem.setDisable(false);
        convertToEmbeddedJavaScriptMenuItem.setDisable(true);
        openInExternalEditorMenuItem.setDisable(true);
    }

    private void convertToEmbeddedPython()
    {
        if ( selected_script_item.text == null
          || selected_script_item.text.trim().isEmpty()
          || selected_script_item.text.trim().equals(ScriptInfo.EXAMPLE_JAVASCRIPT) )
        {
            selected_script_item.text = ScriptInfo.EXAMPLE_PYTHON;
        }

        selected_script_item.file.set(ScriptInfo.EMBEDDED_PYTHON);
        btn_edit.setText(Messages.Edit);
        btn_edit.setGraphic(JFXUtil.getIcon("edit.png"));
        convertToFileMenuItem.setDisable(false);
        convertToEmbeddedPythonMenuItem.setDisable(true);
        convertToEmbeddedJavaScriptMenuItem.setDisable(false);
        openInExternalEditorMenuItem.setDisable(true);
    }

    private void convertToScriptFile()
    {
        if (ScriptInfo.isEmbedded(selected_script_item.getScriptInfo().getPath()))
        {
            if ( ScriptInfo.isJython(selected_script_item.getScriptInfo().getPath()))
                selected_script_item.fileProperty().set(Messages.ScriptsDialog_PythonScriptFile);
            else
                selected_script_item.fileProperty().set(Messages.ScriptsDialog_JavaScriptScriptFile);

            btn_edit.setText(Messages.Select);
            btn_edit.setGraphic(JFXUtil.getIcon("select-file.png"));
            convertToFileMenuItem.setDisable(true);
            convertToEmbeddedPythonMenuItem.setDisable(false);
            convertToEmbeddedJavaScriptMenuItem.setDisable(false);
            openInExternalEditorMenuItem.setDisable(!externalEditorExists());
        }
    }

    private boolean externalEditorExists()
    {
        return null != ApplicationLauncherService.findApplication(ResourceParser.getURI(new File(selected_script_item.file.get())), false, null);
    }

    private void openInExternalEditor()
    {
        String resolved;
        try
        {
            String path = MacroHandler.replace(widget.getMacrosOrProperties(), selected_script_item.getScriptInfo().getPath());
            resolved = ModelResourceUtil.resolveResource(widget.getDisplayModel(), path);
            File file = new File(resolved);
            ApplicationLauncherService.openFile(file, true, (Stage)this.getOwner());
        }
        catch (Exception e)
        {
            logger.warning("Cannot resolve resource " + selected_script_item.getScriptInfo().getPath());
            return;
        }
    }

    private void editOrSelect()
    {
        if (Messages.Select.equals(btn_edit.getText()))
        {
            try
            {
                // Use the script file name, except if that's the example name,
                // because it doesn't exist and file dialog will then start
                // in some random directory instead of the display file's dir.
                String initial = selected_script_item.file.get();
                if ( Messages.ScriptsDialog_DefaultScriptFile.equals(initial))
                    initial = "";

                final String path = FilenameSupport.promptForRelativePath(widget, initial);
                if (path != null)
                {
                    selected_script_item.file.set(path);
                    selected_script_item.text = null;
                }
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot prompt for filename", ex);
            }
            FilenameSupport.performMostAwfulTerribleNoGoodHack(scripts_table);
        }
        else
        {
            final MultiLineInputDialog dlg = new MultiLineInputDialog(scripts_table, selected_script_item.text);
            dlg.showAndWait().ifPresent(result -> selected_script_item.text = result);
        }
    }

    private ImageView getScriptImage(ScriptItem item)
    {
        ImageView imageView = null;

        if (item != null)
        {
            final ScriptInfo info = item.getScriptInfo();
            if (info != null)
            {
                final String path = info.getPath();
                if (ScriptInfo.isEmbedded(path))
                {
                    if (ScriptInfo.isJavaScript(path))
                        imageView = JFXUtil.getIcon("javascript.png");
                    else if (ScriptInfo.isJython(path))
                        imageView = JFXUtil.getIcon("python.png");
                    else
                        // It should never happen.
                        imageView = JFXUtil.getIcon("unknown.png");
                }
                else
                {
                    if (ScriptInfo.isJavaScript(path))
                        imageView = JFXUtil.getIcon("file-javascript.png");
                    else if (ScriptInfo.isJython(path))
                        imageView = JFXUtil.getIcon("file-python.png");
                    else
                        // It should never happen.
                        imageView = JFXUtil.getIcon("file-unknown.png");
                }
            }
        }

        if (imageView != null)
        {
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
        }

        return imageView;
    }
}
