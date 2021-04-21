package com.github.davidmoten.aws.maven;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@Mojo(name = "deployS3")
public final class S3DeployerMojo extends AbstractDeployAwsMojo<AmazonS3ClientBuilder, AmazonS3> {

    @Parameter(property = "bucketName")
    private String bucketName;

    @Parameter(property = "inputDirectory")
    private String inputDirectory;

    @Parameter(property = "outputBasePath")
    private String outputBasePath;

    @Parameter(property = "publicRead", defaultValue = "true")
    private boolean publicRead;

    public S3DeployerMojo() {
        super(AmazonS3ClientBuilder.standard());
    }

    @Override
    protected void execute(AmazonS3 s3Client) {
        S3Deployer deployer = new S3Deployer(getLog(), s3Client);
        deployer.deploy(inputDirectory, bucketName, outputBasePath, publicRead);
    }

}
