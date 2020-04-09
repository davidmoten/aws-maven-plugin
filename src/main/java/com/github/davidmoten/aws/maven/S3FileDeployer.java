package com.github.davidmoten.aws.maven;

import java.io.File;

import com.amazonaws.services.s3.model.SSEAwsKeyManagementParams;
import org.apache.maven.plugin.logging.Log;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;

final class S3FileDeployer {

	private final Log log;

	S3FileDeployer(Log log) {
		this.log = log;
	}

	public void deploy(AWSCredentialsProvider credentials, String region, File file, final String bucketName,
					   final String objectName, Proxy proxy, boolean create, String awsKmsKeyId) {

		if (file == null) {
			throw new RuntimeException("must specify inputDirectory parameter in configuration");
		}

		ClientConfiguration cc = Util.createConfiguration(proxy);

		final AmazonS3 s3 = AmazonS3ClientBuilder.standard() //
				.withCredentials(credentials) //
				.withClientConfiguration(cc) //
				.withRegion(region) //
				.build();

		if (create) {
			if (!s3.doesBucketExistV2(bucketName)) {
				log.info("bucket does not exist so creating");
				s3.createBucket(bucketName);
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
		s3.putObject(req);
		log.info("deployed " + file.getName() + " to s3 " + bucketName + ":" + objectName);

	}

}
