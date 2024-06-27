/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.csstudio.display.actions;


import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.binding.Bindings;
import org.csstudio.display.builder.model.ActionControllerBase;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ScriptInfo;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.representation.javafx.FilenameSupport;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.ui.application.ApplicationLauncherService;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.phoebus.ui.javafx.ImageCache;

/**
 * FXML Controller for the execute script action editor.
 */
public class ExecuteScriptActionController extends ActionControllerBase {

    private final ExecuteScriptAction executeScriptActionInfo;

    private final Widget widget;

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

    private final StringProperty scriptPathProperty = new SimpleStringProperty();
    private final StringProperty scriptBodyProperty = new SimpleStringProperty();

    private final Logger logger =
            Logger.getLogger(ExecuteScriptActionController.class.getName());

    /** @param widget Widget
     *  @param actionInfo Action info
     */
    public ExecuteScriptActionController(Widget widget, ActionInfo actionInfo){
        this.widget = widget;
        this.executeScriptActionInfo = (ExecuteScriptAction)actionInfo;
        descriptionProperty.set(actionInfo.getDescription());
    }

    /** Init */
    @FXML
    public void initialize(){
        super.initialize();

        scriptPathProperty.set(executeScriptActionInfo.getScriptInfo().getPath());
        scriptBodyProperty.set(executeScriptActionInfo.getScriptInfo().getText());
        embedPyButton
                .setGraphic(ImageCache.getImageView(ExecuteScriptActionController.class, "/icons/embedded_script.png"));
        embedJsButton.setGraphic(ImageCache.getImageView(ExecuteScriptActionController.class, "/icons/embedded_script.png"));
        openExternalEditorButton.setGraphic(ImageCache.getImageView(ExecuteScriptActionController.class, "/icons/embedded_script.png"));
        openExternalEditorButton.setDisable(!externalEditorExists());

        scriptPath.textProperty().bindBidirectional(scriptPathProperty);
        scriptBody.textProperty().bindBidirectional(scriptBodyProperty);
        scriptBody.disableProperty().bind(Bindings.createBooleanBinding(() -> scriptPathProperty.get() == null ||
                scriptPathProperty.get().isEmpty() ||
                (!scriptPathProperty.get().equals(ScriptInfo.EMBEDDED_PYTHON) &&
                !scriptPathProperty.get().equals(ScriptInfo.EMBEDDED_JAVASCRIPT)), scriptPathProperty));
    }

    /** Select embedded Py */
    @FXML
    public void embedPy(){
        scriptPathProperty.setValue(ScriptInfo.EMBEDDED_PYTHON);
        final String text = scriptBodyProperty.get();
        if (text == null ||
                text.trim().isEmpty() ||
                text.trim().equals(ScriptInfo.EXAMPLE_JAVASCRIPT) )
            scriptBodyProperty.setValue(ScriptInfo.EXAMPLE_PYTHON);
        openExternalEditorButton.setDisable(true);
    }

    /** Select embedded Js */
    @FXML
    public void embedJs(){
        scriptPathProperty.setValue(ScriptInfo.EMBEDDED_JAVASCRIPT);
        final String text = scriptBodyProperty.get();
        if (text == null ||
                text.trim().isEmpty() ||
                text.trim().equals(ScriptInfo.EXAMPLE_PYTHON) )
            scriptBodyProperty.setValue(ScriptInfo.EXAMPLE_JAVASCRIPT);
        openExternalEditorButton.setDisable(true);
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
                scriptBodyProperty.set("");
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

    public String getScriptPath(){
        return scriptPathProperty.get();
    }

    public String getScriptBody(){
        return scriptBodyProperty.get();
    }

    public void setScriptPath(String scriptPath) {
        this.scriptPathProperty.set(scriptPath);
    }

    public void setScriptBody(String scriptBody) {
        this.scriptBodyProperty.set(scriptBody);
    }
}
