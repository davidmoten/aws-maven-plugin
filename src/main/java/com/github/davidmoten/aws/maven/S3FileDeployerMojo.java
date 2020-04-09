package com.github.davidmoten.aws.maven;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

@Mojo(name = "deployFileS3")
public final class S3FileDeployerMojo extends AbstractDeployAwsMojo<AmazonS3ClientBuilder, AmazonS3> {

    /**
     * Name of the bucket to which the file will be deployed.
     */
    @Parameter(property = "bucketName", required = true)
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

    public S3FileDeployerMojo() {
        super(AmazonS3ClientBuilder.standard());
    }

    @Override
    protected void execute(AmazonS3 s3Client) {
        S3FileDeployer deployer = new S3FileDeployer(getLog(), s3Client);
        if (objectName == null) {
            objectName = file.getName();
        }
        deployer.deploy(file, bucketName, objectName, create, awsKmsKeyId);
    }

}
