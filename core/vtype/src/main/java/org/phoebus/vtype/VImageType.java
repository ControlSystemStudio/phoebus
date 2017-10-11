/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */

package org.phoebus.vtype;

public enum VImageType {
    /**
     * Image type constants
     */
    TYPE_CUSTOM,
    /**
     * Monochromatic image
     */
    TYPE_MONO,
    /**
     * Bayer pattern image, 1 value per pixel but with color filter on
     * detector
     */
    TYPE_BAYER,
    /**
     * RGB image with pixel color interleave, data array is [3, NX, NY]
     */
    TYPE_RGB1,
    /**
     * RGB image with row color interleave, data array is [NX, 3, NY]
     */
    TYPE_RGB2,
    /**
     * RGB image with plane color interleave, data array is [NX, NY, 3]
     */
    TYPE_RGB3,
    /**
     * YUV image, 3 bytes encodes 1 RGB pixel
     */
    TYPE_YUV444,
    /**
     * YUV image, 4 bytes encodes 2 RGB pixel
     */
    TYPE_YUV422,
    /**
     * YUV image, 6 bytes encodes 4 RGB pixels
     */
    TYPE_YUV411,
    /**
     * An image with 8-bit RGB color components, corresponding to a
     * Windows-style BGR color model with the colors Blue, Green, and Red
     * stored in 3 bytes.
     */
    TYPE_3BYTE_BGR,
    /**
     * Represents an image with 8-bit RGBA color components with the colors
     * Blue, Green, and Red stored in 3 bytes and 1 byte of alpha.
     */
    TYPE_4BYTE_ABGR,
    /**
     * Represents an image with 8-bit RGBA color components with the colors
     * Blue, Green, and Red stored in 3 bytes and 1 byte of alpha.
     */
    TYPE_4BYTE_ABGR_PRE,
    /**
     * Represents an opaque byte-packed 1, 2, or 4 bit image.
     */
    TYPE_BYTE_BINARY,
    /**
     * Represents a unsigned byte grayscale image, non-indexed.
     */
    TYPE_BYTE_GRAY,
    /**
     * Represents an indexed byte image.
     */
    TYPE_BYTE_INDEXED,
    /**
     * Represents an image with 8-bit RGBA color components packed into
     * integer pixels.
     * 
     */
    TYPE_INT_ARGB,
    /**
     * Represents an image with 8-bit RGBA color components packed into
     * integer pixels.
     * 
     */
    TYPE_INT_ARGB_PRE,
    /**
     * Represents an image with 8-bit RGB color components, corresponding to
     * a Windows- or Solaris- style BGR color model, with the colors Blue,
     * Green, and Red packed into integer pixels.
     * 
     */
    TYPE_INT_BGR,
    /**
     * Represents an image with 8-bit RGB color components packed into
     * integer pixels.
     * 
     */
    TYPE_INT_RGB,
    /**
     * Represents an image with 5-5-5 RGB color components (5-bits red,
     * 5-bits green, 5-bits blue) with no alpha.
     * 
     */
    TYPE_USHORT_555_RGB,
    /**
     * Represents an image with 5-6-5 RGB color components (5-bits red,
     * 6-bits green, 5-bits blue) with no alpha.
     * 
     */
    TYPE_USHORT_565_RGB,
    /**
     * Represents an unsigned short grayscale image, non-indexed).
     * 
     */
    TYPE_USHORT_GRAY
}