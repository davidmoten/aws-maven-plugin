package com.github.davidmoten.aws.maven;

import java.io.File;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.s3.AmazonS3Client;

public final class Deployer {

	private static final String DATETIME_PATTERN = "yyyyMMddHHmmss";

	public void deploy(File artifact, String accessKey, String secretKey, String region, String applicationName) {
		
		final AWSCredentialsProvider credentials = new StaticCredentialsProvider(
	            new BasicAWSCredentials(accessKey, secretKey));
		
		AmazonS3Client s3 = new AmazonS3Client(credentials, new ClientConfiguration());
		Region r = Region.getRegion(Regions.fromName(region));
        s3.setRegion(r);
        String bucketName = "com.github.davidmoten.aws.maven";
		if (!s3.doesBucketExist(bucketName)) {
			s3.createBucket(bucketName);
		} 
		String dateTime = Instant.now().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern(DATETIME_PATTERN));
		String objectName = "artifact." + dateTime;
		s3.putObject(bucketName,objectName,artifact);

		AWSElasticBeanstalkClient eb = new AWSElasticBeanstalkClient();
		//list buckets owned by user
		//if one bucket starts with com.github.davidmoten.aws.maven then use that
		//else create a new one ending with 12 chars from UUID
		//S3 Object name will be artifact filename plus date time (yyyMMddhhmmss)
		//upload artifact to S3 object in bucket
		//call update-application-version with S3 object address
	}

}
