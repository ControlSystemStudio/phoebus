/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.javafx;

import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;

/** Tab that allows the viewing and selection of images and screen shots from the file system, application, or system clip board.
 *  @author Evan Smith
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ImagesTab extends Tab
{
    private ImageList images = new ImageList();

    /** @param root_node Node that will be used to obtain a screenshot */
    public ImagesTab()
    {
        setText("Images");
        setClosable(false);
        setTooltip(new Tooltip("Add images."));

        setContent(images);
    }

    /** @param node Node to use when taking a screenshot */
    public void setSnapshotNode(final Node node)
    {
        images.setSnapshotNode(node);
    }

    /** @param images Images to show */
    public void setImages(final List<Image> images)
    {
        this.images.setImages(images);
    }

    /** @return Images shown in the tab */
    public List<Image> getImages()
    {
        return images.getImages();
    }
}
