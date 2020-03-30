package com.github.davidmoten.aws.maven;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "createS3Bucket")
public final class S3BucketMojo extends AbstractAwsMojo {

	@Parameter(property = "bucketName")
	private String bucketName;

	@Override
	protected void execute(AwsKeyPair keyPair, String region, Proxy proxy) {
		S3Bucket bucket = new S3Bucket(getLog());
		bucket.create(keyPair, region, bucketName, proxy);
	}

}
