/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import java.util.concurrent.TimeUnit;

import org.csstudio.display.builder.model.util.ModelThreadPool;

import javafx.application.Platform;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Most awful terrible no good hack for Modality
 *
 *  <p>When hosted inside SWT, the modality of JavaFX Dialogs,
 *  {@link Dialog#initModality()},
 *  is ignored with respect to SWT windows.
 *  The dialog can end up below the SWT window,
 *  and the application then appears stuck because it is awaiting
 *  a result from closing the dialog.
 *
 *  <p>This hack periodically moves the dialog
 *  to the front.
 *
 *  @author Kay Kasemir
 */
public class ModalityHack implements Runnable
{
    /** @deprecated No longer needed, only for SWT-hosted JFX content */
    @Deprecated
    public static ModalityHack forDialog(final Dialog<?> dialog)
    {
        final Window window = dialog.getDialogPane().getContent().getScene().getWindow();
        if (! (window instanceof Stage))
            return null;

        final ModalityHack hack = new ModalityHack((Stage) window);
        dialog.setOnHiding(event -> hack.stop());
        return hack;
    }

    private static final int MS_DELAY = 1500;

    /** Stage to keep at front.
     *  Set to <code>null</code> when no longer in use.
     */
    private Stage stage;

    private ModalityHack(final Stage stage)
    {
        this.stage = stage;
        // Disable the hack, it's no longer needed
        // schedule();
    }

    private void stop()
    {
        stage = null;
    }

    private void schedule()
    {
        // Schedule another update, a little later, on UI thread.
        ModelThreadPool.getTimer().schedule(() -> Platform.runLater(this), MS_DELAY, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run()
    {
        final Stage safe_stage = stage;
        if (safe_stage == null)
            return;
        if (safe_stage.isShowing())
            safe_stage.toFront();
        // Schedule another check
        schedule();
    }
}