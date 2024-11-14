/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.Node;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ScrollPane;
import javafx.stage.Window;
import javafx.util.Pair;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.ui.AddModelItemCommand;
import org.csstudio.trends.databrowser3.ui.AddPVDialog;
import org.phoebus.framework.jobs.NamedThreadFactory;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.phoebus.ui.undo.UndoableActionManager;

/** Global Data Browser helper
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Activator
{
    /** Logger for all Data Browser code */
    public static final Logger logger = Logger.getLogger(Activator.class.getPackageName());

    /** Dedicated timer for scrolling/updating plots.
     *
     *  <p>This timer is to be used for short-running tasks.
     */
    public static final ScheduledExecutorService timer;


    /** Thread pool, mostly for long running tasks like fetching archived data.
     *
     *  <p>No upper limit for threads.
     *  Removes all threads after 10 seconds
     */
    public static final ExecutorService thread_pool;

    static
    {
        // Up to 1 timer thread, delete when more than 10 seconds idle
        timer = Executors.newScheduledThreadPool(0, new NamedThreadFactory("DataBrowserTimer"));
        ((ThreadPoolExecutor)timer).setKeepAliveTime(10, TimeUnit.SECONDS);
        ((ThreadPoolExecutor)timer).setMaximumPoolSize(1);

        // After 10 seconds, delete all idle threads
        thread_pool = Executors.newCachedThreadPool(new NamedThreadFactory("DataBrowserPool"));
       ((ThreadPoolExecutor)thread_pool).setKeepAliveTime(10, TimeUnit.SECONDS);
    }

    /** @param base_name Icon base name (no path, no extension)
     *  @return {@link Image}
     */
    public static Image getImage(final String base_name)
    {
        return ImageCache.getImage(Activator.class, "/icons/" + base_name + ".png");
    }

    /** @param base_name Icon base name (no path, no extension)
     *  @return {@link ImageView}
     */
    public static ImageView getIcon(final String base_name)
    {
        return new ImageView(getImage(base_name));
    }

    public static void addPVsToPlotDialog(List<String> pvNames,
                                          UndoableActionManager undoableActionManager,
                                          Model model,
                                          Node nodeToPositionDialogOver)
    {
        // Offer potential PV name in dialog so user can edit/cancel
        // sim://sine sim://ramp sim://noise
        AddPVDialog addPVDialog = new AddPVDialog(pvNames.size(), model, false);

        { // Set layout of addPVDialog:
            int addPVDialogWidth = 750;
            int addPVDialogHeight = 600;

            Window addPVDialowWindow = addPVDialog.getDialogPane().getScene().getWindow();
            addPVDialowWindow.setWidth(addPVDialogWidth);
            addPVDialowWindow.setHeight(addPVDialogHeight);
            addPVDialog.setResizable(false);

            DialogPane dialogPane = addPVDialog.getDialogPane();
            dialogPane.setPrefWidth(addPVDialogWidth);
            dialogPane.setPrefHeight(addPVDialogHeight);
            dialogPane.setMaxWidth(Double.MAX_VALUE);
            dialogPane.setMaxHeight(Double.MAX_VALUE);

            DialogHelper.positionDialog(addPVDialog, nodeToPositionDialogOver, (int) -addPVDialowWindow.getWidth()/2, (int) -addPVDialowWindow.getHeight()/2);
        }

        for (int i=0; i<pvNames.size(); ++i) {
            addPVDialog.setNameAndDisplayName(i, pvNames.get(i));
        }

        if (!addPVDialog.showAndWait().orElse(false)) {
            return;
        }

        for (int i=0; i<pvNames.size(); ++i) {
            AxisConfig axis = addPVDialog.getOrCreateAxis(model, undoableActionManager, addPVDialog.getAxisIndex(i));
            AddModelItemCommand.forPV(undoableActionManager,
                                      model,
                                      addPVDialog.getName(i),
                                      addPVDialog.getDisplayName(i),
                                      addPVDialog.getScanPeriod(i),
                                      axis,
                                      null);
        }
    }

    /*
    public static void main(String[] args) throws Exception
    {
        // Thread pool grows to allow 'unlimited' number of submitted tasks..
        for (int i=0; i<10; ++i)
        {
            final int id = i;
            thread_pool.submit(() ->
            {
                for (int j=0; j<5; ++j)
                {
                    System.out.println(id + " running on " + Thread.currentThread().getName());
                    try
                    {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
                System.out.println(Thread.currentThread().getName() + " running...");
            });
        }

        // timer uses one thread for 'scheduled' items
        // If Task A takes a long time, scheduled task B doesn't
        // get to run until task A completes.
        timer.schedule(() ->
        {
            System.out.println("Long task A ...");
            try
            {
                TimeUnit.SECONDS.sleep(4);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            System.out.println("Long task A Done.");
        }, 1, TimeUnit.SECONDS);

        timer.schedule(() ->
        {
            System.out.println("Long task B ...");
            try
            {
                TimeUnit.SECONDS.sleep(2);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            System.out.println("Long task B Done.");
        }, 2, TimeUnit.SECONDS);

        TimeUnit.SECONDS.sleep(10);
    }
    */
}
