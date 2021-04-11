package com.github.davidmoten.aws.maven;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@Mojo(name = "createS3Bucket")
public final class S3BucketMojo extends AbstractDeployAwsMojo<AmazonS3ClientBuilder, AmazonS3> {

	@Parameter(property = "bucketName", required = true)
	private String bucketName;

	public S3BucketMojo() {
		super(AmazonS3ClientBuilder.standard());
	}

	@Override
	protected void execute(AmazonS3 s3Client) {
		S3Bucket bucket = new S3Bucket(getLog(), s3Client);
		bucket.create(bucketName);
	}

}
