package org.phoebus.applications.uxanalytics.monitor;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Base64;

public class MongoDBImageClient implements ImageClient{

    static MongoDBImageClient instance;
    MongoDatabase database = null;

    private MongoDBImageClient(){
    }

    public static MongoDBImageClient getInstance(){
        if(instance == null){
            instance = new MongoDBImageClient();
        }
        return instance;
    }

    public void connect(MongoClient client){
        database = client.getDatabase("phoebus-analytics");
        database.createCollection("images");
    }

    public boolean imageExists(URI image){
        return database.getCollection("images")
                .find(new Document("name", image.toString()))
                .first() != null;
    }

    @Override
    public Integer uploadImage(URI name, BufferedImage screenshot) {
        try {
            //write png-encoded BufferedImage to output stream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(screenshot, "png", baos);
            //encode the blob in base64
            byte[] imageBytes = baos.toByteArray();
            String base64Blob = Base64.getEncoder().encodeToString(imageBytes);
            Document imageDoc = new Document()
                    .append("name", name.toString())
                    .append("type", "image/png")
                    .append("width", screenshot.getWidth())
                    .append("height", screenshot.getHeight())
                    .append("image", base64Blob);
            database.getCollection("images").insertOne(imageDoc);
            return 0;
        }
        catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

}
