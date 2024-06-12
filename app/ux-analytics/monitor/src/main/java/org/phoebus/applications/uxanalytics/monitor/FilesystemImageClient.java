package org.phoebus.applications.uxanalytics.monitor;

import java.io.File;
import java.net.URI;

public class FilesystemImageClient implements ImageClient{

    //Filesystem location to store images
    private String imageLocation;

    @Override
    public Integer uploadImage(URI image, File file) {
        return 0;
    }

    @Override
    public boolean imageExists(URI image) {
        return false;
    }

    public boolean setImageLocation(String location) {
        this.imageLocation = location;
        File dir = new File(location);
        try{
            if (!dir.exists()) {
                return dir.mkdirs();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
