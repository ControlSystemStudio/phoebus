/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.sandbox;

import java.io.File;

import javafx.application.Application;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

/** JFX Audio demo
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AudioDemo extends Application
{
    public static void main(final String[] args)
    {
        launch(args);
    }

    @Override
    public void start(final Stage stage)
    {
        stage.setTitle("Audio Demo");
        stage.show();

        // Windows and Mac OS X support WAV and MP3
        // Linux: WAV hangs, MP3 results in MediaException for unsupported format
        final File file =
        //                new File("../org.csstudio.display.builder.model/examples/audio/timer.wav");
                          new File("../org.csstudio.display.builder.model/examples/timer/timer.mp3");
        final Media audio = new Media(file.toURI().toString());
        final MediaPlayer player = new MediaPlayer(audio);
        player.setOnEndOfMedia( () -> player.stop());
        player.play();

        player.setOnStopped(() ->
        {
            player.dispose();
            stage.close();
        });
    }
}
