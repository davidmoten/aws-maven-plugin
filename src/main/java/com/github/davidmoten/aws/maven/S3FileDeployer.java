package com.github.davidmoten.aws.maven;

import java.io.File;

import org.apache.maven.plugin.logging.Log;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;

final class S3FileDeployer {

    private final Log log;

    S3FileDeployer(Log log) {
        this.log = log;
    }

    public void deploy(AwsKeyPair keyPair, String region, File file, final String bucketName, final String objectName,
            Proxy proxy, boolean create) {

        if (file == null) {
            throw new RuntimeException("must specify inputDirectory parameter in configuration");
        }

        final AWSCredentialsProvider credentials = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(keyPair.key, keyPair.secret));

        ClientConfiguration cc = Util.createConfiguration(proxy);

        final AmazonS3 s3 = AmazonS3ClientBuilder.standard() //
                .withCredentials(credentials) //
                .withClientConfiguration(cc) //
                .withRegion(region) //
                .build();

        if (create) {
            if (!s3.doesBucketExist(bucketName)) {
                log.info("bucket does not exist so creating");
                s3.createBucket(bucketName);
                log.info("created bucket "+ bucketName);
            }
        }
        
        PutObjectRequest req = new PutObjectRequest(bucketName, objectName, file);

        s3.putObject(req);

        log.info("deployed " + file.getName() + " to s3 " + bucketName + ":" + objectName);

    }

}
