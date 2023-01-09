/*
 * Copyright (C) 2020 European Spallation Source ERIC.
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
 *
 */

package org.phoebus.applications.saveandrestore.ui;

import javafx.scene.image.Image;
import org.phoebus.applications.saveandrestore.ui.search.SearchWindowController;
import org.phoebus.ui.javafx.ImageCache;

/**
 * This class provides static access to image resources used across multiple views and context menus
 * in the save-and-restore app. Purpose is to make management easier as the actual image resources are
 * references only one, i.e. in this class.
 */
public class ImageRepository {

    public static final Image FOLDER =
            ImageCache.getImage(ImageRepository.class, "/icons/save-and-restore/folder.png");
    public static final Image CONFIGURATION =
            ImageCache.getImage(ImageRepository.class, "/icons/save-and-restore/configuration.png");
    public static final Image SNAPSHOT =
            ImageCache.getImage(ImageRepository.class, "/icons/save-and-restore/snapshot.png");
    public static final Image GOLDEN_SNAPSHOT =
            ImageCache.getImage(ImageRepository.class, "/icons/save-and-restore/snapshot-golden.png");
    public static final Image COMPOSITE_SNAPSHOT =
            ImageCache.getImage(SearchWindowController.class, "/icons/save-and-restore/composite-snapshot.png");
    public static final Image EDIT_CONFIGURATION =
            ImageCache.getImage(SearchWindowController.class, "/icons/save-and-restore/edit-configuration.png");
}
