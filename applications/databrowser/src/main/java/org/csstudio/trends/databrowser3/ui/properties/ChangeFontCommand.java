/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import java.util.function.BiConsumer;

import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.phoebus.ui.undo.UndoableAction;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.text.Font;

/** Undo-able command to change plot fonts
 *  @author Kay Kasemir
 */
public class ChangeFontCommand extends UndoableAction
{
    private final Model model;
    private final Font old_font, new_font;
    private final BiConsumer<Model, Font> setter;

    /** Register and perform the command
     *  @param model Model to configure
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param old_font Old font
     *  @param new_font New font
     *  @param setter Will be called to set a font
     */
    public ChangeFontCommand(final Model model,
            final UndoableActionManager operations_manager,
            final Font old_font,
            final Font new_font,
            final BiConsumer<Model, Font> setter)
    {
        super(Messages.FontTT);
        this.model = model;
        this.old_font = old_font;
        this.new_font = new_font;
        this.setter = setter;
        operations_manager.execute(this);
    }

    @Override
    public void run()
    {
        setter.accept(model, new_font);
    }

    @Override
    public void undo()
    {
        setter.accept(model, old_font);
    }
}
