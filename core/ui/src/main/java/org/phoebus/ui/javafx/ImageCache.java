/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.javafx;

import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
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
 *  <p>Note that if phoebus modules are (eventually) built
 *  as actual Java modules, an image lookup will indeed be
 *  based on the provided class name, locating only resources
 *  available to that class.
 *  On the other hand, when phoebus 'modules' are simply
 *  jar files, and all jar files are placed on the classpath,
 *  the content of all `/icons/*` resources appear as just
 *  one large `/icons` resource folder.
 *
 *  <p>To prevent conflicts, icon names should thus be unique.
 *  If the exact same icon is used by several 'modules',
 *  it should be placed in a common module,
 *  or the same icon needs to be copied into each module
 *  to assert that it is available even if phoebus is
 *  eventually strictly modularized.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ImageCache
{
    private static final ConcurrentHashMap<String, SoftReference<Image>> cache = new ConcurrentHashMap<>();

    /** Cache an image
     *
     *  @param key Key for the image
     *  @param provider Will be called to create image if it's not in the cache
     *  @return Image or <code>null</code>
     */
    public static Image cache(final String key, final Supplier<Image> provider)
    {
        // Atomically clean expired entry for the key
        SoftReference<Image> ref = cache.computeIfPresent(key, (k, r) ->
        {
            return (r.get() == null) ? null : r;
        });

        // Have existing image?
        Image img = ref == null ? null : ref.get();
        if (img != null)
            return img;

        // Add new image
        // Not atomic; small chance of multiple threads
        // concurrently adding an image for the same key.
        // Pity, but map is concurrent, i.e. no crash,
        // and better than risking blocking/deadlocks.
        img = provider.get();
        if (img != null)
            cache.put(key, new SoftReference<>(img));
        return img;
    }

    /** Remove image from cache
     *
     *  @param key Key for the image
     *  @return Image or <code>null</code> if not in cache
     */
    public static Image remove(final String key)
    {
        final SoftReference<Image> ref = cache.remove(key);
        return ref == null ? null : ref.get();
    }

    /** @param clazz Class from which to load, if not already cached
     *  @param path Path to the image, based on clazz
     *  @return Image, may be cached copy
     */
    public static Image getImage(final Class<?> clazz, final String path)
    {
        return cache(path, () ->
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
        return cache(path, () -> new Image(path));
    }
}
