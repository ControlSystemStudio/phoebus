/*******************************************************************************
 * Copyright (c) 2015-2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.representation.ToolkitRepresentation;
import org.csstudio.display.builder.model.spi.ActionHandler;
import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.macros.MacroValueProvider;

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Action Helper
 *
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ActionUtil {
    private static final ServiceLoader<ActionHandler> actionHandlers = ServiceLoader.load(ActionHandler.class);

    /**
     * Handle an action
     *
     * @param source_widget Widget from which the action is invoked.
     * @param action        Information about the action to perform
     */
    public static void handleAction(final Widget source_widget, final ActionInfo action) {
        Optional<ServiceLoader.Provider<ActionHandler>> handler = actionHandlers.stream().filter(p -> p.get().matches(action)).findFirst();
        if (handler.isEmpty()) {
            throw new RuntimeException("No ActionHandler found for action " + action);
        }
        RuntimeUtil.getExecutor().execute(() -> handler.get().get().handleAction(source_widget, action));
    }


    /**
     * Passed to newly opened windows to handle runtime shutdown
     * when window is closed
     *
     * @param model Model for which runtime needs to be closed
     */
    public static void handleClose(final DisplayModel model) {
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

    public static String resolve(final Widget source_widget, final String path) throws Exception {
        // Path to resolve, after expanding macros
        final MacroValueProvider macros = source_widget.getMacrosOrProperties();
        final String expanded_path = MacroHandler.replace(macros, path);

        // Resolve file relative to the source widget model (not 'top'!)
        final DisplayModel widget_model = source_widget.getDisplayModel();
        final String parent_file = widget_model.getUserData(DisplayModel.USER_DATA_INPUT_FILE);
        return ModelResourceUtil.resolveResource(parent_file, expanded_path);
    }
}

