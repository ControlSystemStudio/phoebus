package org.phoebus.applications.uxanalytics.monitor;


import java.io.*;
import java.util.logging.Level;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;
import java.util.logging.Logger;

public class S3ImageClient implements ImageClient{

    S3Client s3;
    static final String BUCKET_NAME = "phoebus-screenshots";
    static S3ImageClient instance;
    Logger logger = Logger.getLogger(S3ImageClient.class.getName());

    private S3ImageClient(){
        String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        s3 = S3Client.builder()
                .region(Region.US_EAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
        boolean bucketFound = false;
        for (var bucket : s3.listBuckets().buckets()) {
            if (bucket.name().equals(BUCKET_NAME)) {
                bucketFound = true;
                break;
            }
        }
        if (!bucketFound) {
            logger.log(Level.WARNING, "Bucket " + BUCKET_NAME + " not found, creating it");
            s3.createBucket(builder -> builder.bucket(BUCKET_NAME).build());
            return;
        }
        else{
            logger.log(Level.INFO, "Bucket " + BUCKET_NAME + " found");
        }
    }

    @Override
    public boolean imageExists(URI key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(key.toString())
                    .build();
            s3.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    @Override
    public Integer uploadImage(URI imagePath, File file) {
        if(!imageExists(imagePath)) {
            PutObjectResponse response = s3.putObject(builder -> builder.bucket(BUCKET_NAME)
                            .key(imagePath.toString())
                            .build(),
                    file.toPath());
            return response.sdkHttpResponse().statusCode();
        }
        else{
            return 409;
        }
    }

    public static ImageClient getInstance(){
        if(instance == null){
            instance = new S3ImageClient();
        }
        return instance;
    }
}
