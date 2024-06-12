/*
 * Copyright (C) 2023 European Spallation Source ERIC.
 */

package org.csstudio.display.builder.runtime.actionhandlers;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.PluggableActionInfo;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.representation.javafx.actions.ExecuteCommandAction;
import org.csstudio.display.builder.runtime.script.ScriptUtil;
import org.csstudio.display.builder.runtime.spi.ActionHandler;
import org.phoebus.framework.jobs.CommandExecutor;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.macros.MacroValueProvider;

import java.io.File;
import java.util.logging.Level;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

public class ExecuteCommandActionHandler implements ActionHandler {

    @Override
    public void handleAction(Widget sourceWidget, PluggableActionInfo pluggableActionInfo) {
        ExecuteCommandAction action = (ExecuteCommandAction) pluggableActionInfo;

        try
        {
            // Path to resolve, after expanding macros
            final MacroValueProvider macros = sourceWidget.getMacrosOrProperties();
            final String command = MacroHandler.replace(macros, action.getCommand());
            logger.log(Level.FINER, "{0}, effective macros {1} ({2})", new Object[] { action, macros, command });

            // Resolve command relative to the source widget model (not 'top'!)
            final DisplayModel widget_model = sourceWidget.getDisplayModel();
            final String parent_file = widget_model.getUserData(DisplayModel.USER_DATA_INPUT_FILE);
            String parent_dir = ModelResourceUtil.getDirectory(parent_file);

            // Check if the parent_dir exists or is reachable. If not, use "." as the parent_dir.
            File parentDirectory = new File(parent_dir);
            if (!parentDirectory.exists() || !parentDirectory.isDirectory()) {
                logger.log(Level.WARNING, "Parent directory {0} does not exist or is not reachable. Using current directory instead to execute command.", parent_dir);
                parent_dir = ".";
            }
            // Execute (this is already running on background thread)
            logger.log(Level.FINE, "Executing command {0} in {1}", new Object[] { command, parent_dir });
            final CommandExecutor executor = new CommandExecutor(command, new File(parent_dir));
            executor.call();
        }
        catch (final Throwable ex)
        {
            logger.log(Level.WARNING, action + " failed", ex);
            ScriptUtil.showErrorDialog(sourceWidget, "Cannot execute " + action + ".\n\nSee log for details.");
        }
    }

    @Override
    public boolean matches(PluggableActionInfo pluggableActionInfo) {
        return pluggableActionInfo instanceof ExecuteCommandAction;
    }
}
