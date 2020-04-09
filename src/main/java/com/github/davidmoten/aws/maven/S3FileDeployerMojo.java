package com.github.davidmoten.aws.maven;

import com.amazonaws.auth.AWSCredentialsProvider;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

@Mojo(name = "deployFileS3")
public final class S3FileDeployerMojo extends AbstractAwsMojo {

    /**
     * Name of the bucket to which the file will be deployed.
     */
    @Parameter(property = "bucketName")
    private String bucketName;

    /**
     * The file that will be deployed to S3.
     */
    @Parameter(property = "file", required = true)
    private File file;

    /**
     * The key name of the object in the bucket. The file name will be used by default if the parameter is not
     * configured.
     */
    @Parameter(property = "objectName")
    private String objectName;

    /**
     * Creates a bucket with the given name if it doesn't exist.
     */
    @Parameter(property = "create", defaultValue = "false")
    private boolean create;

    /**
     * AWS Key Management System parameters used to encrypt the objects on server side.
     */
    @Parameter(property = "awsKmsKeyId")
    private String awsKmsKeyId;

    @Override
    protected void execute(AWSCredentialsProvider credentials, String region, Proxy proxy) {
        S3FileDeployer deployer = new S3FileDeployer(getLog());
        if (objectName == null) {
            objectName = file.getName();
        }
        deployer.deploy(credentials, region, file, bucketName, objectName, proxy, create, awsKmsKeyId);
    }

}
