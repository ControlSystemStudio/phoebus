/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.dialog;

import java.io.File;

import javafx.stage.FileChooser;
import javafx.stage.Window;

/** Dialog that prompts for a file name to 'open'
 *  @author Kay Kasemir
 */
public class OpenFileDialog extends SaveAsDialog
{
    @Override
    protected File doExecuteDialog(final Window window, final FileChooser dialog)
    {
        return dialog.showOpenDialog(window);
    }
}
