package org.phoebus.applications.uxanalytics.monitor;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;

public interface ImageClient {
    public Integer uploadImage(URI image, BufferedImage screenshot);
    public boolean imageExists(URI image);
}
