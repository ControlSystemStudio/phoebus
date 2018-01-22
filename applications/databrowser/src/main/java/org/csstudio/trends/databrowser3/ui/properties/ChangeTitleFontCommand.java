/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.text.Font;

/** Undo-able command to change plot fonts
 *  @author Kay Kasemir
 */
public class ChangeTitleFontCommand extends UndoableAction
{
    final private Model model;
    final private Font old_font, new_font;

    /** Register and perform the command
     *  @param model Model to configure
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param new_color New value
     */
    public ChangeTitleFontCommand(final Model model,
            final UndoableActionManager operations_manager,
            final Font new_font)
    {
        super(Messages.TitleFontTT);
        this.model = model;
        this.old_font = model.getTitleFont();
        this.new_font = new_font;
        operations_manager.execute(this);
    }

    @Override
    public void run()
    {
        model.setTitleFont(new_font);
    }

    @Override
    public void undo()
    {
        model.setTitleFont(old_font);
    }
}
