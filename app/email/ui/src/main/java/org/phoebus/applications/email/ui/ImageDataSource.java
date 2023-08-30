/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.email.ui;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;
import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

/** {@link DataSource} for JFX {@link Image}
 *
 *  <p>Turns Image into in-memory PNG.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ImageDataSource implements DataSource
{
    private final Image image;

    public ImageDataSource(final Image image)
    {
        this.image = image;
    }

    @Override
    public String getContentType()
    {
        return "image/png";
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        final BufferedImage img = SwingFXUtils.fromFXImage(image, null);
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ImageIO.write(img, "png", buf);
        return new ByteArrayInputStream(buf.toByteArray());
    }

    @Override
    public String getName()
    {
        return "Image";
    }

    @Override
    public OutputStream getOutputStream() throws IOException
    {
        return null;
    }
}
