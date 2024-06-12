package org.phoebus.applications.uxanalytics.monitor;

import java.io.File;
import java.net.URI;

public interface ImageClient {
    public Integer uploadImage(URI image, File file);
    public boolean imageExists(URI image);
}
