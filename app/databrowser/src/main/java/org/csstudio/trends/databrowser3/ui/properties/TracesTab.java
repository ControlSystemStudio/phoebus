/*******************************************************************************
 * Copyright (c) 2018-2024 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import java.io.IOException;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.phoebus.framework.nls.NLS;
import org.phoebus.ui.undo.UndoableActionManager;
import javafx.scene.control.Tab;

/**
 * Property tab for traces (items, archives)
 *
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TracesTab extends Tab {

    TracesTab(final Model model, final UndoableActionManager undo) {
        super(Messages.TracesTab);
        ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
        FXMLLoader loader = new FXMLLoader();
        loader.setResources(resourceBundle);
        loader.setLocation(this.getClass().getResource("TracesTab.fxml"));

        loader.setControllerFactory(clazz -> {
            if (clazz.isAssignableFrom(TracesTabController.class)) {
                try {
                    return clazz.getConstructor(Model.class, UndoableActionManager.class).newInstance(model, undo);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        });

        try {
            setContent(loader.load());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
