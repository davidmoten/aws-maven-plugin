package com.github.davidmoten.aws.maven;

import com.amazonaws.auth.AWSCredentialsProvider;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "deployS3")
public final class S3DeployerMojo extends AbstractAwsMojo {

    @Parameter(property = "region")
    private String region;

    @Parameter(property = "bucketName")
    private String bucketName;

    @Parameter(property = "inputDirectory")
    private String inputDirectory;

    @Parameter(property = "outputBasePath")
    private String outputBasePath;

    @Override
    protected void execute(AWSCredentialsProvider credentials, String region, Proxy proxy) {
        S3Deployer deployer = new S3Deployer(getLog());
        deployer.deploy(credentials, region,  inputDirectory, bucketName, outputBasePath, proxy);
    }

}
