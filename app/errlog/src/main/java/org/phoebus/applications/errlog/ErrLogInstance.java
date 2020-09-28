/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.errlog;

import java.util.concurrent.CompletableFuture;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

import javafx.geometry.Insets;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** ErrLog application instance, singleton
 *  @author Kay Kasemir
 */
class ErrLogInstance implements AppInstance
{
    /** Singleton instance maintained by {@link ErrLogApp} */
    static ErrLogInstance INSTANCE = null;

    private final AppDescriptor app;
    private final ErrLog errlog;
    private final LineView line_view;
    private final DockItem tab;

    public ErrLogInstance(final AppDescriptor app) throws Exception
    {
        this.app = app;

        line_view = new LineView();

        errlog = new ErrLog(out -> line_view.addLine(out,  false),
                            err -> line_view.addLine(err, true));

        final VBox layout = new VBox(line_view.getControl());
        VBox.setVgrow(line_view.getControl(), Priority.ALWAYS);
        layout.setPadding(new Insets(5.0));
        tab = new DockItem(this, layout);
        tab.addCloseCheck(() ->
        {
            errlog.close();
            INSTANCE = null;
            return CompletableFuture.completedFuture(true);
        });
        DockPane.getActiveDockPane().addTab(tab);
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    void raise()
    {
        tab.select();
    }
}
