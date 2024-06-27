/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.csstudio.display.builder.runtime.app.actionhandlers;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.persist.ModelLoader;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.representation.ToolkitRepresentation;
import org.csstudio.display.builder.runtime.ActionUtil;
import org.csstudio.display.builder.runtime.RuntimeUtil;
import org.csstudio.display.builder.runtime.script.ScriptUtil;
import org.csstudio.display.builder.model.spi.ActionHandler;
import org.csstudio.display.actions.OpenDisplayAction;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.macros.MacroOrSystemProvider;

import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OpenDisplayActionHandler implements ActionHandler {

    private static final Logger logger = Logger.getLogger(OpenDisplayActionHandler.class.getName());

    public boolean matches(ActionInfo pluggableActionInfo){
        return pluggableActionInfo.getType().equalsIgnoreCase(OpenDisplayAction.OPEN_DISPLAY);
    }

    public void handleAction(Widget sourceWidget, ActionInfo actionInfo){
        OpenDisplayAction openDisplayActionInfo = (OpenDisplayAction) actionInfo;
        if (openDisplayActionInfo.getFile().isEmpty())
        {
            logger.log(Level.WARNING, "Open Display action without file");
            return;
        }
        String parent_file = "";
        String expanded_path = "";
        try
        {
            // Path to resolve, after expanding macros of action in environment of source widget
            //final Macros macros = action.getMacros(); // Not copying, just using action's macros
            openDisplayActionInfo.getMacros().expandValues(sourceWidget.getEffectiveMacros());

            // For display path, use the combined macros...
            expanded_path = MacroHandler.replace(new MacroOrSystemProvider(openDisplayActionInfo.getMacros()), openDisplayActionInfo.getFile());
            // .. but fall back to properties
            expanded_path = MacroHandler.replace(sourceWidget.getMacrosOrProperties(), expanded_path);
            logger.log(Level.FINER, "Open Display action, effective macros {1} ({2})", new Object[] { openDisplayActionInfo.getMacros().toExpandedString(), expanded_path });

            // Resolve new display file relative to the source widget model (not 'top'!)
            final DisplayModel widget_model = sourceWidget.getDisplayModel();
            parent_file = widget_model.getUserData(DisplayModel.USER_DATA_INPUT_FILE);

            // Load new model. If that fails, no reason to continue.
            final DisplayModel new_model = ModelLoader.resolveAndLoadModel(parent_file, expanded_path);

            // Model is standalone; source_widget (Action button, ..) is _not_ the parent,
            // but it does add macros to those already defined in the display file.
            // Expand macros down the widget hierarchy
            new_model.expandMacros(openDisplayActionInfo.getMacros());

            // Schedule representation on UI thread...
            final DisplayModel top_model = sourceWidget.getTopDisplayModel();
            final ToolkitRepresentation<Object, Object> toolkit = ToolkitRepresentation.getToolkit(top_model);
            if (openDisplayActionInfo.getTarget().equals(OpenDisplayAction.Target.TAB))
            {
                toolkit.submit(() ->
                {   // Create new panel
                    final ToolkitRepresentation<Object, Object> new_toolkit =
                            toolkit.openPanel(new_model, openDisplayActionInfo.getPane(), ActionUtil::handleClose);
                    RuntimeUtil.hookRepresentationListener(new_toolkit);
                    return null;
                });
            }
            else if (openDisplayActionInfo.getTarget().equals(OpenDisplayAction.Target.WINDOW))
            {
                toolkit.submit(() ->
                {   // Create new top-level window
                    final ToolkitRepresentation<Object, Object> new_toolkit =
                            toolkit.openNewWindow(new_model, ActionUtil::handleClose);
                    RuntimeUtil.hookRepresentationListener(new_toolkit);
                    return null;
                });
            }
            else if (openDisplayActionInfo.getTarget().equals(OpenDisplayAction.Target.STANDALONE))
            {
                toolkit.submit(() ->
                {
                    final ToolkitRepresentation<Object, Object> new_toolkit =
                            toolkit.openStandaloneWindow(new_model, ActionUtil::handleClose);
                    RuntimeUtil.hookRepresentationListener(new_toolkit);
                    return null;
                });
            }
            else
            {   // Default to OpenDisplayActionInfo.Target.REPLACE
                // The DockItemRepresentation starts the runtime for new tabs or windows,
                // but if we update an existing representation, we need to stop & start the runtime.
                RuntimeUtil.stopRuntime(top_model);

                // Update UI on toolkit thread, check for success
                final Future<Boolean> wait_for_ui = toolkit.submit(() ->
                {
                    try
                    {
                        // Close old representation
                        final Object parent = toolkit.disposeRepresentation(top_model);
                        // Tell toolkit about new model to represent
                        toolkit.representModel(parent, new_model);
                    }
                    catch (Throwable ex)
                    {
                        return false;
                    }
                    return true;
                });

                if (wait_for_ui.get())
                {
                    // Back in background thread, start runtime
                    RuntimeUtil.startRuntime(new_model);
                }
            }
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, "Error handling Open Display action", ex);
            // Compute the full file path for the error dialog
            String fullFilePath = ModelResourceUtil.resolveResource(parent_file, expanded_path);
            final String message;
            if (openDisplayActionInfo.getFile().endsWith(".opi"))
                message = "Cannot open " + fullFilePath + " or .bob.\n\nSee log for details.";
            else
                message = "Cannot open " + fullFilePath + ".\n\nSee log for details.";
            ScriptUtil.showErrorDialog(sourceWidget, message);
        }

    }
}
