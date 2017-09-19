/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.application;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/** Splash screen
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Splash
{
    private final Stage stage;

    // The 'fastest' implementation would use the
    // java.awt.SplashScreen and associated
    // JRE command line arguments to display
    // an image before the JVM is fully initialized,
    // but it's unclear how to affect the layering
    // of that AWT SplashScreen and the JavaFX
    // stage(s) that is(are) about to open
    //
    // --> Use the initial JFX stage for the splash,
    // keeping it "always on top"

    public Splash(final Stage stage)
    {
        this.stage = stage;
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        stage.setTitle("Phoebus");

        final Image image = new Image(getClass().getResourceAsStream("/icons/splash.png"));
        final double width = image.getWidth();
        final double height = image.getHeight();
        final Pane layout = new Pane(new ImageView(image));
        stage.setScene(new Scene(layout, width, height));

        // stage.centerOnScreen() uses 1/3 for Y position...
        final Rectangle2D bounds = Screen.getPrimary().getBounds();
        stage.setX((bounds.getMaxX() - width)/2);
        stage.setY((bounds.getMaxY() - height)/2);
        stage.show();
    }

    public void close()
    {
        // Keep the splash for another 3 seconds
        // (so in case of fast startup it's at least shown 3 secs),
        // then fade out.
        final PauseTransition pause = new PauseTransition(Duration.seconds(3));
        final FadeTransition fade = new FadeTransition(Duration.seconds(1.5), stage.getScene().getRoot());
        fade.setFromValue(1.0);
        fade.setToValue(0);

        final SequentialTransition animation = new SequentialTransition(pause, fade);
        animation.setOnFinished(event -> stage.close());
        animation.play();
    }
}
