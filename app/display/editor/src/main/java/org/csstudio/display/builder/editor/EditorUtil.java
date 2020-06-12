/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor;

import java.util.concurrent.ExecutorService;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.util.NamedDaemonPool;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;

import javafx.scene.Scene;

/** Editor utility
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EditorUtil
{
    private static final ExecutorService executor = NamedDaemonPool.createThreadPool("DisplayEditor");

    /** @return ExecutorService for thread pool meant for editor-related background tasks */
    public static ExecutorService getExecutor()
    {
        return executor;
    }

    public static void setSceneStyle(final Scene scene)
    {
        JFXRepresentation.setSceneStyle(scene);
        scene.getStylesheets().add(EditorUtil.class.getResource("opieditor.css").toExternalForm());
    }

    public static boolean isDisplayReadOnly(final DisplayModel display)
    {
        return Boolean.parseBoolean(display.getUserData(DisplayModel.USER_DATA_READONLY));
    }
}
