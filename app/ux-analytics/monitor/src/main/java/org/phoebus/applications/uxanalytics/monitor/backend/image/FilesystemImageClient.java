package org.phoebus.applications.uxanalytics.monitor.backend.image;

import org.phoebus.framework.preferences.PhoebusPreferenceService;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.util.logging.Logger;

public class FilesystemImageClient implements ImageClient {

    static final Logger logger = Logger.getLogger(FilesystemImageClient.class.getName());

    //Filesystem location to store images
    private String imageLocation = "./images";

    public static FilesystemImageClient instance;
    public static FilesystemImageClient getInstance(){
        if(instance == null){
            instance = new FilesystemImageClient();
        }
        return instance;
    }

    private FilesystemImageClient(){
        imageLocation = PhoebusPreferenceService.userNodeForClass(FilesystemImageClient.class).get("directory", "./images");
    }

    @Override
    public Integer uploadImage(URI image, BufferedImage screenshot) {
        try {
            File outputfile = new File(imageLocation +"/"+ image.getPath()+".png");
            //make directories if they don't exist
            outputfile.getParentFile().mkdirs();
            logger.info("Saving image to: " + outputfile.getAbsolutePath());
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
