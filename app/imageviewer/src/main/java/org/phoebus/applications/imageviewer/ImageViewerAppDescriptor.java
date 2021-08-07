/*
 * Copyright (C) 2021 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.phoebus.applications.imageviewer;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.phoebus.applications.imageviewer.ImageViewerInstance.ViewMode;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockStage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/** Image Viewer App Descriptor
 *  @author Georg Weiss
 */
@SuppressWarnings("nls")
public class ImageViewerAppDescriptor implements AppResourceDescriptor
{
    static final String NAME = "imageviewer";
    static final String DISPLAY_NAME = "Image Viewer";

    static final List<String> FILE_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "gif", "png", "svg"
    );

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getDisplayName()
    {
        return DISPLAY_NAME;
    }

    @Override
    public URL getIconURL()
    {
        return ImageViewerAppDescriptor.class.getResource("/icons/picture.png");
    }

    @Override
    public AppInstance create()
    {
        return new ImageViewerInstance(this, null, null);
    }

    @Override
    public List<String> supportedFileExtentions()
    {
        return FILE_EXTENSIONS;
    }

    @Override
    public ImageViewerInstance create(final URI uri)
    {
        Entry<String, String> entry = ResourceParser.getQueryItemStream(uri)
                .filter(item -> item.getKey().equalsIgnoreCase("viewmode"))
                .findFirst().orElse(null);
        ViewMode viewMode = ViewMode.TAB;
        if(entry != null && entry.getValue() != null && entry.getValue().equalsIgnoreCase("dialog")){
            viewMode = ViewMode.DIALOG;
        }

        if(viewMode == ViewMode.TAB){
            final DockItemWithInput existing = DockStage.getDockItemWithInput(NAME, uri);
            final ImageViewerInstance instance;
            if (existing != null) {
                instance = existing.getApplication();
                instance.raise();
                return null;
            }
            else{
                return new ImageViewerInstance(this, uri, viewMode);
            }
        }
        else{
            return null;
        }
    }
}