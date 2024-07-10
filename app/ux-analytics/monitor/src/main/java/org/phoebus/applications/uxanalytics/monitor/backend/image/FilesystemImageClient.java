package org.phoebus.applications.uxanalytics.monitor.backend.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;

public class FilesystemImageClient implements ImageClient {

    //Filesystem location to store images
    private String imageLocation;

    @Override
    public Integer uploadImage(URI image, BufferedImage screenshot) {
        try {
            File outputfile = new File(imageLocation +"/"+ image.getPath()+".png");
            //make directories if they don't exist
            outputfile.getParentFile().mkdirs();
            System.out.println("Saving image to: " + outputfile.getAbsolutePath());
            javax.imageio.ImageIO.write(screenshot, "png", outputfile);
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public boolean imageExists(URI image) {
        return new File(imageLocation +"/"+ image.getPath()+".png").exists();
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
