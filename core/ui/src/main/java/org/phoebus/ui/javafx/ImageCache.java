/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.javafx;

import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.phoebus.ui.application.PhoebusApplication;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Cache for images
 *
 *  <p>Loads images from class resources.
 *  Caches them by name, so an image "/icons/my_image.png"
 *  loaded by class A would later also be returned
 *  when queried by class B, since only the name is used.
 *
 *  <p>Supports alternate resolution icons:
 *  Given "icon-name.png", it will
 *  actually load "icon-name@2x.png" or "icon-name@3x.png",
 *  if available,
 *  on high resolution displays.
 *
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ImageCache
{
    private static final ConcurrentHashMap<String, Image> cache = new ConcurrentHashMap<>();

    /** @param clazz Class from which to load, if not already cached
     *  @param path Path to the image, based on clazz
     *  @return ImageView for image, always a new ImageView, even for cached Image
     */
    public static ImageView getImageView(final Class<?> clazz, final String path)
    {
        final Image image = getImage(clazz, path);
        if (image != null)
            return new ImageView(image);
        return new ImageView();
    }

    /** @param clazz Class from which to load, if not already cached
     *  @param path Path to the image, based on clazz
     *  @return Image, may be cached copy
     */
    public static Image getImage(final Class<?> clazz, final String path)
    {
        return cache.computeIfAbsent(path, p ->
        {
            final URL resource = clazz.getResource(path);
            if (resource == null)
            {
                PhoebusApplication.logger.log(Level.WARNING, "Cannot load image '" + path + "' for " + clazz.getName());
                return null;
            }
            try
            {
                return new Image(resource.toExternalForm());
            }
            catch (Throwable ex)
            {
                PhoebusApplication.logger.log(Level.WARNING, "No image support to load image '" + path + "' for " + clazz.getName(), ex);
                return null;
            }
        });
    }

    /** @param url Image URL
     *  @return ImageView for image, always a new ImageView, even for cached Image
     */
    public static ImageView getImageView(final URL url)
    {
        final Image image = getImage(url);
        if (image != null)
            return new ImageView(image);
        return new ImageView();
    }

    /** @param url Image URL
     *  @return Image, may be cached copy
     */
    public static Image getImage(final URL url)
    {
        if (url == null)
            return null;
        final String path = url.toExternalForm();
        return cache.computeIfAbsent(path, p -> new Image(path));
    }
}
