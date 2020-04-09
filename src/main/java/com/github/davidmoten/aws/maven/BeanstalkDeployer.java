package com.github.davidmoten.aws.maven;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.S3Location;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest;
import com.amazonaws.services.s3.AmazonS3;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.util.Date;

final class BeanstalkDeployer {

    private final Log log;
    private final AWSElasticBeanstalk beanstalkClient;
    private final AmazonS3 s3Client;

    BeanstalkDeployer(Log log, AWSElasticBeanstalk beanstalkClient, AmazonS3 s3Client) {
        this.log = log;
        this.beanstalkClient = beanstalkClient;
        this.s3Client = s3Client;
    }

    void deploy(File artifact, String applicationName, String environmentName, String versionLabel) {

        String bucketName = getS3BucketName();

        String dateTime = Util.formatDateTime(new Date());

        String objectName = artifact.getName() + "_" + dateTime;

        uploadArtifact(artifact, bucketName, objectName);

        createApplicationVersion(applicationName, beanstalkClient, bucketName, objectName, versionLabel);

        updateEnvironment(applicationName, environmentName, beanstalkClient, versionLabel);
    }

    private String getS3BucketName() {
        log.info("getting s3 bucket name to deploy artifact to");
        String bucketName = beanstalkClient.createStorageLocation().getS3Bucket();
        log.info("s3Bucket=" + bucketName);
        return bucketName;
    }

    private void uploadArtifact(File artifact, String bucketName, String objectName) {
        log.info("uploading " + artifact + " to " + bucketName + ":" + objectName);
        s3Client.putObject(bucketName, objectName, artifact);
    }

    private void createApplicationVersion(String applicationName, AWSElasticBeanstalk eb, String bucketName,
            String objectName, String versionLabel) {
        log.info("creating version label=" + versionLabel);
        CreateApplicationVersionRequest request = new CreateApplicationVersionRequest()
                .withApplicationName(applicationName).withAutoCreateApplication(true)
                .withSourceBundle(new S3Location(bucketName, objectName)).withVersionLabel(versionLabel);
        eb.createApplicationVersion(request);
    }

    private void updateEnvironment(String applicationName, String environmentName, AWSElasticBeanstalk eb,
            String versionLabel) {
        log.info("requesting update of environment to new version label");
        UpdateEnvironmentRequest request = new UpdateEnvironmentRequest().withApplicationName(applicationName)
                .withEnvironmentName(environmentName).withVersionLabel(versionLabel);
        eb.updateEnvironment(request);
        log.info("requested");
    }

}
