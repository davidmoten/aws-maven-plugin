package com.github.davidmoten.aws.maven;

import org.apache.maven.plugin.logging.Log;

import com.amazonaws.services.s3.AmazonS3;

final class S3Bucket {

	private final Log log;
	private final AmazonS3 s3Client;

	S3Bucket(Log log, AmazonS3 s3Client) {
		this.log = log;
		this.s3Client = s3Client;
	}

	void create(String bucketName) {
		if (!s3Client.doesBucketExistV2(bucketName)) {
			log.info("bucket does not exist so creating");
			s3Client.createBucket(bucketName);
			log.info("created bucket " + bucketName);
		} else {
			log.info("bucket " + bucketName + " already exists, skipping create");
		}
	}
}
