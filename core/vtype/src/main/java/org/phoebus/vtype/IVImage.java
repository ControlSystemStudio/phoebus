/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import org.phoebus.util.array.ListNumber;

/**
 * Image implementation
 *
 * @author carcassi
 */
class IVImage extends IVMetadata implements VImage {

    private final int height;
    private final int width;
    private final ListNumber data;
    private final VImageDataType imageDataType;
    private final VImageType imageType;

    public IVImage(int height, int width, ListNumber data, 
            VImageDataType imageDataType, VImageType imageType, 
            Alarm alarm, Time time) {
        super(alarm, time);
        this.height = height;
        this.width = width;
        this.data = data;
        this.imageDataType = imageDataType;
        this.imageType = imageType;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public ListNumber getData() {
        return data;
    }

    @Override
    public VImageDataType getDataType() {
        return imageDataType;
    }

    @Override
    public VImageType getVImageType() {
        return imageType;
    }

}
