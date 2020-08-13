/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.ActionInfo.ActionType;
import org.csstudio.display.builder.model.properties.ActionInfos;
import org.csstudio.display.builder.model.properties.ExecuteCommandActionInfo;
import org.csstudio.display.builder.model.properties.ExecuteScriptActionInfo;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo.Target;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.model.properties.OpenFileActionInfo;
import org.csstudio.display.builder.model.properties.OpenWebpageActionInfo;
import org.csstudio.display.builder.model.properties.ScriptInfo;
import org.csstudio.display.builder.model.properties.WritePVActionInfo;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.macros.Macros;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.ui.application.ApplicationLauncherService;
import org.phoebus.ui.autocomplete.PVAutocompleteMenu;
import org.phoebus.ui.dialog.DialogHelper;

import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Dialog for editing {@link ActionInfo} list
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ActionsDialog extends Dialog<ActionInfos>
{
    // XXX: Smoother handling of script type changes
    // Prompt if embedded text should be deleted when changing to external file
    // Read existing file into embedded text when switching from file to embedded

    private final Widget widget;

    /** Actions edited by the dialog */
    private final ObservableList<ActionInfo> actions = FXCollections.observableArrayList();

    /** Currently selected item in <code>actions</code> */
    private int selected_action_index = -1;

    /** Table of actions */
    private final ListView<ActionInfo> action_list = new ListView<>(actions);
    private final CheckBox execute_all = new CheckBox(Messages.ActionsDialog_ExecuteAll);


    // UI elements for OpenDisplayAction
    private final TextField open_display_description = new TextField(),
                            open_display_path = new TextField(),
                            open_display_pane = new TextField();
    private ToggleGroup open_display_targets;
    private MacrosTable open_display_macros;

    // UI elements for WritePVAction
    private final TextField write_pv_description = new TextField(),
                            write_pv_name = new TextField(),
                            write_pv_value = new TextField();

    // UI elements for ExecuteScriptAction
    private final TextField execute_script_description = new TextField(),
                            execute_script_file = new TextField();
    private final TextArea execute_script_text = new TextArea();
    private final Button btn_external_editor = new Button();

    // UI elements for ExecuteCommandAction
    private final TextField execute_command_description = new TextField(),
                            execute_command_file = new TextField();

    // UI elements for OpenFileAction
    private final TextField open_file_description = new TextField(),
                            open_file_file = new TextField();

    // UI elements for OpenWebpageAction
    private final TextField open_web_description = new TextField(),
                            open_web_url = new TextField();

    /** Prevent circular updates */
    private boolean updating = false;

    /** ListView cell for ActionInfo, shows title if possible */
    private static class ActionInfoCell extends ListCell<ActionInfo>
    {
        @Override
        protected void updateItem(final ActionInfo action, final boolean empty)
        {
            super.updateItem(action, empty);
            try
            {
                if (action == null)
                {
                    setText("");
                    setGraphic(null);
                }
                else
                {
                    setText(action.toString());
                    setGraphic(new ImageView(new Image(action.getType().getIconURL().toExternalForm())));
                }
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Error displaying " + action, ex);
            }
        }
    };

    /** Create dialog
     *  @param widget Widget
     *  @param initial_actions Initial list of actions
     *  @param owner Node that started this dialog
     */
    public ActionsDialog(final Widget widget, final ActionInfos initial_actions, final Node owner)
    {
        this.widget = widget;

        actions.addAll(initial_actions.getActions());

        setTitle(Messages.ActionsDialog_Title);
        setHeaderText(Messages.ActionsDialog_Info);

        // Actions:           Action Detail:
        // | List |  [Add]    |  Pane       |
        // | List |  [Remove] |  Pane       |
        // | List |           |  Pane       |
        //
        // Inside Action Detail pane, only one the *_details sub-pane
        // suitable for the selected action is visible.
        final GridPane layout = new GridPane();
        // layout.setGridLinesVisible(true); // For debugging
        layout.setHgap(10);
        layout.setVgap(10);
        layout.setPadding(new Insets(10));

        // Left "Actions" column of UI
        layout.add(new Label(Messages.ActionsDialog_Actions), 0, 0);

        action_list.setCellFactory(view -> new ActionInfoCell());
        layout.add(action_list, 0, 1);

        execute_all.setSelected(initial_actions.isExecutedAsOne());
        layout.add(execute_all, 0, 2);

        GridPane.setVgrow(action_list, Priority.ALWAYS);
        GridPane.setVgrow(execute_all, Priority.NEVER);

        // Middle button column of UI
        final MenuButton add = new MenuButton(Messages.Add, JFXUtil.getIcon("add.png"));
        for (ActionType type : ActionType.values())
        {
            final ImageView icon = new ImageView(new Image(type.getIconURL().toExternalForm()));
            final MenuItem item = new MenuItem(type.toString(), icon);
            item.setOnAction(event ->
            {
                final ActionInfo new_action = ActionInfo.createAction(type);
                actions.add(new_action);
                action_list.getSelectionModel().select(new_action);
            });
            add.getItems().add(item);
        }
        add.setMaxWidth(Double.MAX_VALUE);

        final Button remove = new Button(Messages.Remove, JFXUtil.getIcon("delete.png"));
        remove.setMaxWidth(Double.MAX_VALUE);
        remove.setOnAction(event ->
        {
            if (selected_action_index >= 0  &&  selected_action_index < actions.size())
                actions.remove(selected_action_index);
        });

        final Button up = new Button(Messages.MoveUp, JFXUtil.getIcon("up.png"));
        up.setMaxWidth(Double.MAX_VALUE);
        up.setOnAction(event ->
        {
            if (selected_action_index > 0  &&  selected_action_index < actions.size())
            {
                updating = true;
                try
                {
                    final ActionInfo item = actions.remove(selected_action_index);
                    -- selected_action_index;
                    actions.add(selected_action_index, item);
                    action_list.getSelectionModel().select(item);
                }
                finally
                {
                    updating = false;
                }
            }
        });

        final Button down = new Button(Messages.MoveDown, JFXUtil.getIcon("down.png"));
        down.setMaxWidth(Double.MAX_VALUE);
        down.setOnAction(event ->
        {
            if (selected_action_index >= 0  &&  selected_action_index < actions.size() - 1)
            {
                updating = true;
                try
                {
                    final ActionInfo item = actions.remove(selected_action_index);
                    ++ selected_action_index;
                    actions.add(selected_action_index, item);
                    action_list.getSelectionModel().select(item);
                }
                finally
                {
                    updating = false;
                }
            }
        });

        final VBox buttons = new VBox(10, add, remove, up, down);
        layout.add(buttons, 1, 1);

        // Right "Action Detail" column of UI
        layout.add(new Label(Messages.ActionsDialog_Detail), 2, 0);

        final GridPane open_display_details = createOpenDisplayDetails();
        final GridPane write_pv_details = createWritePVDetails();
        final GridPane execute_script_details = createExecuteScriptDetails();
        final GridPane execute_command_details = createExecuteCommandDetails();
        final GridPane open_file_details = createOpenFileDetails();
        final GridPane open_web_details = createOpenWebDetails();

        open_display_details.setVisible(false);
        write_pv_details.setVisible(false);
        execute_script_details.setVisible(false);
        execute_command_details.setVisible(false);
        open_file_details.setVisible(false);
        open_web_details.setVisible(false);

        final StackPane details = new StackPane(open_display_details, write_pv_details,
                                                execute_script_details, execute_command_details,
                                                open_file_details, open_web_details);
        layout.add(details, 2, 1, 1, 2);
        GridPane.setHgrow(details, Priority.ALWAYS);
        GridPane.setVgrow(details, Priority.ALWAYS);

        getDialogPane().setContent(layout);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().getStylesheets().add(getClass().getResource("opibuilder.css").toExternalForm());
        setResizable(true);

        // Show and initialize *_details sub-pane for selected action
        action_list.getSelectionModel().selectedItemProperty().addListener((l, old, action) ->
        {
            if (updating)
                return;
            final int selection = action_list.getSelectionModel().getSelectedIndex();
            if (selection < 0)
            {   // Selection was lost because user clicked on some other UI element.
                // If previously selected index is otherwise still valid, keep it
                if (selected_action_index >= 0  &&  selected_action_index < actions.size())
                    return;
            }
            selected_action_index = selection;
            if (action instanceof OpenDisplayActionInfo)
            {
                selectStackPane(details, open_display_details);
                showOpenDisplayAction((OpenDisplayActionInfo) action);
            }
            else if (action instanceof WritePVActionInfo)
            {
                selectStackPane(details, write_pv_details);
                showWritePVAction((WritePVActionInfo) action);
            }
            else if (action instanceof ExecuteScriptActionInfo)
            {
                selectStackPane(details, execute_script_details);
                showExecuteScriptAction((ExecuteScriptActionInfo)action);
            }
            else if (action instanceof ExecuteCommandActionInfo)
            {
                selectStackPane(details, execute_command_details);
                showExecuteCommandAction((ExecuteCommandActionInfo)action);
            }
            else if (action instanceof OpenFileActionInfo)
            {
                selectStackPane(details, open_file_details);
                showOpenFileAction((OpenFileActionInfo)action);
            }
            else if (action instanceof OpenWebpageActionInfo)
            {
                selectStackPane(details, open_web_details);
                showOpenWebAction((OpenWebpageActionInfo)action);
            }
            else
                selectStackPane(details, null);
        });

        setResultConverter(button ->
        {
            if (button == ButtonType.OK)
                return new ActionInfos(actions, execute_all.isSelected());
            return null;
        });

        // Select first action, if there is one
        if (actions.size() > 0)
            action_list.getSelectionModel().select(0);

        DialogHelper.positionAndSize(this, owner,
                                     PhoebusPreferenceService.userNodeForClass(ActionsDialog.class));
    }

    /** @param details StackPane
     *  @param active Child pane to show, all others are hidden
     */
    private void selectStackPane(final StackPane details, final GridPane active)
    {
        for (Node pane : details.getChildren())
            pane.setVisible(pane == active);
    }

    /** @return Sub-pane for OpenDisplay action */
    private GridPane createOpenDisplayDetails()
    {
        final InvalidationListener update = whatever ->
        {
            if (updating  ||  selected_action_index < 0)
                return;
            actions.set(selected_action_index, getOpenDisplayAction());
        };

        final GridPane open_display_details = new GridPane();
        // open_display_details.setGridLinesVisible(true);
        open_display_details.setHgap(10);
        open_display_details.setVgap(10);

        open_display_details.add(new Label(Messages.ActionsDialog_Description), 0, 0);
        open_display_description.textProperty().addListener(update);
        open_display_details.add(open_display_description, 1, 0);
        GridPane.setHgrow(open_display_description, Priority.ALWAYS);

        open_display_details.add(new Label(Messages.ActionsDialog_DisplayPath), 0, 1);
        open_display_path.textProperty().addListener(update);
        final Button select = new Button("...");
        select.setOnAction(event ->
        {
            try
            {
                final String path = FilenameSupport.promptForRelativePath(widget, open_display_path.getText());
                if (path != null)
                    open_display_path.setText(path);
                FilenameSupport.performMostAwfulTerribleNoGoodHack(action_list);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot prompt for filename", ex);
            }
        });
        final HBox path_box = new HBox(open_display_path, select);
        HBox.setHgrow(open_display_path, Priority.ALWAYS);
        open_display_details.add(path_box, 1, 1);

        final HBox modes_box = new HBox(10);
        open_display_targets = new ToggleGroup();
        final Target[] modes = Target.values();
        for (int i=0; i<modes.length; ++i)
        {
            // Suppress deprecated legacy mode which is handled as WINDOW
            if (modes[i] == Target.STANDALONE)
                continue;

            final RadioButton target = new RadioButton(modes[i].toString());
            target.setToggleGroup(open_display_targets);
            target.selectedProperty().addListener(update);
            modes_box.getChildren().add(target);

            if (modes[i] == Target.TAB)
                open_display_pane.disableProperty().bind(target.selectedProperty().not());
        }
        open_display_pane.textProperty().addListener(update);
        open_display_details.add(modes_box, 0, 2, 2, 1);

        open_display_details.add(new Label("Pane:"), 0, 3);
        open_display_details.add(open_display_pane, 1, 3);

        open_display_macros = new MacrosTable(new Macros());
        open_display_macros.addListener(update);
        open_display_details.add(open_display_macros.getNode(), 0, 4, 2, 1);
        GridPane.setHgrow(open_display_macros.getNode(), Priority.ALWAYS);
        GridPane.setVgrow(open_display_macros.getNode(), Priority.ALWAYS);

        return open_display_details;
    }

    /** @param info {@link OpenDisplayActionInfo} to show */
    private void showOpenDisplayAction(final OpenDisplayActionInfo info)
    {
        updating = true;
        try
        {
            open_display_description.setText(info.getDescription());
            open_display_path.setText(info.getFile());
            // Mapping is needed if bob file was created in CS Studio/Eclipse
            Target target = info.getTarget();
            if(target.equals(Target.STANDALONE)){
                target = Target.WINDOW;
            }
            open_display_targets.getToggles().get(target.ordinal()).setSelected(true);
            open_display_pane.setText(info.getPane());
            open_display_macros.setMacros(info.getMacros());
        }
        finally
        {
            updating = false;
        }
    }

    /** @return {@link OpenDisplayActionInfo} from sub pane */
    private OpenDisplayActionInfo getOpenDisplayAction()
    {
        Target target = Target.REPLACE;
        List<Toggle> modes = open_display_targets.getToggles();
        for (int i=0; i<modes.size(); ++i)
            if (modes.get(i).isSelected())
            {
                target = Target.values()[i];
                break;
            }

        return new OpenDisplayActionInfo(open_display_description.getText(),
                                         open_display_path.getText().trim(),
                                         open_display_macros.getMacros(),
                                         target,
                                         open_display_pane.getText().trim());
    }

    /** @return Sub-pane for WritePV action */
    private GridPane createWritePVDetails()
    {
        final InvalidationListener update = whatever ->
        {
            if (updating  ||  selected_action_index < 0)
                return;
            actions.set(selected_action_index, getWritePVAction());
        };

        final GridPane write_pv_details = new GridPane();
        write_pv_details.setHgap(10);
        write_pv_details.setVgap(10);

        write_pv_details.add(new Label(Messages.ActionsDialog_Description), 0, 0);
        write_pv_description.textProperty().addListener(update);
        write_pv_details.add(write_pv_description, 1, 0);
        GridPane.setHgrow(write_pv_description, Priority.ALWAYS);

        write_pv_details.add(new Label(Messages.ActionsDialog_PVName), 0, 1);
        PVAutocompleteMenu.INSTANCE.attachField(write_pv_name);
        write_pv_name.textProperty().addListener(update);
        write_pv_details.add(write_pv_name, 1, 1);

        write_pv_details.add(new Label(Messages.ActionsDialog_Value), 0, 2);
        write_pv_value.textProperty().addListener(update);
        write_pv_details.add(write_pv_value, 1, 2);

        return write_pv_details;
    }

    /** @param action {@link WritePVActionInfo} to show */
    private void showWritePVAction(final WritePVActionInfo action)
    {
        updating = true;
        try
        {
            write_pv_description.setText(action.getDescription());
            write_pv_name.setText(action.getPV());
            write_pv_value.setText(action.getValue());
        }
        finally
        {
            updating = false;
        }
    }

    /** @return {@link WritePVActionInfo} from sub pane */
    private WritePVActionInfo getWritePVAction()
    {
        return new WritePVActionInfo(write_pv_description.getText(),
                                     write_pv_name.getText(),
                                     write_pv_value.getText());
    }

    /** @return Sub-pane for ExecuteScript action */
    private GridPane createExecuteScriptDetails()
    {
        final InvalidationListener update = whatever ->
        {
            if (updating  ||  selected_action_index < 0)
                return;
            actions.set(selected_action_index, getExecuteScriptAction());
        };

        final GridPane execute_script_details = new GridPane();
        execute_script_details.setHgap(10);
        execute_script_details.setVgap(10);

        execute_script_details.add(new Label(Messages.ActionsDialog_Description), 0, 0);
        execute_script_description.textProperty().addListener(update);
        execute_script_details.add(execute_script_description, 1, 0);
        GridPane.setHgrow(execute_script_description, Priority.ALWAYS);

        execute_script_details.add(new Label(Messages.ActionsDialog_ScriptPath), 0, 1);
        execute_script_file.textProperty().addListener(update);
        final Button select = new Button("...");
        select.setOnAction(event ->
        {
            try
            {
                final String path = FilenameSupport.promptForRelativePath(widget, execute_script_file.getText());
                if (path != null)
                {
                    execute_script_file.setText(path);
                    btn_external_editor.setDisable(!externalEditorExists());
                }
                FilenameSupport.performMostAwfulTerribleNoGoodHack(action_list);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot prompt for filename", ex);
            }
        });
        final HBox path_box = new HBox(execute_script_file, select);
        HBox.setHgrow(execute_script_file, Priority.ALWAYS);
        execute_script_details.add(path_box, 1, 1);

        final Button btn_embed_py = new Button(Messages.ScriptsDialog_BtnEmbedPy, JFXUtil.getIcon("embedded_script.png"));
        btn_embed_py.setOnAction(event ->
        {
            execute_script_file.setText(ScriptInfo.EMBEDDED_PYTHON);
            final String text = execute_script_text.getText();
            if (text == null ||
                text.trim().isEmpty() ||
                text.trim().equals(ScriptInfo.EXAMPLE_JAVASCRIPT) )
                execute_script_text.setText(ScriptInfo.EXAMPLE_PYTHON);
                btn_external_editor.setDisable(true);
        });

        final Button btn_embed_js = new Button(Messages.ScriptsDialog_BtnEmbedJS, JFXUtil.getIcon("embedded_script.png"));
        btn_embed_js.setOnAction(event ->
        {
            execute_script_file.setText(ScriptInfo.EMBEDDED_JAVASCRIPT);
            final String text = execute_script_text.getText();
            if (text == null ||
                text.trim().isEmpty() ||
                text.trim().equals(ScriptInfo.EXAMPLE_PYTHON) )
                execute_script_text.setText(ScriptInfo.EXAMPLE_JAVASCRIPT);
                btn_external_editor.setDisable(true);
        });

        btn_external_editor.setText(Messages.OpenInExternalEditor);
        btn_external_editor.setGraphic(JFXUtil.getIcon("file.png"));
        btn_external_editor.setOnAction(event -> openInExternalEditor());
        btn_external_editor.setDisable(true);

        execute_script_details.add(new HBox(10, btn_embed_py, btn_embed_js, btn_external_editor), 1, 2);
        execute_script_details.add(new Label(Messages.ActionsDialog_ScriptText), 0, 3);
        execute_script_text.setText(null);
        execute_script_text.textProperty().addListener(update);
        execute_script_details.add(execute_script_text, 0, 4, 2, 1);
        GridPane.setVgrow(execute_script_text, Priority.ALWAYS);

        return execute_script_details;
    }

    /** @return Whether an external Python or Javascript editor is configured */
    private boolean externalEditorExists()
    {
        return null != ApplicationLauncherService.findApplication(ResourceParser.getURI(new File(execute_script_file.getText())), false, null);
    }

    /** Open script file in a configured external Python or Javascript editor */
    private void openInExternalEditor()
    {
        String resolved;
        try
        {
            String path = MacroHandler.replace(widget.getMacrosOrProperties(), execute_script_file.getText());
            resolved = ModelResourceUtil.resolveResource(widget.getDisplayModel(), path);
            File file = new File(resolved);
            ApplicationLauncherService.openFile(file, true, (Stage)this.getOwner());
        }
        catch (Exception e)
        {
            logger.warning("Cannot resolve resource " + execute_script_file.getText());
            return;
        }
    }

    /** @param action {@link ExecuteScriptActionInfo} to show */
    private void showExecuteScriptAction(final ExecuteScriptActionInfo action)
    {
        updating = true;
        try
        {
            execute_script_description.setText(action.getDescription());
            execute_script_file.setText(action.getInfo().getPath());
            execute_script_text.setText(action.getInfo().getText());
            btn_external_editor.setDisable(!externalEditorExists()
                    || action.getInfo().getPath().equals(ScriptInfo.EMBEDDED_PYTHON)
                    || action.getInfo().getPath().equals(ScriptInfo.EMBEDDED_JAVASCRIPT));
        }
        finally
        {
            updating = false;
        }
    }

    /** @return {@link ExecuteScriptActionInfo} from sub pane */
    private ExecuteScriptActionInfo getExecuteScriptAction()
    {
        final String file = execute_script_file.getText();
        final String text = (file.equals(ScriptInfo.EMBEDDED_PYTHON) ||
                             file.equals(ScriptInfo.EMBEDDED_JAVASCRIPT))
                            ? execute_script_text.getText()
                            : null;
        return new ExecuteScriptActionInfo(execute_script_description.getText(),
                                           new ScriptInfo(file, text, false, Collections.emptyList()));
    }

    /** @return Sub-pane for ExecuteCommand action */
    private GridPane createExecuteCommandDetails()
    {
        final InvalidationListener update = whatever ->
        {
            if (updating  ||  selected_action_index < 0)
                return;
            actions.set(selected_action_index, getExecuteCommandAction());
        };

        final GridPane execute_command_details = new GridPane();
        execute_command_details.setHgap(10);
        execute_command_details.setVgap(10);

        execute_command_details.add(new Label(Messages.ActionsDialog_Description), 0, 0);
        execute_command_description.textProperty().addListener(update);
        execute_command_details.add(execute_command_description, 1, 0);
        GridPane.setHgrow(execute_command_description, Priority.ALWAYS);

        execute_command_details.add(new Label(Messages.ActionsDialog_Command), 0, 1);
        execute_command_file.textProperty().addListener(update);
        final Button select = new Button("...");
        select.setOnAction(event ->
        {
            try
            {
                final String path = FilenameSupport.promptForRelativePath(widget, execute_command_file.getText());
                if (path != null)
                    execute_command_file.setText(path);
                FilenameSupport.performMostAwfulTerribleNoGoodHack(action_list);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot prompt for filename", ex);
            }
        });
        final HBox path_box = new HBox(execute_command_file, select);
        HBox.setHgrow(execute_command_file, Priority.ALWAYS);
        execute_command_details.add(path_box, 1, 1);

        return execute_command_details;
    }

    /** @param action {@link ExecuteCommandActionInfo} to show */
    private void showExecuteCommandAction(final ExecuteCommandActionInfo action)
    {
        updating = true;
        try
        {
            execute_command_description.setText(action.getDescription());
            execute_command_file.setText(action.getCommand());
        }
        finally
        {
            updating = false;
        }
    }

    /** @return {@link ExecuteCommandActionInfo} from sub pane */
    private ExecuteCommandActionInfo getExecuteCommandAction()
    {
        return new ExecuteCommandActionInfo(execute_command_description.getText(),
                                            execute_command_file.getText());
    }

    /** @return Sub-pane for OpenFile action */
    private GridPane createOpenFileDetails()
    {
        final InvalidationListener update = whatever ->
        {
            if (updating  ||  selected_action_index < 0)
                return;
            actions.set(selected_action_index, getOpenFileAction());
        };

        final GridPane open_file_details = new GridPane();
        open_file_details.setHgap(10);
        open_file_details.setVgap(10);

        open_file_details.add(new Label(Messages.ActionsDialog_Description), 0, 0);
        open_file_description.textProperty().addListener(update);
        open_file_details.add(open_file_description, 1, 0);
        GridPane.setHgrow(open_file_description, Priority.ALWAYS);

        open_file_details.add(new Label(Messages.ActionsDialog_FilePath), 0, 1);
        open_file_file.textProperty().addListener(update);
        final Button select = new Button("...");
        select.setOnAction(event ->
        {
            try
            {
                final String path = FilenameSupport.promptForRelativePath(widget, open_file_file.getText());
                if (path != null)
                    open_file_file.setText(path);
                FilenameSupport.performMostAwfulTerribleNoGoodHack(action_list);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot prompt for filename", ex);
            }
        });
        final HBox path_box = new HBox(open_file_file, select);
        HBox.setHgrow(open_file_file, Priority.ALWAYS);
        open_file_details.add(path_box, 1, 1);

        return open_file_details;
    }

    /** @param action {@link OpenFileActionInfo} to show */
    private void showOpenFileAction(final OpenFileActionInfo action)
    {
        updating = true;
        try
        {
            open_file_description.setText(action.getDescription());
            open_file_file.setText(action.getFile());
        }
        finally
        {
            updating = false;
        }
    }

    /** @return {@link OpenFileActionInfo} from sub pane */
    private OpenFileActionInfo getOpenFileAction()
    {
        return new OpenFileActionInfo(open_file_description.getText(),
                                      open_file_file.getText().trim());
    }

    /** @return Sub-pane for OpenWeb action */
    private GridPane createOpenWebDetails()
    {
        final InvalidationListener update = whatever ->
        {
            if (updating  ||  selected_action_index < 0)
                return;
            actions.set(selected_action_index, getOpenWebAction());
        };

        final GridPane open_web_details = new GridPane();
        open_web_details.setHgap(10);
        open_web_details.setVgap(10);

        open_web_details.add(new Label(Messages.ActionsDialog_Description), 0, 0);
        open_web_description.textProperty().addListener(update);
        open_web_details.add(open_web_description, 1, 0);
        GridPane.setHgrow(open_web_description, Priority.ALWAYS);

        open_web_details.add(new Label(Messages.ActionsDialog_URL), 0, 1);
        open_web_url.textProperty().addListener(update);
        open_web_details.add(open_web_url, 1, 1);

        return open_web_details;
    }

    /** @param action {@link OpenWebpageActionInfo} to show */
    private void showOpenWebAction(final OpenWebpageActionInfo action)
    {
        updating = true;
        try
        {
            open_web_description.setText(action.getDescription());
            open_web_url.setText(action.getURL());
        }
        finally
        {
            updating = false;
        }
    }

    /** @return {@link OpenWebpageActionInfo} from sub pane */
    private OpenWebpageActionInfo getOpenWebAction()
    {
        return new OpenWebpageActionInfo(open_web_description.getText(),
                                         open_web_url.getText().trim());
    }
}
