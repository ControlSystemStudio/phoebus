/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import org.phoebus.framework.macros.Macros;
import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.ui.dialog.DialogHelper;

import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

/** Dialog for editing {@link Macros}
 *  @author Kay Kasemir
 */
public class MacrosDialog extends Dialog<Macros>
{
    private final MacrosTable table;

    /** Create dialog
     *  @param initial_macros Initial {@link Macros}
     * @param owner Node that invoked this dialog
     */
    public MacrosDialog(final Macros initial_macros, final Node owner)
    {
        setTitle(Messages.MacrosDialog_Title);
        setHeaderText(Messages.MacrosDialog_Info);

        table = new MacrosTable(initial_macros);

        getDialogPane().setContent(table.getNode());
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        setResizable(true);

        setResultConverter(button ->
        {
            if (button == ButtonType.OK)
                return table.getMacros();
            return null;
        });

        DialogHelper.positionAndSize(this, owner,
                                     PhoebusPreferenceService.userNodeForClass(MacrosDialog.class));
    }
}
