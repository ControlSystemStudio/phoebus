/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.util.logging.Level;

import org.csstudio.display.builder.runtime.Messages;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Action to enter/exit 'full screen' mode
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FullScreenAction extends MenuItem
{
    private static final Image enter = ImageCache.getImage(FullScreenAction.class, "/icons/fullscreen.png"),
                               exit = ImageCache.getImage(FullScreenAction.class, "/icons/exitfullscreen.png");

    public FullScreenAction(final Scene scene)
    {
        final Window window = scene.getWindow();
        if (! (window instanceof Stage))
        {
            logger.log(Level.WARNING, "Need 'Stage' for full-screen");
            return;
        }
        final Stage stage = (Stage) window;
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        // XXX Automatically combine 'full screen' with DockPane.alwaysShowTabs(false) ?

        final boolean full = stage.isFullScreen();
        setText(full ? Messages.ExitFullscreen : Messages.EnterFullscreen);
        setGraphic(new ImageView(full ? exit : enter));
        setOnAction(event -> stage.setFullScreen(! full));
    }
}
