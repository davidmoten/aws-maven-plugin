package com.github.davidmoten.aws.maven;

import org.apache.maven.plugin.logging.Log;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

final class S3Bucket {

	private final Log log;

	S3Bucket(Log log) {
		this.log = log;
	}

	void create(AWSCredentialsProvider credentials, String region, final String bucketName, Proxy proxy) {
		ClientConfiguration cc = Util.createConfiguration(proxy);

		final AmazonS3 s3 = AmazonS3ClientBuilder.standard() //
				.withCredentials(credentials) //
				.withClientConfiguration(cc) //
				.withRegion(region) //
				.build();

		if (!s3.doesBucketExistV2(bucketName)) {
			log.info("bucket does not exist so creating");
			s3.createBucket(bucketName);
			log.info("created bucket " + bucketName);
		} else {
			log.info("bucket " + bucketName + " already exists, skipping create");
		}
	}
}
