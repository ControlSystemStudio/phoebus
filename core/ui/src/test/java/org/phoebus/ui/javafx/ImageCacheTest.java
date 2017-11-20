/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.javafx;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.phoebus.ui.application.PhoebusApplication;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

/** Demo of the {@link ImageCache}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ImageCacheTest extends Application
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        Platform.exit();
    }

    @Test
    public void testImageCache() throws Exception
    {
        // Need to start JFX app
        // (which then closes itself)
        // to initialize JFX and avoid
        // "Internal graphics not initialized yet."
        // when testing ImageCache
        try
        {
            launch();
        }
        catch (Throwable ex)
        {
            System.out.println("Cannot start JFX");
            return;
        }

        // Find an icon
        final Image image = ImageCache.getImage(PhoebusApplication.class, "/icons/undo.png");
        assertThat(image, not(nullValue()));

        // Get cached instance for the Image, which can be re-used in multiple ImageViews
        Image image2 = ImageCache.getImage(PhoebusApplication.class, "/icons/undo.png");
        assertThat(image2, sameInstance(image));

        // Different image
        image2 = ImageCache.getImage(PhoebusApplication.class, "/icons/redo.png");
        assertThat(image, not(nullValue()));
        assertThat(image2, not(sameInstance(image)));

        // ImageView is always new, because it cannot be re-used
        final ImageView view = ImageCache.getImageView(PhoebusApplication.class, "/icons/undo.png");
        assertThat(view, not(nullValue()));
        final ImageView view2 = ImageCache.getImageView(PhoebusApplication.class, "/icons/undo.png");
        assertThat(view2, not(sameInstance(view)));
    }
}
