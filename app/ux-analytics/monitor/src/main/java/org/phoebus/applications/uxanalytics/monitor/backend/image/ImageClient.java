package org.phoebus.applications.uxanalytics.monitor.backend.image;

import java.awt.image.BufferedImage;
import java.net.URI;

public interface ImageClient {
    public Integer uploadImage(URI image, BufferedImage screenshot);
    public boolean imageExists(URI image);
    public default void connect(String username, String password){}
    public default void disconnect(){}
}
