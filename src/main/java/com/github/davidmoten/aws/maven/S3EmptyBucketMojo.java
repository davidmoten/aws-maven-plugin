package com.github.davidmoten.aws.maven;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import java.util.List;

@Mojo(name = "emptyS3")
public final class S3EmptyBucketMojo extends AbstractDeployAwsMojo<AmazonS3ClientBuilder, AmazonS3> {

    @Parameter(property = "region")
    private String region;

    @Parameter(property = "bucketName")
    private String bucketName;

    @Parameter(property = "excludes")
    private List<String> excludes;

    @Parameter(property = "dryRun", defaultValue = "false")
    private boolean isDryRun;

    public S3EmptyBucketMojo() {
        super(AmazonS3ClientBuilder.standard());
    }

    @Override
    protected void execute(AmazonS3 s3Client) {
        S3EmptyBucket emptyBucket = new S3EmptyBucket(getLog(), s3Client);
        emptyBucket.empty(bucketName, excludes, isDryRun);
    }
}
