package com.github.davidmoten.aws.maven;

import java.io.File;
import java.util.Date;

import org.apache.maven.plugin.logging.Log;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClientBuilder;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.S3Location;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

final class BeanstalkDeployer {

    private final Log log;

    BeanstalkDeployer(Log log) {
        this.log = log;
    }

    void deploy(AWSCredentialsProvider credentials, String region, File artifact, String applicationName, String environmentName,
                String versionLabel, Proxy proxy) {

        ClientConfiguration cc = Util.createConfiguration(proxy);

        AWSElasticBeanstalk eb = AWSElasticBeanstalkClientBuilder.standard().withCredentials(credentials)
                .withClientConfiguration(cc).withRegion(region).build();

        String bucketName = getS3BucketName(eb);

        String dateTime = Util.formatDateTime(new Date());

        String objectName = artifact.getName() + "_" + dateTime;

        uploadArtifact(artifact, credentials, region, cc, bucketName, objectName);

        createApplicationVersion(applicationName, eb, bucketName, objectName, versionLabel);

        updateEnvironment(applicationName, environmentName, eb, versionLabel);
    }

    private String getS3BucketName(AWSElasticBeanstalk eb) {
        log.info("getting s3 bucket name to deploy artifact to");
        String bucketName = eb.createStorageLocation().getS3Bucket();
        log.info("s3Bucket=" + bucketName);
        return bucketName;
    }

    private void uploadArtifact(File artifact, final AWSCredentialsProvider credentials, final String region,
            ClientConfiguration cc, String bucketName, String objectName) {
        AmazonS3 s3 = AmazonS3ClientBuilder.standard().withCredentials(credentials).withClientConfiguration(cc)
                .withRegion(region).build();
        log.info("uploading " + artifact + " to " + bucketName + ":" + objectName);
        s3.putObject(bucketName, objectName, artifact);
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
