package org.phoebus.applications.uxanalytics.monitor.backend.image;


import java.awt.image.BufferedImage;
import java.io.*;
import java.util.logging.Level;

import org.phoebus.framework.preferences.PhoebusPreferenceService;
import org.phoebus.security.tokens.AuthenticationScope;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.imageio.ImageIO;
import java.net.URI;
import java.util.logging.Logger;

public class S3ImageClient implements ImageClient{

    S3Client s3;
    static String bucketName = PhoebusPreferenceService.userNodeForClass(S3ImageClient.class).get("bucket", "phoebus-screenshots");
    static S3ImageClient instance;
    public static S3ImageClient getInstance(){
        if(instance == null){
            instance = new S3ImageClient();
        }
        return instance;
    }
    Logger logger = Logger.getLogger(S3ImageClient.class.getName());

    private S3ImageClient(){
    }

    public void connect(String accessKey, String secretKey) {
        s3 = S3Client.builder()
                .region(Region.of(PhoebusPreferenceService.userNodeForClass(this.getClass()).get("region", "us-east-2")))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
        boolean bucketFound = false;
        for (var bucket : s3.listBuckets().buckets()) {
            if (bucket.name().equals(bucketName)) {
                bucketFound = true;
                break;
            }
        }
        if (!bucketFound) {
            logger.log(Level.WARNING, "Bucket " + bucketName + " not found, creating it");
            s3.createBucket(builder -> builder.bucket(bucketName).build());
            return;
        }
        else{
            logger.log(Level.INFO, "Bucket " + bucketName + " found");
        }
    }

    @Override
    public boolean imageExists(URI key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key.toString())
                    .build();
            s3.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    @Override
    public Integer uploadImage(URI imagePath, BufferedImage screenshot) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(screenshot, "png", os);
            byte[] buffer = os.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(imagePath.toString())
                    .contentType("image/png")
                    .build();
            PutObjectResponse resp = s3.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, buffer.length));
            logger.log(Level.INFO, "Uploaded image to S3: " + imagePath.toString());
            return resp.sdkHttpResponse().statusCode();
        }
        catch (IOException e) {
            logger.log(Level.WARNING, "Failed to upload image to S3", e);
            return 500;
        }

    }

    public void disconnect() {
        s3.close();
    }
}
