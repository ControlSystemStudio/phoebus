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

package org.csstudio.display.builder.representation.javafx.actionsdialog;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.io.File;
import java.util.Collections;
import java.util.logging.Level;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.ExecuteScriptActionInfo;
import org.csstudio.display.builder.model.properties.ScriptInfo;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.representation.javafx.FilenameSupport;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.ui.application.ApplicationLauncherService;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/** FXML Controller */
public class ExecuteScriptDetailsController implements ActionDetailsController{

    private ExecuteScriptActionInfo executeScriptActionInfo;

    private Widget widget;

    @FXML
    private TextField description;
    @FXML
    private TextField scriptPath;
    @FXML
    private TextArea scriptBody;
    @FXML
    private Button embedPyButton;
    @FXML
    private Button embedJsButton;
    @FXML
    private Button openExternalEditorButton;

    private StringProperty descriptionProperty = new SimpleStringProperty();
    private StringProperty scriptPathProperty = new SimpleStringProperty();
    private StringProperty scriptBodyPorperty = new SimpleStringProperty();

    /** @param widget Widget
     *  @param actionInfo Action info
     */
    public ExecuteScriptDetailsController(Widget widget, ActionInfo actionInfo){
        this.widget = widget;
        this.executeScriptActionInfo = (ExecuteScriptActionInfo)actionInfo;
    }

    /** Init */
    @FXML
    public void initialize(){
        descriptionProperty.setValue(executeScriptActionInfo.getDescription());
        scriptPathProperty.setValue(executeScriptActionInfo.getInfo().getPath());
        scriptBodyPorperty.setValue(executeScriptActionInfo.getInfo().getText());
        embedPyButton.setGraphic(JFXUtil.getIcon("embedded_script.png"));
        embedJsButton.setGraphic(JFXUtil.getIcon("embedded_script.png"));
        openExternalEditorButton.setGraphic(JFXUtil.getIcon("embedded_script.png"));
        openExternalEditorButton.setDisable(!externalEditorExists());

        description.textProperty().bindBidirectional(descriptionProperty);
        scriptPath.textProperty().bindBidirectional(scriptPathProperty);
        scriptBody.textProperty().bindBidirectional(scriptBodyPorperty);
        scriptBody.setDisable(executeScriptActionInfo.getInfo().getPath() != null &&
                !executeScriptActionInfo.getInfo().getPath().isEmpty() &&
                !executeScriptActionInfo.getInfo().getPath().equals(ScriptInfo.EMBEDDED_PYTHON) &&
                !executeScriptActionInfo.getInfo().getPath().equals(ScriptInfo.EMBEDDED_JAVASCRIPT));
    }

    /** Select embedded Py */
    @FXML
    public void embedPy(){
        scriptPathProperty.setValue(ScriptInfo.EMBEDDED_PYTHON);
        final String text = scriptBodyPorperty.get();
        if (text == null ||
                text.trim().isEmpty() ||
                text.trim().equals(ScriptInfo.EXAMPLE_JAVASCRIPT) )
            scriptBodyPorperty.setValue(ScriptInfo.EXAMPLE_PYTHON);
        openExternalEditorButton.setDisable(true);
        scriptBody.setDisable(false);
    }

    /** Select embedded Js */
    @FXML
    public void embedJs(){
        scriptPathProperty.setValue(ScriptInfo.EMBEDDED_JAVASCRIPT);
        final String text = scriptBodyPorperty.get();
        if (text == null ||
                text.trim().isEmpty() ||
                text.trim().equals(ScriptInfo.EXAMPLE_PYTHON) )
            scriptBodyPorperty.setValue(ScriptInfo.EXAMPLE_JAVASCRIPT);
        openExternalEditorButton.setDisable(true);
        scriptBody.setDisable(false);
    }

    /** Open external script file */
    @FXML
    public void openExternalEditor(){
        String resolved;
        try
        {
            String path = MacroHandler.replace(widget.getMacrosOrProperties(), scriptPathProperty.get());
            resolved = ModelResourceUtil.resolveResource(widget.getDisplayModel(), path);
            File file = new File(resolved);
            ApplicationLauncherService.openFile(file, true, null);
        }
        catch (Exception e)
        {
            logger.warning("Cannot resolve resource " + scriptPathProperty.get());
            return;
        }
    }

    /** Select external script file */
    @FXML
    public void selectScriptFile(){
        try
        {
            final String path = FilenameSupport.promptForRelativePath(widget, scriptPathProperty.get());
            if (path != null)
            {
                scriptPathProperty.setValue(path);
                openExternalEditorButton.setDisable(!externalEditorExists());
                scriptBody.setDisable(true);
                scriptBodyPorperty.set("");
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot prompt for filename", ex);
        }
    }

    /** @return Whether an external Python or Javascript editor is configured */
    private boolean externalEditorExists()
    {
        return null !=
                ApplicationLauncherService.findApplication(ResourceParser.getURI(new File(scriptPathProperty.get())), false, null);
    }

    /** @return ActionInfo */
    @Override
    public ActionInfo getActionInfo(){
        final String text = (scriptPathProperty.get().equals(ScriptInfo.EMBEDDED_PYTHON) ||
                scriptPathProperty.get().equals(ScriptInfo.EMBEDDED_JAVASCRIPT))
                ? scriptBodyPorperty.get()
                : null;
        ScriptInfo scriptInfo = new ScriptInfo(scriptPathProperty.get(),
                text,
                false,
                Collections.emptyList());
        return new ExecuteScriptActionInfo(descriptionProperty.get(), scriptInfo);
    }
}
