/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.sandbox;

import java.io.File;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.util.Duration;

/** JFX Audio demo
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AudioDemo extends ApplicationWrapper
{
    // At least with JDK11 and Mac OS X,
    // MediaPlayer will stop, presumably because of GC,
    // unless we hold on to it via e.g. a static reference
    static MediaPlayer player;
    
    public static void main(final String[] args)
    {
        launch(AudioDemo.class, args);
    }

    @Override
    public void start(final Stage stage)
    {
        stage.setTitle("Audio Demo");
        stage.show();

        // Windows and Mac OS X support WAV and MP3
        // Linux: WAV hangs, MP3 results in MediaException for unsupported format
        final File file = new File("../model/src/main/resources/examples/timer/timer.mp3");
        final Media audio = new Media(file.toURI().toString());
        player = new MediaPlayer(audio);
        player.setOnError(() -> System.out.println("Error!"));
        player.setOnStopped(() ->
        {
            System.out.println("Stopped.");
            player.dispose();
            stage.close();
        });
        player.setOnEndOfMedia( () ->
        {
            System.out.println("Done.");
            player.stop();
        });
        // Wasn't necessary with JDK9, but is with 11 on Mac OS X
        player.setStartTime(Duration.seconds(0));
        player.play();
        System.out.println("Playing...");
    }
}
