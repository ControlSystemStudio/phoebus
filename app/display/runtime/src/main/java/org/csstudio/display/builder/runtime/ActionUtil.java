/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.io.File;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.persist.ModelLoader;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.ExecuteCommandActionInfo;
import org.csstudio.display.builder.model.properties.ExecuteScriptActionInfo;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo;
import org.csstudio.display.builder.model.properties.OpenFileActionInfo;
import org.csstudio.display.builder.model.properties.OpenWebpageActionInfo;
import org.csstudio.display.builder.model.properties.WritePVActionInfo;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.representation.ToolkitRepresentation;
import org.csstudio.display.builder.runtime.script.ScriptUtil;
import org.phoebus.framework.jobs.CommandExecutor;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.macros.MacroValueProvider;
import org.phoebus.framework.macros.Macros;

/** Action Helper
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ActionUtil
{
    /** Handle an action
     *  @param source_widget Widget from which the action is invoked.
     *  @param action Information about the action to perform
     */
    public static void handleAction(final Widget source_widget, final ActionInfo action)
    {
        // Move off the UI Thread
        if (action instanceof OpenDisplayActionInfo)
            RuntimeUtil.getExecutor().execute(() -> openDisplay(source_widget, (OpenDisplayActionInfo) action));
        else if (action instanceof WritePVActionInfo)
            RuntimeUtil.getExecutor().execute(() -> writePV(source_widget, (WritePVActionInfo) action));
        else if (action instanceof ExecuteScriptActionInfo)
            RuntimeUtil.getExecutor().execute(() -> executeScript(source_widget, (ExecuteScriptActionInfo) action));
        else if (action instanceof ExecuteCommandActionInfo)
            RuntimeUtil.getExecutor().execute(() -> executeCommand(source_widget, (ExecuteCommandActionInfo) action));
        else if (action instanceof OpenFileActionInfo)
            RuntimeUtil.getExecutor().execute(() -> openFile(source_widget, (OpenFileActionInfo) action));
        else if (action instanceof OpenWebpageActionInfo)
            RuntimeUtil.getExecutor().execute(() -> openWebpage(source_widget, (OpenWebpageActionInfo) action));
        else
            logger.log(Level.SEVERE, "Cannot handle unknown " + action);
    }

    /** Open a display
     *
     *  <p>Depending on the target of the action,
     *  this will open a new display or replace
     *  an existing display
     *
     *  @param source_widget Widget from which the action is invoked
     *                       Used to resolve the potentially relative path of the
     *                       display specified in the action
     *  @param action        Information on which display to open and how
     */
    private static void openDisplay(final Widget source_widget,
                                    final OpenDisplayActionInfo action)
    {
        if (action.getFile().isEmpty())
        {
            logger.log(Level.WARNING, "Action without file: {0}", action);
            return;
        }
        String parent_file = "";
        String expanded_path = "";
        try
        {
            // Path to resolve, after expanding macros of source widget and action
            final Macros expanded = new Macros(action.getMacros());
            expanded.expandValues(source_widget.getEffectiveMacros());
            final Macros macros = Macros.merge(source_widget.getEffectiveMacros(), expanded);
            expanded_path = MacroHandler.replace(macros, action.getFile());
            logger.log(Level.FINER, "{0}, effective macros {1} ({2})", new Object[] { action, macros, expanded_path });

            // Resolve new display file relative to the source widget model (not 'top'!)
            final DisplayModel widget_model = source_widget.getDisplayModel();
            parent_file = widget_model.getUserData(DisplayModel.USER_DATA_INPUT_FILE);

            // Load new model. If that fails, no reason to continue.
            final DisplayModel new_model = ModelLoader.resolveAndLoadModel(parent_file, expanded_path);

            // Model is standalone; source_widget (Action button, ..) is _not_ the parent,
            // but it does add macros to those already defined in the display file.
            final Macros combined_macros = Macros.merge(macros, new_model.propMacros().getValue());
            new_model.propMacros().setValue(combined_macros);

            // Schedule representation on UI thread...
            final DisplayModel top_model = source_widget.getTopDisplayModel();
            final ToolkitRepresentation<Object, Object> toolkit = ToolkitRepresentation.getToolkit(top_model);
            if (action.getTarget() == OpenDisplayActionInfo.Target.TAB)
            {
                toolkit.submit(() ->
                {   // Create new panel
                    final ToolkitRepresentation<Object, Object> new_toolkit =
                        toolkit.openPanel(new_model, action.getPane(), ActionUtil::handleClose);
                    RuntimeUtil.hookRepresentationListener(new_toolkit);
                    return null;
                });
            }
            else if (action.getTarget() == OpenDisplayActionInfo.Target.WINDOW)
            {
                toolkit.submit(() ->
                {   // Create new top-level window
                    final ToolkitRepresentation<Object, Object> new_toolkit =
                        toolkit.openNewWindow(new_model, ActionUtil::handleClose);
                    RuntimeUtil.hookRepresentationListener(new_toolkit);
                    return null;
                });
            }
            else if (action.getTarget() == OpenDisplayActionInfo.Target.STANDALONE)
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
            logger.log(Level.WARNING, "Error handling " + action, ex);
            // Compute the full file path for the error dialog
            String fullFilePath = ModelResourceUtil.resolveResource(parent_file, expanded_path);
            final String message;
            if (action.getFile().endsWith(".opi"))
                message = "Cannot open " + fullFilePath + " or .bob.\n\nSee log for details.";
            else
                message = "Cannot open " + fullFilePath + ".\n\nSee log for details.";
            ScriptUtil.showErrorDialog(source_widget, message);
        }
    }

    /** Passed to newly opened windows to handle runtime shutdown
     *  when window is closed
     *  @param model Model for which runtime needs to be closed
     */
    public static void handleClose(final DisplayModel model)
    {
        // Called on UI thread.
        // Runtime needs to stop first.
        // If stopped later, it would not find any widgets,
        // and thus not completely stop PVs.
        // Could stop runtime in background thread,
        // but would have to wait for that before then disposing toolkit on UI thread.
        // Finally, caller wants to be sure that we're done.
        // -> Work is sequential anyway, do all in this thread
        RuntimeUtil.stopRuntime(model);

        // After runtime stopped and is no longer accessing the model,
        // dispose representation.
        final ToolkitRepresentation<Object, Object> toolkit = ToolkitRepresentation.getToolkit(model);
        toolkit.disposeRepresentation(model);

        // Finally, dispose model
        model.dispose();
    }

    /** Write a PV
     *  @param source_widget Widget from which the action is invoked
     *  @param action        What to write to which PV
     */
    private static void writePV(final Widget source_widget, final WritePVActionInfo action)
    {
        final WidgetRuntime<Widget> runtime = RuntimeUtil.getRuntime(source_widget);
        // System.out.println(action.getDescription() + ": Set " + action.getPV() + " = " + action.getValue());
        final MacroValueProvider macros = source_widget.getMacrosOrProperties();
        String pv_name = action.getPV(), value = action.getValue();
        try
        {
            pv_name = MacroHandler.replace(macros, pv_name);
        }
        catch (Exception ignore)
        {
            // NOP
        }
        try
        {
            value = MacroHandler.replace(macros, value);
        }
        catch (Exception ignore)
        {
            // NOP
        }
        try
        {
            runtime.writePV(pv_name,value);
        }
        catch (final Exception ex)
        {
            final String message = "Cannot write '" + pv_name + "' = " + value;
            logger.log(Level.WARNING, message, ex);
            ScriptUtil.showErrorDialog(source_widget, message + ".\n\nSee log for details.");
        }
    }

    /** Execute script
     *  @param source_widget Widget from which the action is invoked
     *  @param action        Script action to execute
     */
    private static void executeScript(final Widget source_widget, final ExecuteScriptActionInfo action)
    {
        final WidgetRuntime<Widget> runtime = RuntimeUtil.getRuntime(source_widget);
        try
        {
            runtime.executeScriptAction(action);
        }
        catch (final Throwable ex)
        {
            logger.log(Level.WARNING, action + " failed", ex);
            ScriptUtil.showErrorDialog(source_widget, "Cannot execute " + action.getInfo().getPath() + ".\n\nSee log for details.");
        }
    }

    /** Execute external command
     *  @param source_widget Widget from which the action is invoked
     *  @param action        Command action to execute
     */
    private static void executeCommand(final Widget source_widget, final ExecuteCommandActionInfo action)
    {
        try
        {
            // Path to resolve, after expanding macros
            final Macros macros = source_widget.getEffectiveMacros();
            final String command = MacroHandler.replace(macros, action.getCommand());
            logger.log(Level.FINER, "{0}, effective macros {1} ({2})", new Object[] { action, macros, command });

            // Resolve command relative to the source widget model (not 'top'!)
            final DisplayModel widget_model = source_widget.getDisplayModel();
            final String parent_file = widget_model.getUserData(DisplayModel.USER_DATA_INPUT_FILE);
            final String parent_dir = ModelResourceUtil.getDirectory(parent_file);

            // Execute (this is already running on background thread)
            logger.log(Level.FINE, "Executing command {0} in {1}", new Object[] { command, parent_dir });
            final CommandExecutor executor = new CommandExecutor(command, new File(parent_dir));
            executor.call();
        }
        catch (final Throwable ex)
        {
            logger.log(Level.WARNING, action + " failed", ex);
            ScriptUtil.showErrorDialog(source_widget, "Cannot execute " + action + ".\n\nSee log for details.");
        }
    }

    /** Open a file
     *  @param source_widget Widget from which the action is invoked.
     *                       Used to resolve the potentially relative path of the
     *                       file specified in the action
     *  @param action        Information on which file to open
     */
    private static void openFile(final Widget source_widget, final OpenFileActionInfo action)
    {
        if (action.getFile().isEmpty())
        {
            logger.log(Level.WARNING, "Action without file: {0}", action);
            return;
        }
        try
        {
            final String resolved_name = resolve(source_widget, action.getFile());
            final DisplayModel top_model = source_widget.getTopDisplayModel();
            final ToolkitRepresentation<Object, Object> toolkit = ToolkitRepresentation.getToolkit(top_model);
            toolkit.execute(() ->
            {
                try
                {
                    toolkit.openFile(resolved_name);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot open " + action, ex);
                    toolkit.showErrorDialog(source_widget, "Cannot open " + resolved_name);
                }
            });
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, "Error handling " + action, ex);
            ScriptUtil.showErrorDialog(source_widget, "Cannot open " + action.getFile() + ".\n\nSee log for details.");
        }
    }

    /** Open a web page
     *  @param source_widget Widget from which the action is invoked.
     *                       Used to resolve the potentially relative path of the
     *                       file specified in the action
     *  @param action        Information on which URL to open
     */
    private static void openWebpage(final Widget source_widget, final OpenWebpageActionInfo action)
    {
        if (action.getURL().isEmpty())
        {
            logger.log(Level.WARNING, "Action without URL: {0}", action);
            return;
        }
        try
        {
            final String resolved_name = resolve(source_widget, action.getURL());
            final DisplayModel top_model = source_widget.getTopDisplayModel();
            final ToolkitRepresentation<Object, Object> toolkit = ToolkitRepresentation.getToolkit(top_model);
            toolkit.execute(() ->
            {
                try
                {
                    toolkit.openWebBrowser(resolved_name);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot open " + action, ex);
                    toolkit.showErrorDialog(source_widget, "Cannot open " + resolved_name);
                }
            });
        }
        catch (final Exception ex)
        {
            logger.log(Level.WARNING, "Error handling " + action, ex);
            ScriptUtil.showErrorDialog(source_widget, "Cannot open " + action.getURL() + ".\n\nSee log for details.");
        }
    }

    private static String resolve(final Widget source_widget, final String path) throws Exception
    {
        // Path to resolve, after expanding macros
        final Macros macros = source_widget.getEffectiveMacros();
        final String expanded_path = MacroHandler.replace(macros, path);

        // Resolve file relative to the source widget model (not 'top'!)
        final DisplayModel widget_model = source_widget.getDisplayModel();
        final String parent_file = widget_model.getUserData(DisplayModel.USER_DATA_INPUT_FILE);
        return ModelResourceUtil.resolveResource(parent_file, expanded_path);
    }
}

