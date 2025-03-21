/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.monitor;

import static org.csstudio.scan.ScanSystem.logger;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import org.csstudio.scan.client.ScanClient;
import org.csstudio.scan.client.ScanInfoModel;
import org.csstudio.scan.client.ScanInfoModelListener;
import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.info.ScanServerInfo;
import org.csstudio.scan.ui.Messages;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.ToolbarHelper;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Scan monitor application instance (singleton)
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanMonitor implements AppInstance
{
    /** Singleton instance maintained by {@link ScanMonitorApplication} */
    static ScanMonitor INSTANCE = null;

    private ScanMonitorApplication app;
    private final DockItem tab;
    private ScanInfoModel model;
    private ScansTable scans;

    private final ScanInfoModelListener model_listener = new ScanInfoModelListener()
    {
        @Override
        public void scanServerUpdate(final ScanServerInfo server_info)
        {
            Platform.runLater(() -> scans.update(server_info));
        }

        @Override
        public void scanUpdate(final List<ScanInfo> infos)
        {
            Platform.runLater(() -> scans.update(infos));
        }

        @Override
        public void connectionError()
        {
            Platform.runLater(() ->
            {
                scans.update(Collections.emptyList());
                scans.update((ScanServerInfo) null);
            });
        }
    };

    public ScanMonitor(final ScanMonitorApplication app)
    {
        this.app = app;

        tab = new DockItem(this, create());
        tab.addCloseCheck(() ->
        {
            dispose();
            return CompletableFuture.completedFuture(true);
        });
        tab.addClosedNotification(() -> INSTANCE = null);
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

    private Node create()
    {
        try
        {
            model = ScanInfoModel.getInstance();
            final ScanClient client = model.getScanClient();

            final Button resume = new Button("", ImageCache.getImageView(StateCell.class, "/icons/resume.png"));
            resume.setTooltip(new Tooltip(Messages.scan_resume_all));
            resume.setOnAction(event ->
            {
                try
                {
                    client.resumeScan(-1);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot resume scans", ex);
                }
            });

            final Button pause = new Button("", ImageCache.getImageView(StateCell.class, "/icons/pause.png"));
            pause.setTooltip(new Tooltip(Messages.scan_pause_all));
            pause.setOnAction(event ->
            {
                try
                {
                    client.pauseScan(-1);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot pause scans", ex);
                }
            });

            final Button abort = new Button("", ImageCache.getImageView(StateCell.class, "/icons/abort.png"));
            abort.setTooltip(new Tooltip(Messages.scan_abort_all));
            abort.setOnAction(event ->
            {
                try
                {
                    client.abortScan(-1);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot abort scans", ex);
                }
            });

            final ToolBar toolbar = new ToolBar(ToolbarHelper.createSpring(), resume, pause, abort);
            scans = new ScansTable(client);
            model.addListener(model_listener);

            VBox.setVgrow(scans, Priority.ALWAYS);

            return new VBox(toolbar, scans);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot create scan monitor", ex);
            return new Label("Cannot create scan monitor");
        }
    }

    @Override
    public void restore(final Memento memento)
    {
        scans.restore(memento);
    }

    @Override
    public void save(final Memento memento)
    {
        scans.save(memento);
    }

    private void dispose()
    {
        model.removeListener(model_listener);
        model.release();
        model = null;
    }
}
