package com.github.davidmoten.aws.maven;

import java.io.File;

import com.amazonaws.services.s3.model.SSEAwsKeyManagementParams;
import org.apache.maven.plugin.logging.Log;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;

final class S3FileDeployer {

	private final Log log;
	private final AmazonS3 s3Client;

	S3FileDeployer(Log log, AmazonS3 s3Client) {
		this.log = log;
		this.s3Client = s3Client;
	}

	void deploy(File file, final String bucketName, final String objectName, boolean create, String awsKmsKeyId) {

		if (file == null) {
			throw new RuntimeException("must specify inputDirectory parameter in configuration");
		}

		if (create) {
			if (!s3Client.doesBucketExistV2(bucketName)) {
				log.info("bucket does not exist so creating");
				s3Client.createBucket(bucketName);
				log.info("created bucket " + bucketName);
			}
		}

        final PutObjectRequest req;
        if (awsKmsKeyId != null) {
            req = new PutObjectRequest(bucketName, objectName, file)
                    .withSSEAwsKeyManagementParams(new SSEAwsKeyManagementParams(awsKmsKeyId));
        } else {
            req = new PutObjectRequest(bucketName, objectName, file);
        }

		log.info("uploading object to s3:" + bucketName + ":" + objectName + ", " + file.length() + " bytes");
		s3Client.putObject(req);
		log.info("deployed " + file.getName() + " to s3 " + bucketName + ":" + objectName);

	}

}
