/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.application;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Level;

import org.phoebus.framework.workbench.Locations;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/** Splash screen
 *
 *  <p>Defaults to the <code>/icons/splash.png</code> resource.
 *
 *  <p>For site-specific splash, place a <code>site_splash.png</code>
 *  in the install location.
 *  See {@link Locations#install()} or runtime menu Help, About
 *  for install location.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Splash
{
    private final Stage stage;
    private ProgressBar progress;
    private TextField status;

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
        stage.initStyle(StageStyle.UNDECORATED);
        // Should keep the stage on top,
        // but doesn't always work,
        // so calling toFront() below whenever updating
        stage.setAlwaysOnTop(true);
        stage.setTitle(Messages.ProgressTitle);

        Image image = null;
        final File custom_splash = new File(Locations.install(), "site_splash.png");
        if (custom_splash.exists())
        {
            try
            {
                image = new Image(new FileInputStream(custom_splash));
            }
            catch (FileNotFoundException ex)
            {
                logger.log(Level.WARNING, "Cannot open " + custom_splash, ex);
                image = null;
            }
        }
        if (image == null)
            image = new Image(getClass().getResourceAsStream("/icons/splash.png"));
        final double width = image.getWidth();
        final double height = image.getHeight();

        progress = new ProgressBar();
        progress.relocate(5, height-55);
        progress.setPrefSize(width-10, 20);

        status = new TextField();
        status.setEditable(false);
        status.relocate(5, height-30);
        status.setPrefSize(width-10, 25);

        final Pane layout = new Pane(new ImageView(image), progress, status);

        stage.setScene(new Scene(layout, width, height));

        // stage.centerOnScreen() uses 1/3 for Y position...
        final Rectangle2D bounds = Screen.getPrimary().getBounds();
        stage.setX((bounds.getMaxX() - width)/2);
        stage.setY((bounds.getMaxY() - height)/2);
        stage.show();
    }

    /** @param percentage Progress 0..100, or negative for indeterminate */
    public void updateProgress(final int percentage)
    {
        final double progress = percentage >= 0 ? percentage/100.0 : ProgressIndicator.INDETERMINATE_PROGRESS;
        Platform.runLater(() ->
        {
            this.progress.setProgress(progress);
            stage.toFront();
        });
    }

    /** @param status Status text */
    public void updateStatus(final String status)
    {
        Platform.runLater(() ->
        {
            this.status.setText(status);
            stage.toFront();
        });
    }

    /** Close the splash screen */
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
