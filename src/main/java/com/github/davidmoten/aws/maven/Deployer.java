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
import com.amazonaws.services.elasticbeanstalk.model.UpdateApplicationVersionRequest;
import com.amazonaws.services.s3.AmazonS3Client;

public final class Deployer {

	private static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

	public void deploy(File artifact, String accessKey, String secretKey, String region, String applicationName) {

		final AWSCredentialsProvider credentials = new StaticCredentialsProvider(
				new BasicAWSCredentials(accessKey, secretKey));

		Region r = Region.getRegion(Regions.fromName(region));
		
		AWSElasticBeanstalkClient eb = new AWSElasticBeanstalkClient(credentials).withRegion(r);
		String bucketName = eb.createStorageLocation().getS3Bucket();
		String dateTime = Instant.now().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern(DATETIME_PATTERN));
		
		AmazonS3Client s3 = new AmazonS3Client(credentials, new ClientConfiguration()).withRegion(r);
		String objectName = "artifact." + dateTime;
		s3.putObject(bucketName, objectName, artifact);

		UpdateApplicationVersionRequest request = new UpdateApplicationVersionRequest()
				.withApplicationName(applicationName).withVersionLabel(bucketName);
		eb.updateApplicationVersion(request);
	}

	public static void main(String[] args) {
		System.out.println(Instant.now().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern(DATETIME_PATTERN)));
	}

}
