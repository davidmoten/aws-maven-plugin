package com.github.davidmoten.aws.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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
    protected void execute(AwsKeyPair keyPair, String region, Proxy proxy) throws MojoExecutionException, MojoFailureException {
        S3Deployer deployer = new S3Deployer(getLog());
        deployer.deploy(keyPair, region,  inputDirectory, bucketName, outputBasePath, proxy);
    }

}
