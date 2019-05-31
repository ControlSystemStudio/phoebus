/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.properties;

import java.util.List;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.rules.RuleInfo;
import org.csstudio.display.builder.model.widgets.LabelWidget;
import org.phoebus.ui.javafx.ApplicationWrapper;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.stage.Stage;

/** Standalone demo if the RulesDialog
 *  @author Kay Kasemir
 */
public class RulesDialogDemo extends ApplicationWrapper
{
    /** JavaFX Start */
    @Override
    public void start(final Stage stage)
    {
        final UndoableActionManager undo = new UndoableActionManager(10);
        final Widget widget = new LabelWidget();
        final List<RuleInfo> rules = widget.propRules().getValue();
        final RulesDialog dialog = new RulesDialog(undo, rules, widget, null);
        System.out.println(dialog.showAndWait());
    }

    /** JavaFX main
     *  @throws Exception
     */
    public static void main(final String[] args) throws Exception
    {
        launch(RulesDialogDemo.class, args);
    }
}
